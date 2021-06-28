
#include "BluetoothSerial.h"
#include "EEPROM.h"
#include <ArduinoJson.h>

BluetoothSerial SerialBT;

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

#define EEPROM_SIZE 128

#define RXD2 16
#define TXD2 17

bool atRun = true;
int addr = 0;

void setup() {
  Serial.begin(115200);

  Serial2.begin(115200, SERIAL_8N1, RXD2, TXD2);
  SerialBT.begin("KekW-001"); //Bluetooth device name
  // Serial.println("The device started, now you can pair it with bluetooth!");
  EEPROM.begin(EEPROM_SIZE);
  // LORA
  // loraSetup();
}

//AT-Commands für LoRa Module
void ATScripts(int addr) {
  Serial2.println("AT");
  Serial2.println("AT+RX");
  Serial2.println("AT+ADDR=" + addr);
  Serial2.println("AT+DEST=FFFF");
  Serial2.println("AT+CFG=433000000,20,6,10,1,1,0,0,0,3000,8,4,10");
  Serial2.println("AT+SAVE");

  String msg = "hello";

  Serial2.println("AT+SEND=5");
  Serial2.println(msg);
}

void loop() {
  if (atRun) {
    ATScripts(0001);
    atRun = false;
  }

  //readNewMessages();
  getESP32SerialClientInput();
}

void pin() {
  Serial.println("Serial Txd is on pin: " + String(TX));
  Serial.println("Serial Rxd is on pin: " + String(RX));
  delay(1000);
}

String toSend = "AT+SEND=";

void storageMsg(String msg) {
  EEPROM.put(addr, msg);

  addr = addr + 1;
  if (addr == EEPROM.length()) {
    addr = 0;
  }
}

String recmsg;
String repmsg;

void getESP32SerialClientInput() {

  //ToCLient
  if (Serial2.available()) {
    int data = Serial2.read();
    if (data != 10 || data != 13) {
      Serial.print((char)data);
      repmsg += (char)data;
    } else {
      storageMsg(repmsg);
      Serial.print(data);
    }
    SerialBT.write(data);
  }

  //fromClient
  if (SerialBT.available()) {
    int data = SerialBT.read();
    if (data != 10 || data != 13) {
      Serial.print((char)data);
      recmsg += (char)data;
    } else {
      storageMsg(recmsg);
      Serial.print(data);
    }

    if ((char)data == 'g') {
      String value;
      EEPROM.get(addr, value);
      Serial.print(addr);
      Serial.print("\t");
      Serial.print(value);
      Serial.println();
    }


    Serial2.write(data);
  }
  delay(20);
}
