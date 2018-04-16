package lauriemarceau.heart_rate_monitor_ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.UUID;

public class DeviceActivity extends AppCompatActivity {

    public static UUID HEART_RATE_SERVICE_UUID = convertFromInteger(0x180D);
    public static UUID HEART_RATE_MEASUREMENT_CHAR_UUID = convertFromInteger(0x2A37);
    public static UUID HEART_RATE_CONTROL_POINT_CHAR_UUID = convertFromInteger(0x2A39);
    public static UUID BATTERY_SERVICE_UUID = convertFromInteger(0x180F);
    public static UUID BATTERY_LEVEL_UUID = convertFromInteger(0x2A19);
    public static UUID DEVICE_INFORMATION_SERVICE_UUID = convertFromInteger( 0x180A);
    public static UUID DEVICE_NAME_UUID = convertFromInteger(0x2A29);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);


    }

    /**
     * convert from an integer to UUID.
     * @param i integer input
     * @return UUID
     */
     public static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }
}
