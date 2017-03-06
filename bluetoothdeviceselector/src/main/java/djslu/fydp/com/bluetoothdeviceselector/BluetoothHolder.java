package djslu.fydp.com.bluetoothdeviceselector;

import djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth;

public class BluetoothHolder {
    private final Bluetooth mBluetoothBSD;
    private final Bluetooth mBluetoothRightNav;
    private final Bluetooth mBluetoothLeftNav;

    public BluetoothHolder(Bluetooth bsd, Bluetooth rNav, Bluetooth lNav) {
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
