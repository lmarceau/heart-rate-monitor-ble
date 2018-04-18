package lauriemarceau.heart_rate_monitor_ble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceActivity extends AppCompatActivity {

    private final static String TAG = DeviceActivity.class.getSimpleName();

    private ArrayList<ArrayList<GattAttributes>> mGattCharacteristics = new ArrayList<>();
    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;

    public TextView deviceName;
    public TextView batteryLevelValue;
    public TextView heartRateValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        mDevice =  getIntent().getParcelableExtra("Device");

        deviceName = findViewById(R.id.DeviceName);
        deviceName.setText(mDevice.getName());

        batteryLevelValue = findViewById(R.id.batteryLevelValue);
        heartRateValue = findViewById(R.id.heartRateValueText);

    }
}
