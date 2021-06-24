package de.kai_morich.simple_bluetooth_terminal.Lora;

import android.util.Log;

import java.util.ArrayList;

public class LoraATCommands implements ILoraATCommands {

    private ArrayList<byte[]> list = null;

    @Override
    public void run(String command) throws Exception {

        command = command.replaceAll("[\\n\\r ]", "");
        boolean isSetup = false;

        switch (command) {
            case LoraConstants.onLora:
                LoraConstants.isLora = true;
                LoraConstants.SKIP = true;
                break;
            case LoraConstants.offLora:
                LoraConstants.isLora = false;
                LoraConstants.SKIP = true;
                break;
            case LoraConstants.AT:
                LoraConstants.SKIP = true;
                break;
            case LoraConstants.SETUP:

                if (LoraConstants.isLora) {
                    throw new IllegalArgumentException("Lora must be off before we can start with configuration");
                }

                this.list = new ArrayList<>();
                list.add(LoraConstants.AT.getBytes());
                list.add(LoraConstants.AT_RX.getBytes());
                list.add(LoraConstants.AT_CFG_DEFAULT.getBytes());
                list.add(LoraConstants.AT_ADDR_DEFAULT.getBytes());
                list.add(LoraConstants.AT_DEST_DEFAULT.getBytes());
                list.add(LoraConstants.AT_SAVE.getBytes());

                isSetup = true;
                break;

        }

        Log.i("lora", "isLora status:" + LoraConstants.isLora);
        Log.i("lora", "skip at status: " + LoraConstants.SKIP);
        Log.i("lora", "setup status:" + isSetup);
        Log.i("lora", String.valueOf("is list empty status:" + list == null));

    }

    public ArrayList<byte[]> getList() {
        return list;
    }

    public void setList(ArrayList<byte[]> list) {
        this.list = list;
    }
}
