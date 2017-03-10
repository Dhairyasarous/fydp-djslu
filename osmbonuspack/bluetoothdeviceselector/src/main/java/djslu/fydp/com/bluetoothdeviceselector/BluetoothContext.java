package djslu.fydp.com.bluetoothdeviceselector;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth;

import static djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth.BluetoothType.BLE;
import static djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth.BluetoothType.CLASSIC;
import static djslu.fydp.com.bluetoothdeviceselector.Constants.BT_ID_BSD;
import static djslu.fydp.com.bluetoothdeviceselector.Constants.BT_ID_LEFT_NAV;
import static djslu.fydp.com.bluetoothdeviceselector.Constants.BT_ID_RIGHT_NAV;
import static djslu.fydp.com.bluetoothdeviceselector.Constants.EMPTY_STRING;
import static djslu.fydp.com.bluetoothdeviceselector.Constants.PERFS_BLUETOOTH;
import static djslu.fydp.com.bluetoothdeviceselector.Constants.PERFS_BT_ADDRESS_BSD;
import static djslu.fydp.com.bluetoothdeviceselector.Constants.PERFS_BT_ADDRESS_LEFT_NAV;
import static djslu.fydp.com.bluetoothdeviceselector.Constants.PERFS_BT_ADDRESS_RIGHT_NAV;

public class BluetoothContext extends Application {
    private static final String TAG = BluetoothContext.class.getSimpleName();

    private BluetoothHolder mBluetoothHolder;
    private boolean mHasBluetoothHolder = false;
    private boolean mIsBluetoothContextInitialized = false;
    private Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    /**
     * Function tries to auto connect to previously connected devices if a Bluetooth object
     * is not currently connected to another device. Bluetooth.CommunicationCallback.onConnected()
     * will be notified when the devices are connected.
     */
    public void autoConnect() {
        SharedPreferences settings = mContext.getSharedPreferences(PERFS_BLUETOOTH, 0);
        String bluetoothAddressBSD = settings.getString(PERFS_BT_ADDRESS_BSD, "");
        String bluetoothAddressRightNav = settings.getString(PERFS_BT_ADDRESS_RIGHT_NAV, "");
        String bluetoothAddressLeftNav = settings.getString(PERFS_BT_ADDRESS_LEFT_NAV, "");

        BluetoothHolder bluetoothHolder = ((BluetoothContext) mContext).getBluetoothHolder();
        Bluetooth bsd = bluetoothHolder.getBluetoothBSD();
        Bluetooth rNav = bluetoothHolder.getBluetoothRightNav();
        Bluetooth lNav = bluetoothHolder.getBluetoothLeftNav();

        if (!bluetoothAddressBSD.equals(EMPTY_STRING) && !bsd.isConnected()) {
            bsd.connectToAddress(bluetoothAddressBSD);
        }
        if (!bluetoothAddressRightNav.equals(EMPTY_STRING) && !rNav.isConnected()) {
            rNav.connectToAddress(bluetoothAddressRightNav);
        }
        if (!bluetoothAddressLeftNav.equals(EMPTY_STRING) && !lNav.isConnected()) {
            lNav.connectToAddress(bluetoothAddressLeftNav);
        }
    }

    /**
     * This function will only ever initialize once.
     */
    public void initialize() {
        if (!mIsBluetoothContextInitialized) {
            BluetoothContext bluetoothContext = (BluetoothContext) mContext;
            Bluetooth bsd = new Bluetooth(bluetoothContext, BLE, BT_ID_BSD);
            Bluetooth rightNav = new Bluetooth(bluetoothContext, CLASSIC, BT_ID_RIGHT_NAV);
            Bluetooth leftNav = new Bluetooth(bluetoothContext, CLASSIC, BT_ID_LEFT_NAV);
            BluetoothHolder bluetoothHolder = new BluetoothHolder(bsd, rightNav, leftNav);
            bluetoothContext.setBluetoothHolder(bluetoothHolder);
            mIsBluetoothContextInitialized = true;
            Log.d(TAG, "BluetoothContext initialized");
        }
    }

    public boolean hasBluetoothHolder() {
        return mHasBluetoothHolder;
    }

    public boolean isBluetoothContextInitialized() {
        return mIsBluetoothContextInitialized;
    }

    public BluetoothHolder getBluetoothHolder() {
        return mBluetoothHolder;
    }

    public void setBluetoothHolder(@NonNull BluetoothHolder bluetoothHolder) {
        mBluetoothHolder = bluetoothHolder;
        mHasBluetoothHolder = true;
    }
}
