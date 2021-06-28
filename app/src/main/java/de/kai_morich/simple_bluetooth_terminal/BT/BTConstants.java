package de.kai_morich.simple_bluetooth_terminal.BT;

import java.util.UUID;

public class BTConstants {

    public static final String ESP_TAG = "ESP:";

    public static final String ESP_MODE_ON = "ESPON";

    public static final String ESP_MODE_OFF = "ESPOFF";

    public static final String ESP_SENDER_NAME = "NAME: ";

    public static final String ESP_SENDER_ID = "SIDCHANGE";

    private static String senderID = UUID.randomUUID().toString().substring(0,8);

    public static void setSenderID() {
        senderID = UUID.randomUUID().toString().substring(0,8);
    }

    public static String getSenderID(){
        return senderID;
    }

    //legt fest ob die Nachricht mit data overhead verschickt werden soll oder nicht
    private static boolean isESP = false;

    public static void setIsESP(boolean isESP){
        BTConstants.isESP = isESP;
    }

    public static boolean getIsESP() {
        return isESP;
    }

}
