package com.limelight.binding.input;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.InputDevice;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opt-in, in-stream motion monitor. Samples are reported at the actual controller
 * motion transmission call sites, not from a separate Android sensor subscription.
 */
public final class GyroDiagnosticOverlay {
    private static final Object lock = new Object();
    private static final int maxRows = 1800;
    private static final List<String> rows = new ArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static volatile String latest = "Waiting for controller motion...";
    private static volatile boolean recording;
    private static volatile boolean flydigiCandidateEnabled;
    public static final String PREF_SHOW_DIAGNOSTICS = "checkbox_show_gyro_diagnostics";
    private static volatile boolean enabled;

    /** Manual test setting; never enabled automatically. */
    static boolean isFlydigiCandidateEnabled() {
        return flydigiCandidateEnabled;
    }
    private static volatile TextView display;
    private static volatile Activity activity;
    private static volatile boolean visible = true;

    private GyroDiagnosticOverlay() {}

    public static void report(String path, int controller, byte type, float x, float y, float z) {
        // Skip formatting and recording unless diagnostics are explicitly enabled.
        if (!enabled) return;
        String label = type == com.limelight.nvstream.jni.MoonBridge.LI_MOTION_TYPE_GYRO
                ? "gyro" : "accel";
        latest = String.format(Locale.US, "%s controller=%d %s\nX=%+.4f  Y=%+.4f  Z=%+.4f",
                path, controller, label, x, y, z);
        synchronized (lock) {
            if (recording && rows.size() < maxRows) {
                rows.add(String.format(Locale.US, "%d,%s,%d,%s,%.6f,%.6f,%.6f",
                        android.os.SystemClock.elapsedRealtimeNanos(), path, controller,
                        label, x, y, z));
            }
        }
    }

    public static void attach(Activity host) {
        enabled = PreferenceManager.getDefaultSharedPreferences(host)
                .getBoolean(PREF_SHOW_DIAGNOSTICS, false);
        if (!enabled) return;
        visible = true;
        flydigiCandidateEnabled = false;
        activity = host;
        FrameLayout root = host.findViewById(android.R.id.content);
        LinearLayout panel = new LinearLayout(host);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(16, 12, 16, 12);
        panel.setBackgroundColor(0xDD101820);

        TextView label = new TextView(host);
        label.setTextColor(Color.WHITE);
        label.setTextSize(12);
        label.setText("GYRO DIAGNOSTICS\nWaiting for controller motion...");
        panel.addView(label);
        display = label;

        LinearLayout buttons = new LinearLayout(host);
        Button capture = new Button(host);
        capture.setText("Record");
        capture.setOnClickListener(v -> {
            synchronized (lock) {
                if (!recording) {
                    rows.clear();
                    rows.add("elapsedNanos,path,controller,type,x,y,z");
                }
                recording = !recording;
                capture.setText(recording ? "Stop" : "Record");
            }
        });
        buttons.addView(capture);

        Button mapping = new Button(host);
        mapping.setText("Override: Auto/settings");
        mapping.setOnClickListener(v -> {
            flydigiCandidateEnabled = !flydigiCandidateEnabled;
            mapping.setText(flydigiCandidateEnabled ? "Override: Force Flydigi XYZ" : "Override: Auto/settings");
        });
        buttons.addView(mapping);

        // Inspect Android's actual controller identifiers before enabling automatic matching.
        Button identify = new Button(host);
        identify.setText("Identify controllers");
        identify.setOnClickListener(v -> {
            StringBuilder devices = new StringBuilder("ANDROID INPUT DEVICES");
            for (int deviceId : InputDevice.getDeviceIds()) {
                InputDevice device = InputDevice.getDevice(deviceId);
                if (device == null) continue;
                int sources = device.getSources();
                if ((sources & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD
                        && (sources & InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK) {
                    continue;
                }
                devices.append("\\nID=").append(deviceId)
                        .append(" name=").append(device.getName())
                        .append(" VID=").append(device.getVendorId())
                        .append(" PID=").append(device.getProductId())
                        .append(" descriptor=").append(device.getDescriptor())
                        .append(" sources=0x").append(Integer.toHexString(sources));
            }
            latest = devices.toString();
            synchronized (lock) {
                // Include the identifiers in the shareable diagnostic output.
                rows.add("# " + devices.toString().replace("\\n", " | "));
            }
        });
        buttons.addView(identify);

        Button share = new Button(host);
        share.setText("Share CSV");
        share.setOnClickListener(v -> {
            String csv;
            synchronized (lock) {
                csv = android.text.TextUtils.join("\n", rows);
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "nightjarGyro.csv");
            intent.putExtra(Intent.EXTRA_TEXT, csv);
            host.startActivity(Intent.createChooser(intent, "Export gyro samples"));
        });
        buttons.addView(share);

        Button hide = new Button(host);
        hide.setText("Hide");
        hide.setOnClickListener(v -> {
            visible = !visible;
            label.setVisibility(visible ? View.VISIBLE : View.GONE);
            capture.setVisibility(visible ? View.VISIBLE : View.GONE);
            share.setVisibility(visible ? View.VISIBLE : View.GONE);
            mapping.setVisibility(visible ? View.VISIBLE : View.GONE);
            identify.setVisibility(visible ? View.VISIBLE : View.GONE);
            hide.setText(visible ? "Hide" : "Show");
        });
        buttons.addView(hide);
        panel.addView(buttons);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        root.addView(panel, params);
        mainHandler.removeCallbacks(refresh);
        mainHandler.post(refresh);
    }

    private static final Runnable refresh = new Runnable() {
        @Override public void run() {
            TextView target = display;
            if (target != null && activity != null) {
                target.setText("GYRO DIAGNOSTICS\n" + latest +
                        (recording ? "\nRECORDING" : ""));
                mainHandler.postDelayed(this, 100);
            }
        }
    };

    public static void detach(Activity host) {
        if (activity == host) {
            enabled = false;
            mainHandler.removeCallbacks(refresh);
            display = null;
            activity = null;
            recording = false;
            flydigiCandidateEnabled = false;
        }
    }
}
