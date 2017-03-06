package djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import java.nio.charset.Charset;
import java.util.List;
import java.util.WeakHashMap;

import static djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth.BluetoothType.BLE;
import static djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth.BluetoothType.CLASSIC;

public class Bluetooth {
    public static final int REQUEST_ENABLE_BT = 1;
    public enum BluetoothType {CLASSIC, BLE}

    public interface DiscoveryCallback {
        void onDeviceFound(int requestCode, final BluetoothDevice device);
        void onPair(int requestCode, final BluetoothDevice device);
        void onUnpair(int requestCode, final BluetoothDevice device);
        void onErrorDiscovery(int requestCode, final String message);
        void onFinishDiscovery(int requestCode);
    }

    public interface CommunicationCallback {
        void onConnected(int requestCode, BluetoothDevice device);
        void onDisconnected(int requestCode);
        void onMessage(int requestCode, String message);
        void onErrorCommunication(int requestCode, String message);
        void onConnectError(int requestCode, String message);
    }

    private final BluetoothType mBluetoothType;
    private final Activity mActivity;
    private final BluetoothAdapter mBluetoothAdapter;
    private final BluetoothClassic mBluetoothClassic;
    private final BluetoothLeUart mBluetoothBle;
    private final int mId;

    private WeakHashMap<DiscoveryCallback, Object> mDiscoveryCallbacks;
    private WeakHashMap<CommunicationCallback, Object> mCommunicationCallbacks;

    public Bluetooth(Activity activity, BluetoothType bluetoothType, int id) {
        this.mDiscoveryCallbacks = new WeakHashMap<DiscoveryCallback, Object>();
        this.mCommunicationCallbacks = new WeakHashMap<CommunicationCallback, Object>();
        this.mActivity = activity;
        this.mBluetoothType = bluetoothType;
        this.mId = id;
        BluetoothManager bluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = bluetoothManager.getAdapter();
        this.mBluetoothClassic = new BluetoothClassic(activity);
        this.mBluetoothBle = new BluetoothLeUart(activity);
        requestEnableBluetooth();
    }

    private void requestEnableBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void send(String msg){
        switch (mBluetoothType) {
            case CLASSIC:
                mBluetoothClassic.send(msg);
                break;
            case BLE:
                mBluetoothBle.send(msg);
                break;
        }
    }

    public void connectToAddress(String address) {
        switch (mBluetoothType) {
            case CLASSIC:
                mBluetoothClassic.connectToAddress(address);
                break;
            case BLE:
                mBluetoothBle.connectAddress(address);
                break;
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        switch (mBluetoothType) {
            case CLASSIC:
                mBluetoothClassic.connectToDevice(device);
                break;
            case BLE:
                mBluetoothBle.connectAddress(device.getAddress());
                break;
        }
    }

    private static byte[] getBytes(String data) {
        if (data != null && !data.isEmpty()) {
            return data.getBytes(Charset.forName("UTF-8"));
        }
        return "".getBytes(Charset.forName("UTF-8"));
    }

    private static String getString(byte[] data) {
        return new String(data, Charset.forName("UTF-8"));
    }

    public void disconnect() {
        switch (mBluetoothType) {
            case CLASSIC:
                mBluetoothClassic.disconnect();
                break;
            case BLE:
                mBluetoothBle.disconnect();
                break;
        }
    }

    public boolean isConnected(){
        boolean isConnected = false;
        switch (mBluetoothType) {
            case CLASSIC:
                isConnected = mBluetoothClassic.isConnected();
                break;
            case BLE:
                isConnected = mBluetoothBle.isConnected();
                break;
        }
        return isConnected;
    }

    public BluetoothDevice getDevice(){
        BluetoothDevice device = null;
        switch (mBluetoothType) {
            case CLASSIC:
                device = mBluetoothClassic.getDevice();
                break;
            case BLE:
                device = mBluetoothBle.getDevice();
                break;
        }
        return device;
    }

    public List<BluetoothDevice> getPairedDevices() {
        return mBluetoothClassic.getPairedDevices();
    }

    public int getmId() {
        return mId;
    }

    public void scanDevices() {
        switch (mBluetoothType) {
            case CLASSIC:
                mBluetoothClassic.scanDevices();
                break;
            case BLE:
                mBluetoothBle.scanLeDevice(true);
                break;
        }
    }

    public void pair(BluetoothDevice device) {
        switch (mBluetoothType) {
            case CLASSIC:
                mBluetoothClassic.pair(device);
                break;
            case BLE:
                break;
        }
    }

    public void unpair(BluetoothDevice device) {
        switch (mBluetoothType) {
            case CLASSIC:
                mBluetoothClassic.unpair(device);
                break;
            case BLE:
                break;
        }
    }

    public void registerCommunicationCallback(CommunicationCallback communicationCallback) {
        mCommunicationCallbacks.put(communicationCallback, null);
        if (mBluetoothType == CLASSIC)
            mBluetoothClassic.setCommunicationCallbackClassic(mCommunicationCallbackClassic);
        else if (mBluetoothType == BLE)
            mBluetoothBle.registerCommunicationCallback(mCommunicationCallbackBle);
    }

    public void unregisterCommunicationCallback(CommunicationCallback communicationCallback) {
        mCommunicationCallbacks.remove(communicationCallback);
        if (mBluetoothType == CLASSIC)
            mBluetoothClassic.removeCommunicationCallback(mCommunicationCallbackClassic);
        if (mBluetoothType == BLE)
            mBluetoothBle.unregisterCommunicationCallback(mCommunicationCallbackBle);
    }

    public void registerDiscoveryCallback(DiscoveryCallback discoveryCallback) {
        mDiscoveryCallbacks.put(discoveryCallback, null);
        if (mBluetoothType == CLASSIC)
            mBluetoothClassic.setDiscoveryCallbackClassic(mDiscoveryCallbackClassic);
        if (mBluetoothType == BLE)
            mBluetoothBle.registerDiscoveryCallback(mDiscoveryCallbackBle);
    }

    public void unregisterDiscoveryCallback(DiscoveryCallback discoveryCallback) {
        mDiscoveryCallbacks.remove(discoveryCallback);
        if (mBluetoothType == CLASSIC)
            mBluetoothClassic.removeDiscoveryCallback(mDiscoveryCallbackClassic);
        else if (mBluetoothType == BLE)
            mBluetoothBle.unregisterDiscoveryCallback(mDiscoveryCallbackBle);
    }

    /* Discovery Callback Wrappers */
    private void notifyDeviceFound(int requestCode, final BluetoothDevice device) {
        for (DiscoveryCallback dcb : mDiscoveryCallbacks.keySet()) {
            if (dcb != null) {
                dcb.onDeviceFound(requestCode, device);
            }
        }
    }
    private void notifyPair(int requestCode, final BluetoothDevice device) {
        for (DiscoveryCallback dcb : mDiscoveryCallbacks.keySet()) {
            if (dcb != null) {
                dcb.onPair(requestCode, device);
            }
        }
    }
    private void notifyUnpair(int requestCode, final BluetoothDevice device) {
        for (DiscoveryCallback dcb : mDiscoveryCallbacks.keySet()) {
            if (dcb != null) {
                dcb.onUnpair(requestCode, device);
            }
        }
    }
    private void notifyDiscoveryError(int requestCode, final String message) {
        for (DiscoveryCallback dcb : mDiscoveryCallbacks.keySet()) {
            if (dcb != null) {
                dcb.onErrorDiscovery(requestCode, message);
            }
        }
    }
    private void notifyFinish(int requestCode) {
        for (DiscoveryCallback dcb : mDiscoveryCallbacks.keySet()) {
            if (dcb != null) {
                dcb.onFinishDiscovery(requestCode);
            }
        }
    }

    /* Communication Callback Wrappers */
    public void notifyConnected(int requestCode, BluetoothDevice device) {
        for (CommunicationCallback ccb : mCommunicationCallbacks.keySet()) {
            if (ccb != null) {
                ccb.onConnected(requestCode, device);
            }
        }
    }
    public void notifyDisconnected(int requestCode) {
        for (CommunicationCallback ccb : mCommunicationCallbacks.keySet()) {
            if (ccb != null) {
                ccb.onDisconnected(requestCode);
            }
        }
    }
    public void notifyMessage(int requestCode, String message) {
        for (CommunicationCallback ccb : mCommunicationCallbacks.keySet()) {
            if (ccb != null) {
                ccb.onMessage(requestCode, message);
            }
        }
    }
    public void notifyCommunicationError(int requestCode, String message) {
        for (CommunicationCallback ccb : mCommunicationCallbacks.keySet()) {
            if (ccb != null) {
                ccb.onErrorCommunication(requestCode, message);
            }
        }
    }
    public void notifyConnectError(int requestCode, String message) {
        for (CommunicationCallback ccb : mCommunicationCallbacks.keySet()) {
            if (ccb != null) {
                ccb.onConnectError(requestCode, message);
            }
        }
    }

    private final BluetoothClassic.CommunicationCallbackClassic mCommunicationCallbackClassic =
            new BluetoothClassic.CommunicationCallbackClassic() {
                @Override
                public void onConnect(BluetoothDevice device) {
                    notifyConnected(mId, device);
                }

                @Override
                public void onDisconnect(BluetoothDevice device, String message) {
                    notifyDisconnected(mId);
                }

                @Override
                public void onMessage(BluetoothDevice device, String message) {
                   notifyMessage(mId, message);
                }

                @Override
                public void onErrorCommunication(BluetoothDevice device, String message) {
                    notifyCommunicationError(mId, message);
                }

                @Override
                public void onConnectError(BluetoothDevice device, String message) {
                    notifyConnectError(mId, message);
                }
            };

    private final BluetoothClassic.DiscoveryCallbackClassic mDiscoveryCallbackClassic =
            new BluetoothClassic.DiscoveryCallbackClassic() {
                @Override
                public void onFinish() {
                    notifyFinish(mId);
                }

                @Override
                public void onDevice(BluetoothDevice device) {
                    notifyDeviceFound(mId, device);
                }

                @Override
                public void onPair(BluetoothDevice device) {
                    notifyPair(mId, device);
                }

                @Override
                public void onUnpair(BluetoothDevice device) {
                    notifyUnpair(mId, device);
                }

                @Override
                public void onErrorDiscovery(String message) {
                    notifyDiscoveryError(mId, message);
                }
            };

    private final BluetoothLeUart.CommunicationCallbackBle mCommunicationCallbackBle = new BluetoothLeUart.CommunicationCallbackBle() {
        @Override
        public void onConnected(BluetoothLeUart uart, BluetoothDevice device) {
            notifyConnected(mId, device);
        }

        @Override
        public void onConnectFailed(BluetoothLeUart uart, String message) {
            notifyConnectError(mId, message);
        }

        @Override
        public void onDisconnected(BluetoothLeUart uart) {
            mBluetoothBle.close();
            notifyDisconnected(mId);
        }

        @Override
        public void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
            notifyMessage(mId, getString(rx.getValue()));
        }

        @Override
        public void onDeviceInfoAvailable() {

        }
    };

    private final BluetoothLeUart.DiscoveryCallbackBle mDiscoveryCallbackBle = new BluetoothLeUart.DiscoveryCallbackBle() {
        @Override
        public void onDeviceFound(BluetoothDevice device) {
            notifyDeviceFound(mId, device);
        }

        @Override
        public void onDiscoveryFinished() {
            notifyFinish(mId);
        }
    };
}
