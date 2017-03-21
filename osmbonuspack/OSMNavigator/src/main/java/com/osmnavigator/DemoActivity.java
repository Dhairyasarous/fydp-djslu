package com.osmnavigator;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import djslu.fydp.com.bluetoothdeviceselector.BluetoothContext;
import djslu.fydp.com.bluetoothdeviceselector.BluetoothHolder;
import djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth;
import djslu.fydp.com.bluetoothdeviceselector.Constants;

public class DemoActivity extends Activity implements Bluetooth.CommunicationCallback, RadioButton.OnClickListener {

    private Bluetooth mBtBSD;
    private Bluetooth mBtRightNav;
    private Bluetooth mBtLeftNav;
    private Button mLeftVibrationButton;
    private Button mRightVibrationButton;
    public static MapActivity.VibrationIntensity mRightVibrationIntensity;
    public static MapActivity.VibrationIntensity mLeftVibrationIntensity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        /**
         * Bluetooth
         */
        BluetoothContext bluetoothContext = (BluetoothContext) getApplicationContext();
        BluetoothHolder bluetoothHolder = bluetoothContext.getBluetoothHolder();
        mBtBSD = bluetoothHolder.getBluetoothBSD();
        mBtRightNav = bluetoothHolder.getBluetoothRightNav();
        mBtLeftNav = bluetoothHolder.getBluetoothLeftNav();
        mBtBSD.registerCommunicationCallback(DemoActivity.this);
        mBtRightNav.registerCommunicationCallback(DemoActivity.this);
        mBtLeftNav.registerCommunicationCallback(DemoActivity.this);
        RadioButton radioButtonLow = (RadioButton) findViewById(R.id.radioButtonLow);
        RadioButton radioButtonMedium = (RadioButton) findViewById(R.id.radioButtonMedium);
        RadioButton radioButtonHigh = (RadioButton) findViewById(R.id.radioButtonHigh);
        RadioButton radioButtonOff = (RadioButton) findViewById(R.id.radioButtonOff);
        radioButtonLow.setOnClickListener(this);
        radioButtonMedium.setOnClickListener(this);
        radioButtonHigh.setOnClickListener(this);
        radioButtonOff.setOnClickListener(this);

    }

    //region Bluetooth
    @Override
    public void onConnected(int requestCode, BluetoothDevice device) {
        switch (requestCode) {
            case Constants.BT_ID_BSD:
                break;
            case Constants.BT_ID_LEFT_NAV:
                break;
            case Constants.BT_ID_RIGHT_NAV:
                break;
        }
    }

    @Override
    public void onDisconnected(int requestCode) {
        switch (requestCode) {
            case Constants.BT_ID_BSD:
                break;
            case Constants.BT_ID_LEFT_NAV:
                break;
            case Constants.BT_ID_RIGHT_NAV:
                break;
        }
    }

    @Override
    public void onMessage(int requestCode, String message) {
        try {
            // Retrieve the vibration intensity
            int intensity = (int) Double.parseDouble(message);
            MapActivity.VibrationIntensity vibrationIntensity = MapActivity.VibrationIntensity.values()[intensity];

            switch (requestCode) {
                case Constants.BT_ID_BSD:
                    // Send the messages to the handlebars
                    mRightVibrationIntensity = vibrationIntensity;
                    mLeftVibrationIntensity = vibrationIntensity;
//                    mTextView.setText(String.valueOf(intensity));
                    if(mBtLeftNav.isConnected())
                        mBtLeftNav.send(String.valueOf(intensity));

                    if(mBtRightNav.isConnected())
                        mBtRightNav.send(String.valueOf(intensity));

                    break;
                case Constants.BT_ID_LEFT_NAV:
                    break;
                case Constants.BT_ID_RIGHT_NAV:
                    break;
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onErrorCommunication(int requestCode, String message) {
        switch (requestCode) {
            case Constants.BT_ID_BSD:
                break;
            case Constants.BT_ID_LEFT_NAV:
                break;
            case Constants.BT_ID_RIGHT_NAV:
                break;
        }
    }

    @Override
    public void onConnectError(int requestCode, String message) {
        switch (requestCode) {
            case Constants.BT_ID_BSD:
                break;
            case Constants.BT_ID_LEFT_NAV:
                break;
            case Constants.BT_ID_RIGHT_NAV:
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (mBtRightNav.isConnected() && mBtLeftNav.isConnected()) {
            boolean checked = ((RadioButton) view).isChecked();

            // Check which radio button was clicked
            switch(view.getId()) {
                case R.id.radioButtonHigh:
                    if (checked) {
                        mLeftVibrationIntensity = MapActivity.VibrationIntensity.HIGH;
                        mBtLeftNav.send(String.valueOf(MapActivity.VibrationIntensity.HIGH.ordinal()));
                        mBtRightNav.send(String.valueOf(MapActivity.VibrationIntensity.HIGH.ordinal()));
                    }
                    break;

                case R.id.radioButtonMedium:
                    if (checked) {
                        mLeftVibrationIntensity = MapActivity.VibrationIntensity.MEDIUM;
                        mBtLeftNav.send(String.valueOf(MapActivity.VibrationIntensity.MEDIUM.ordinal()));
                        mBtRightNav.send(String.valueOf(MapActivity.VibrationIntensity.MEDIUM.ordinal()));
                    }
                    break;
                case R.id.radioButtonLow:
                    if (checked) {
                        mLeftVibrationIntensity = MapActivity.VibrationIntensity.LOW;
                        mBtLeftNav.send(String.valueOf(MapActivity.VibrationIntensity.LOW.ordinal()));
                        mBtRightNav.send(String.valueOf(MapActivity.VibrationIntensity.LOW.ordinal()));
                    }
                    break;
                case R.id.radioButtonOff:
                    if (checked) {
                        mLeftVibrationIntensity = MapActivity.VibrationIntensity.NONE;
                        mBtLeftNav.send(String.valueOf(MapActivity.VibrationIntensity.NONE.ordinal()));
                        mBtRightNav.send(String.valueOf(MapActivity.VibrationIntensity.NONE.ordinal()));
                    }
                    break;
            }
        }

    }
}
