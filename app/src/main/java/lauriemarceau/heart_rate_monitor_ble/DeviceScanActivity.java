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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
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
    private MenuItem menuScanItem;
    private Boolean mConnected;
    private Boolean mScanning = false;
    private Handler mHandler;
    private int SCAN_PERIOD = 5000; // 5 seconds scan period

    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    Button connectToDevice;
    Button showDeviceData;
    EditText deviceIndexInput;
    TextView deviceTextView;
    TextView headerTextView;
    int deviceIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);

        deviceTextView = findViewById(R.id.DeviceTextView);
        deviceTextView.setMovementMethod(new ScrollingMovementMethod());
        deviceIndexInput = findViewById(R.id.InputIndex);
        //deviceIndexInput.setText("0");
        //deviceIndexInput.setVisibility(View.INVISIBLE);
        headerTextView = findViewById(R.id.HeaderTextView);

        connectToDevice = findViewById(R.id.ConnectButton);
        //connectToDevice.setVisibility(View.INVISIBLE);
        // TO DO : Use lambda function everywhere where it's needed in the code
        connectToDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (devicesDiscovered.isEmpty()) return;
                connectToDeviceSelected();
            }
         });

        showDeviceData = findViewById(R.id.DataButton);
        //showDeviceData.setVisibility(View.INVISIBLE);
        showDeviceData.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                showDeviceSelected();
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
        deviceTextView.setText(null);
        deviceIndex = 0;

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
        if (devicesDiscovered.isEmpty()) {
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
        deviceTextView.append("Trying to connect to device at index: " + deviceIndexInput.getText() + "\n");
        int deviceSelected = Integer.parseInt(deviceIndexInput.getText().toString());
        mGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, true, gattClientCallback);
    }

    public void showDeviceSelected() {
        deviceTextView.append("Show data from device\n");
        // TO DO : intent
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
     *
     * @param gattServices
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            final String uuid = gattService.getUuid().toString();
            System.out.println("Service discovered: " + uuid);
            DeviceScanActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    deviceTextView.append("Service discovered: " + uuid + "\n");
                }
            });
            new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic :
                    gattCharacteristics) {

                final String charUuid = gattCharacteristic.getUuid().toString();
                System.out.println("Characteristic discovered for service: " + charUuid);
                DeviceScanActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        deviceTextView.append("Characteristic discovered for service: "
                                + charUuid + "\n");
                    }
                });
            }
        }
    }

    /**
     *  Bluetooth scan callback
     *  Extends the ScanCallback class to add results in the Hashmap
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
            final int scrollAmount = deviceTextView.getLayout().getLineTop(deviceTextView.getLineCount())
                    - deviceTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0) {
                deviceTextView.scrollTo(0, scrollAmount);
            }
        }
    }

    /**
     * Gatt client callback, append the text view, show contextual buttons and discover services
     */
    private final BluetoothGattCallback gattClientCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_FAILURE) {
                deviceTextView.append("Connection failure\n");
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                deviceTextView.append("Connection failure\n");
                disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                DeviceScanActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        mConnected = true;
                        deviceTextView.setText(null);
                        deviceTextView.append("Connection success\n");
                        connectToDevice.setVisibility(View.GONE);
                        deviceIndexInput.setVisibility(View.GONE);
                        showDeviceData.setVisibility(View.VISIBLE);
                        menuScanItem.setVisible(false);
                    }
                });
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                DeviceScanActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        deviceTextView.append("Connection failure\n");
                        connectToDevice.setVisibility(View.GONE);
                        deviceIndexInput.setVisibility(View.GONE);
                        showDeviceData.setVisibility(View.INVISIBLE);
                        disconnectGattServer();
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            DeviceScanActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    headerTextView.setText(R.string.showing_gatt);
                    deviceTextView.append("Device services have been discovered\n");
                }
            });
            displayGattServices(gatt.getServices());
        }
    };
}

