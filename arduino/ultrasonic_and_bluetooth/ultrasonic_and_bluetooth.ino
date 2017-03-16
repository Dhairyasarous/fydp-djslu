#include <SPI.h>
#include <SoftwareSerial.h>
#include "Adafruit_BLE_UART.h"

static const long MILLI_TO_SEC = 1000;

/* SENSOR */
#define trigPinSensorLeft 3
#define echoPinSensorLeft 4
#define trigPinSensorRight 7
#define echoPinSensorRight 8
#define trigPinSensorCenter 5
#define echoPinSensorCenter 6

static const long MAX_RANGE = 500; // cm
static const long SENSOR_INTERVAL = 100; // milliseconds
static unsigned long previous_millis = 0;

/* NRF8001 */
#define ADAFRUITBLE_REQ 10
#define ADAFRUITBLE_RDY 2     // This should be an interrupt pin, on Uno thats #2 or #3
#define ADAFRUITBLE_RST 9
Adafruit_BLE_UART BTLEserial = Adafruit_BLE_UART(ADAFRUITBLE_REQ, ADAFRUITBLE_RDY, ADAFRUITBLE_RST);
aci_evt_opcode_t laststatus = ACI_EVT_DISCONNECTED;

/* BSD */
typedef struct {
  String id;
  double MIN_VELOCITY_THRESHOLD = -40;
  double ALPHA = 0.8;
  long samples_recorded = 0;
  int trigPin; // defined in setup
  int echoPin; // defined in setup
  double raw_distance = MAX_RANGE-1; // cm
  double raw_distance_prev = MAX_RANGE-1; // cm
  double filtered_distance = 0; // cm 
  double filtered_distance_prev = 0; // cm
  double velocity = 0; // cm/s
  int warn_user = 0; // user is warned when this is set to 1
} Sensor;

static const long MIN_SAMPLES_REQUIRED = 10; // samples
static const double MIN_DISTANCE_THRESHOLD = 150; // cm
static const double MIN_DISTANCE_THRESHOLD_MIN = 100; // cm
static const double MIN_DISTANCE_DELTA_THRESHOLD = 100; // cm
static const double MIN_DISTANCE_UPDATE_STEP_SIZE = MIN_DISTANCE_DELTA_THRESHOLD/20; // cm
static double MIN_VELOCITY_THRESHOLD = -40;
static double ALPHA = 0.8;

static Sensor sensor_left;
static Sensor sensor_right;
static Sensor sensor_center;
static double global_distance;
static double global_warn_user, previous_global_warn_user = 0;

void setupSensor(Sensor *sensor) {
  pinMode(sensor->trigPin, OUTPUT);
  pinMode(sensor->echoPin, INPUT);
}

void setup() {
  Serial.begin (9600);
  /* SENSOR */
  sensor_left.id = "left";
  sensor_left.trigPin = (int)trigPinSensorLeft;
  sensor_left.echoPin = (int)echoPinSensorLeft;
  sensor_right.id = "right";
  sensor_right.trigPin = (int)trigPinSensorRight;
  sensor_right.echoPin = (int)echoPinSensorRight;
  sensor_center.id = "center";
  sensor_center.trigPin = (int)trigPinSensorCenter;
  sensor_center.echoPin = (int)echoPinSensorCenter;
  setupSensor(&sensor_left);
  setupSensor(&sensor_right);
  setupSensor(&sensor_center);

  /* NRF8001 */
  while(!Serial); // Leonardo/Micro should wait for serial init
  Serial.println(F("Adafruit Bluefruit Low Energy nRF8001 Print echo demo"));
  BTLEserial.setDeviceName("BSD"); /* 7 characters max! */
  BTLEserial.begin();
}

void print_debug(Sensor *sensor) {
    Serial.print(sensor->id);
    Serial.print(", ");
  
    Serial.print("global warn user = ");
    Serial.print(global_warn_user);
    Serial.print(", ");
  
    Serial.print("global distance = ");
    Serial.print(global_distance);
    Serial.print("cm, ");
    
    Serial.print("distance = ");
    Serial.print(sensor->raw_distance);
    Serial.print(" cm, ");

    Serial.print("filtered_distance = ");
    Serial.print(sensor->filtered_distance);
    Serial.print(" cm, ");

    Serial.print("velocity = ");
    Serial.print(sensor->velocity);
    Serial.println(" cm/s");
}

void get_distance (Sensor *sensor) {
  long duration;
  // send pulse for 10 ms and then check echo
  digitalWrite(sensor->trigPin, LOW);  // Added this line
  delayMicroseconds(2); // Added this line
  digitalWrite(sensor->trigPin, HIGH);
  delayMicroseconds(10); // Added this line
  digitalWrite(sensor->trigPin, LOW);
  
  // calculate distance based on the duration of the pulse
  duration = pulseIn(sensor->echoPin, HIGH);
  sensor->raw_distance = (double)(duration/2) / (double)29.1;
  
  // apply max and min cutoffs
  if (sensor->raw_distance >= MAX_RANGE){
    sensor->raw_distance = sensor->raw_distance_prev; 
  }
  else if (sensor->raw_distance <= 0) {
    sensor->raw_distance = sensor->raw_distance_prev; 
  } 
  else if (global_distance < MIN_DISTANCE_THRESHOLD_MIN && abs(sensor->raw_distance - sensor->raw_distance_prev) > MIN_DISTANCE_DELTA_THRESHOLD) {
    if (sensor->raw_distance < sensor->raw_distance_prev) {
      sensor->raw_distance = sensor->raw_distance_prev - MIN_DISTANCE_UPDATE_STEP_SIZE;  
    } else {
      sensor->raw_distance = sensor->raw_distance_prev + MIN_DISTANCE_UPDATE_STEP_SIZE; 
    }
  }
  sensor->raw_distance_prev = sensor->raw_distance; // replace the junk value with last appropriate value
}

void apply_hack(Sensor *sensor) {
  if (sensor->raw_distance < MIN_DISTANCE_THRESHOLD_MIN) {
    sensor->ALPHA = 0.1;
  } 
  else if (sensor->raw_distance < MIN_DISTANCE_THRESHOLD) {
    sensor->ALPHA = 0.3;
  }
  else {
    sensor->ALPHA = 0.8;
  }
}

void apply_hack2(double distance) {
  if (distance < MIN_DISTANCE_THRESHOLD) {
    MIN_VELOCITY_THRESHOLD = -100;
  } else {
    MIN_VELOCITY_THRESHOLD = -40;
  }
}

void apply_filtering_and_calc_velocity (Sensor *sensor) {
  if (sensor->samples_recorded <= MIN_SAMPLES_REQUIRED) {
    sensor->filtered_distance = sensor->raw_distance;
    sensor->samples_recorded++;
  } else {
    apply_hack(sensor);
    sensor->filtered_distance_prev = sensor->filtered_distance;
    sensor->filtered_distance = sensor->filtered_distance_prev + sensor->ALPHA * (sensor->raw_distance - sensor->filtered_distance_prev);
    sensor->velocity = (sensor->filtered_distance - sensor->filtered_distance_prev) / (double)((double)SENSOR_INTERVAL/(double)MILLI_TO_SEC);
  }
}

void compute_bsd(Sensor *sensor) {
  sensor->warn_user = 0;
  if (sensor->samples_recorded > MIN_SAMPLES_REQUIRED) {
    if (sensor->filtered_distance < MIN_DISTANCE_THRESHOLD && sensor->velocity < MIN_VELOCITY_THRESHOLD) {
      sensor->warn_user = 1;
    }
  }
}

void loop_sensor() {
  unsigned long current_millis = millis();
  if (current_millis - previous_millis >= SENSOR_INTERVAL) {
    previous_millis = current_millis;
    
    // compute bsd for left sensor
    get_distance(&sensor_left);
    apply_filtering_and_calc_velocity(&sensor_left);
    
    // compute bsd for right sensor
    get_distance(&sensor_right);
    apply_filtering_and_calc_velocity(&sensor_right);
    
    // compute bsd for right sensor
    get_distance(&sensor_center);
    apply_filtering_and_calc_velocity(&sensor_center);
    
    // set the global distance based on the information from all of the sensors, and decide whether to warn the user
    global_distance = min(sensor_right.filtered_distance, min(sensor_left.filtered_distance, sensor_center.filtered_distance));
    apply_hack2(global_distance);
    compute_bsd(&sensor_right);
    compute_bsd(&sensor_center);
    compute_bsd(&sensor_left);
    
    global_warn_user = sensor_left.warn_user || sensor_right.warn_user || sensor_center.warn_user;

//    print_debug(&sensor_left);
    print_debug(&sensor_center);
//    print_debug(&sensor_right);
  }
}

int is_bsd_state_changed() {
  if (global_warn_user != previous_global_warn_user) {
    previous_global_warn_user = global_warn_user;
    return 1;
  }
  return 0;
}

void loop_bluetooth() {
  /* NRF8001 */
  // Tell the nRF8001 to do whatever it should be working on.
  BTLEserial.pollACI();
  
  // Ask what is our current status
  aci_evt_opcode_t status = BTLEserial.getState();
  
  // If the status changed....
  if (status != laststatus) {
    // print it out!
    if (status == ACI_EVT_DEVICE_STARTED) {
        Serial.println(F("* Advertising started"));
    }
    if (status == ACI_EVT_CONNECTED) {
        Serial.println(F("* Connected!"));
    }
    if (status == ACI_EVT_DISCONNECTED) {
        Serial.println(F("* Disconnected or advertising timed out"));
    }
    // OK set the last status change to this one
    laststatus = status;
  }

  if (status == ACI_EVT_CONNECTED) {
    // Lets see if there's any data for us!
    if (BTLEserial.available()) {
      Serial.print("* "); Serial.print(BTLEserial.available()); Serial.println(F(" bytes available from BTLE"));
    }
    // OK while we still have something to read, get a character and print it out
    while (BTLEserial.available()) {
      char c = BTLEserial.read();
      Serial.print(c);
    }

    if (is_bsd_state_changed()) {
      // Send the distance using bluetooth
      String global_warn_string = String(global_warn_user);
      String global_distance_string = String(global_distance);
      String sensor_left_distance = String(sensor_left.filtered_distance);
      String sensor_right_distance = String(sensor_right.filtered_distance);
      String sensor_center_distance = String(sensor_center.filtered_distance);
      String s = String(global_warn_string + ", " + global_distance_string + ", " + sensor_center_distance + ", " + sensor_left_distance + "; ");
//      String s = String(global_warn_string);
  
      // We need to convert the line to bytes, no more than 20 at this time
      uint8_t sendbuffer[20];
      s.getBytes(sendbuffer, 20);
      char sendbuffersize = min(20, s.length());
  
      Serial.print(F("\n* Sending -> \"")); Serial.print((char *)sendbuffer); Serial.println("\"");
  
      // write the data
      BTLEserial.write(sendbuffer, sendbuffersize);
    }
    
  }
}

void loop() {
  loop_sensor();
  loop_bluetooth();
}
