package djslu.fydp.com.bluetoothdeviceselector;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth;
import djslu.fydp.com.bluetoothdeviceselector.BluetoothLibrary.Bluetooth.DiscoveryCallback;

public class ScanActivity extends Activity implements AdapterView.OnItemClickListener{
    public static final String TAG_BT_TYPE = "BLUETOOTH_TYPE";
    public static final String TAG_BT_POS = "BLUETOOTH_POSITION";
    public static final String TAG_BT_ADDRESS = "BLUETOOTH_ADDRESS";
    public static final int BT_ID_GENERIC = 4;

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private TextView state;
    private ProgressBar progress;
    private Button scan;
    private List<BluetoothDevice> devices;
    private Bluetooth bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        listView = (ListView)findViewById(R.id.scan_list);
        state = (TextView) findViewById(R.id.scan_state);
        progress = (ProgressBar) findViewById(R.id.scan_progress);
        scan = (Button) findViewById(R.id.scan_scan_again);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);


        final Bluetooth.BluetoothType type
                = Bluetooth.BluetoothType.valueOf(getIntent().getExtras().getString(TAG_BT_TYPE));
        bluetooth = new Bluetooth(this, type, BT_ID_GENERIC);
        bluetooth.registerDiscoveryCallback(mDiscoveryCallback);
        bluetooth.scanDevices();
        progress.setVisibility(View.VISIBLE);
        state.setText("Scanning...");
//        listView.setEnabled(false);

        scan.setEnabled(false);
        devices = new ArrayList<>();

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.clear();
                        scan.setEnabled(false);
                    }
                });

                devices = new ArrayList<>();
                progress.setVisibility(View.VISIBLE);
                state.setText("Scanning...");
                bluetooth.scanDevices();
            }
        });
    }

    private DiscoveryCallback mDiscoveryCallback = new DiscoveryCallback() {
        @Override
        public void onDeviceFound(int requestCode, final BluetoothDevice device) {
            if (!devices.contains(device)) {
                devices.add(device);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.add(device.getAddress()+" - "+device.getName());
                    }
                });
            }
        }

        @Override
        public void onFinishDiscovery(int requestCode) {
            setProgressVisibility(View.INVISIBLE);
            setText("ScanActivity finished!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scan.setEnabled(true);
                    listView.setEnabled(true);
                }
            });
        }

        @Override
        public void onPair(int requestCode, BluetoothDevice device) {
        }

        @Override
        public void onUnpair(int requestCode, BluetoothDevice device) {
        }

        @Override
        public void onErrorDiscovery(int requestCode, String message) {
            setProgressVisibility(View.INVISIBLE);
            setText("Error: "+message);
        }
    };

    private void setText(final String txt){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                state.setText(txt);
            }
        });
    }

    private void setProgressVisibility(final int id){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progress.setVisibility(id);
            }
        });
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Intent i = new Intent();
        i.putExtra(TAG_BT_ADDRESS, devices.get(position).getAddress());
        setResult(Activity.RESULT_OK, i);
        finish();
    }
}
