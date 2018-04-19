package lauriemarceau.heart_rate_monitor_ble;

import android.support.v7.app.AppCompatActivity;

import java.util.HashMap;
import java.util.UUID;

/**
 * Gatt profile attributes needed for the actual project; i.e heart rate service,
 * battery service and device information service
 */

public class GattAttributes extends AppCompatActivity {

    private static HashMap<UUID, String> gattAttributes = new HashMap<>();

    public static UUID HEART_RATE_SERVICE_UUID = convertFromInteger(0x180D);
    public static UUID HEART_RATE_MEASUREMENT_CHAR_UUID = convertFromInteger(0x2A37);
    public static UUID HEART_RATE_CONTROL_POINT_CHAR_UUID = convertFromInteger(0x2A39);
    public static UUID BATTERY_SERVICE_UUID = convertFromInteger(0x180F);
    public static UUID BATTERY_LEVEL_UUID = convertFromInteger(0x2A19);
    public static UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = convertFromInteger(0x2902);

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

    static {
         gattAttributes.put(HEART_RATE_SERVICE_UUID, "Heart rate service");
         gattAttributes.put(HEART_RATE_MEASUREMENT_CHAR_UUID, "Heart rate measurement char");
         gattAttributes.put(HEART_RATE_CONTROL_POINT_CHAR_UUID, "Heart rate control point");
         gattAttributes.put(BATTERY_SERVICE_UUID, "Battery service");
         gattAttributes.put(BATTERY_LEVEL_UUID, "Battery level");
         gattAttributes.put(CLIENT_CHARACTERISTIC_CONFIG_UUID, "Client char config");
    }

    public static String findName(UUID uuid, String defaultName){
         String name = gattAttributes.get(uuid);
         return name == null ? defaultName : name;
    }
}
