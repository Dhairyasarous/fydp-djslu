package djslu.fydp.com.bluetoothdeviceselector;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth;
import djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth.CommunicationCallback;

import static djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth.BluetoothType.BLE;
import static djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth.BluetoothType.CLASSIC;
import static djslu.fydp.com.bluetoothdeviceselector.BluetoothSelectorActivity.DeviceType.BSD;
import static djslu.fydp.com.bluetoothdeviceselector.BluetoothSelectorActivity.DeviceType.LEFT_NAV;
import static djslu.fydp.com.bluetoothdeviceselector.BluetoothSelectorActivity.DeviceType.RIGHT_NAV;
import static djslu.fydp.com.bluetoothdeviceselector.ScanActivity.TAG_BT_ADDRESS;
import static djslu.fydp.com.bluetoothdeviceselector.ScanActivity.TAG_BT_TYPE;

public class BluetoothSelectorActivity extends AppCompatActivity implements ImageButton.OnClickListener, CommunicationCallback {
    public static final int REQUEST_ENABLE_BT = 51;
    public static final int REQUEST_PERMISSIONS = 52;
    public static final int REQUEST_SCAN_DEVICE_BSD = 53;
    public static final int REQUEST_SCAN_DEVICE_RIGHT_NAV = 54;
    public static final int REQUEST_SCAN_DEVICE_LEFT_NAV = 55;

    public static final int BT_ID_BSD = 1;
    public static final int BT_ID_LEFT_NAV = 2;
    public static final int BT_ID_RIGHT_NAV = 3;

    private Bluetooth mBtBSD, mBtRightNAV, mBtLeftNAV;
    private ImageButton mImageViewBSD, mImageViewRightNav, mImageViewLeftNav;
    private TextView mTextViewBSD, mTextViewRightNav, mTextViewLeftNav;

    @Override
    public void onClick(View view) {
        Intent i = new Intent(BluetoothSelectorActivity.this, ScanActivity.class);
        if (view.getId() == mImageViewBSD.getId()) {
            if (mBtBSD.isConnected()) {
                mBtBSD.disconnect();
                setText(mTextViewBSD, getString(R.string.status_disconnecting));
            } else {
                i.putExtra(TAG_BT_TYPE, BLE.name());
                startActivityForResult(i, REQUEST_SCAN_DEVICE_BSD);
            }
        } else if (view.getId() == mImageViewRightNav.getId()) {
            if (mBtRightNAV.isConnected()) {
                mBtRightNAV.disconnect();
                setText(mTextViewRightNav, getString(R.string.status_disconnecting));

            } else {
                i.putExtra(TAG_BT_TYPE, CLASSIC.name());
                startActivityForResult(i, REQUEST_SCAN_DEVICE_RIGHT_NAV);
            }
        } else if (view.getId() == mImageViewLeftNav.getId()) {
            if (mBtLeftNAV.isConnected()) {
                mBtLeftNAV.disconnect();
                setText(mTextViewLeftNav, getString(R.string.status_disconnecting));
            } else {
                i.putExtra(TAG_BT_TYPE, CLASSIC.name());
                startActivityForResult(i, REQUEST_SCAN_DEVICE_LEFT_NAV);
            }
        }
    }

    @Override
    public void onConnected(int requestCode, BluetoothDevice device) {
        if (requestCode == BT_ID_BSD) {
            handleConnected(BSD, device.getName());
        } else if (requestCode == BT_ID_RIGHT_NAV) {
            handleConnected(RIGHT_NAV, device.getName());
        } else if (requestCode == BT_ID_LEFT_NAV) {
            handleConnected(LEFT_NAV, device.getName());
        }
    }

    @Override
    public void onDisconnected(int requestCode) {
        if (requestCode == BT_ID_BSD) {
            handleDisconnected(BSD);
        } else if (requestCode == BT_ID_RIGHT_NAV) {
            handleDisconnected(RIGHT_NAV);
        } else if (requestCode == BT_ID_LEFT_NAV) {
            handleDisconnected(LEFT_NAV);
        }
    }

    @Override
    public void onMessage(int requestCode, String message) {
        if (requestCode == BT_ID_BSD) ;
        if (requestCode == BT_ID_RIGHT_NAV) ;
        if (requestCode == BT_ID_LEFT_NAV) ;
    }

    @Override
    public void onErrorCommunication(int requestCode, String message) {
        if (requestCode == BT_ID_BSD) {
            handleDisconnected(BSD);
            showToast(message);
        }
        if (requestCode == BT_ID_RIGHT_NAV) {
            handleDisconnected(RIGHT_NAV);
            showToast(message);
        }
        if (requestCode == BT_ID_LEFT_NAV) {
            handleDisconnected(LEFT_NAV);
            showToast(message);
        }
    }

    @Override
    public void onConnectError(int requestCode, String message) {
        onErrorCommunication(requestCode, message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_selector);

        setupBluetooth();
    }

    private void setupBluetooth() {
        this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
        mImageViewBSD = (ImageButton) findViewById(R.id.buttonBSD);
        mImageViewRightNav = (ImageButton) findViewById(R.id.buttonRightNav);
        mImageViewLeftNav = (ImageButton) findViewById(R.id.buttonLeftNav);

        mTextViewBSD = (TextView) findViewById(R.id.textViewBSD);
        mTextViewRightNav = (TextView) findViewById(R.id.textViewRightNav);
        mTextViewLeftNav = (TextView) findViewById(R.id.textViewLeftNav);

        mBtBSD = new Bluetooth(this, BLE, BT_ID_BSD);
        mBtRightNAV = new Bluetooth(this, CLASSIC, BT_ID_RIGHT_NAV);
        mBtLeftNAV = new Bluetooth(this, CLASSIC, BT_ID_LEFT_NAV);

        mBtBSD.registerCommunicationCallback(this);
        mBtRightNAV.registerCommunicationCallback(this);
        mBtLeftNAV.registerCommunicationCallback(this);

        mImageViewBSD.setOnClickListener(this);
        mImageViewRightNav.setOnClickListener(this);
        mImageViewLeftNav.setOnClickListener(this);
    }

    private void handleConnected(DeviceType deviceType, String deviceName) {
        switch (deviceType) {
            case BSD:
                setText(mTextViewBSD, deviceName);
                setDisconnectButton(mImageViewBSD);
                break;
            case RIGHT_NAV:
                setText(mTextViewRightNav, deviceName);
                setDisconnectButton(mImageViewRightNav);
                break;
            case LEFT_NAV:
                setText(mTextViewLeftNav, deviceName);
                setDisconnectButton(mImageViewLeftNav);
                break;
        }
    }

    private void handleDisconnected(DeviceType deviceType) {
        switch (deviceType) {
            case BSD:
                setText(mTextViewBSD, getResources().getString(R.string.status_disconnected));
                setSearchButton(mImageViewBSD);
                break;
            case RIGHT_NAV:
                setText(mTextViewRightNav, getResources().getString(R.string.status_disconnected));
                setSearchButton(mImageViewRightNav);
                break;
            case LEFT_NAV:
                setText(mTextViewLeftNav, getResources().getString(R.string.status_disconnected));
                setSearchButton(mImageViewLeftNav);
                break;
        }
    }

    private void setText(final TextView textView, final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(message);
            }
        });
    }

    private void setSearchButton(final ImageView imageView) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageResource(R.drawable.ic_search_black_24dp);
            }
        });
    }

    private void setDisconnectButton(final ImageView imageView) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageResource(R.drawable.ic_clear_black_24dp);
            }
        });
    }

    private void showToast(final String message) {
        this.runOnUiThread(new Runnable() {
           @Override
           public void run() {
               Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
           }
       });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    System.out.println("Bluetooth Enabled");
                }
                break;
            case REQUEST_PERMISSIONS:
                if (resultCode == RESULT_OK) {
                    System.out.println("Permissions Granted");
                }
                break;
            case REQUEST_SCAN_DEVICE_BSD:
                if (resultCode == RESULT_OK) {
                    String address = data.getExtras().getString(TAG_BT_ADDRESS);
                    mTextViewBSD.setText(getString(R.string.status_connecting));
                    mBtBSD.connectToAddress(address);
                }
                break;
            case REQUEST_SCAN_DEVICE_RIGHT_NAV:
                if (resultCode == RESULT_OK) {
                    String address = data.getExtras().getString(TAG_BT_ADDRESS);
                    mTextViewRightNav.setText(getString(R.string.status_connecting));
                    mBtRightNAV.connectToAddress(address);
                }
                break;
            case REQUEST_SCAN_DEVICE_LEFT_NAV:
                if (resultCode == RESULT_OK) {
                    String address = data.getExtras().getString(TAG_BT_ADDRESS);
                    mTextViewLeftNav.setText(getString(R.string.status_connecting));
                    mBtLeftNAV.connectToAddress(address);
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        BluetoothContext bluetoothContext = (BluetoothContext) getApplicationContext();
        BluetoothHolder bluetoothHolder = new BluetoothHolder(mBtBSD, mBtRightNAV, mBtLeftNAV);
        bluetoothContext.setBluetoothHolder(bluetoothHolder);
        Intent i = new Intent();
        setResult(Activity.RESULT_OK, i);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public enum DeviceType {BSD, RIGHT_NAV, LEFT_NAV}
}
