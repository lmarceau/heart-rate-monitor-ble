package lauriemarceau.heart_rate_monitor_ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Handler;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppCompatActivity {

    private static final String TAG = DeviceScanActivity.class.getSimpleName();

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_COARSE_LOCATION = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private BluetoothGatt mGatt;
    private HashMap<String, BluetoothDevice> mScanResults;

    int deviceIndex = 0;
    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    Button connectToDevice;
    Button disconnectDevice;
    EditText deviceIndexInput;
    TextView deviceTextView;

    private MenuItem menuScanItem;
    private Boolean mConnected;
    private Boolean mScanning = false;
    private Handler mHandler;
    private int SCAN_PERIOD = 5000; // 5 seconds scan period

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);

        deviceTextView = findViewById(R.id.DeviceTextView);
        deviceTextView.setMovementMethod(new ScrollingMovementMethod());
        deviceIndexInput = findViewById(R.id.InputIndex);
        deviceIndexInput.setText("0");

        connectToDevice = findViewById(R.id.ConnectButton);
        connectToDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connectToDeviceSelected();
            }
         });

        disconnectDevice = findViewById(R.id.DisconnectButton);
        disconnectDevice.setVisibility(View.INVISIBLE);
        disconnectDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                disconnectDeviceSelected();
            }
        });

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (!checkBluetoothSupport(mBluetoothAdapter)) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.v(TAG, "No LE Support.");
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_scan_device, menu);
        menuScanItem = menu.findItem(R.id.scan);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                startScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Bluetooth scan that will save all ScanResults into a HashMap
     */
    private void startScan() {
        if (!hasPermissions() || mScanning) {
            return;
        }
        disconnectGattServer();

        devicesDiscovered.clear();
        deviceIndex = 0;

        mScanResults = new HashMap<>();
        mScanCallback = new BleScanCallback(mScanResults);

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        List<ScanFilter> filters = new ArrayList<>();

        /*ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();
        filters.add(scanFilter);*/

        // Only scan for BLE devices
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mHandler = new Handler();
        mHandler.postDelayed(this::stopScan, SCAN_PERIOD);

        mScanning = true;
        Log.d(TAG, "Started scanning.");
    }

    /**
     * Stop scanning, call scan complete and clean up scan related variables
     */
    private void stopScan() {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            scanComplete();
        }

        mScanCallback = null;
        mScanning = false;
        mHandler = null;
    }

    /**
     * Log the found devices
     */
    private void scanComplete() {
        if (mScanResults.isEmpty()) {
            return;
        }
        for (Object deviceAddress : mScanResults.keySet()) {
            Log.d(TAG, "Found device: " + deviceAddress );
        }
    }

    public void connectToDeviceSelected() {
        deviceTextView.append("Trying to connect to device at index: " + deviceIndexInput.getText() + "\n");
        int deviceSelected = Integer.parseInt(deviceIndexInput.getText().toString());
        mGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, true, gattClientCallback);
    }

    public void disconnectDeviceSelected() {
        deviceTextView.append("Disconnecting from device\n");
        mGatt.disconnect();
    }

    public void disconnectGattServer() {
        mConnected = false;
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @param bluetoothAdapter System {@link BluetoothAdapter}.
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }

    /**
     * Verify permissions and if bluetooth is enabled, if not asked for them
     * @return false when Bluetooth or permission are not enabled
     */
    private boolean hasPermissions() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
            return false;
        } else if (!hasLocationPermissions()) {
            requestLocationPermission();
            return false;
        }
        return true;
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.d(TAG, "Requested user enables Bluetooth. Try starting the scan again.");
    }

    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
    }

    /**
     *  Bluetooth scan callback
     *  Extends the ScanCallback class to add results in the Hashmap
     */
    private class BleScanCallback extends ScanCallback {

            private Map<String, BluetoothDevice> mScanResults;

            BleScanCallback(Map<String, BluetoothDevice> scanResults) {
            mScanResults = scanResults;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // TO DO: filter and remove null names
            deviceTextView.append("Index: " + deviceIndex + ", Device Name: "
                    + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");
            devicesDiscovered.add(result.getDevice());
            deviceIndex++;
            // auto scroll for text view
            final int scrollAmount = deviceTextView.getLayout().getLineTop(deviceTextView.getLineCount()) - deviceTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0) {
                deviceTextView.scrollTo(0, scrollAmount);
            }
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code " + errorCode);
        }
        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getAddress();
            mScanResults.put(deviceAddress, device);
        }
    }

    /**
     * Gatt client callback
     */
    private final BluetoothGattCallback gattClientCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer();
            }
        }

    };

}

