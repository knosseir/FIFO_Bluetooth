package com.example.admin.fifo;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothFragment extends Fragment {

    private static final String TAG = "BluetoothFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private static final double AVERAGE_WAIT_TIME_PER_PERSON = 1; // minutes

    TextView peopleAheadCount, eta;

    // Layout Views
    private ListView mQueue;
    private EditText mNameEditText;
    private Button mHostButton;
    private Button mCustomerButton;
    private Map<String,String> customerMap = new HashMap<>();
    private Map<String,String> MACMap = new HashMap<>();
    private int position;

    /**
     * Name of user
     */
    private String mUserName = null;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService mChatService = null;

    private Set<String> devices = new HashSet<>();

    private boolean isHost = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }

        /*
        connectDevice("E4:32:CB:35:82:EE", true);
        while(mChatService.getState() != BluetoothService.STATE_CONNECTED);
        mChatService.stop();
        mChatService.stop();
        connectDevice("94:65:2D:DC:72:90", true);
        while(mChatService.getState() != BluetoothService.STATE_CONNECTED);
        mChatService.stop();
        mChatService.stop();
        connectDevice("E4:32:CB:35:82:EE", true);
        while(mChatService.getState() != BluetoothService.STATE_CONNECTED);
        mChatService.stop();
        mChatService.stop();
        connectDevice("94:65:2D:DC:72:90", true);
        while(mChatService.getState() != BluetoothService.STATE_CONNECTED);
        mChatService.stop();
        mChatService.stop();
        */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    // TODO: WHEN CUSTOMER IS REMOVED FOR ANY REASON, EXPLICITLY CALL CANCEL() ON BOTH ENDS
    private void updateDevices() {
        devices = mChatService.getDevices();
        for (String device : devices) {
            //mChatService.stop();
            if(device == ""){
                continue;
            }
            Log.e("MAC Address 2", device);
            connectDevice(device, true);
            int count = 1;
            while(mChatService.getState() != BluetoothService.STATE_CONNECTED){
                while(mChatService.getState() == BluetoothService.STATE_CONNECTING);
                if(count == 500000){
                    return;
                }
                if((mChatService.getState() == BluetoothService.STATE_NONE) || (mChatService.getState() == BluetoothService.STATE_LISTEN)){
                    count++;
                    if ((count % 50000) == 0) {
                        mChatService.stop();
                        connectDevice(device, true);
                    }
                }
            }
            Log.e("Device", device);
            boolean isPresent = false;
            String msg ="deleteAll ";
            for (int i = 0; i < mConversationArrayAdapter.getCount(); i++) {
                String curr = mConversationArrayAdapter.getItem(i);
                msg += curr;
                msg += " ";
                if(customerMap.get(mConnectedDeviceName).equals(curr)){
                    isPresent = true;
                    if(MACMap.get(curr) == null) {
                        MACMap.put(curr, device);
                    }
                }
            }
            if(mConversationArrayAdapter.getCount() > 0 && customerMap.get(mConnectedDeviceName).equals(mConversationArrayAdapter.getItem(0))){
                msg += "fifo ";
            } else if(!isPresent){
                msg += "notify ";
                mChatService.removeDevice(mConnectedDeviceName);
            }
            sendMessage(msg);
        }
        mChatService.stop();
        mChatService.start();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mQueue = view.findViewById(R.id.in);
        //mOutEditText = view.findViewById(R.id.edit_text_out);
        mNameEditText = view.findViewById(R.id.edit_text_name);
        //mSendButton = view.findViewById(R.id.button_send);
        mHostButton = view.findViewById(R.id.host_button);
        mCustomerButton = view.findViewById(R.id.customer_button);

        mHostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isHost = true;
                Toast.makeText(getContext(), "Host mode enabled", Toast.LENGTH_SHORT).show();
            }
        });

        mQueue.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                Toast.makeText(getActivity(), adapterView.getItemAtPosition(i).toString(), Toast.LENGTH_SHORT).show();

                // need to get the instance of the LayoutInflater, use the context of this activity
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                // inflate the view from a predefined XML layout (no need for root id, using entire layout)
                View layout = inflater.inflate(R.layout.host_options, null);

                final PopupWindow pw = new PopupWindow(layout);

                pw.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                pw.setTouchInterceptor(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                            pw.dismiss();
                            return true;
                        }
                        return false;
                    }
                });
                pw.setOutsideTouchable(true);

                pw.setHeight(400);
                pw.setWidth(400);

                pw.showAtLocation(view, Gravity.CENTER, 0, 0);

                Button checkout = layout.findViewById(R.id.checkout);
                Button bump = layout.findViewById(R.id.bump);
                Button kick = layout.findViewById(R.id.kick);


                checkout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getActivity(), adapterView.getItemAtPosition(i).toString() + " has been checked out.", Toast.LENGTH_SHORT).show();
                        mConversationArrayAdapter.remove(adapterView.getItemAtPosition(i).toString());
                        updateDevices();
                    }
                });

                bump.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getActivity(), adapterView.getItemAtPosition(i).toString() + " has been bumped.", Toast.LENGTH_SHORT).show();
                        String s = adapterView.getItemAtPosition(i).toString();
                        mConversationArrayAdapter.remove(s);
                        mConversationArrayAdapter.add(s);
                        updateDevices();
                    }
                });

                kick.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getActivity(), adapterView.getItemAtPosition(i).toString() + " has been kicked.", Toast.LENGTH_SHORT).show();
                        mConversationArrayAdapter.remove(adapterView.getItemAtPosition(i).toString());
                        updateDevices();
                    }
                });
            }
        });
    }

    /**
     * Set up the UI and background operations BT communication
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.message);

        mQueue.setAdapter(mConversationArrayAdapter);

        mCustomerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = getView();
                if (view != null) {
                    TextView tv = view.findViewById(R.id.edit_text_name);
                    String name = tv.getText().toString();
                    sendMessage(name);
                    mUserName = name;

                    // need to get the instance of the LayoutInflater, use the context of this activity
                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    // inflate the view from a predefined XML layout (no need for root id, using entire layout)
                    View layout = inflater.inflate(R.layout.activity_customer, null);

                    final PopupWindow pw = new PopupWindow(layout, ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT, true);

                    peopleAheadCount = layout.findViewById(R.id.people_ahead_count);
                    eta = layout.findViewById(R.id.eta);
                    Button exit = layout.findViewById(R.id.exit_queue_button);

                    for(int i = 0; i < mConversationArrayAdapter.getCount(); i++){
                        if(mUserName.equals(mConversationArrayAdapter.getItem(i))) {
                            position = i;
                        }
                    }
                    peopleAheadCount.setText("There are " + Integer.toString(position) + " people ahead of you in line.");
                    eta.setText(String.valueOf("ETA: " + AVERAGE_WAIT_TIME_PER_PERSON * position) + " minutes");
                    exit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mConversationArrayAdapter.remove(mUserName);
                            devices = mChatService.getDevices();
                            Object[] items = devices.toArray();
                            connectDevice((String)items[0], true);
                            int count = 1;
                            while(mChatService.getState() != BluetoothService.STATE_CONNECTED){
                                while(mChatService.getState() == BluetoothService.STATE_CONNECTING);
                                if(count == 500000){
                                    return;
                                }
                                if((mChatService.getState() == BluetoothService.STATE_NONE) || (mChatService.getState() == BluetoothService.STATE_LISTEN)){
                                    count++;
                                    if ((count % 50000) == 0) {
                                        mChatService.stop();
                                        connectDevice((String)items[0], true);
                                    }
                                }
                            }
                            sendMessage("delete " + mUserName);
                            Toast.makeText(getContext(), "Thanks for shopping!", Toast.LENGTH_LONG);
                            getActivity().finishAffinity();
                        }
                    });

                    pw.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                    pw.setTouchInterceptor(new View.OnTouchListener() {
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                                pw.dismiss();
                                return true;
                            }
                            return false;
                        }
                    });
                    pw.setOutsideTouchable(false);

                    pw.showAtLocation(view, Gravity.CENTER, 0, 0);
                }
            }
        });

        // initialize the BluetoothService to perform Bluetooth connections
        mChatService = new BluetoothService(getActivity(), mHandler);

        // initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), "Not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            //message += "\0";
            byte[] send = message.getBytes();
            message.length();
            String str = new String(send);
            //str += "\0";
            mChatService.write(send);
            Log.e("send message", str);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // if the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus("Connected to " + mConnectedDeviceName);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus("connecting...");
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus("Not connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    // if (!writeMessage.contains("delete") && !writeMessage.contains("notify"))
                        // mConversationArrayAdapter.add(writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String tempMessage = new String(readBuf, 0, msg.arg1);
                    Log.e("full message", tempMessage);
                    String  readMessage[] = tempMessage.split(" ");
                    Log.e("message size", Integer.toString(readMessage.length));
                    outerloop:
                    for(int z = 0; z < readMessage.length; z++) {
                        Log.e("message read", readMessage[z]);
                        if (readMessage[z].contains("deleteAll") && !isHost) {
                            mConversationArrayAdapter.clear();
                        } else if (readMessage[z].contains("delete")) {
                            mConversationArrayAdapter.remove(readMessage[z].replace("delete ", ""));
                        } else if (readMessage[z].contains("notify")) {
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Notice")
                                    .setMessage("You have been removed from the queue.")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Toast.makeText(getContext(), "Thanks for shopping!", Toast.LENGTH_LONG);
                                            getActivity().finishAffinity();
                                        }
                                    }).show();
                        } else if (readMessage[z].contains("fifo")) {
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Notice")
                                    .setMessage("You are at the front of the line.\nPlease proceed to the register.")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                        }
                                    }).show();
                        } else {
                            for (int i = 0; i < mConversationArrayAdapter.getCount(); i++) {
                                String s = mConversationArrayAdapter.getItem(i);
                                Log.e("s added: ", s);
                                Log.e("readMessage added: ", readMessage[z]);

                                if (s.equals(readMessage[z])) {    // don't add duplicate customer names
                                    break outerloop;
                                }
                            }
                            if (!isHost) {
                                peopleAheadCount.setText("There are " + String.valueOf(mConversationArrayAdapter.getCount()) + " people ahead of you in line.");
                                eta.setText(String.valueOf("ETA: " + AVERAGE_WAIT_TIME_PER_PERSON * mConversationArrayAdapter.getCount()) + " minutes");
                            }
                            if (isHost) {
                                customerMap.put(mConnectedDeviceName, readMessage[z]);
                            }
                            mConversationArrayAdapter.add(readMessage[z]);

                        }
                    }

                    if (isHost)
                            updateDevices();
                    if (!isHost){
                        for(int i = 0; i < mConversationArrayAdapter.getCount(); i++){
                            if(mUserName.equals(mConversationArrayAdapter.getItem(i))) {
                                position = i;
                            }
                        }
                        peopleAheadCount.setText("There are " + Integer.toString(position) + " people ahead of you in line.");
                        eta.setText(String.valueOf("ETA: " + AVERAGE_WAIT_TIME_PER_PERSON * position) + " minutes");
                    }

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), "Bluetooth not enabled",
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

       // if (device.getName().equals(mConnectedDeviceName))
         //   return;
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    public void connectDevice(String data, boolean secure) {
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(data);

        String s = device.getName();
        //Log.e("Connected to : ", s);
       // if (s.equals(mConnectedDeviceName)) {
        //    return;
        //}
        // Attempt to connect to the device
        mChatService.connect(device, secure);
        Log.e("Connected to : ", s);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
        }
        return false;
    }

}
