package lauriemarceau.heart_rate_monitor_ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.ComponentName;
import android.content.Intent;
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

        batteryLevelValue = findViewById(R.id.batteryLevelValue);
        heartRateValue = findViewById(R.id.heartRateValueText);

        Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO register receiver
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connectToDevice(mDeviceAddress);
            Log.d(TAG, "The connection was = " + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO : unregister receiver
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
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
}
