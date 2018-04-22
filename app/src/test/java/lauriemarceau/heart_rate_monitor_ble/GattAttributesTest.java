package lauriemarceau.heart_rate_monitor_ble;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class GattAttributesTest {

    @Test
    public void convertFromInteger() {
        UUID uuidActualService = GattAttributes.convertFromInteger(0x180D);
        UUID uuidExpectedService = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
        assertEquals("Conversion from int to uuid failed for service",
                uuidExpectedService, uuidActualService);

        UUID uuidActualChar = GattAttributes.convertFromInteger(0x2A37);
        UUID uuidExpectedChar = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
        assertEquals("Conversion from int to uuid failed for characteristic",
                uuidExpectedChar, uuidActualChar);
    }
}