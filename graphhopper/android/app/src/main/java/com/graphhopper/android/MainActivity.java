package com.graphhopper.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.graphhopper.PathWrapper;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.*;
import com.graphhopper.util.Parameters.*;
import com.graphhopper.util.shapes.GHPoint3D;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.ConnectionResult;

import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Queue;
import java.util.LinkedList;

public class MainActivity extends Activity implements ConnectionCallbacks,OnConnectionFailedListener,LocationListener
{
    private MapView mapView;
    private GraphHopper hopper;
    private LatLong start;
    private LatLong end;
    private Spinner localSpinner;
    private Button localButton;
    private Spinner remoteSpinner;
    private Button remoteButton;
    private volatile boolean prepareInProgress = false;
    private volatile boolean shortestPathRunning = false;
    private String currentArea = "ontario";
    private String fileListURL = "http://download2.graphhopper.com/public/maps/" + Constants.getMajorVersion() + "/";
    private String prefixURL = fileListURL;
    private String downloadURL;
    private File mapsFolder;
    private TileCache tileCache;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private Marker currentLocationMarker;
    //    private Marker greenFlag;
    private Marker redFlag;
    private Polyline polyline;
    private ArrayList<NavPoint> route;
    private ArrayList<Location> spoofLocations;
    private NavPoint nextPoint;
    private int nextTurnIndex;
    private double oldNextDistance;
    private int pointIndex;

    private final double TURN_ON_DISTANCE_THRESHOLD = 100;
    private final double TURN_OFF_DISTANCE_THRESHOLD = 10;
    private final double NEXT_POINT_THRESHOLD = 10;

    private enum bearings {NE,NW,SE,SW};

    private TextView debugView;

    private LineChart mChart;
    private float usDistance = 0;

    // --------------------------------------
    public static final String TAG = "BasicBluetooth";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect,btnVibrate;
    private TextView textViewDistance;

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        btnVibrate.setEnabled(true);
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        btnVibrate.setEnabled(false);
                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        //setUiState();

                    }
                });
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = new String(txValue, "UTF-8");
                            usDistance = Float.parseFloat(text);
                            textViewDistance.setText(text);
                            addEntry(usDistance);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }


        }
    };

    private float filter(float unfilteredDistance, float alpha) {
        float filteredDistance = 0;
        filteredDistance = usDistance + alpha * (unfilteredDistance - usDistance);
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

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    // --------------------------------------

    protected boolean onMapTap(LatLong tapLatLong)
    {
        if (!isReady())
            return false;

        if (shortestPathRunning)
        {
            logUser("Calculation still in progress");
            return false;
        }
        Layers layers = mapView.getLayerManager().getLayers();

        if (start != null && end == null)
        {
            end = tapLatLong;
            shortestPathRunning = true;
            Marker marker = createMarker(tapLatLong, R.drawable.flag_red);
            redFlag = marker;
            if (marker != null)
            {
                layers.add(marker);
            }

            Log.d("das","Start Longitude: " + start.longitude);
            Log.d("das","Start Latitude: " + start.latitude);

            calcPath(start.latitude, start.longitude, end.latitude,
                    end.longitude);
        }
        else
        {
//            start = tapLatLong;
            start = new LatLong(mLastLocation.getLatitude(),mLastLocation.getLongitude());
//            start = new LatLong(43.472356, -80.540956);
//            start = new LatLong(43.471787, -80.542484);
//            start = new LatLong(43.471209, -80.541407);
            end = null;
            // remove all layers but the first one, which is the map

            if (redFlag != null && polyline != null) {
//                layers.remove(layers.indexOf(greenFlag));
                layers.remove(layers.indexOf(redFlag));
                layers.remove(layers.indexOf(polyline));
            }

//            Marker marker = createMarker(start, R.drawable.flag_green);
//            greenFlag = marker;
//
//            if (marker != null)
//            {
//                layers.add(marker);
//            }
        }
        return true;
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        AndroidGraphicFactory.createInstance(getApplication());

        // ===================================
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnVibrate = (Button) findViewById(R.id.buttonVibrate);
        btnConnectDisconnect=(Button) findViewById(R.id.buttonSelect);
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
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    if (btnConnectDisconnect.getText().equals("Connect")){

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice!=null)
                        {
                            mService.disconnect();

                        }
                    }
                }
            }
        });
        // Handle Send button
        btnVibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageToBluetooth();
            }
        });
        // ===================================

        // Initialize
        route = new ArrayList<>();
        pointIndex = 0;

        mapView = new MapView(this);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(true);

        tileCache = AndroidUtil.createTileCache(this, getClass().getSimpleName(), mapView.getModel().displayModel.getTileSize(),
                1f, mapView.getModel().frameBufferModel.getOverdrawFactor());

        final EditText input = new EditText(this);
        input.setText(currentArea);
        boolean greaterOrEqKitkat = Build.VERSION.SDK_INT >= 19;
        if (greaterOrEqKitkat)
        {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                logUser("GraphHopper is not usable without an external storage!");
                return;
            }
            mapsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "/graphhopper/maps/");
        } else
            mapsFolder = new File(Environment.getExternalStorageDirectory(), "/graphhopper/maps/");

        if (!mapsFolder.exists())
            mapsFolder.mkdirs();

        TextView welcome = (TextView) findViewById(R.id.welcome);
        welcome.setText("Welcome to GraphHopper " + Constants.VERSION + "!");
        welcome.setPadding(6, 3, 3, 3);
        localSpinner = (Spinner) findViewById(R.id.locale_area_spinner);
        localButton = (Button) findViewById(R.id.locale_button);
        remoteSpinner = (Spinner) findViewById(R.id.remote_area_spinner);
        remoteButton = (Button) findViewById(R.id.remote_button);
        // TODO get user confirmation to download
        // if (AndroidHelper.isFastDownload(this))
        chooseAreaFromRemote();
        chooseAreaFromLocal();
    }

    @Override
    protected void onDestroy()
    {
        mGoogleApiClient.disconnect();
        super.onDestroy();
        if (hopper != null)
            hopper.close();

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;

        hopper = null;
        // necessary?
        System.gc();

        // Cleanup Mapsforge
        this.mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
    }

    boolean isReady()
    {
        // only return true if already loaded
        if (hopper != null)
            return true;

        if (prepareInProgress)
        {
            logUser("Preparation still in progress");
            return false;
        }
        logUser("Prepare finished but hopper not ready. This happens when there was an error while loading the files");
        return false;
    }

    private void initFiles( String area )
    {
        prepareInProgress = true;
        currentArea = area;
        downloadingFiles();

        oldNextDistance = Integer.MAX_VALUE;

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mGoogleApiClient.connect();
    }

    private void chooseAreaFromLocal()
    {
        List<String> nameList = new ArrayList<>();
        String[] files = mapsFolder.list(new FilenameFilter()
        {
            @Override
            public boolean accept( File dir, String filename )
            {
                return filename != null
                        && (filename.endsWith(".ghz") || filename
                        .endsWith("-gh"));
            }
        });
        for (String file : files)
        {
            nameList.add(file);
        }

        if (nameList.isEmpty())
            return;

        chooseArea(localButton, localSpinner, nameList,
                new MySpinnerListener()
                {
                    @Override
                    public void onSelect( String selectedArea, String selectedFile )
                    {
                        initFiles(selectedArea);
                    }
                });
    }

    private void chooseAreaFromRemote()
    {
        new GHAsyncTask<Void, Void, List<String>>()
        {
            protected List<String> saveDoInBackground( Void... params )
                    throws Exception
            {
                String[] lines = new AndroidDownloader().downloadAsString(fileListURL, false).split("\n");
                List<String> res = new ArrayList<>();
                for (String str : lines)
                {
                    int index = str.indexOf("href=\"");
                    if (index >= 0)
                    {
                        index += 6;
                        int lastIndex = str.indexOf(".ghz", index);
                        if (lastIndex >= 0)
                            res.add(prefixURL + str.substring(index, lastIndex)
                                    + ".ghz");
                    }
                }

                return res;
            }

            @Override
            protected void onPostExecute( List<String> nameList )
            {
                if (hasError())
                {
                    getError().printStackTrace();
                    logUser("Are you connected to the internet? Problem while fetching remote area list: "
                            + getErrorMessage());
                    return;
                } else if (nameList == null || nameList.isEmpty())
                {
                    logUser("No maps created for your version!? " + fileListURL);
                    return;
                }

                MySpinnerListener spinnerListener = new MySpinnerListener()
                {
                    @Override
                    public void onSelect( String selectedArea, String selectedFile )
                    {
                        if (selectedFile == null
                                || new File(mapsFolder, selectedArea + ".ghz").exists()
                                || new File(mapsFolder, selectedArea + "-gh").exists())
                        {
                            downloadURL = null;
                        } else
                        {
                            downloadURL = selectedFile;
                        }
                        initFiles(selectedArea);
                    }
                };
                chooseArea(remoteButton, remoteSpinner, nameList,
                        spinnerListener);
            }
        }.execute();
    }

    private void chooseArea( Button button, final Spinner spinner,
                             List<String> nameList, final MySpinnerListener myListener )
    {
        final Map<String, String> nameToFullName = new TreeMap<>();
        for (String fullName : nameList)
        {
            String tmp = Helper.pruneFileEnd(fullName);
            if (tmp.endsWith("-gh"))
                tmp = tmp.substring(0, tmp.length() - 3);

            tmp = AndroidHelper.getFileName(tmp);
            nameToFullName.put(tmp, fullName);
        }
        nameList.clear();
        nameList.addAll(nameToFullName.keySet());
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, nameList);
        spinner.setAdapter(spinnerArrayAdapter);
        button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick( View v )
            {
                Object o = spinner.getSelectedItem();
                if (o != null && o.toString().length() > 0 && !nameToFullName.isEmpty())
                {
                    String area = o.toString();
                    myListener.onSelect(area, nameToFullName.get(area));
                } else
                {
                    myListener.onSelect(null, null);
                }
            }
        });
    }

    public interface MySpinnerListener
    {
        void onSelect( String selectedArea, String selectedFile );
    }

    void downloadingFiles()
    {
        final File areaFolder = new File(mapsFolder, currentArea + "-gh");
        if (downloadURL == null || areaFolder.exists())
        {
            loadMap(areaFolder);
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Downloading and uncompressing " + downloadURL);
        dialog.setIndeterminate(false);
        dialog.setMax(100);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.show();

        new GHAsyncTask<Void, Integer, Object>()
        {
            protected Object saveDoInBackground( Void... _ignore )
                    throws Exception
            {
                String localFolder = Helper.pruneFileEnd(AndroidHelper.getFileName(downloadURL));
                localFolder = new File(mapsFolder, localFolder + "-gh").getAbsolutePath();
                log("downloading & unzipping " + downloadURL + " to " + localFolder);
                AndroidDownloader downloader = new AndroidDownloader();
                downloader.setTimeout(30000);
                downloader.downloadAndUnzip(downloadURL, localFolder,
                        new ProgressListener()
                        {
                            @Override
                            public void update( long val )
                            {
                                publishProgress((int) val);
                            }
                        });
                return null;
            }

            protected void onProgressUpdate( Integer... values )
            {
                super.onProgressUpdate(values);
                dialog.setProgress(values[0]);
            }

            protected void onPostExecute( Object _ignore )
            {
                dialog.dismiss();
                if (hasError())
                {
                    String str = "An error happened while retrieving maps:" + getErrorMessage();
                    log(str, getError());
                    logUser(str);
                } else
                {
                    loadMap(areaFolder);
                }
            }
        }.execute();
    }

    void loadMap( File areaFolder )
    {
        logUser("loading map");
        MapDataStore mapDataStore = new MapFile(new File(areaFolder, currentArea + ".map"));

        mapView.getLayerManager().getLayers().clear();

//        MapPosition test = new MapPosition()

//        mapView.getModel().mapViewPosition.

        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                mapView.getModel().mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE)
        {
            @Override
            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY)
            {
                return onMapTap(tapLatLong);
            }
        };
        tileRendererLayer.setTextScale(1.5f);
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
        mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(mapDataStore.boundingBox().getCenterPoint(), (byte) 15));
        mapView.getLayerManager().getLayers().add(tileRendererLayer);

//        RelativeLayout rel = (RelativeLayout) find(R.layout.debug_map);



//        setContentView(mapView);
        setContentView(R.layout.debug_map);
        RelativeLayout rel = (RelativeLayout) findViewById(R.id.debugLayout);
        rel.addView(mapView);
        debugView = (TextView) findViewById(R.id.tView);
        debugView.bringToFront();

        loadGraphStorage();
    }

    void loadGraphStorage()
    {
        logUser("loading graph (" + Constants.VERSION + ") ... ");
        new GHAsyncTask<Void, Void, Path>()
        {
            protected Path saveDoInBackground( Void... v ) throws Exception
            {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                tmpHopp.load(new File(mapsFolder, currentArea).getAbsolutePath() + "-gh");
                log("found graph " + tmpHopp.getGraphHopperStorage().toString() + ", nodes:" + tmpHopp.getGraphHopperStorage().getNodes());
                hopper = tmpHopp;
                return null;
            }

            protected void onPostExecute( Path o )
            {
                if (hasError())
                {
                    logUser("An error happened while creating graph:"
                            + getErrorMessage());
                } else
                {
                    logUser("Finished loading graph. Press long to define where to start and end the route.");
                }

                finishPrepare();
            }
        }.execute();
    }

    private void finishPrepare()
    {
        prepareInProgress = false;
    }

    private Polyline createPolyline( PathWrapper response )
    {
        Paint paintStroke = AndroidGraphicFactory.INSTANCE.createPaint();
        paintStroke.setStyle(Style.STROKE);
        paintStroke.setColor(Color.argb(128, 0, 0xCC, 0x33));
        paintStroke.setDashPathEffect(new float[]
                {
                        25, 15
                });
        paintStroke.setStrokeWidth(8);

        Polyline line = new Polyline(paintStroke, AndroidGraphicFactory.INSTANCE);
        List<LatLong> geoPoints = line.getLatLongs();
        PointList tmp = response.getPoints();
        for (int i = 0; i < response.getPoints().getSize(); i++)
        {
            geoPoints.add(new LatLong(tmp.getLatitude(i), tmp.getLongitude(i)));
        }

        return line;
    }

    @SuppressWarnings("deprecation")
    private Marker createMarker(LatLong p, int resource )
    {
        Drawable drawable = getResources().getDrawable(resource);
        Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        return new Marker(p, bitmap, 0, -bitmap.getHeight() / 2);
    }

    public void calcPath( final double fromLat, final double fromLon,
                          final double toLat, final double toLon )
    {

        log("calculating path ...");
        new AsyncTask<Void, Void, PathWrapper>()
        {
            float time;

            protected PathWrapper doInBackground( Void... v )
            {
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).
                        setAlgorithm(Algorithms.DIJKSTRA_BI);
                req.getHints().
                        put(Routing.INSTRUCTIONS, "false");
                GHResponse resp = hopper.route(req);
                time = sw.stop().getSeconds();
                return resp.getBest();
            }

            protected void onPostExecute( PathWrapper resp )
            {
                if (!resp.hasErrors())
                {
                    log("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
                            + toLon + " found path with distance:" + resp.getDistance()
                            / 1000f + ", nodes:" + resp.getPoints().getSize() + ", time:"
                            + time + " " + resp.getDebugInfo());
                    logUser("the route is " + (int) (resp.getDistance() / 100) / 10f
                            + "km long, time:" + resp.getTime() / 60000f + "min, debug:" + time);

                    Location loc1 = new Location("");
                    Location loc2 = new Location("");

                    //Clear old route
                    route.clear();
                    Iterator<GHPoint3D> test = resp.getPoints().iterator();

                    GHPoint3D p1;
                    GHPoint3D p2 = test.next();
                    double prevAngle = 0;

                    // Initialize first point
                    Location initial = new Location("");
                    initial.setLongitude(p2.getLon());
                    initial.setLatitude(p2.getLat());

                    NavPoint initPoint = new NavPoint(initial);
                    nextPoint = initPoint;

                    while (test.hasNext()) {

                        p1 = p2;
                        p2 = test.next();

                        loc1.setLongitude(p1.getLon());
                        loc1.setLatitude(p1.getLat());

                        loc2.setLongitude(p2.getLon());
                        loc2.setLatitude(p2.getLat());

                        double curAngle = loc1.bearingTo(loc2);
                        double difference = prevAngle - curAngle;
                        bearings prevBearing = getBearing(prevAngle);
                        bearings curBearing = getBearing(curAngle);

                        NavPoint curPoint = new NavPoint(loc2);

//                        Log.d("d","Distance between two points: " + loc1.distanceTo(loc2));
//                        Log.d("s","Angle Between the 2 Points: " + curAngle);

                        Marker marker = createMarker(new LatLong(p1.getLat(),p1.getLon()), R.drawable.route_point);
                        mapView.getLayerManager().getLayers().add(marker);

                        if (prevAngle != 0) {
                            if (Math.abs(difference) > 40) {
                                if (prevBearing == bearings.NE && curBearing == bearings.SE
                                        || (prevBearing == bearings.SW && curBearing == bearings.NW)
                                        || (prevBearing == bearings.NW && curBearing == bearings.NE)
                                        || (prevBearing == bearings.SE && curBearing == bearings.SW)
                                        || (prevBearing == bearings.NE && curBearing == bearings.SE)) {
                                    Log.d("m", "Turn right.");curPoint.turnRight = true;
                                } else if ((prevBearing == bearings.NW && curBearing == bearings.SW)
                                        || (prevBearing == bearings.SE && curBearing == bearings.NE)
                                        || (prevBearing == bearings.NE && curBearing == bearings.NW)
                                        || (prevBearing == bearings.SW && curBearing == bearings.SE)) {
                                    Log.d("m", "Turn left.");
                                    curPoint.turnLeft = true;
                                }
                            }
                        }

                        prevAngle = loc1.bearingTo(loc2);
                        route.add(curPoint);
//                        logUser("Distance test: " + loc1.distanceTo(loc2));
//                        logUser("Angle Between the 2 Points: " +
//                                angleFromCoordinate(loc1.getLatitude(),loc1.getLongitude(),
//                                        loc2.getLatitude(),loc2.getLongitude()));
                    }

                    polyline = createPolyline(resp);

                    mapView.getLayerManager().getLayers().add(polyline);
                    //mapView.redraw();
                } else
                {
                    logUser("Error:" + resp.getErrors());
                }
                shortestPathRunning = false;
            }
        }.execute();
    }

    private void log( String str )
    {
        Log.i("GH", str);
    }

    private void log( String str, Throwable t )
    {
        Log.i("GH", str, t);
    }

    private void logUser( String str )
    {
        log(str);
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

    private static final int NEW_MENU_ID = Menu.FIRST + 1;

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, NEW_MENU_ID, 0, "Google");
        // menu.add(0, NEW_MENU_ID + 1, 0, "Other");
        return true;
    }

    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch (item.getItemId())
        {
            case NEW_MENU_ID:
                if (start == null || end == null)
                {
                    logUser("tap screen to set start and end of route");
                    break;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                // get rid of the dialog
                intent.setClassName("com.google.android.apps.maps",
                        "com.google.android.maps.MapsActivity");
                intent.setData(Uri.parse("http://maps.google.com/maps?saddr="
                        + start.latitude + "," + start.longitude + "&daddr="
                        + end.latitude + "," + end.longitude));
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
//            Log.d("M","ENTERED");
//            Toast.makeText(this, "Latitude:" + mLastLocation.getLatitude()+", Longitude:"+mLastLocation.getLongitude(),Toast.LENGTH_LONG).show();
            createLocationRequest();
            startLocationUpdates();

            LatLong current = new LatLong(mLastLocation.getLatitude(),mLastLocation.getLongitude());
            updateCurrentLocationMarker(current);

            mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(current, (byte) 15));
//            setContentView(mapView);
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        checkNextPoint();
//        Toast.makeText(this, "Latitude:" + mLastLocation.getLatitude()+", Longitude:"+mLastLocation.getLongitude(),Toast.LENGTH_LONG).show();

        LatLong current = new LatLong(mLastLocation.getLatitude(),mLastLocation.getLongitude());
        Log.d("dsd", "Lat: " + mLastLocation.getLatitude() + " ,Long: " + mLastLocation.getLongitude());
        updateCurrentLocationMarker(current);
    }

    public void updateCurrentLocationMarker(LatLong current) {
        Layers layers = mapView.getLayerManager().getLayers();
        Marker marker = createMarker(current, R.drawable.current_location);

        if (currentLocationMarker != null) {
            layers.remove(layers.indexOf(currentLocationMarker));
        }

        currentLocationMarker = marker;
        layers.add(marker);
    }

    public void checkNextPoint() {

        if (route.size() > 1) {

//            for (int i =0; i < route.size(); i++) {
//                NavPoint cur = route.get(i);
//                Log.d("so","Left: " + cur.turnLeft);
//                Log.d("so","Right: " + cur.turnRight);
//            }
//            return;

            boolean turnIn3PointsRight = false;
            boolean turnIn3PointsLeft = false;

            NavPoint nextNextPoint = cloneNavPoint(route.get(0));
            double distance1 = mLastLocation.distanceTo(nextPoint.location);
            double distance2 = mLastLocation.distanceTo(nextNextPoint.location);
            double combDistance = distance1 + nextPoint.location.distanceTo(nextNextPoint.location);
            double newDistance = 0;

            if (nextTurnIndex < 1) {
                nextTurnIndex = getNextTurn();
            }
            else if (nextTurnIndex == 1) {
                NavPoint nextNextNextPoint = route.get(nextTurnIndex);
                newDistance = combDistance + nextNextPoint.location.distanceTo(nextNextNextPoint.location);
                turnIn3PointsRight = (newDistance <= TURN_ON_DISTANCE_THRESHOLD) && nextNextNextPoint.turnRight;
                turnIn3PointsLeft = (newDistance <= TURN_ON_DISTANCE_THRESHOLD) && nextNextNextPoint.turnLeft;
//                nextTurnIndex = 0;
            }

//            Log.d("Test", "Test");
//            Toast.makeText(this, "Distance to next: " + distance1 + ", Distance of old: " + oldNextDistance, Toast.LENGTH_SHORT).show();
//            debugView.setText("Distance to next: " + distance1 + ", Distance of old: " + oldNextDistance);
            debugView.setText("Next Turn Index: " + nextTurnIndex + ", Turn distance: " + newDistance);
            Log.d("dd","Distance to next: " + distance1 + ", Distance of old: " + oldNextDistance);

            if (/*(nextPoint.turnRight && distance1 <= TURN_ON_DISTANCE_THRESHOLD)
                    ||*/ ((nextNextPoint.turnRight && combDistance <= TURN_ON_DISTANCE_THRESHOLD)
                    && (!nextPoint.turnRight || distance1 > TURN_OFF_DISTANCE_THRESHOLD ))
                    || turnIn3PointsRight) {

                if (mState == UART_PROFILE_CONNECTED) {
                    sendMessageToBluetooth();
                }

//                Toast.makeText(this, "Turn Right", Toast.LENGTH_SHORT).show();
                debugView.setText("Turn right");
            }
            else if (/*(nextPoint.turnLeft && distance2 <= TURN_ON_DISTANCE_THRESHOLD)
                    ||*/ ((nextNextPoint.turnLeft && combDistance <= TURN_ON_DISTANCE_THRESHOLD)
                    && (!nextPoint.turnLeft || distance1 > TURN_OFF_DISTANCE_THRESHOLD ))
                    || turnIn3PointsLeft){

                if (mState == UART_PROFILE_CONNECTED) {
                    sendMessageToBluetooth();
                }
//                Toast.makeText(this, "Turn Left", Toast.LENGTH_SHORT).show();
                debugView.setText("Turn left");
            }

            if (oldNextDistance < distance1 && (distance1 - oldNextDistance) > 2) {
                Toast.makeText(this, "Passed point", Toast.LENGTH_SHORT).show();
                nextPoint = cloneNavPoint(route.get(0));
                oldNextDistance = distance2;
                route.remove(0);
                nextTurnIndex--;
            }
            else {
                oldNextDistance = distance1;
            }
        }
        else if (route.size() == 1) {
            double distance1 = mLastLocation.distanceTo(nextPoint.location);

            if (oldNextDistance < distance1 && (distance1 - oldNextDistance) > 2) {
                Toast.makeText(this, "Passed point", Toast.LENGTH_SHORT).show();
                nextPoint = cloneNavPoint(route.get(0));
                route.remove(0);
                nextTurnIndex--;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);
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

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
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

    public void sendMessageToBluetooth() {
        String message = "1";
        byte[] value;
        try {
            //send data to service
            value = message.getBytes("UTF-8");
            mService.writeRXCharacteristic(value);

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public int getNextTurn() {
        for (int i = 0; i < route.size(); i++) {
            NavPoint point = route.get(i);
            if (point.turnLeft || point.turnRight) {
                // Add 1 because one point is initially not in the
                // route because it is stored in a variable
                return i;
            }
        }
        return 0;
    }

    public bearings getBearing(double degrees) {

        if (degrees >= 0 && degrees <= 90) {
            return bearings.NE;
        }
        else if (degrees > 90 && degrees <= 180) {
            return bearings.SE;
        }
        else if (degrees < 0 && degrees >= -90) {
            return bearings.NW;
        }
        else if (degrees < -90 && degrees >= -180) {
            return bearings.SW;
        }

        return null;
//        throw new Exception("Bearing not found.");
    }

    public NavPoint cloneNavPoint(NavPoint original) {
        NavPoint clone = new NavPoint(original.location);
        clone.turnRight = original.turnRight;
        clone.turnLeft = original.turnLeft;

        return clone;
    }

    private class NavPoint {
        boolean turnRight;
        boolean turnLeft;
        Location location;

        public NavPoint(Location location) {
            this.turnRight = false;
            this.turnLeft = false;
            this.location = new Location("");
            this.location.setLatitude(location.getLatitude());
            this.location.setLongitude(location.getLongitude());
        }

    }
}
