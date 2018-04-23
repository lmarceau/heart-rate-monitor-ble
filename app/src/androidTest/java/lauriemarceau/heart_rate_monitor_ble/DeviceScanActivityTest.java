package lauriemarceau.heart_rate_monitor_ble;


import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceScanActivityTest {

    @Rule
    public ActivityTestRule<DeviceScanActivity> mActivityTestRule =
            new ActivityTestRule<>(DeviceScanActivity.class);

    @Test
    public void deviceScanActivityTest() {

    }

}
