package de.kai_morich.simple_bluetooth_terminal;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.concurrent.Delayed;

import de.kai_morich.simple_bluetooth_terminal.BT.BTConstants;
import de.kai_morich.simple_bluetooth_terminal.BT.MessageData;
import de.kai_morich.simple_bluetooth_terminal.Lora.LoraConstants;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        View atbtn = view.findViewById(R.id.AT_button);
        atbtn.setOnClickListener(v -> send(LoraConstants.AT));

        View aton = view.findViewById(R.id.AT_ON_button);
        aton.setOnClickListener(v -> send(LoraConstants.onLora));

        View atoff = view.findViewById(R.id.AT_OFF_button);
        atoff.setOnClickListener(v -> send(LoraConstants.offLora));

        View setup = view.findViewById(R.id.AT_Setup_button);
        setup.setOnClickListener(v -> send(LoraConstants.SETUP));

        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> {

            String str = sendText.getText().toString();
            //turn on Esp mode for additional information
            if (str.toUpperCase().equals(BTConstants.ESP_MODE_ON)) {
                BTConstants.setIsESP(true);
                sendText.setText("");
                receiveText.setText(receiveText.getText() + "Your messages now have a data overhead for ESP\r\n");
                return;
            }

            //turn off Esp mode for less information
            if (str.toUpperCase().equals(BTConstants.ESP_MODE_OFF)) {
                BTConstants.setIsESP(false);
                sendText.setText("");
                receiveText.setText(receiveText.getText() + "Your messages no longer have a data overhead for ESP\r\n");
                return;
            }

            //change devices name in additional information for esp mode
            if (str.toUpperCase().contains(BTConstants.ESP_SENDER_NAME)) {
                String neuerSenderName;
                if(str.contains(BTConstants.ESP_SENDER_NAME.toLowerCase())) neuerSenderName = str.replace(BTConstants.ESP_SENDER_NAME.toLowerCase(), "");
                else neuerSenderName = str.replace(BTConstants.ESP_SENDER_NAME, "");
                neuerSenderName.replace("\r\n", "");
                MessageData.setSenderName(neuerSenderName);
                sendText.setText("");
                receiveText.setText(receiveText.getText() + "Your devices name has been changed to: [" + MessageData.getSenderName() + "]\r\n");

                return;
            }
            if(str.toUpperCase().equals(BTConstants.ESP_SENDER_ID)){
                BTConstants.setSenderID();
                sendText.setText("");
                receiveText.setText(receiveText.getText() + "Deine SenderID wurde geändert\r\n");
                return;
            }

            send(sendText.getText().toString());
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } else if (id == R.id.AT_Commands) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("AT-Commands");
            builder.setItems(LoraConstants.items, (dialogInterface, i) -> {
                sendText.setText(LoraConstants.items[i]);
            });

            builder.create().show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if (hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] data) {
        if (hexEnabled) {
            receiveText.append(TextUtil.toHexString(data) + '\n');
        } else {
            String msg = new String(data);
            String msgSenderID;
            String msgMessageID;
            ////////////////////
            //Handelt es sich um eine Nachricht mit data Overhead?
            if(msg.substring(0,4).equals(BTConstants.ESP_TAG)){
                msg = msg.substring(4);//ESP: TAG abschneiden
                msgSenderID = msg.substring(0,8);
                msg = msg.substring(8);
                msgMessageID = msg.substring(0,8);
                msg = msg.substring(8); //msg ohne sender oder messageID

                //Handelt es sich um eine Nachricht von diesem Device?
                //Wenn dieser Sender die Nachricht verschickt hat, wird die Nachricht zu einer Bestätigung des Empfangs durch den ESP

                if(msgSenderID.equals(BTConstants.getSenderID())) {
                    //wenn die gesendete Nachricht schon einmal bestätigt wurde gib nichts aus
                    if(MessageData.IDversendet(msgMessageID))msg = "";
                        //wenn die gesendete Nachricht das erste mal bestätigt wird gib Bestätigung aus
                    else{
                        msg = "ESP hat deine Nachricht empfangen\r\n";
                        MessageData.addSentMessageID(msgMessageID);
                    }
                }
                //Bei der Nachricht einen anderen Senders wird die Sender-ID(8 zeichen) entfernt um nur senderName,zeit und inhalt auszugegeben
                else {
                    //receiveText.setText("NEUE NACHRICHT HERE\r\n");

                    //wurde diese Nachricht schonmal erhalten?, dann soll sie nicht wieder angezeigt werden
                    if(MessageData.foreignMessageIDknown(msgMessageID)){
                        msg = "";
                    }
                    else{
                        MessageData.addForeignMessageID(msgMessageID);
                        msg += "\r\n";
                    }
                }
            }
            ///////////////////
            if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg.charAt(0) == '\n') {
                    Editable edt = receiveText.getEditableText();
                    if (edt != null && edt.length() > 1)
                        edt.replace(edt.length() - 2, edt.length(), "");
                }
                pendingNewline = msg.charAt(msg.length() - 1) == '\r';
            }
            receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
        }
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        Log.d("data", "read: " + new String(data));
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

}
