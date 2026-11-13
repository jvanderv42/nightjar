package com.limelight.binding.input;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.InputDevice;

/**
 * Gyro mapping preferences keyed to the Android input descriptor, never the
 * generic Nintendo VID/PID advertised by Flydigi's Switch Bluetooth mode.
 */
public final class GyroMappingPreferences {
    public static final String AUTO = "auto";
    public static final String IDENTITY = "identity";
    public static final String FLYDIGI = "flydigi";

    private static final String STORE = "nightjar_gyro_mapping";
    private static final String PREFIX = "descriptor_";
    private static final String APEX_4 = "874157d9fb2e682d4c2eab8f623109d67ce11434";
    private static final String VADER_4_PRO = "4e035567e6776a39545a37442b30e562c79cedb3";

    private GyroMappingPreferences() {}

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }

    public static String getChoice(Context context, String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) return AUTO;
        String choice = preferences(context).getString(PREFIX + descriptor, AUTO);
        return IDENTITY.equals(choice) || FLYDIGI.equals(choice) ? choice : AUTO;
    }

    public static void setChoice(Context context, String descriptor, String choice) {
        if (descriptor == null || descriptor.isEmpty()) return;
        SharedPreferences.Editor editor = preferences(context).edit();
        if (IDENTITY.equals(choice) || FLYDIGI.equals(choice)) {
            editor.putString(PREFIX + descriptor, choice);
        } else {
            editor.remove(PREFIX + descriptor);
        }
        editor.apply();
    }

    public static boolean shouldUseFlydigi(Context context, String descriptor) {
        String choice = getChoice(context, descriptor);
        if (FLYDIGI.equals(choice)) return true;
        if (IDENTITY.equals(choice)) return false;
        return APEX_4.equals(descriptor) || VADER_4_PRO.equals(descriptor);
    }

    public static boolean shouldUseFlydigi(Context context, InputDevice device) {
        return device != null && shouldUseFlydigi(context, device.getDescriptor());
    }
}
