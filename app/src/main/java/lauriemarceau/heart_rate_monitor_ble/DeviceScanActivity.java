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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that scan BLE devices. Proper permissions and bluetooth enabling are handled.
 */
public class DeviceScanActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_COARSE_LOCATION = 1;

    private static final String TAG = DeviceScanActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private Boolean mScanning = false;
    private Handler mHandler;

    public ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    public Button connectToDevice;
    public BluetoothDevice mDevice;
    public EditText deviceIndexInput;
    public TextView deviceTextView;
    public TextView headerTextView;
    public int deviceIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);

        deviceTextView = findViewById(R.id.DeviceTextView);
        deviceTextView.setMovementMethod(new ScrollingMovementMethod());
        deviceIndexInput = findViewById(R.id.InputIndex);
        headerTextView = findViewById(R.id.HeaderTextView);


        connectToDevice = findViewById(R.id.ConnectButton);
        connectToDevice.setOnClickListener((View v) -> onClickConnectButton());

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

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
     * When the user ask to connect, intent extras are passed to the deviceActivity
     * and this activity is initiated
     */
    private void onClickConnectButton() {
        if (devicesDiscovered.isEmpty()) return;
        Log.d(TAG, "Connecting to device " + deviceIndexInput.getText().toString());

        int deviceSelected = Integer.parseInt(deviceIndexInput.getText().toString());
        mDevice = devicesDiscovered.get(deviceSelected);

        final Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra(DeviceActivity.EXTRAS_DEVICE_NAME, mDevice.getName());
        intent.putExtra(DeviceActivity.EXTRAS_DEVICE_ADDRESS, mDevice.getAddress());
        startActivity(intent);
    }

    /**
     * Bluetooth scan that will save all ScanResults into a Arraylist
     */
    private void startScan() {
        if (!hasPermissions() || mScanning) return;

        //mBluetoothLeService.disconnectGattServer(); TO DO

        devicesDiscovered.clear();
        deviceTextView.setText(null);
        deviceIndex = 0;
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
        headerTextView.setText(R.string.scanning_text);
        mScanning = true;
        Log.d(TAG, "Started scanning.");
    }

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
            headerTextView.setText(R.string.no_device_found);
            return;
        }
        for (BluetoothDevice device : devicesDiscovered ) {
            Log.d(TAG, "Found device: " + device.getAddress() );
        }
        deviceIndexInput.setVisibility(View.VISIBLE);
        connectToDevice.setVisibility(View.VISIBLE);
        headerTextView.setText(R.string.select_device);
    }

    /**
     * Verify bluetooth support on this hardware
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
     *  Bluetooth scan callback
     *  Extends the ScanCallback class to add results in the Arraylist
     */
    private class BleScanCallback extends ScanCallback {

            private ArrayList<BluetoothDevice> devicesDiscovered;

            BleScanCallback(ArrayList<BluetoothDevice> scanResults) {
                devicesDiscovered = scanResults;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // TO DO: replace arraylist by hashmap so in case of multiple found devices it
            // won't have to iterate through the whole list
            if (result.getDevice().getName() != null) {
                if (devicesDiscovered.isEmpty()) {
                    addScanResult(result);
                } else if (!devicesDiscovered.get(0).getAddress().contains
                        (result.getDevice().getAddress())) {
                    addScanResult(result);
                }
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code " + errorCode);
        }
        private void addScanResult(ScanResult result) {
            deviceTextView.append("Index: " + deviceIndex + ", Device Name: "
                    + result.getDevice().getName() + ", Address: "
                    + result.getDevice().getAddress() + "\n");
            devicesDiscovered.add(result.getDevice());
            deviceIndex++;

            // auto scroll for text view
            final int scrollAmount = deviceTextView.getLayout().getLineTop
                    (deviceTextView.getLineCount()) - deviceTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <= 0
            if (scrollAmount > 0) {
                deviceTextView.scrollTo(0, scrollAmount);
            }
        }
    }

}

