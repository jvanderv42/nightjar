package com.limelight.binding.input;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.InputDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Descriptor-specific mapping and reusable, named custom gyroscope profiles. */
public final class GyroMappingPreferences {
    public static final String AUTO = "auto";
    public static final String IDENTITY = "identity";
    public static final String FLYDIGI = "flydigi";
    public static final String CUSTOM_PREFIX = "custom:";

    private static final String STORE = "nightjar_gyro_mapping";
    private static final String PREFIX = "descriptor_";
    private static final String PROFILES = "custom_profiles";
    private static final String APEX_4 = "874157d9fb2e682d4c2eab8f623109d67ce11434";
    private static final String VADER_4_PRO = "4e035567e6776a39545a37442b30e562c79cedb3";
    private static final GyroAxisTransformer FLYDIGI_TRANSFORM =
            new GyroAxisTransformer(1, 2, 0, -1, 1, 1);

    private GyroMappingPreferences() {}

    /** Immutable user profile. Axes are a permutation of 0,1,2; signs must be +1 or -1. */
    public static final class Profile {
        public final String id;
        public final String name;
        public final int[] axes;
        public final int[] signs;

        public Profile(String id, String name, int[] axes, int[] signs) {
            if (name == null || name.trim().isEmpty() || axes == null || signs == null
                    || axes.length != 3 || signs.length != 3) {
                throw new IllegalArgumentException("Name and three axes/signs are required");
            }
            boolean[] seen = new boolean[3];
            for (int i = 0; i < 3; i++) {
                if (axes[i] < 0 || axes[i] > 2 || seen[axes[i]]
                        || (signs[i] != -1 && signs[i] != 1)) {
                    throw new IllegalArgumentException("Axes must be unique X, Y, Z with signs +1/-1");
                }
                seen[axes[i]] = true;
            }
            this.id = id == null || id.isEmpty() ? UUID.randomUUID().toString() : id;
            this.name = name.trim();
            this.axes = axes.clone();
            this.signs = signs.clone();
        }

        public GyroAxisTransformer transformer() {
            return new GyroAxisTransformer(axes[0], axes[1], axes[2],
                    signs[0], signs[1], signs[2]);
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }

    public static List<Profile> profiles(Context context) {
        List<Profile> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences(context).getString(PROFILES, "[]"));
            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject p = array.getJSONObject(i);
                    JSONArray axes = p.getJSONArray("axes");
                    JSONArray signs = p.getJSONArray("signs");
                    result.add(new Profile(p.getString("id"), p.getString("name"),
                            new int[] {axes.getInt(0), axes.getInt(1), axes.getInt(2)},
                            new int[] {signs.getInt(0), signs.getInt(1), signs.getInt(2)}));
                } catch (JSONException | IllegalArgumentException ignored) {
                    // One damaged record must not disable all otherwise valid profiles.
                }
            }
        } catch (JSONException ignored) {
            // Invalid stored JSON behaves as an empty profile collection.
        }
        return result;
    }

    private static void writeProfiles(Context context, List<Profile> profiles) {
        JSONArray array = new JSONArray();
        for (Profile p : profiles) {
            JSONObject json = new JSONObject();
            try {
                json.put("id", p.id);
                json.put("name", p.name);
                json.put("axes", new JSONArray(p.axes));
                json.put("signs", new JSONArray(p.signs));
                array.put(json);
            } catch (JSONException exception) {
                throw new IllegalStateException("Failed to serialize gyro profile", exception);
            }
        }
        preferences(context).edit().putString(PROFILES, array.toString()).apply();
    }

    public static void saveProfile(Context context, Profile profile) {
        List<Profile> list = profiles(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(profile.id)) {
                list.set(i, profile);
                writeProfiles(context, list);
                return;
            }
        }
        list.add(profile);
        writeProfiles(context, list);
    }

    public static void deleteProfile(Context context, String id) {
        List<Profile> list = profiles(context);
        list.removeIf(profile -> profile.id.equals(id));
        writeProfiles(context, list);
        // Existing controller assignments safely fall back to identity if their profile is deleted.
    }

    public static String getChoice(Context context, String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) return AUTO;
        String choice = preferences(context).getString(PREFIX + descriptor, AUTO);
        if (IDENTITY.equals(choice) || FLYDIGI.equals(choice)) return choice;
        if (choice != null && choice.startsWith(CUSTOM_PREFIX)) {
            String id = choice.substring(CUSTOM_PREFIX.length());
            for (Profile profile : profiles(context)) {
                if (profile.id.equals(id)) return choice;
            }
        }
        return AUTO;
    }

    public static void setChoice(Context context, String descriptor, String choice) {
        if (descriptor == null || descriptor.isEmpty()) return;
        SharedPreferences.Editor editor = preferences(context).edit();
        boolean custom = choice != null && choice.startsWith(CUSTOM_PREFIX)
                && profiles(context).stream().anyMatch(p -> (CUSTOM_PREFIX + p.id).equals(choice));
        if (IDENTITY.equals(choice) || FLYDIGI.equals(choice) || custom) {
            editor.putString(PREFIX + descriptor, choice);
        } else {
            editor.remove(PREFIX + descriptor);
        }
        editor.apply();
    }

    public static GyroAxisTransformer transformer(Context context, String descriptor) {
        String choice = getChoice(context, descriptor);
        if (FLYDIGI.equals(choice)) return FLYDIGI_TRANSFORM;
        if (IDENTITY.equals(choice)) return GyroAxisTransformer.identity();
        if (choice.startsWith(CUSTOM_PREFIX)) {
            String id = choice.substring(CUSTOM_PREFIX.length());
            for (Profile p : profiles(context)) {
                if (p.id.equals(id)) return p.transformer();
            }
            return GyroAxisTransformer.identity();
        }
        return APEX_4.equals(descriptor) || VADER_4_PRO.equals(descriptor)
                ? FLYDIGI_TRANSFORM : GyroAxisTransformer.identity();
    }

    public static GyroAxisTransformer transformer(Context context, InputDevice device) {
        return transformer(context, device == null ? null : device.getDescriptor());
    }

    public static boolean shouldUseFlydigi(Context context, InputDevice device) {
        String choice = getChoice(context, device == null ? null : device.getDescriptor());
        return FLYDIGI.equals(choice) || AUTO.equals(choice) && device != null
                && (APEX_4.equals(device.getDescriptor()) || VADER_4_PRO.equals(device.getDescriptor()));
    }
}
