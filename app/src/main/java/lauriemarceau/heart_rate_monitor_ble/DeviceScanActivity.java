package lauriemarceau.heart_rate_monitor_ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_COARSE_LOCATION = 1;

    private static final String TAG = DeviceScanActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private BluetoothGatt mGatt;
    private MenuItem menuScanItem;
    private Boolean mScanning = false;
    private Handler mHandler;

    public ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    public BluetoothDevice mDevice;
    public Button connectToDevice;
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
        connectToDevice.setOnClickListener((View v) -> {
            if (devicesDiscovered.isEmpty()) return;
            connectToDeviceSelected();
        });

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
     * Bluetooth scan that will save all ScanResults into a Arraylist
     */
    private void startScan() {
        if (!hasPermissions() || mScanning) return;

        disconnectGattServer();

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

    public void connectToDeviceSelected() {
        deviceTextView.append("Connecting to device: " + deviceIndexInput.getText() + "\n");
        int deviceSelected = Integer.parseInt(deviceIndexInput.getText().toString());
        mDevice = devicesDiscovered.get(deviceSelected);
        mGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, true, gattClientCallback);
    }

    public void disconnectGattServer() {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
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
     * Display the Gatt services and characteristics for the found device
     * @param gattServices list of the found bluetooth LE gatt services for the selected device
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            final String uuid = gattService.getUuid().toString();
            System.out.println("Service discovered: " + uuid);
            DeviceScanActivity.this.runOnUiThread(() ->
                Log.d(TAG, "Service discovered: " + uuid + "\n"));

            new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic :
                    gattCharacteristics) {

                final String charUuid = gattCharacteristic.getUuid().toString();
                System.out.println("Characteristic discovered for service: " + charUuid);
                DeviceScanActivity.this.runOnUiThread(() ->
                        Log.d(TAG,"Characteristic discovered for service: "
                                + charUuid + "\n"));
            }
        }
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

    /**
     * Gatt client callback: append the text view, show/hide contextual buttons
     *                       and discover services
     */
    private final BluetoothGattCallback gattClientCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_FAILURE) {
                deviceTextView.setText(R.string.connection_failure);
                Log.d(TAG,"Connection failure\n");
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Connection failure\n");
                deviceTextView.setText(R.string.connection_failure);
                disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                DeviceScanActivity.this.runOnUiThread(() -> {
                    deviceTextView.setText(null);
                    Log.d(TAG,"Connection success\n");
                    connectToDevice.setVisibility(View.GONE);
                    deviceIndexInput.setVisibility(View.GONE);
                    headerTextView.setVisibility(View.GONE);
                    menuScanItem.setVisible(false);
                    Intent intent = new Intent(getApplicationContext(), DeviceActivity.class);
                    intent.putExtra("Device", mDevice);
                    startActivity(intent);
                });
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                DeviceScanActivity.this.runOnUiThread(() -> {
                    deviceTextView.setText(R.string.connection_failure);
                    Log.d(TAG,"Connection failure\n");
                    disconnectGattServer();
                });
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            DeviceScanActivity.this.runOnUiThread(() ->
                    Log.d(TAG,"@string/showing_gatt"));

            displayGattServices(gatt.getServices());
        }
    };
}

