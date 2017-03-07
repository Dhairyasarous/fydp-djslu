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

    /**
     * Function tries to auto connect to previously connected devices.
     *
     * @param context
     * @return true if previous address were found in shared preferences, otherwise false.
     * Bluetooth.CommunicationCallback.onConnected() will be notified when the devices are
     * connected.
     */
    public static void autoConnect(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PERFS_BLUETOOTH, 0);
        String bluetoothAddressBSD = settings.getString(PERFS_BT_ADDRESS_BSD, "");
        String bluetoothAddressRightNav = settings.getString(PERFS_BT_ADDRESS_RIGHT_NAV, "");
        String bluetoothAddressLeftNav = settings.getString(PERFS_BT_ADDRESS_LEFT_NAV, "");

        BluetoothHolder bluetoothHolder = ((BluetoothContext) context.getApplicationContext()).getBluetoothHolder();

        if (!bluetoothAddressBSD.equals(EMPTY_STRING)) {
            Bluetooth bsd = bluetoothHolder.getBluetoothBSD();
            bsd.connectToAddress(bluetoothAddressBSD);
        }
        if (!bluetoothAddressRightNav.equals(EMPTY_STRING)) {
            Bluetooth rNav = bluetoothHolder.getBluetoothRightNav();
            rNav.connectToAddress(bluetoothAddressRightNav);
        }
        if (!bluetoothAddressLeftNav.equals(EMPTY_STRING)) {
            Bluetooth lNav = bluetoothHolder.getBluetoothLeftNav();
            lNav.connectToAddress(bluetoothAddressLeftNav);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
        autoConnect(getApplicationContext());
    }

    private void initialize() {
        BluetoothContext bluetoothContext = (BluetoothContext) getApplicationContext();
        Bluetooth bsd = new Bluetooth(bluetoothContext, BLE, BT_ID_BSD);
        Bluetooth rightNav = new Bluetooth(bluetoothContext, CLASSIC, BT_ID_RIGHT_NAV);
        Bluetooth leftNav = new Bluetooth(bluetoothContext, CLASSIC, BT_ID_LEFT_NAV);
        BluetoothHolder bluetoothHolder = new BluetoothHolder(bsd, rightNav, leftNav);
        bluetoothContext.setBluetoothHolder(bluetoothHolder);
        Log.d(TAG, "BluetoothContext initialized");
    }

    public boolean hasBluetoothHolder() {
        return mHasBluetoothHolder;
    }

    public BluetoothHolder getBluetoothHolder() {
        return mBluetoothHolder;
    }

    public void setBluetoothHolder(@NonNull BluetoothHolder s) {
        if (s != null) {
            mBluetoothHolder = s;
            mHasBluetoothHolder = true;
        } else {
            mHasBluetoothHolder = false;
        }
    }
}
