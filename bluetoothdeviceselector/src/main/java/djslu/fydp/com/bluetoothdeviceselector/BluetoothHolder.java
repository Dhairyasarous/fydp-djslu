package djslu.fydp.com.bluetoothdeviceselector;

import android.support.annotation.NonNull;

import djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth;

public class BluetoothHolder {
    private final Bluetooth mBluetoothBSD, mBluetoothRightNav, mBluetoothLeftNav;

    public BluetoothHolder(@NonNull Bluetooth bsd, @NonNull Bluetooth rNav, @NonNull Bluetooth lNav) {
        this.mBluetoothBSD = bsd;
        this.mBluetoothRightNav = rNav;
        this.mBluetoothLeftNav = lNav;
    }

    public Bluetooth getBluetoothBSD() {
        return mBluetoothBSD;
    }

    public Bluetooth getBluetoothRightNav() {
        return mBluetoothRightNav;
    }

    public Bluetooth getBluetoothLeftNav() {
        return mBluetoothLeftNav;
    }
}
