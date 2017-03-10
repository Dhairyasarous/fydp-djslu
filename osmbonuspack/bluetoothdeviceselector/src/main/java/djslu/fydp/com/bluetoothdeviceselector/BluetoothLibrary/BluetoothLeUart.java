package djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BluetoothLeUart extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback {
    public final static String TAG = BluetoothLeUart.class.getSimpleName();

    public static final long SCAN_PERIOD = 5000; // 5 Seconds

    // UUIDs for UART service and associated characteristics.
    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID   = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID   = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    // UUID for the UART BTLE client characteristic which is necessary for notifications.
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Internal UART state.
    private Context context;
    private WeakHashMap<CommunicationCallbackBle, Object> communicationCallbacks;
    private WeakHashMap<DiscoveryCallbackBle, Object> discoveryCallbacks;
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;
    private boolean writeInProgress; // Flag to indicate a write is currently in progress

    private String mBluetoothDeviceAddress;
    private BluetoothDevice mDevice;
    private Handler mHandler;
    private boolean mScanning = false;
    private boolean mDisconnectSignal = false;

    private boolean disAvailable;

    // Queues for characteristic read (synchronous)
    private Queue<BluetoothGattCharacteristic> readQueue;

    // Interface for a BluetoothLeUart client to be notified of UART actions.
    public interface CommunicationCallbackBle {
        void onConnected(BluetoothLeUart uart, BluetoothDevice device);
        void onConnectFailed(BluetoothLeUart uart, String message);
        void onDisconnected(BluetoothLeUart uart);
        void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx);
        void onDeviceInfoAvailable();
    }
    public interface DiscoveryCallbackBle {
        void onDeviceFound(BluetoothDevice device);
        void onDiscoveryFinished();
    }

    public BluetoothLeUart(Context context) {
        super();
        this.context = context;
        this.mHandler = new Handler();
        this.communicationCallbacks = new WeakHashMap<CommunicationCallbackBle, Object>();
        this.discoveryCallbacks = new WeakHashMap<DiscoveryCallbackBle, Object>();
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.gatt = null;
        this.tx = null;
        this.rx = null;
        this.disAvailable = false;
        this.writeInProgress = false;
        this.readQueue = new ConcurrentLinkedQueue<BluetoothGattCharacteristic>();
    }

    // Return instance of BluetoothGatt.
    public BluetoothGatt getGatt() {
        return gatt;
    }

    // Return true if connected to UART device, false otherwise.
    public boolean isConnected() {
        return (tx != null && rx != null);
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public boolean deviceInfoAvailable() { return disAvailable; }

    // Send data to connected UART device.
    public void send(byte[] data) {
        if (tx == null || data == null || data.length == 0) {
            // Do nothing if there is no connection or message to send.
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        tx.setValue(data);
        writeInProgress = true; // Set the write in progress flag
        gatt.writeCharacteristic(tx);
        // ToDo: Update to include a timeout in case this goes into the weeds
        while (writeInProgress); // Wait for the flag to clear in onCharacteristicWrite
    }

    // Send data to connected UART device.
    public void send(String data) {
        if (data != null && !data.isEmpty()) {
            send(data.getBytes(Charset.forName("UTF-8")));
        }
    }

    // Register the specified communicationCallbackBle to receive UART communicationCallbacks.
    public void registerCommunicationCallback(CommunicationCallbackBle communicationCallbackBle) {
        communicationCallbacks.put(communicationCallbackBle, null);
    }

    // Unregister the specified communicationCallbackBle.
    public void unregisterCommunicationCallback(CommunicationCallbackBle communicationCallbackBle) {
        communicationCallbacks.remove(communicationCallbackBle);
    }

    // Register the specified communicationCallbackBle to receive UART communicationCallbacks.
    public void registerDiscoveryCallback(DiscoveryCallbackBle discoveryCallbackBle) {
        discoveryCallbacks.put(discoveryCallbackBle, null);
    }

    // Unregister the specified communicationCallbackBle.
    public void unregisterDiscoveryCallback(DiscoveryCallbackBle discoveryCallbackBle) {
        discoveryCallbacks.remove(discoveryCallbackBle);
    }

    // Disconnect to a device if currently connected.
    public void disconnect() {
        mDisconnectSignal = true;
        if (gatt != null) {
            gatt.disconnect();
        }
        mDevice = null;
        gatt = null;
        tx = null;
        rx = null;
    }

    public void close() {
        if (gatt == null) {
            return;
        }
        Log.w(TAG, "mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        gatt.close();
        gatt = null;
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined activity_scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    adapter.stopLeScan(BluetoothLeUart.this);
                    notifyFinishedDiscovery();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            adapter.startLeScan(BluetoothLeUart.this);
        } else {
            mScanning = false;
            adapter.stopLeScan(BluetoothLeUart.this);
            notifyFinishedDiscovery();
        }
    }

    /**
     * Connects to the GATT server hosted on the BluetoothClassic LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connectAddress(final String address) {
        if (adapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && gatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (gatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = adapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connectAddress.");
            return false;
        }
        // We want to directly connectAddress to the device, so we are setting the autoConnect
        // parameter to false.
        mDevice = device;
        mDisconnectSignal = false;
        gatt = device.connectGatt(context, true, this);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    // Handlers for BluetoothGatt and LeScan events.
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Connected to device, start discovering services.
                if (!gatt.discoverServices()) {
                    // Error starting service discovery.
                    connectFailure("Error starting service discovery");
                }
            }
            else {
                // Error connecting to device.
                connectFailure("Error connecting to device");
            }
        }
        else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            // Disconnected, notify communicationCallbacks of disconnection.
            rx = null;
            tx = null;
            mDevice = null;
            notifyDisconnected(this);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        // Notify connection failure if service discovery failed.
        if (status == BluetoothGatt.GATT_FAILURE) {
            connectFailure("Error service discovery failed");
            return;
        }

        // Save reference to each UART characteristic.
        tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
        rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);

        // Setup notifications on RX characteristic changes (i.e. data received).
        // First call setCharacteristicNotification to enable notification.
        if (!gatt.setCharacteristicNotification(rx, true)) {
            // Stop if the characteristic notification setup failed.
            connectFailure("Error characteristic notification setup failed");
            return;
        }
        // Next update the RX characteristic's client descriptor to enable notifications.
        BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
        if (desc == null) {
            // Stop if the RX characteristic has no client descriptor.
            connectFailure("Error RX characteristic has no client descriptor");
            return;
        }
        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(desc)) {
            // Stop if the client descriptor could not be written.
            connectFailure("Error client descriptor could not be written");
            return;
        }
        // Notify of connection completion.
        if (!mDisconnectSignal)
            notifyConnected(this, gatt.getDevice());
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        notifyReceive(this, characteristic);
    }

    @Override
    public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            //Log.w("DIS", characteristic.getStringValue(0));
            // Check if there is anything left in the queue
            BluetoothGattCharacteristic nextRequest = readQueue.poll();
            if(nextRequest != null){
                // Send a read request for the next item in the queue
                gatt.readCharacteristic(nextRequest);
            }
            else {
                // We've reached the end of the queue
                disAvailable = true;
                notifyDeviceInfoAvailable();
            }
        }
        else {
            //Log.w("DIS", "Failed reading characteristic " + characteristic.getUuid().toString());
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            // Log.d(TAG,"Characteristic write successful");
        }
        writeInProgress = false;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // Stop if the device doesn't have the UART service.
        if (!parseUUIDs(scanRecord).contains(UART_UUID)) {
            notifyFinishedDiscovery();
            return;
        }
        // Notify registered communicationCallbacks of found device.
        notifyDeviceFound(device);
    }

    // Private functions to simplify the notification of all communicationCallbacks of a certain event.
    private void notifyConnected(BluetoothLeUart uart, BluetoothDevice device) {
        for (CommunicationCallbackBle cb : communicationCallbacks.keySet()) {
            if (cb != null) {
                cb.onConnected(uart, device);
            }
        }
    }

    private void notifyConnectFailed(BluetoothLeUart uart, String message) {
        for (CommunicationCallbackBle cb : communicationCallbacks.keySet()) {
            if (cb != null) {
                cb.onConnectFailed(uart, message);
            }
        }
    }

    private void notifyDisconnected(BluetoothLeUart uart) {
        for (CommunicationCallbackBle cb : communicationCallbacks.keySet()) {
            if (cb != null) {
                cb.onDisconnected(uart);
            }
        }
    }

    private void notifyReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
        for (CommunicationCallbackBle cb : communicationCallbacks.keySet()) {
            if (cb != null ) {
                cb.onReceive(uart, rx);
            }
        }
    }

    private void notifyDeviceFound(BluetoothDevice device) {
        for (DiscoveryCallbackBle cb : discoveryCallbacks.keySet()) {
            if (cb != null) {
                cb.onDeviceFound(device);
            }
        }
    }

    private void notifyDeviceInfoAvailable() {
        for (CommunicationCallbackBle cb : communicationCallbacks.keySet()) {
            if (cb != null) {
                cb.onDeviceInfoAvailable();
            }
        }
    }

    private void notifyFinishedDiscovery() {
        for (DiscoveryCallbackBle cb : discoveryCallbacks.keySet()) {
            if (cb != null) {
                cb.onDiscoveryFinished();
            }
        }
    }

    // Notify communicationCallbacks of connection failure, and reset connection state.
    private void connectFailure(String message) {
        rx = null;
        tx = null;
        notifyConnectFailed(this, message);
    }

    // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
    //   http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
    // This is a workaround function from the SO thread to manually parse advertisement data.
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            //Log.e(LOG_TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }
        return uuids;
    }
}