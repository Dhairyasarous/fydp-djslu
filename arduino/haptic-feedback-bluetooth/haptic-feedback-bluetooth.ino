#include <Wire.h>
#include "Adafruit_DRV2605.h"
#include <SoftwareSerial.h>

// bluetooth
SoftwareSerial BT(10, 11); // connect BT module TX to D10, RX to D11, Vcc to 5V and GND to GND
char bluetooth_input; // stores incoming character from other device

// haptic feedback
Adafruit_DRV2605 drv;
static const long VIBRATION_INTERVAL = 500; // milliseconds
static const uint8_t pattern_high = 16, pattern_mid = 15, pattern_low = 14, pattern_off = 0;
uint8_t current_vibration_pattern;
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
  current_vibration_pattern = pattern_off;
  drv.begin();
  drv.selectLibrary(1);
  drv.setMode(DRV2605_MODE_INTTRIG);

  // led
  pinMode(13, OUTPUT);
  digitalWrite(13, HIGH); // turn on led to signal on state for device
}

void setNewPattern()
{
  unsigned long current_millis = millis();
  if (current_millis - previous_millis >= VIBRATION_INTERVAL) {
    previous_millis = current_millis;
    drv.setWaveform(0, current_vibration_pattern);
    drv.setWaveform(1, 0);
    drv.go();
  }
}

void loopBluetooth() {
  if (BT.available())
  {
    bluetooth_input = (BT.read());
    if (bluetooth_input == '0') {
      current_vibration_pattern = pattern_off;
    }
    else if (bluetooth_input == '1') {
      current_vibration_pattern = pattern_high;
    }
    else if (bluetooth_input == '2') {
      current_vibration_pattern = pattern_mid;
    }
    else if (bluetooth_input == '3') {
      current_vibration_pattern = pattern_low;
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
      current_vibration_pattern = pattern_off;
    }
    else if (serial_input == '1') {
      current_vibration_pattern = pattern_high;
    }
    else if (serial_input == '2') {
      current_vibration_pattern = pattern_mid;
    }
    else if (serial_input == '3') {
      current_vibration_pattern = pattern_low;
    }

    Serial.print ("serial_input = ");
    Serial.println(serial_input);
  }
}

void loop()
{
  loopBluetooth();
  setNewPattern();
}
