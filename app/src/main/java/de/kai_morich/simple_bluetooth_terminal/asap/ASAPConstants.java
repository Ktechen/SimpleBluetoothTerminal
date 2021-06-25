package de.kai_morich.simple_bluetooth_terminal.asap;

public class ASAPConstants {

    /**
     * Payload of ASAP
     * APP
     * URL
     * byte[] messages
     */

    public static CharSequence APP = "ASAP_MESSENGER";
    public static CharSequence URI = "asap://Public";

    public static byte[] APPtoBytes = ((String) APP).getBytes();
    public static byte[] URItoBytes = ((String) URI).getBytes();

}
