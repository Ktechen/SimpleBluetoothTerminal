//blauer Sender, der eine Nachricht verschickt und sie in einem Array abspeichert der mit Android Nachrichten austauschen kann

/*
  Rui Santos
  Complete project details at https://RandomNerdTutorials.com/esp-now-esp32-arduino-ide/
  
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files.
  
  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.
*/

#include <esp_now.h>
#include <WiFi.h>

////////////////////////Android BT Communication
#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;
String btName = "BlueESP32";
////////////////////////

//MAC Addresse des ESP32, mit dem Nachrichten ausgetauscht werden
uint8_t broadcastAddress[] = {0x10, 0x52, 0x1C, 0x68, 0x48, 0xAC};//Rot

// Structure example to send data
// Must match the receiver structure
typedef struct struct_message {
    String messageID;
    String senderName;                                                                             //TODO: change to char array
    String senderID;
    String zeit; 
    char text[100];
                                                                                       //TODO: needs to be changed to time format for comparing possibilities
} struct_message;

//Objekt zur Zwischenspeicherung einer zu verschickenden Nachricht 
struct_message rausgehendeNachricht;

//Objekt zur Zwischenspeicherung einer von einem ESP erhaltenen Nachricht
struct_message erhalteneESPNachricht;
                                                                                            //TODO: Nachrichten permanent speichern nicht nur zwischenspeichern
//Array aller aktuell (zwischen)gespeicherten Nachrichten
struct_message savedMessage[10];
//Speicherplatz im Array der als nächstes mit einer neuen Nachricht belegt werden soll
int freierSpeicherplatz = 0;

//checkt ob die erhaltene Message schon gespeichert wurde
boolean containsMessageID(String messageIDpara){                                               //TODO: globale Variable für anzahl der Arrayplätze
  for(int i = 0; i<10;i++){
    if(messageIDpara.equals(savedMessage[i].messageID))return true;
  }
  return false;
}

void forwardingToEsp(){
  //leere Nachrichten(ohne msgID) werden nicht versendet
  if(rausgehendeNachricht.messageID.length() == 0) return;
  
  // Synchronisation des NachrichenSpeichers mit anderen ESPS über ESP-NOW
  esp_err_t result = esp_now_send(broadcastAddress, (uint8_t *) &rausgehendeNachricht, sizeof(rausgehendeNachricht));
   
  if (result == ESP_OK) {
    Serial.println("Sent with success");
  }
  else {
    Serial.println("Error sending the data");
  }

  delay(1000);
}

void forwardingToAndroid(){
  //leere Nachrichten(ohne msgID) werden nicht versendet
  if(rausgehendeNachricht.messageID.length() == 0) return;
  
  //Message Inhalt als String zusammenfassen und in uint8_t array speichern
  //um es an den Android Client zu senden
  String weiterleitung = "ESP:";
  weiterleitung += rausgehendeNachricht.senderID +
                   rausgehendeNachricht.messageID +
                   rausgehendeNachricht.senderName + ": " 
                   + rausgehendeNachricht.text + " (" 
                   + rausgehendeNachricht.zeit + ")";
  char antwortInChar[weiterleitung.length()];
  strcpy(antwortInChar,weiterleitung.c_str());
  uint8_t androidAntwort[weiterleitung.length()];
  for(int i = 0; i<weiterleitung.length();i++){
    androidAntwort[i] =  antwortInChar[i];
  }
  SerialBT.write(androidAntwort,sizeof(androidAntwort));
}

void speicherSynchronisieren(){
  for(int i = 0; i <10; i++){
    rausgehendeNachricht = savedMessage[i];
    if(rausgehendeNachricht.messageID.length()!=0){
      forwardingToAndroid();
      forwardingToEsp();
    }
  }
}

void speicherAusgeben(){
  //Ausgabe des (zwischen)Speicherinhalts
  Serial.print("Speicherinhalt BLAU:\n");
  for(int i = 0; i<10;i++){
     Serial.printf("[%d: %s | %s | %s | %s | %s]\n",i,
                                      savedMessage[i].senderName,
                                      savedMessage[i].senderID,
                                      savedMessage[i].text,
                                      savedMessage[i].zeit,
                                      savedMessage[i].messageID);
  }
  Serial.println();
}

// callback Funktion, die aufgerufen wird, wenn Nachricht verschickt wurde
void OnDataSent(const uint8_t *mac_addr, esp_now_send_status_t status) {
  Serial.print("\r\nLast Packet Send Status:\t");
  Serial.println(status == ESP_NOW_SEND_SUCCESS ? "Delivery Success" : "Delivery Fail");
}

// callback function die aufgerufen wird, wenn eine Nachricht über ESP-NOW erhalten wird
void OnDataRecv(const uint8_t * mac, const uint8_t *incomingData, int len) {
  //erhaltene Daten werden im objekt erhalteneESPNachricht zwischengespeichert
  memcpy(&erhalteneESPNachricht, incomingData, sizeof(erhalteneESPNachricht));
  //Nachrichten ohne Overhead werden nicht gespeichert 
  if(erhalteneESPNachricht.messageID.length()==0) return;
  //Wenn die Nachricht aktuell schon (zwischen)gespeichert ist(erkennbar an Message ID), dann wird sie nicht erneut gespeichert
  if(containsMessageID(erhalteneESPNachricht.messageID)){
    Serial.printf("Nachricht(%s) bereits gespeichert\n",erhalteneESPNachricht.messageID);
  }
  //Die Nachricht ist aktuell nicht (zwischen)gespeichert und wird im Array abgelegt
  else{
    if(freierSpeicherplatz == 10) freierSpeicherplatz = 0;//wenn das Array voll ist, wird die aelteste Nachricht wieder ueberschrieben
    savedMessage[freierSpeicherplatz].senderName = erhalteneESPNachricht.senderName;
    savedMessage[freierSpeicherplatz].senderID = erhalteneESPNachricht.senderID;
    memcpy(savedMessage[freierSpeicherplatz].text,erhalteneESPNachricht.text,100);
    savedMessage[freierSpeicherplatz].zeit = erhalteneESPNachricht.zeit;
    savedMessage[freierSpeicherplatz].messageID = erhalteneESPNachricht.messageID;
    
    Serial.printf("Nachricht gespeichert unter Platz: %d\n", freierSpeicherplatz);
    
    Serial.printf("Weiterleiten der Nachricht an andere ESPs\n");
    rausgehendeNachricht.senderName = erhalteneESPNachricht.senderName;
    rausgehendeNachricht.senderID = erhalteneESPNachricht.senderID;
    memcpy(rausgehendeNachricht.text,erhalteneESPNachricht.text,100);
    rausgehendeNachricht.zeit = erhalteneESPNachricht.zeit;
    rausgehendeNachricht.messageID = erhalteneESPNachricht.messageID;
    
    forwardingToEsp();
    forwardingToAndroid();
    
    ++freierSpeicherplatz;
  }
  //Ausgabe der erhaltenen gespeicherten Nachricht
  Serial.printf("Von Esp erhaltene Nachricht:[%s | %s | %s | %s | %s]\n",erhalteneESPNachricht.messageID,
                                                         erhalteneESPNachricht.senderName,
                                                         erhalteneESPNachricht.senderID,
                                                         erhalteneESPNachricht.zeit,
                                                         erhalteneESPNachricht.text);
  speicherAusgeben();
}
 
void setup() {
  delay(2000);
  
  // Init Serial Monitor
  Serial.begin(115200);

//////////////////////////////////////////////////////BTCommunication
  SerialBT.begin(btName); //Bluetooth device name
  Serial.println("The device started, now you can pair it with bluetooth!");
////////////////////////////////////////////////////////////////////
 
  // Set device as a Wi-Fi Station
  WiFi.mode(WIFI_STA);

  // Init ESP-NOW
  if (esp_now_init() != ESP_OK) {
    Serial.println("Error initializing ESP-NOW");
    return;
  }

  // Once ESPNow is successfully Init, we will register for Send CB to
  // get the status of Trasnmitted packet
  esp_now_register_send_cb(OnDataSent);

  // Once ESPNow is successfully Init, we will register for recv CB to
  // get recv packer info
  esp_now_register_recv_cb(OnDataRecv);
  
  // Register peer
  esp_now_peer_info_t peerInfo;
  memcpy(peerInfo.peer_addr, broadcastAddress, 6);
  peerInfo.channel = 0;  
  peerInfo.encrypt = false;
  
  // Add peer        
  if (esp_now_add_peer(&peerInfo) != ESP_OK){
    Serial.println("Failed to add peer");
    return;
  }
}

//Variablen, die die Zeitabstände zwischen den Synchronisierungen festlegen
unsigned long millisToWait = 10000;
unsigned long previousMillis = 0;

void loop() {

  unsigned long currentMillis = millis();

  //BT-Nachricht von Android Client empfangen und Daten (ggf.) speichern
   if (SerialBT.available()) {
    String nachricht = SerialBT.readString();
    nachricht.remove(nachricht.length()-2);//abschneiden des Zeilenumbruchs am Ende des Inhalts der Nachricht
   
    //Nachricht mit overhead wird gespeichert, wenn die messageID noch nicht vorliegt
    if(nachricht.substring(0,4).equals("ESP:")){

      nachricht = nachricht.substring(4);//cut of "ESP:" tag
      int startCut = 0;
      int endCut = nachricht.indexOf('\r');

      //Empfangene BT Nachricht wird nur gespeichert, wenn ID noch nicht gegeben(sollte eigentlich nicht schon existieren)
      if(!containsMessageID(nachricht.substring(startCut,endCut))){

        if(freierSpeicherplatz == 10) freierSpeicherplatz = 0;//wenn das Array voll ist, wird die aelteste Nachricht wieder ueberschrieben

        //Speichern der Android Client Nachricht mit overhead und definieren der Nachricht die gebroadcastet werden soll
        savedMessage[freierSpeicherplatz].messageID = nachricht.substring(startCut,endCut);
        rausgehendeNachricht.messageID = nachricht.substring(startCut,endCut);
        nachricht = nachricht.substring(endCut+2);
        endCut = nachricht.indexOf('\r');
        savedMessage[freierSpeicherplatz].senderName = nachricht.substring(startCut,endCut);
        rausgehendeNachricht.senderName = nachricht.substring(startCut,endCut);
        nachricht = nachricht.substring(endCut+2);
        endCut = nachricht.indexOf('\r');
        savedMessage[freierSpeicherplatz].senderID = nachricht.substring(startCut,endCut);
        rausgehendeNachricht.senderID = nachricht.substring(startCut,endCut);
        nachricht = nachricht.substring(endCut+2);
        endCut = nachricht.indexOf('\r');
        savedMessage[freierSpeicherplatz].zeit = nachricht.substring(startCut,endCut);
        rausgehendeNachricht.zeit = nachricht.substring(startCut,endCut);
        nachricht = nachricht.substring(endCut+2);
        strcpy(savedMessage[freierSpeicherplatz].text,nachricht.c_str());
        strcpy(rausgehendeNachricht.text,nachricht.c_str());

        Serial.printf("Nachricht gespeichert unter Platz: %d\n", freierSpeicherplatz);

        //Weiterleiten der neuen gespeicherten Android Client Nachricht mit overhead an andere ESP
        Serial.printf("Weiterleiten der Nachricht");
        Serial.printf("[%s | %s | %s | %s | %s]",
                                      rausgehendeNachricht.senderName,
                                      rausgehendeNachricht.senderID,
                                      rausgehendeNachricht.text,
                                      rausgehendeNachricht.zeit,
                                      rausgehendeNachricht.messageID);
        Serial.printf("an andere ESPs\n");
        forwardingToEsp();
        forwardingToAndroid();
        
        ++freierSpeicherplatz;
      }
    }
    
    //Android Client Nachricht ohne overhead wird nicht gespeichert
    else{
      Serial.printf("Nachricht ohne Overhead erhalten\n");
      uint8_t fehlerhafterOverhead[] = {'k','e','i','n',' ','E','S','P','-','T','A','G','\r','\n'};
      SerialBT.write(fehlerhafterOverhead,sizeof(fehlerhafterOverhead));
    }
    
    speicherAusgeben();
  }

  //Synchronisation des Speicherinhaltes mit verbundenen Esps und Android Devices ca alle 5sekunden
  if((currentMillis - previousMillis) >= millisToWait){
    previousMillis = currentMillis;
    Serial.printf("Zeit Synchro mit Esp und Android . . .\n");
    speicherSynchronisieren();
  }
 
}
