package djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Created by Omar on 14/07/2015.
 */
public class BluetoothClassic {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // SPP Service
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private BluetoothDevice device, devicePair;
    private BufferedReader input;
    private OutputStream out;

    private boolean connected=false;
    private WeakHashMap<DiscoveryCallbackClassic, Object> mDiscoveryCallbacksClassic;
    private WeakHashMap<CommunicationCallbackClassic, Object> mCommunicationCallbacksClassic;

    private Context context;

    public BluetoothClassic(Context context){
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mDiscoveryCallbacksClassic = new WeakHashMap<DiscoveryCallbackClassic, Object>();
        this.mCommunicationCallbacksClassic = new WeakHashMap<CommunicationCallbackClassic, Object>();
    }

    public void enableBluetooth(){
        if(bluetoothAdapter!=null) {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        }
    }

    public void disableBluetooth(){
        if(bluetoothAdapter!=null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
            }
        }
    }

    public void connectToAddress(String address) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        new ConnectThread(device).start();
    }

    public void connectToName(String name) {
        for (BluetoothDevice blueDevice : bluetoothAdapter.getBondedDevices()) {
            if (blueDevice.getName().equals(name)) {
                connectToAddress(blueDevice.getAddress());
                return;
            }
        }
    }

    public void connectToDevice(BluetoothDevice device){
        new ConnectThread(device).start();
    }

    public void disconnect() {
        try {
            socket.close();
            notifyDisconnect(device, "Disconnected");
        } catch (IOException e) {
            notifyErrorCommunication(device, e.getMessage());
        }
    }

    public boolean isConnected(){
        return connected;
    }

    public void send(String msg){
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            connected=false;
            notifyErrorCommunication(device, e.getMessage());
        }
    }

    private class ReceiveThread extends Thread implements Runnable {
        public void run(){
            String msg;
            try {
                while ((msg = input.readLine()) != null) {
                    notifyMessage(device, msg);
                }
            } catch (IOException e) {
                connected=false;
                notifyErrorCommunication(device, e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {
        public ConnectThread(BluetoothDevice device) {
            BluetoothClassic.this.device=device;
            try {
                BluetoothClassic.this.socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                notifyErrorCommunication(device, e.getMessage());
            }
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
                out = socket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected=true;

                new ReceiveThread().start();

                notifyConnect(device);
            } catch (IOException e) {
                notifyConnectError(device, e.getMessage());

                try {
                    socket.close();
                } catch (IOException closeException) {
                    notifyErrorCommunication(device, closeException.getMessage());
                }
            }
        }
    }

    public List<BluetoothDevice> getPairedDevices(){
        List<BluetoothDevice> devices = new ArrayList<>();
        for (BluetoothDevice blueDevice : bluetoothAdapter.getBondedDevices()) {
            devices.add(blueDevice);
        }
        return devices;
    }

    public BluetoothSocket getSocket(){
        return socket;
    }

    public BluetoothDevice getDevice(){
        return device;
    }

    public void scanDevices(){
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        context.registerReceiver(mReceiverScan, filter);
        bluetoothAdapter.startDiscovery();
    }

    public void pair(BluetoothDevice device){
        context.registerReceiver(mPairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        devicePair=device;
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            notifyErrorDiscovery(e.getMessage());
        }
    }

    public void unpair(BluetoothDevice device) {
        devicePair=device;
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            notifyErrorDiscovery(e.getMessage());
        }
    }

    private BroadcastReceiver mReceiverScan = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        notifyErrorDiscovery("BluetoothClassic turned off");
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    context.unregisterReceiver(mReceiverScan);
                    notifyFinish();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    notifyDevice(device);
                    break;
            }
        }
    };

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState	= intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    context.unregisterReceiver(mPairReceiver);
                    notifyPair(devicePair);
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    context.unregisterReceiver(mPairReceiver);
                    notifyUnpair(devicePair);
                }
            }
        }
    };

    public void notifyConnect(BluetoothDevice device) {
        for (CommunicationCallbackClassic ccb: mCommunicationCallbacksClassic.keySet()) {
            if (ccb != null) {
                ccb.onConnect(device);
            }
        }
    }
    public void notifyDisconnect(BluetoothDevice device, String message) {
        for (CommunicationCallbackClassic ccb: mCommunicationCallbacksClassic.keySet()) {
            if (ccb != null) {
                ccb.onDisconnect(device, message);
            }
        }
    }
    public void notifyMessage(BluetoothDevice device, String message) {
        for (CommunicationCallbackClassic ccb: mCommunicationCallbacksClassic.keySet()) {
            if (ccb != null) {
                ccb.onMessage(device, message);
            }
        }
    }
    public void notifyErrorCommunication(BluetoothDevice device, String message) {
        for (CommunicationCallbackClassic ccb: mCommunicationCallbacksClassic.keySet()) {
            if (ccb != null) {
                ccb.onErrorCommunication(device, message);
            }
        }
    }
    public void notifyConnectError(BluetoothDevice device, String message) {
        for (CommunicationCallbackClassic ccb: mCommunicationCallbacksClassic.keySet()) {
            if (ccb != null) {
                ccb.onConnectError(device, message);
            }
        }
    }

    public void notifyFinish() {
        for (DiscoveryCallbackClassic dcb: mDiscoveryCallbacksClassic.keySet()) {
            if (dcb != null) {
                dcb.onFinish();
            }
        }
    }
    public void notifyDevice(BluetoothDevice device) {
        for (DiscoveryCallbackClassic dcb: mDiscoveryCallbacksClassic.keySet()) {
            if (dcb != null) {
                dcb.onDevice(device);
            }
        }
    }
    public void notifyPair(BluetoothDevice device) {
        for (DiscoveryCallbackClassic dcb: mDiscoveryCallbacksClassic.keySet()) {
            if (dcb != null) {
                dcb.onPair(device);
            }
        }
    }
    public void notifyUnpair(BluetoothDevice device) {
        for (DiscoveryCallbackClassic dcb: mDiscoveryCallbacksClassic.keySet()) {
            if (dcb != null) {
                dcb.onUnpair(device);
            }
        }
    }
    public void notifyErrorDiscovery(String message) {
        for (DiscoveryCallbackClassic dcb: mDiscoveryCallbacksClassic.keySet()) {
            if (dcb != null) {
                dcb.onErrorDiscovery(message);
            }
        }
    }

    public interface CommunicationCallbackClassic {
        void onConnect(BluetoothDevice device);
        void onDisconnect(BluetoothDevice device, String message);
        void onMessage(BluetoothDevice device, String message);
        void onErrorCommunication(BluetoothDevice device, String message);
        void onConnectError(BluetoothDevice device, String message);
    }

    public interface DiscoveryCallbackClassic {
        void onFinish();
        void onDevice(BluetoothDevice device);
        void onPair(BluetoothDevice device);
        void onUnpair(BluetoothDevice device);
        void onErrorDiscovery(String message);
    }

    public void setCommunicationCallbackClassic(CommunicationCallbackClassic communicationCallbackClassic) {
        mCommunicationCallbacksClassic.put(communicationCallbackClassic, null);
    }

    public void removeCommunicationCallback(CommunicationCallbackClassic communicationCallbackClassic){
        mCommunicationCallbacksClassic.remove(communicationCallbackClassic);
    }

    public void setDiscoveryCallbackClassic(DiscoveryCallbackClassic discoveryCallbackClassic){
        mDiscoveryCallbacksClassic.put(discoveryCallbackClassic, null);
    }

    public void removeDiscoveryCallback(DiscoveryCallbackClassic discoveryCallbackClassic){
        mDiscoveryCallbacksClassic.remove(discoveryCallbackClassic);
    }

}


