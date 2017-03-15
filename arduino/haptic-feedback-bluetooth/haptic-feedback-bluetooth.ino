#include <Wire.h>
#include "Adafruit_DRV2605.h"
#include <SoftwareSerial.h>

// bluetooth
SoftwareSerial BT(10, 11); // connect BT module TX to D10, RX to D11, Vcc to 5V and GND to GND
char bluetooth_input; // stores incoming character from other device

// haptic feedback
#define pwmPin1 3
#define pwmPin2 5
typedef struct{
  int time_on;
  int time_off;
} Pattern; 
static Pattern pattern_high, pattern_mid, pattern_low, pattern_off;
static Pattern* current_pattern;
static int current_state;
long previous_millis = 0;

// led pin
static const int led_pin = 13;

void setup()
{
  Serial.begin(9600);
  Serial.println("Navigation Device Demo");
  
  // bluetooth
  BT.begin(9600);

  // haptic feedback
  current_state = 0;
  pattern_high.time_on = 900;
  pattern_high.time_off = 100;
  pattern_mid.time_on = 500;
  pattern_mid.time_off = 500;
  pattern_low.time_on = 100;
  pattern_low.time_off = 750;
  pattern_off.time_on = 0;
  pattern_off.time_off = 1000;
  pinMode(pwmPin1, OUTPUT);
  pinMode(pwmPin2, OUTPUT);

  // led
  pinMode(13, OUTPUT);
  digitalWrite(13, HIGH); // turn on led to signal on state for device
}

void setState(int state) {
  digitalWrite(pwmPin1, state);
  digitalWrite(pwmPin2, state);
  current_state = state;
  Serial.print("current_state=");
  Serial.println(state);
}

void setPattern(Pattern *pattern) 
{
  unsigned long current_millis = millis();
  unsigned long interval;

  if (current_state == 1) {                     // current state is on
    interval = pattern->time_on;
     if (current_millis - previous_millis >= interval) {
        previous_millis = current_millis;
        setState(0);
     } 
  }
  else {                                        // current state is off
    interval = pattern->time_off;
    if (current_millis - previous_millis >= interval) {
        previous_millis = current_millis;
        setState(1);
    }
  }
}

void loopBluetooth() {
  if (BT.available())
  {
    bluetooth_input = (BT.read());
    if (bluetooth_input == '0') {
      current_pattern = &pattern_off;
    }
    else if (bluetooth_input == '1') {
      current_pattern = &pattern_high;
    }
    else if (bluetooth_input == '2') {
      current_pattern = &pattern_mid;
    }
    else if (bluetooth_input == '3') {
      current_pattern = &pattern_low;
    }
    Serial.print ("bluetooth_input = ");
    Serial.println(bluetooth_input);
  }
}

void loopSerial() {
  if (Serial.available())
  {
    char serial_input = (Serial.read());
    if (serial_input == '0') {
      current_pattern = &pattern_off;
    }
    else if (serial_input == '1') {
      current_pattern = &pattern_high;
    }
    else if (serial_input == '2') {
      current_pattern = &pattern_mid;
    }
    else if (serial_input == '3') {
      current_pattern = &pattern_low;
    }

    Serial.print ("serial_input = ");
    Serial.println(serial_input);
  }
}

void loop()
{
//  loopSerial();
  loopBluetooth();
  setPattern(current_pattern);
}
