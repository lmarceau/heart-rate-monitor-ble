package lauriemarceau.heart_rate_monitor_ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that scan BLE devices. Proper permissions and bluetooth enabling are handled.
 */
public class DeviceScanActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_COARSE_LOCATION = 1;

    private static final String TAG = DeviceScanActivity.class.getSimpleName();

    private ArrayBLEAdapter mArrayAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private ListView mDeviceListView;
    private Boolean mScanning = false;
    private Handler mHandler;

    public ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    public ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);

        progressBar = findViewById(R.id.progress_bar);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (!checkBluetoothSupport(mBluetoothAdapter)) {
            finish();
        }

        mDeviceListView = findViewById(R.id.device_list);
        mArrayAdapter = new ArrayBLEAdapter(this, devicesDiscovered);
        mDeviceListView.setAdapter(mArrayAdapter);

        mDeviceListView.setOnItemClickListener((parent, view, position, id)
                -> onClickToConnect(position));

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
    protected void onPause() {
        super.onPause();
        stopScan();
        devicesDiscovered.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_scan_device, menu);
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
     * When the user click on a device to connect, intent extras are passed to the deviceActivity
     * and this activity is initiated
     */
    private void onClickToConnect(int position) {
        if (devicesDiscovered.isEmpty()) return;

        if (position >= devicesDiscovered.size()) {
            Log.w(TAG, "Illegal position.");
            return;
        }

        BluetoothDevice selectedDevice = devicesDiscovered.get(position);
        Toast.makeText(getApplicationContext(), "Selected: "
                        + selectedDevice.getName(), Toast.LENGTH_LONG).show();

        Log.d(TAG, "Connecting to device " + selectedDevice.getName());

        final Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra(DeviceActivity.EXTRAS_DEVICE_NAME, selectedDevice.getName());
        intent.putExtra(DeviceActivity.EXTRAS_DEVICE_ADDRESS, selectedDevice.getAddress());
        startActivity(intent);
    }

    /**
     * Bluetooth scan that will save all ScanResults into a Arraylist
     */
    private void startScan() {
        if (!hasPermissions() || mScanning) return;

        devicesDiscovered.clear();

        int SCAN_PERIOD = 5000; // 5 seconds scan period

        mScanCallback = new BleScanCallback(devicesDiscovered);

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        List<ScanFilter> filters = new ArrayList<>();

        // Only scan for BLE devices
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mHandler = new Handler();
        mHandler.postDelayed(this::stopScan, SCAN_PERIOD);
        progressBar.setVisibility(View.VISIBLE);
        mScanning = true;
        Log.d(TAG, "Started scanning.");
    }

    /**
     * Stop scanning for BLE devices after 5 seconds
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

    private void scanComplete() {
        if (devicesDiscovered.isEmpty()) {
            return;
        }
        for (BluetoothDevice device : devicesDiscovered ) {
            Log.d(TAG, "Found device: " + device.getName() + " " + device.getAddress() );
        }
        progressBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Verify bluetooth support on this hardware
     * @param bluetoothAdapter {@link BluetoothAdapter}
     * @return true if Bluetooth is properly supported, false otherwise
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
     * Verify permissions and enable bluetooth if it's not
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
     *  Extends the ScanCallback class to add results in the Arraylist
     */
    private class BleScanCallback extends ScanCallback {

            private ArrayList<BluetoothDevice> devicesDiscovered;

            BleScanCallback(ArrayList<BluetoothDevice> scanResults) {
                devicesDiscovered = scanResults;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (devicesDiscovered.isEmpty()) {
                Log.d(TAG, "First device found: " + result.getDevice().getAddress());
                addLeScanResult(result);
                return;
            }

            // This would be better with a HashMap
            if (!devicesDiscovered.contains(result.getDevice())) {
                addLeScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code " + errorCode);
        }

        private void addLeScanResult(ScanResult result) {
            devicesDiscovered.add(result.getDevice());
            Log.d(TAG, "Added device: " + result.getDevice().getName()
                    + ", with address: " + result.getDevice().getAddress());
            mDeviceListView.setAdapter(mArrayAdapter);
        }
    }

    /**
     * Custom adapter for the {@link ListView}
     */
    private class ArrayBLEAdapter extends ArrayAdapter<BluetoothDevice> {

        private Context mContext;
        private List<BluetoothDevice> devices;

        private ArrayBLEAdapter(@NonNull Context context, ArrayList<BluetoothDevice> list) {
            super(context, 0 , list);
            mContext = context;
            devices = list;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            if (convertView == null)
                convertView = LayoutInflater.from(mContext).inflate(R.layout.device_list,
                        parent,false);

            BluetoothDevice currentDevice = devices.get(position);

            TextView deviceAddress = convertView.findViewById(R.id.device_address);
            deviceAddress.setText(currentDevice.getAddress());

            TextView deviceName = convertView.findViewById(R.id.device_name);
            if (currentDevice.getName() != null) {
                deviceName.setText(currentDevice.getName());
            }
            else {
                deviceName.setText(R.string.no_name);
            }

            return convertView;
        }
    }
}

