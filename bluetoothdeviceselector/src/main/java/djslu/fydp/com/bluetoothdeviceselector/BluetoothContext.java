package djslu.fydp.com.bluetoothdeviceselector;

import android.app.Application;

public class BluetoothContext extends Application{
    private BluetoothHolder mBluetoothHolder;

    public BluetoothHolder getBluetoothHolder(){
        return mBluetoothHolder;
    }
    public void setBluetoothHolder(BluetoothHolder s){
        mBluetoothHolder = s;
    }
}
