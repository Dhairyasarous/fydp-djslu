#include <Wire.h>
#include "Adafruit_DRV2605.h"

/*bluetooth*/
#include <SPI.h>
#include "Adafruit_BLE_UART.h"

#define ADAFRUITBLE_REQ 10
#define ADAFRUITBLE_RDY 2
#define ADAFRUITBLE_RST 9

Adafruit_BLE_UART uart = Adafruit_BLE_UART(ADAFRUITBLE_REQ, ADAFRUITBLE_RDY, ADAFRUITBLE_RST);
uint8_t effect = 16;
Adafruit_DRV2605 drv;

/**************************************************************************/
/*!
    This function is called whenever select ACI events happen
*/
/**************************************************************************/
void aciCallback(aci_evt_opcode_t event)
{
  switch(event)
  {
    case ACI_EVT_DEVICE_STARTED:
      Serial.println(F("Advertising started"));
      break;
    case ACI_EVT_CONNECTED:
      Serial.println(F("Connected!"));
      break;
    case ACI_EVT_DISCONNECTED:
      Serial.println(F("Disconnected or advertising timed out"));
      break;
    default:
      break;
  }
}

/**************************************************************************/
/*!
    This function is called whenever data arrives on the RX channel
*/
/**************************************************************************/
void rxCallback(uint8_t *buffer, uint8_t len)
{
  if((char)buffer[0] == '1')
  {
    // put your main code here, to run repeatedly:
    Serial.print("Effect #"); Serial.println(effect);

    // set the effect to play
    drv.setWaveform(0, effect);  // play effect 
    drv.setWaveform(1, 0);       // end waveform

    // play the effect!
    drv.go();

    // wait a bit
    delay(500);
  }
  Serial.print(F("Received "));
  Serial.print(len);
  Serial.print(F(" bytes: "));
  for(int i=0; i<len; i++)
   Serial.print((char)buffer[i]); 

  Serial.print(F(" ["));

  for(int i=0; i<len; i++)
  {
    Serial.print(" 0x"); Serial.print((char)buffer[i], HEX); 
  }
  Serial.println(F(" ]"));

  /* Echo the same data back! */
  uart.write(buffer, len);
}


void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  Serial.println("DRV test");
  drv.begin();
  
  drv.selectLibrary(1);
  uint8_t effect = 1;
  
  // I2C trigger by sending 'go' command 
  // default, internal trigger when sending GO command
  drv.setMode(DRV2605_MODE_INTTRIG); 

  while(!Serial); // Leonardo/Micro should wait for serial init
  Serial.println(F("Adafruit Bluefruit Low Energy nRF8001 Callback Echo demo"));

  uart.setRXcallback(rxCallback);
  uart.setACIcallback(aciCallback);
  // uart.setDeviceName("NEWNAME"); /* 7 characters max! */
  uart.begin();
}

const int anPin = 0;
uint8_t mm, mmm;
uint8_t *buffersend; 
void read_sensor()
{
  buffersend[0] = 48;
  buffersend[1] = 48;
  buffersend[2] = 48;
  buffersend[3] = 48;
  buffersend[4] = 48;
  buffersend[5] = 48;
  buffersend[6] = 48;
  bool flag = true; 
  int temp = 0,cntr = 6;
  mmm = analogRead(anPin);
  mmm = 5*mmm;
  mm = mmm;
  while(flag)
  {
    temp = mmm%10;
    mmm = mmm/10;
    buffersend[cntr] = 48+temp;
    cntr = cntr - 1;
    if(temp == 0 || cntr ==0)
    {
      flag = false;
    }
  }
  uart.write(buffersend, 7);
  
} 

void print_data()
{
  Serial.println(mm);
  delay(10);
}
void loop() {
  uart.pollACI();
  read_sensor();
  print_data();
}
