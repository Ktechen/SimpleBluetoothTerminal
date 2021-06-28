package de.kai_morich.simple_bluetooth_terminal.BT;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MessageData {

    //Liste aller messageIDs der Nachrichten, die versendet wurden und vom ESP bestätigt wurden
    private static List<String> sentMessageIDs = new LinkedList<>();

    public static void addSentMessageID(String messageID){
        if(!sentMessageIDs.contains(messageID)) sentMessageIDs.add(messageID);
    }

    public static boolean IDversendet(String messageID){
        if (sentMessageIDs.contains(messageID)) return true;
        else return false;
    }

    //hier werden die Message Ids der letzten empfangenen 10 Nachrichten von anderen SenderIDs hinterlegt
    private static String[] foreignReceivedMessageIDs = {"","","","","","","","","",""};
    private static int saveHere = 0;

    public static void addForeignMessageID(String messageID){
        if(saveHere == 10) saveHere = 0;
        foreignReceivedMessageIDs[saveHere] = messageID;
        saveHere = saveHere + 1;
    }

    public static boolean foreignMessageIDknown(String messageID){
        for(String fmID: foreignReceivedMessageIDs){
            if(fmID.equals(messageID)) return true;
        }
        return false;
    }

    private static String senderName = "anon";
    private static String zeit;
    private static String messageID;
    private static String inhalt;


    public static void setInhalt(String inhalt) {
        MessageData.inhalt = inhalt;
    }

    private static final String dataDivider = "\r\n";

    public MessageData(){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        zeit = sdf.format(new Date());
        messageID = UUID.randomUUID().toString();
    }

    public MessageData(String senderName) {
        MessageData.senderName = senderName;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        zeit = sdf.format(new Date());
        messageID = UUID.randomUUID().toString();
    }

    public static String getSenderName() {
        return senderName;
    }

    public static String getZeit() {
        return zeit;
    }

    public static String getMessageID() {
        return messageID;
    }

    public static void setSenderName(String senderName) {
        MessageData.senderName = senderName;
    }

    public static void setZeit(String zeit) {
        MessageData.zeit = zeit;
    }

    public static void setMessageID() {
        MessageData.messageID = UUID.randomUUID().toString().substring(0,8);

    }

    public static String dataToString(){
        String ergebnis = messageID + dataDivider +
                          senderName + dataDivider +
                          BTConstants.getSenderID() + dataDivider
                          + zeit + dataDivider
                          +inhalt;


        return ergebnis;
    }
}
