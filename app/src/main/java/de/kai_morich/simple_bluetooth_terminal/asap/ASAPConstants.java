package de.kai_morich.simple_bluetooth_terminal.asap;

public class ASAPConstants {

    /**
     * Payload of ASAP
     * APP
     * URL
     * byte[] messages
     * boolean persistent
     * https://github.com/SharedKnowledge/ASAPAndroid/blob/master/app/src/main/java/net/sharksystem/asap/android/ASAPServiceMessage.java
     */

    public static CharSequence APP = "ASAP_MESSENGER";
    public static CharSequence URI = "asap://Public";

    public static byte[] APPtoBytes = ((String) APP).getBytes();
    public static byte[] URItoBytes = ((String) URI).getBytes();

}
