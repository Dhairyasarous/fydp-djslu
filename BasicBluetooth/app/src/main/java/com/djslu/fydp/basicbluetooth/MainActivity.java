package com.djslu.fydp.basicbluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.UnsupportedEncodingException;

public class MainActivity extends Activity {
    public enum FydpDevices { ULTRASONIC, LEFT_NAV, RIGHT_NAV }
    public static final String FYDP_DEVICE_TAG = "FYDP_DEVICE";
    public static final String TAG = "BasicBluetooth";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    private int mState = UART_PROFILE_DISCONNECTED;
    private int mState2 = UART_PROFILE_DISCONNECTED;
    private int mState3 = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private UartService2 mService2 = null;
    private BluetoothDevice mDevice = null;
    private BluetoothDevice mDevice2 = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect1, btnVibrate1, btnConnectDisconnect2, btnConnectDisconnect3, btnVibrate2;
    private TextView textViewDistance;
    private LineChart mChart;
    private float distance = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnConnectDisconnect1 =(Button) findViewById(R.id.buttonConnect1);
        btnVibrate1 = (Button) findViewById(R.id.buttonVibrate1);
        btnConnectDisconnect2 =(Button) findViewById(R.id.buttonConnect2);
        btnVibrate2 = (Button) findViewById(R.id.buttonVibrate2);
        btnConnectDisconnect3 =(Button) findViewById(R.id.buttonConnect3);
        textViewDistance = (TextView) findViewById(R.id.textViewDistance);
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.setData(new LineData());
        mChart.setDescription("");
        mChart.getAxisLeft().setDrawGridLines(true);
        mChart.getAxisRight().setEnabled(false);
        mChart.getXAxis().setDrawAxisLine(false);
        mChart.getXAxis().setEnabled(true);
        mChart.getXAxis().setDrawGridLines(true);
        mChart.getLegend().setEnabled(false);
        for (int i = 0; i < 100; i++) addEntry(0);
        service_init();

        // Handle Disconnect & Connect button
        btnConnectDisconnect1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    if (btnConnectDisconnect1.getText().equals("Connect")){
                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        newIntent.putExtra(FYDP_DEVICE_TAG, FydpDevices.RIGHT_NAV.name());
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice!=null) mService.disconnect();
                    }
                }
            }
        });

        // WRITE
        btnVibrate1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "1";
                byte[] value;
                try {
                    //send data to service
                    value = message.getBytes("UTF-8");
                    mService.writeRXCharacteristic(value);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        // Handle Disconnect & Connect button
        btnConnectDisconnect2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    if (btnConnectDisconnect2.getText().equals("Connect")){
                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        newIntent.putExtra(FYDP_DEVICE_TAG, FydpDevices.LEFT_NAV.name());
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice2!=null) mService2.disconnect();
                    }
                }
            }
        });

        // WRITE
        btnVibrate2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "1";
                byte[] value;
                try {
                    //send data to service
                    value = message.getBytes("UTF-8");
                    mService2.writeRXCharacteristic(value);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String broadcaster = intent.getStringExtra("className");
            String className = UartService.TAG;
            if (broadcaster != null && broadcaster.equals(className)) {
                if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.d(TAG, "UART_CONNECT_MSG");
                            btnConnectDisconnect1.setText("Disconnect");
                            btnVibrate1.setEnabled(true);
                            ((TextView) findViewById(R.id.deviceNameRNav)).setText(mDevice.getName() + " - ready");
                            mState = UART_PROFILE_CONNECTED;
                        }
                    });
                }
                if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.d(TAG, "UART_DISCONNECT_MSG");
                            btnConnectDisconnect1.setText("Connect");
                            btnVibrate1.setEnabled(false);
                            ((TextView) findViewById(R.id.deviceNameRNav)).setText("Not Connected");
                            mState = UART_PROFILE_DISCONNECTED;
                            mService.close();
                            //setUiState();
                        }
                    });
                }
                if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                    mService.enableTXNotification();
                }
                // READ
                if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                    final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                String text = new String(txValue, "UTF-8");
                                distance = filter(Float.parseFloat(text), (float) 0.2);
                                addEntry(distance);
                                textViewDistance.setText(text);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                }
                if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                    showMessage("Device doesn't support UART. Disconnecting");
                    mService.disconnect();
                }
            }
        }
    };

    BroadcastReceiver UARTStatusChangeReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String broadcaster = intent.getStringExtra("className");
            String className = UartService2.TAG;
            if (broadcaster != null && broadcaster.equals(className)) {


                if (action.equals(UartService2.ACTION_GATT_CONNECTED)) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.d(TAG, "UART_CONNECT_MSG");
                            btnConnectDisconnect2.setText("Disconnect");
                            btnVibrate2.setEnabled(true);
                            ((TextView) findViewById(R.id.deviceNameLNav)).setText(mDevice2.getName() + " - ready");
                            mState2 = UART_PROFILE_CONNECTED;
                        }
                    });
                }
                if (action.equals(UartService2.ACTION_GATT_DISCONNECTED)) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.d(TAG, "UART_DISCONNECT_MSG");
                            btnConnectDisconnect2.setText("Connect");
                            btnVibrate2.setEnabled(false);
                            ((TextView) findViewById(R.id.deviceNameLNav)).setText("Not Connected");
                            mState2 = UART_PROFILE_DISCONNECTED;
                            mService2.close();
                            //setUiState();
                        }
                    });
                }
                if (action.equals(UartService2.ACTION_GATT_SERVICES_DISCOVERED))
                    mService2.enableTXNotification();

                // READ
                if (action.equals(UartService2.ACTION_DATA_AVAILABLE)) {
                    final byte[] txValue = intent.getByteArrayExtra(UartService2.EXTRA_DATA);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                String text = new String(txValue, "UTF-8");
                                distance = filter(Float.parseFloat(text), (float) 0.2);
                                addEntry(distance);
                                textViewDistance.setText(text);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                }
                if (action.equals(UartService2.DEVICE_DOES_NOT_SUPPORT_UART)) {
                    showMessage("Device doesn't support UART. Disconnecting");
                    mService2.disconnect();
                }
            }
        }
    };

    private float filter(float unfilteredDistance, float alpha) {
        float filteredDistance = 0;
        filteredDistance = distance + alpha * (unfilteredDistance - distance);
        return filteredDistance;
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Random Data");
        set.setColor(Color.BLACK);
        set.setLineWidth(0.5f);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setDrawFilled(false);
        return set;
    }

    private void addEntry(float value) {
        LineData data = mChart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), value), 0);
            data.notifyDataChanged();
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(100);
            mChart.moveViewToX(data.getEntryCount());
        }
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }
        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };

    private ServiceConnection mServiceConnection2 = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService2 = ((UartService2.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService2);
            if (!mService2.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }
        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService2 = null;
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    private static IntentFilter makeGattUpdateIntentFilter2() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService2.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService2.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService2.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService2.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService2.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        Intent bindIntent2 = new Intent(this, UartService2.class);
        bindService(bindIntent2, mServiceConnection2, Context.BIND_AUTO_CREATE);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver2, makeGattUpdateIntentFilter2());
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onStart() {
        super.onStart();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver2);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        unbindService(mServiceConnection2);
        mService.stopSelf();
        mService2.stopSelf();
        mService = null;
        mService2 = null;

    }
    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }
    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    String fydpDevice = data.getStringExtra(FYDP_DEVICE_TAG);
                    Log.d(FYDP_DEVICE_TAG, fydpDevice);
                    if (fydpDevice.equals(FydpDevices.RIGHT_NAV.name())) {
                        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                        Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                        ((TextView) findViewById(R.id.deviceNameRNav)).setText(String.format("%s - connecting", mDevice.getName()));
                        mService.connect(deviceAddress);
                    }
                    else if (fydpDevice.equals(FydpDevices.LEFT_NAV.name())) {
                        mDevice2 = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                        Log.d(TAG, "... onActivityResultdevice.address==" + mDevice2 + "mserviceValue" + mService2);
                        ((TextView) findViewById(R.id.deviceNameLNav)).setText(String.format("%s - connecting", mDevice2.getName()));
                        mService2.connect(deviceAddress);
                    }
                    else if (fydpDevice.equals(FydpDevices.ULTRASONIC.name())) {
                        mDevice2 = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                        Log.d(TAG, "... onActivityResultdevice.address==" + mDevice2 + "mserviceValue" + mService2);
                        ((TextView) findViewById(R.id.deviceNameLNav)).setText(String.format("%s - connecting", mDevice2.getName()));
                        mService2.connect(deviceAddress);
                    }
                    else {
                        showMessage("Connection Failed. Try connecting again.");
                    }
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }
    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else if (mState2 == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }
}
