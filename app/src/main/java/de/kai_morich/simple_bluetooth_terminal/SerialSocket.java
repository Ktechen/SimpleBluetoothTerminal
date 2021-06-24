package de.kai_morich.simple_bluetooth_terminal;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Executors;

import de.kai_morich.simple_bluetooth_terminal.Lora.LoraATCommands;
import de.kai_morich.simple_bluetooth_terminal.Lora.LoraConstants;

class SerialSocket implements Runnable {

    private static final UUID BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BroadcastReceiver disconnectBroadcastReceiver;

    private final Context context;
    private SerialListener listener;
    private final BluetoothDevice device;
    private BluetoothSocket socket;
    private boolean connected;

    private final LoraATCommands loraATCommands;

    SerialSocket(Context context, BluetoothDevice device) {
        if (context instanceof Activity)
            throw new InvalidParameterException("expected non UI context");
        this.context = context;
        this.device = device;
        disconnectBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (listener != null)
                    listener.onSerialIoError(new IOException("background disconnect"));
                disconnect(); // disconnect now, else would be queued until UI re-attached
            }
        };

        this.loraATCommands = new LoraATCommands();
    }

    String getName() {
        return device.getName() != null ? device.getName() : device.getAddress();
    }

    /**
     * connect-success and most connect-errors are returned asynchronously to listener
     */
    void connect(SerialListener listener) throws IOException {
        this.listener = listener;
        context.registerReceiver(disconnectBroadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_DISCONNECT));
        Executors.newSingleThreadExecutor().submit(this);
    }

    void disconnect() {
        listener = null; // ignore remaining data and errors
        // connected = false; // run loop will reset connected
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
        try {
            context.unregisterReceiver(disconnectBroadcastReceiver);
        } catch (Exception ignored) {
        }
    }

    void write(byte[] data) throws IOException {
        if (!connected)
            throw new IOException("not connected");

        String analyseData = new String(data);
        Log.d("data", "write: " + analyseData);

        try {
            Log.i("lora", "data:" + analyseData);
            loraATCommands.run(analyseData.toUpperCase());

            if (LoraConstants.isLora) {
                if (!LoraConstants.SKIP) {
                    String msg = "AT+SEND=" + data.length;
                    byte[] at = msg.getBytes();
                    socket.getOutputStream().write(at);
                    socket.getOutputStream().write(data);
                }
                LoraConstants.SKIP = false;
            }

            if (loraATCommands.getList() != null) {
                for (byte[] item : loraATCommands.getList()) {
                    socket.getOutputStream().write(item);
                }
                loraATCommands.setList(null);
            }

        } catch (IllegalArgumentException e) {
            socket.getOutputStream().write(e.getMessage().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (loraATCommands.getList() == null) {
            socket.getOutputStream().write(data);
        }

    }

    @Override
    public void run() { // connect & read
        try {
            socket = device.createRfcommSocketToServiceRecord(BLUETOOTH_SPP);
            socket.connect();
            if (listener != null)
                listener.onSerialConnect();
        } catch (Exception e) {
            if (listener != null)
                listener.onSerialConnectError(e);
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
            return;
        }
        connected = true;
        try {
            byte[] buffer = new byte[1024];
            int len;
            //noinspection InfiniteLoopStatement
            while (true) {
                len = socket.getInputStream().read(buffer);
                byte[] data = Arrays.copyOf(buffer, len);
                if (listener != null)
                    listener.onSerialRead(data);
            }
        } catch (Exception e) {
            connected = false;
            if (listener != null)
                listener.onSerialIoError(e);
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
    }

}
