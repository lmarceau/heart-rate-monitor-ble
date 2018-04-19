package lauriemarceau.heart_rate_monitor_ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceActivity extends AppCompatActivity {

    private final static String TAG = DeviceActivity.class.getSimpleName();

    private ArrayList<ArrayList<GattAttributes>> mGattCharacteristics = new ArrayList<>();
    private BluetoothLeService mBluetoothLeService;
    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private String mDeviceName;
    private String mDeviceAddress;
    private TextView mConnectionState;
    private boolean mConnected = false;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public TextView deviceName;
    public TextView batteryLevelValue;
    public TextView heartRateValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        Log.d(TAG, "Device address: " + mDeviceAddress);

        deviceName = findViewById(R.id.DeviceNameValue);
        deviceName.setText(mDeviceName);

        mConnectionState = findViewById(R.id.ConnectionStateValue);
        batteryLevelValue = findViewById(R.id.batteryLevelValue);
        heartRateValue = findViewById(R.id.heartRateValueText);

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void ClearTextViews() {
        deviceName.setText(R.string.no_device_found);
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

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connection_success);
            } else if (BluetoothLeService.ACTION_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.connection_failure);
                ClearTextViews();
            } else if (BluetoothLeService.
                    ACTION_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                Log.d(TAG,"Displaying the device services");
                mBluetoothLeService.getSupportedGattServices(); // TODO: ATM only debug display
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
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
        runOnUiThread(() ->  mConnectionState.setText(resourceId));
    }

    private void displayData(String data) {
        if (data != null) {
            Log.d(TAG, "Data received: " + data);
            heartRateValue.setText(data);
        }
    }
}
