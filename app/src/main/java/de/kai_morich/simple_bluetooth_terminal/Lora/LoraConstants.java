package de.kai_morich.simple_bluetooth_terminal.Lora;

public class LoraConstants {

    public static boolean isLora = false;

    //Lora Commands

    public final static String onLora = "ONLORA";
    public final static String offLora = "OFFLORA";
    public final static String SETUP = "SETUP";
    public final static String HELP = "HELP";

    //AT Comamnds
    public static boolean SKIP = false;

    public final static String AT = "AT\r\n";
    public final static String AT_RX = "AT+RX\r\n";

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
    public final static String AT_SAVE = "AT+SAVE\r\n";

    /**
     * Need length of byte in int
     */
    public final static String AT_SEND = "AT+SEND=";

    //DEFAULT AT COMMANDS
    public static String defaultCfg = "433000000,20,6,10,1,1,0,0,0,3000,8,4,10\r\n";
    public static String defaultAddr = "0001\r\n";
    public static String defaultDest = "FFFF\r\n";

    public final static String AT_CFG_DEFAULT = AT_CFG + defaultCfg;
    public final static String AT_ADDR_DEFAULT = AT_ADDR + defaultAddr;
    public final static String AT_DEST_DEFAULT = AT_DEST + defaultDest;

}
