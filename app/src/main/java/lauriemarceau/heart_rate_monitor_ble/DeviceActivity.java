package lauriemarceau.heart_rate_monitor_ble;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

public class DeviceActivity extends AppCompatActivity {

    private final static String TAG = DeviceActivity.class.getSimpleName();

    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public TextView batteryLevelValue;
    public TextView heartRateValue;
    public LineChart heartRateChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        final Intent intent = getIntent();
        String mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        Log.d(TAG, "Device address: " + mDeviceAddress);

        this.setTitle(mDeviceName);

        batteryLevelValue = findViewById(R.id.batteryLevelValue);
        heartRateValue = findViewById(R.id.heartRateValueText);

        /* HEART RATE CHART */ // TODO Move in another .java
        heartRateChart = findViewById(R.id.heartRateChart);
        //heartRateChart.setOnChartValueSelectedListener(this); TODO?
        heartRateChart.getDescription().setEnabled(true);
        heartRateChart.setDrawGridBackground(false);
        heartRateChart.setBackgroundColor(Color.TRANSPARENT);

        LineData heartRateData = new LineData();
        heartRateData.setValueTextColor(Color.GREEN);
        heartRateChart.setData(heartRateData);

        XAxis xl = heartRateChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = heartRateChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = heartRateChart.getAxisRight();
        rightAxis.setEnabled(false);
        /* HEART RATE CHART */

        Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
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

        if (thread != null) {
            thread.interrupt();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void ClearTextViews() {
        batteryLevelValue.setText(R.string.no_battery_level);
        heartRateValue.setText(R.string.no_data);
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
            // Automatically connects to the device upon successful start-up initialization.
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

            } else if (BluetoothLeService.
                    ACTION_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics in the log debug
                Log.d(TAG,"Displaying the device services");
                mBluetoothLeService.getSupportedGattServices();

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
        intentFilter.addAction(BluetoothLeService.ACTION_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(() ->  Log.d(TAG, "New connection state is: " + resourceId));
    }

    private void displayHeartRateData(String data) {
        if (data != null) {
            Log.d(TAG, "Heart rate received: " + data);
            heartRateValue.setText(data);
            feedMultiple();
        }
    }
    private void displayBatteryData(String data) {
        if (data != null) {
            Log.d(TAG, "Battery level received: " + data);
            batteryLevelValue.setText(getString(R.string.battery_value, String.valueOf(data)));
        }
    }

    private void addEntry() {

        LineData data = heartRateChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 40) + 30f), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            heartRateChart.notifyDataSetChanged();

            // limit the number of visible entries
            heartRateChart.setVisibleXRangeMaximum(120);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            heartRateChart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private Thread thread;

    private void feedMultiple() {

        if (thread != null)
            thread.interrupt();

        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                addEntry();
            }
        };

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {

                    // Don't generate garbage runnables inside the loop.
                    runOnUiThread(runnable);

                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

}
