package lauriemarceau.heart_rate_monitor_ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// TODO import autrement c'est laid
import static lauriemarceau.heart_rate_monitor_ble.GattAttributes.CLIENT_CHARACTERISTIC_CONFIG_UUID;
import static lauriemarceau.heart_rate_monitor_ble.GattAttributes.HEART_RATE_CONTROL_POINT_CHAR_UUID;
import static lauriemarceau.heart_rate_monitor_ble.GattAttributes.HEART_RATE_MEASUREMENT_CHAR_UUID;
import static lauriemarceau.heart_rate_monitor_ble.GattAttributes.HEART_RATE_SERVICE_UUID;

public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothGatt mGatt;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private int mConnectionStatus = STATE_FAILURE;

    private static final int STATE_FAILURE = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_SUCCESS = 2;

    public final static String ACTION_CONNECTED =
            "lauriemarceau.heart_rate_monitor_ble.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_DISCONNECTED =
            "lauriemarceau.heart_rate_monitor_ble.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_SERVICES_DISCOVERED =
            "lauriemarceau.heart_rate_monitor_ble.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "lauriemarceau.heart_rate_monitor_blebluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "lauriemarceau.heart_rate_monitor_ble.bluetooth.le.EXTRA_DATA";


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
            return true;
        }
        else {
            Log.e(TAG, "Bluetooth adapter init didn't work");
            return false;
        }
    }

    public boolean connectToDevice(final String address) {

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "No Bluetooth adapter or no address");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mGatt != null) {
            Log.d(TAG, "Trying to use an existing mGatt for connection.");
            if (mGatt.connect()) {
                mConnectionStatus = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mGatt = device.connectGatt(this, false, gattClientCallback);
        Log.d(TAG, "Connecting to selected device");
        mBluetoothDeviceAddress = address;
        mConnectionStatus = STATE_CONNECTING;
        return true;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
    }

    public void disconnectGattServer() {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
    }

    /**
     * Display the Gatt services and characteristics for the found device
     * @param gattServices list of the found bluetooth LE gatt services for the selected device
     */
    public void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            final String uuid = gattService.getUuid().toString();
            Log.d(TAG, "Service discovered: " + uuid + "\n");

            new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic :
                    gattCharacteristics) {

                final String charUuid = gattCharacteristic.getUuid().toString();
                Log.d(TAG,"Characteristic discovered for service: " + charUuid + "\n");
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
            String intentAction;
            if (status == BluetoothGatt.GATT_FAILURE) {
                mConnectionStatus = STATE_FAILURE;
                Log.d(TAG,"Connection failure, disconnected from server");
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                mConnectionStatus = STATE_FAILURE;
                Log.d(TAG,"Connection failure, disconnected from server");
                disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_CONNECTED;
                mConnectionStatus = STATE_SUCCESS;
                broadcastUpdate(intentAction);
                Log.d(TAG,"Succes: connecting to gatt and discovering services");
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionStatus = STATE_FAILURE;
                intentAction = ACTION_DISCONNECTED;
                Log.d(TAG,"Connection failure, disconnected from server");
                broadcastUpdate(intentAction);
                disconnectGattServer();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Device service discovery unsuccessful, status " + status);
                return;
            }
            else {
                broadcastUpdate(ACTION_SERVICES_DISCOVERED);
            }

            setHeartRateNotification(gatt, true);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status){

            BluetoothGattCharacteristic characteristic =
                    gatt.getService(HEART_RATE_SERVICE_UUID)
                            .getCharacteristic(HEART_RATE_CONTROL_POINT_CHAR_UUID);

            characteristic.setValue(new byte[]{1, 1});
            gatt.writeCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    public void setHeartRateNotification(BluetoothGatt gatt,
                                         boolean enabled) {
        BluetoothGattCharacteristic characteristic =
                gatt.getService(HEART_RATE_SERVICE_UUID)
                        .getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID);

        gatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);

        descriptor.setValue(
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gatt.writeDescriptor(descriptor);
    }

    /**
     * Request a read on a given characteristic. The read result is reported
     * asynchronously through the gattClientCallback/onCharacteristicRead .
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mGatt == null) {
            Log.w(TAG, "Bluetooth isn't set");
            return;
        }
        mGatt.readCharacteristic(characteristic);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // Following heart rate profile specification
        if (HEART_RATE_MEASUREMENT_CHAR_UUID.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                        stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public void getSupportedGattServices() {
        if (mGatt == null) return;

        displayGattServices(mGatt.getServices());
    }
}
