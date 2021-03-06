package org.odk.collect.android.utilities;

import android.location.Location;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.geo.MapPoint;
import org.odk.collect.android.location.LocationTestUtils;
import org.odk.collect.shared.TempFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.location.LocationManager.GPS_PROVIDER;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.odk.collect.android.geo.MapFragment.KEY_REFERENCE_LAYER;

@RunWith(AndroidJUnit4.class)
public class GeoUtilsTest {
    private final List<MapPoint> points = new ArrayList<>(Arrays.asList(
            new MapPoint(11, 12, 13, 14),
            new MapPoint(21, 22, 23, 24),
            new MapPoint(31, 32, 33, 34)
    ));

    @Test
    public void whenPointsAreNull_formatPoints_returnsEmptyString() {
        assertEquals(GeoUtils.formatPointsResultString(Collections.emptyList(), true), "");
        assertEquals(GeoUtils.formatPointsResultString(Collections.emptyList(), false), "");
    }

    @Test
    public void geotraces_areSeparatedBySemicolon_withoutTrialingSemicolon() {
        assertEquals(GeoUtils.formatPointsResultString(points, false),
                "11.0 12.0 13.0 14.0;21.0 22.0 23.0 24.0;31.0 32.0 33.0 34.0");
    }

    @Test
    public void geoshapes_areSeparatedBySemicolon_withoutTrialingSemicolon_andHaveMatchingFirstAndLastPoints() {
        assertEquals(GeoUtils.formatPointsResultString(points, true),
                "11.0 12.0 13.0 14.0;21.0 22.0 23.0 24.0;31.0 32.0 33.0 34.0;11.0 12.0 13.0 14.0");
    }

    @Test
    public void test_formatLocationResultString() {
        Location location = LocationTestUtils.createLocation("GPS", 1, 2, 3, 4);
        assertEquals(GeoUtils.formatLocationResultString(location), "1.0 2.0 3.0 4.0");
    }

    @Test
    public void capitalizesGps() {
        String input = "gps";
        assertEquals("GPS", GeoUtils.capitalizeGps(input));

        String locationProvider = "network";
        assertEquals("network", GeoUtils.capitalizeGps(locationProvider));

        String nullLocationProvider = null;
        assertNull(GeoUtils.capitalizeGps(nullLocationProvider));
    }

    @Test
    public void getReferenceLayerFile_whenPathIsNull_should_getReferenceLayerFileReturnNull() {
        String layersPath = TempFiles.createTempDir().getAbsolutePath();
        Bundle config = new Bundle();
        config.putString(KEY_REFERENCE_LAYER, null);

        assertNull(GeoUtils.getReferenceLayerFile(config, layersPath));
    }

    @Test
    public void whenOfflineLayerFileDoesNotExist_should_getReferenceLayerFileReturnNull() {
        String layersPath = TempFiles.createTempDir().getAbsolutePath();
        Bundle config = new Bundle();
        config.putString(KEY_REFERENCE_LAYER, "blah");

        assertNull(GeoUtils.getReferenceLayerFile(config, layersPath));
    }

    @Test
    public void whenOfflineLayerFileExist_should_getReferenceLayerFileReturnThatFile() {
        String layersPath = TempFiles.createTempDir().getAbsolutePath();
        FileUtils.write(new File(layersPath, "blah"), new byte[]{});

        Bundle config = new Bundle();
        config.putString(KEY_REFERENCE_LAYER, "blah");

        assertNotNull(GeoUtils.getReferenceLayerFile(config, layersPath));
    }

    @Test
    public void whenAccuracyIsNegative_shouldBeSetToZeroAfterSanitizing() {
        Location location = LocationTestUtils.createLocation(GPS_PROVIDER, 7, 2, 3, -1.0f);
        Location sanitizedLocation = GeoUtils.sanitizeAccuracy(location);
        assertThat(sanitizedLocation.getLatitude(), is(7.0));
        assertThat(sanitizedLocation.getLongitude(), is(2.0));
        assertThat(sanitizedLocation.getAltitude(), is(3.0));
        assertThat(sanitizedLocation.getAccuracy(), is(0.0f));
    }

    @Test
    public void whenLocationIsMocked_shouldAccuracyBeSetToZeroAfterSanitizing() {
        Location location = LocationTestUtils.createLocation(GPS_PROVIDER, 7, 2, 3, 5.0f, true);
        Location sanitizedLocation = GeoUtils.sanitizeAccuracy(location);
        assertThat(sanitizedLocation.getLatitude(), is(7.0));
        assertThat(sanitizedLocation.getLongitude(), is(2.0));
        assertThat(sanitizedLocation.getAltitude(), is(3.0));
        assertThat(sanitizedLocation.getAccuracy(), is(0.0f));
    }

    @Test
    public void whenLocationIsNull_shouldNullBeReturnedAfterSanitizing() {
        assertThat(GeoUtils.sanitizeAccuracy(null), nullValue());
    }
}
