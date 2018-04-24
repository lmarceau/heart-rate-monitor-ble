package lauriemarceau.heart_rate_monitor_ble;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

/**
 * Activity  that connects to the BluetoothLeService, handles the UI activity which
 * include the battery service, the device name and the heart rate with its history chart
 */
public class DeviceActivity extends AppCompatActivity {

    private final static String TAG = DeviceActivity.class.getSimpleName();

    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public TextView batteryLevelTextView;
    public TextView heartRateTextView;
    public LineChart heartRateChart;
    public int actualHeartRateValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        final Intent intent = getIntent();
        String mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        this.setTitle(mDeviceName);
        Log.d(TAG, "Device address: " + mDeviceAddress);

        batteryLevelTextView = findViewById(R.id.batteryLevelValue);
        heartRateTextView = findViewById(R.id.heartRateValueText);
        heartRateChart = findViewById(R.id.heartRateChart);
        setHeartRateChart();

        Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_disconnect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.disconnect:
                final Intent intent = new Intent(this, DeviceScanActivity.class);

                mBluetoothLeService.disconnectGattServer();
                startActivity(intent);
                Log.d(TAG,"Going back to scanning activity");
                finish();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeUpdateIntentFilter());

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connectToDevice(mDeviceAddress);
            Log.d(TAG, "The connection was = " + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void ClearTextViews() {
        batteryLevelTextView.setText(R.string.no_battery_level);
        heartRateTextView.setText(R.string.no_data);
    }

    /**
     * Bind to a service to interact with it and perform interprocess communication (IPC)
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service){
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            if (!mBluetoothLeService.initBluetooth()) {
                Log.e(TAG, "Failure to start bluetooth");
                finish();
            }

            /* Automatically connects to the device upon successful start-up initialization. */
            mBluetoothLeService.connectToDevice(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName){
            mBluetoothLeService = null;
        }
    };

    /**
    * Handles various events fired by the Service: ACTION_GATT_CONNECTED, ACTION_GATT_DISCONNECTED,
    * ACTION_GATT_SERVICES_DISCOVERED ACTION_DATA_AVAILABLE. This can be a
    * result of read or notification operations.
    */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_CONNECTED.equals(action)) {
                Log.d(TAG,"ACTION_CONNECTED");
                updateConnectionState(R.string.connection_success);

            } else if (BluetoothLeService.ACTION_DISCONNECTED.equals(action)) {
                Log.d(TAG,"ACTION_DISCONNECTED");
                updateConnectionState(R.string.connection_failure);
                ClearTextViews();

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG,"ACTION_DATA_AVAILABLE");
                mBluetoothLeService.getBattery();
                displayHeartRateData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_HEART_RATE));
                displayBatteryData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_BATTERY));
            }
        }
    };

    private static IntentFilter makeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.ACTION_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateConnectionState(final int resourceId) {
        Log.d(TAG, "New connection state is: " + resourceId);
    }

    private void displayHeartRateData(String data) {
        if (data != null) {
            Log.d(TAG, "Heart rate received: " + data);
            heartRateTextView.setText(data);
            actualHeartRateValue = Integer.parseInt(data);
            addHeartRateEntry();
        }
    }
    private void displayBatteryData(String data) {
        if (data != null) {
            Log.d(TAG, "Battery level received: " + data);
            batteryLevelTextView.setText(getString(R.string.battery_value, String.valueOf(data)));
        }
    }

    /**
     * Set all parameters relative to the heart rate chart
     */
    public void setHeartRateChart() {
        heartRateChart.getDescription().setEnabled(true);
        heartRateChart.setDrawGridBackground(false);
        heartRateChart.setBackgroundColor(Color.TRANSPARENT);

        LineData heartRateData = new LineData();
        heartRateChart.setData(heartRateData);
        heartRateChart.setMinimumHeight(350);

        XAxis xAxis = heartRateChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setEnabled(true);

        YAxis yAxis = heartRateChart.getAxisLeft();
        yAxis.setTextColor(Color.WHITE);
        yAxis.setAxisMaximum(140f);
        yAxis.setAxisMinimum(50f);
        yAxis.setDrawGridLines(true);

        Legend legend = heartRateChart.getLegend();
        legend.setEnabled(false);

        YAxis rightAxis = heartRateChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    /**
    * Every time a new heart rate value is received, it is added to the real time chart
    */
    private void addHeartRateEntry() {
        LineData data = heartRateChart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), actualHeartRateValue), 0);
            data.notifyDataChanged();

            heartRateChart.notifyDataSetChanged();
            heartRateChart.setVisibleXRangeMaximum(120);
            heartRateChart.moveViewToX(data.getEntryCount());
        }
    }

    /**
     * Set the parameters for the LineDataSet
     * @return a {@link LineDataSet} for the heart rate chart
     */
    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Heart Rate Data");

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.RED);
        set.setCircleColor(Color.RED);
        set.setLineWidth(2f);
        set.setCircleRadius(1f);
        set.setFillAlpha(65);
        set.setFillColor(Color.RED);
        set.setHighLightColor(Color.RED);
        set.setValueTextColor(Color.RED);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    public static Handler UIHandler;

    static {
        UIHandler = new Handler(Looper.getMainLooper());
    }

    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }
}
