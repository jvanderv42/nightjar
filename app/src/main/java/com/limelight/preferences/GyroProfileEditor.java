package com.limelight.preferences;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.InputDevice;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.limelight.binding.input.GyroMappingPreferences;
import com.limelight.binding.input.GyroMappingPreferences.Profile;
import com.limelight.ui.NavigationDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Settings dialogs for assigning and editing per-device gyro mapping profiles. */
public final class GyroProfileEditor {
    private GyroProfileEditor() {}

    public static void open(Activity activity) {
        ArrayList<InputDevice> devices = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device == null || device.getDescriptor() == null) continue;
            int sources = device.getSources();
            if ((sources & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD
                    && (sources & InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK) continue;
            devices.add(device);
            String choice = GyroMappingPreferences.getChoice(activity, device.getDescriptor());
            String display = choice;
            if (choice.startsWith(GyroMappingPreferences.CUSTOM_PREFIX)) {
                for (Profile p : GyroMappingPreferences.profiles(activity)) {
                    if (choice.equals(GyroMappingPreferences.CUSTOM_PREFIX + p.id)) {
                        display = p.name;
                        break;
                    }
                }
            }
            // Switch-mode Flydigi models both report "Pro Controller"; show a descriptor
            // prefix so the two physical pads can be distinguished in the picker.
            String descriptorLabel = device.getDescriptor().substring(0,
                    Math.min(8, device.getDescriptor().length()));
            labels.add(device.getName() + " (" + device.getVendorId() + ":" +
                    device.getProductId() + ", " + descriptorLabel + ") — " + display);
        }
        labels.add("Manage custom profiles");
        new NavigationDialogBuilder(activity)
                .setTitle("Gyroscope Mapping")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which == devices.size()) manageProfiles(activity);
                    else chooseMapping(activity, devices.get(which));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void chooseMapping(Activity activity, InputDevice device) {
        final String descriptor = device.getDescriptor();
        if (descriptor == null || descriptor.isEmpty()) return;
        List<Profile> profiles = GyroMappingPreferences.profiles(activity);
        ArrayList<String> options = new ArrayList<>();
        options.add("Automatic (recommended)");
        options.add("Identity (standard)");
        options.add("Flydigi XYZ");
        for (Profile p : profiles) options.add(p.name + " (custom)");
        options.add("Create new custom profile…");

        String selected = GyroMappingPreferences.getChoice(activity, descriptor);
        int checked = GyroMappingPreferences.IDENTITY.equals(selected) ? 1
                : GyroMappingPreferences.FLYDIGI.equals(selected) ? 2 : 0;
        for (int i = 0; i < profiles.size(); i++) {
            if (selected.equals(GyroMappingPreferences.CUSTOM_PREFIX + profiles.get(i).id)) {
                checked = 3 + i;
                break;
            }
        }
        new NavigationDialogBuilder(activity)
                .setTitle(device.getName() + " gyro mapping")
                .setSingleChoiceItems(options.toArray(new String[0]), checked, (dialog, index) -> {
                    dialog.dismiss();
                    if (index == options.size() - 1) {
                        editProfile(activity, null, descriptor);
                        return;
                    }
                    String choice = index == 0 ? GyroMappingPreferences.AUTO
                            : index == 1 ? GyroMappingPreferences.IDENTITY
                            : index == 2 ? GyroMappingPreferences.FLYDIGI
                            : GyroMappingPreferences.CUSTOM_PREFIX + profiles.get(index - 3).id;
                    GyroMappingPreferences.setChoice(activity, descriptor, choice);
                    Toast.makeText(activity, "Gyro mapping saved. Start a new stream to apply.",
                            Toast.LENGTH_LONG).show();
                })
                .setNeutralButton("Manage profiles", (dialog, which) -> manageProfiles(activity))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void manageProfiles(Activity activity) {
        List<Profile> profiles = GyroMappingPreferences.profiles(activity);
        ArrayList<String> items = new ArrayList<>();
        items.add("Create new profile…");
        for (Profile profile : profiles) items.add(profile.name);
        new NavigationDialogBuilder(activity)
                .setTitle("Custom gyro profiles")
                .setItems(items.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) editProfile(activity, null, null);
                    else profileActions(activity, profiles.get(which - 1));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void profileActions(Activity activity, Profile profile) {
        new NavigationDialogBuilder(activity)
                .setTitle(profile.name)
                .setItems(new String[]{"Edit", "Duplicate", "Delete"}, (dialog, which) -> {
                    if (which == 0) editProfile(activity, profile, null);
                    else if (which == 1) editProfile(activity, new Profile(null, profile.name + " copy",
                            profile.axes, profile.signs,
                            profile.horizontalSensitivity, profile.verticalSensitivity), null);
                    else confirmDelete(activity, profile);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void confirmDelete(Activity activity, Profile profile) {
        new NavigationDialogBuilder(activity)
                .setTitle("Delete gyro profile?")
                .setMessage("Delete " + profile.name + "? Any controller using it will return to its automatic mapping.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    GyroMappingPreferences.deleteProfile(activity, profile.id);
                    Toast.makeText(activity, "Gyro profile deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void editProfile(Activity activity, Profile source, String assignDescriptor) {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        column.setPadding(pad, pad, pad, pad);

        TextView hint = new TextView(activity);
        hint.setText("Choose each output axis's source and whether to invert it. " +
                "Each source axis must be used exactly once. " +
                "Sensitivity adjusts the mapped camera yaw and pitch axes.");
        column.addView(hint);

        EditText name = new EditText(activity);
        name.setSingleLine(true);
        name.setHint("Profile name");
        name.setText(source == null ? "" : source.name);
        column.addView(name);

        Spinner[] axes = new Spinner[3];
        CheckBox[] inverse = new CheckBox[3];
        String[] names = {"X", "Y", "Z"};
        for (int i = 0; i < 3; i++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView label = new TextView(activity);
            label.setText("Output " + names[i] + " = ");
            row.addView(label);

            Spinner axis = new Spinner(activity);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            axis.setAdapter(adapter);
            axis.setSelection(source == null ? i : source.axes[i]);
            row.addView(axis, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            axes[i] = axis;

            CheckBox invert = new CheckBox(activity);
            invert.setText("Invert");
            invert.setChecked(source != null && source.signs[i] == -1);
            row.addView(invert);
            inverse[i] = invert;
            column.addView(row);
        }

        // 0.10x to 3.00x in steps of 0.05x (58 intervals), with 1.00x the default.
        float[] sensitivity = {source == null ? 1f : source.horizontalSensitivity,
                source == null ? 1f : source.verticalSensitivity};
        String[] sensitivityLabels = {"Horizontal sensitivity", "Vertical sensitivity"};
        // Give the custom dialog explicit on-surface text colors: on some devices the
        // default TextView foreground disappears against Nightjar's glass panel.
        int labelColor = com.google.android.material.color.MaterialColors.getColor(
                column, com.google.android.material.R.attr.colorOnSurface);
        for (int i = 0; i < sensitivityLabels.length; i++) {
            final int axisIndex = i;
            LinearLayout heading = new LinearLayout(activity);
            heading.setOrientation(LinearLayout.HORIZONTAL);
            heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            headingParams.topMargin = pad;
            column.addView(heading, headingParams);

            TextView axisLabel = new TextView(activity);
            axisLabel.setText(sensitivityLabels[i]);
            axisLabel.setTextColor(labelColor);
            axisLabel.setTextSize(16);
            axisLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            heading.addView(axisLabel, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView valueLabel = new TextView(activity);
            valueLabel.setText(String.format(Locale.US, "%.2f×", sensitivity[i]));
            valueLabel.setTextColor(labelColor);
            valueLabel.setTextSize(16);
            valueLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            valueLabel.setGravity(android.view.Gravity.END);
            heading.addView(valueLabel);

            SeekBar seekBar = new SeekBar(activity);
            seekBar.setMax(58);
            seekBar.setProgress(Math.round((sensitivity[i] - 0.1f) / 0.05f));
            seekBar.setContentDescription(sensitivityLabels[i]);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    sensitivity[axisIndex] = (progress + 2) / 20f;
                    valueLabel.setText(String.format(Locale.US, "%.2f×", sensitivity[axisIndex]));
                    bar.setContentDescription(String.format(Locale.US, "%s: %.2f×",
                            sensitivityLabels[axisIndex], sensitivity[axisIndex]));
                }
                @Override public void onStartTrackingTouch(SeekBar bar) {}
                @Override public void onStopTrackingTouch(SeekBar bar) {}
            });
            column.addView(seekBar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout limits = new LinearLayout(activity);
            limits.setOrientation(LinearLayout.HORIZONTAL);
            TextView minimum = new TextView(activity);
            minimum.setText("Min 0.10×");
            minimum.setTextColor(labelColor);
            limits.addView(minimum, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView maximum = new TextView(activity);
            maximum.setText("Max 3.00×");
            maximum.setTextColor(labelColor);
            maximum.setGravity(android.view.Gravity.END);
            limits.addView(maximum, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            column.addView(limits, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(column);
        AlertDialog dialog = new NavigationDialogBuilder(activity)
                .setTitle(source == null ? "Create gyro profile" : "Edit gyro profile")
                .setView(scroll)
                .setPositiveButton("Save", null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int[] selectedAxes = new int[3];
                    int[] signs = new int[3];
                    for (int i = 0; i < 3; i++) {
                        selectedAxes[i] = axes[i].getSelectedItemPosition();
                        signs[i] = inverse[i].isChecked() ? -1 : 1;
                    }
                    try {
                        Profile profile = new Profile(source == null ? null : source.id,
                                name.getText().toString(), selectedAxes, signs,
                                sensitivity[0], sensitivity[1]);
                        GyroMappingPreferences.saveProfile(activity, profile);
                        if (assignDescriptor != null) {
                            GyroMappingPreferences.setChoice(activity, assignDescriptor,
                                    GyroMappingPreferences.CUSTOM_PREFIX + profile.id);
                        }
                        Toast.makeText(activity, "Profile saved. Start a new stream to apply.",
                                Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } catch (IllegalArgumentException exception) {
                        Toast.makeText(activity, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }));
        dialog.show();
    }
}
