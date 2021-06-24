package de.kai_morich.simple_bluetooth_terminal.Lora;

public class LoraConstants {

    public static boolean LoraOn = false;

    //AT Comamnds

    public final static String AT = "AT";
    public final static String AT_RX = "AT+RX";

    /**
     * Need a addr like 0001
     */
    public final static String AT_ADDR = "AT+ADDR=";

    /**
     * Need a dest addr like FFFF as broadcast
     */
    public final static String AT_DEST = "AT+DEST=";

    /**
     * Need CFG
     */
    public final static String AT_CFG = "AT+CFG=";

    /**
     * Save the current cfg
     */
    public final static String AT_SAVE = "AT+SAVE";

    /**
     * Need length of byte in int
     */
    public final static String AT_SEND = "AT+SEND=";

    //DEFAULT AT COMMANDS
    public final static String DEFAULT_CFG = "433000000,20,6,10,1,1,0,0,0,3000,8,4,10";
    public final static String DEFAULT_DEST = "FFFF";

    public final static String AT_CFG_DEFAULT = AT_CFG + DEFAULT_CFG;
    public final static String AT_DEST_DEFAULT = AT_DEST + DEFAULT_CFG;

}
