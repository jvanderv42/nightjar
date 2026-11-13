package com.limelight.binding.input;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class GyroMappingPreferencesTest {
    private Application app;

    @Before public void setUp() {
        app = ApplicationProvider.getApplicationContext();
        app.getSharedPreferences("nightjar_gyro_mapping", 0).edit().clear().commit();
    }

    @After public void tearDown() {
        app.getSharedPreferences("nightjar_gyro_mapping", 0).edit().clear().commit();
    }

    @Test public void knownFlydigiDescriptorsUseVerifiedTransformOnlyInAutomatic() {
        String apex = "874157d9fb2e682d4c2eab8f623109d67ce11434";
        float[] expected = {-2f, 3f, 1f};
        assertArrayEquals(expected,
                GyroMappingPreferences.transformer(app, apex).transform(1f, 2f, 3f), 0f);
        assertArrayEquals(new float[] {1f, 2f, 3f},
                GyroMappingPreferences.transformer(app, "unknown").transform(1f, 2f, 3f), 0f);
        GyroMappingPreferences.setChoice(app, apex, GyroMappingPreferences.IDENTITY);
        assertArrayEquals(new float[] {1f, 2f, 3f},
                GyroMappingPreferences.transformer(app, apex).transform(1f, 2f, 3f), 0f);
    }

    @Test public void customAssignmentAndSavedAxisInversionSurviveStorageRoundTrip() {
        String descriptor = "custom-controller";
        GyroMappingPreferences.Profile profile = new GyroMappingPreferences.Profile(null, "My gyro",
                new int[] {2, 0, 1}, new int[] {-1, 1, -1});
        GyroMappingPreferences.saveProfile(app, profile);
        GyroMappingPreferences.setChoice(app, descriptor, GyroMappingPreferences.CUSTOM_PREFIX + profile.id);
        assertEquals("My gyro", GyroMappingPreferences.profiles(app).get(0).name);
        assertArrayEquals(new float[] {-3f, 1f, -2f},
                GyroMappingPreferences.transformer(app, descriptor).transform(1f, 2f, 3f), 0f);
        GyroMappingPreferences.deleteProfile(app, profile.id);
        assertEquals(GyroMappingPreferences.AUTO, GyroMappingPreferences.getChoice(app, descriptor));
        assertArrayEquals(new float[] {1f, 2f, 3f},
                GyroMappingPreferences.transformer(app, descriptor).transform(1f, 2f, 3f), 0f);
    }

    @Test public void invalidAxesCannotBeSaved() {
        assertThrows(IllegalArgumentException.class, () -> new GyroMappingPreferences.Profile(
                null, "Invalid", new int[] {0, 0, 2}, new int[] {1, 1, 1}));
    }
}
