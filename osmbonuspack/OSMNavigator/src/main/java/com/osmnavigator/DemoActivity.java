package com.osmnavigator;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import djslu.fydp.com.bluetoothdeviceselector.BluetoothContext;
import djslu.fydp.com.bluetoothdeviceselector.BluetoothHolder;
import djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth;
import djslu.fydp.com.bluetoothdeviceselector.Constants;

public class DemoActivity extends Activity implements Bluetooth.CommunicationCallback {

    private Bluetooth mBtBSD;
    private Bluetooth mBtRightNav;
    private Bluetooth mBtLeftNav;
    private TextView mTextView;
    private Button mLeftVibrationButton;
    private Button mRightVibrationButton;
    private EditText mEditText;
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

        mEditText = (EditText)findViewById(R.id.intensity);
        mTextView = (TextView) findViewById(R.id.debug_text);

        mTextView.setText("Text");

        // Initialize the left vibration button listener
        mLeftVibrationButton = (Button)findViewById(R.id.left_vibration);
        mLeftVibrationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String text = mEditText.getText().toString();
                if(mBtLeftNav.isConnected() && !text.isEmpty()) {
                    // Change the intensity
                    mLeftVibrationIntensity = MapActivity.VibrationIntensity.values()[Integer.parseInt(text)];
                    mBtLeftNav.send(text);
                }
            }
        });

        // Initialize the right vibration button listener
        mRightVibrationButton = (Button)findViewById(R.id.right_vibration);
        mRightVibrationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String text = mEditText.getText().toString();
                if(mBtRightNav.isConnected() && !text.isEmpty()) {
                    // Change the intensity
                    mRightVibrationIntensity = MapActivity.VibrationIntensity.values()[Integer.parseInt(text)];
                    mBtRightNav.send(text);
                }
            }
        });
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
//            mTextView.setText(String.valueOf(intensity));

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
}
