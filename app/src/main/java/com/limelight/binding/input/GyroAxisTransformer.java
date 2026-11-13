package com.limelight.binding.input;

/**
 * Maps controller motion axes without changing their units.
 *
 * An identity mapping is the safe default. Controller-specific mappings must be
 * selected only after comparing sensor readings against a known-good controller.
 */
final class GyroAxisTransformer {
    private final int[] sourceAxes;
    private final int[] signs;
    private final float horizontalSensitivity;
    private final float verticalSensitivity;

    GyroAxisTransformer(int xAxis, int yAxis, int zAxis,
                        int xSign, int ySign, int zSign) {
        this(xAxis, yAxis, zAxis, xSign, ySign, zSign, 1f, 1f);
    }

    GyroAxisTransformer(int xAxis, int yAxis, int zAxis,
                        int xSign, int ySign, int zSign,
                        float horizontalSensitivity, float verticalSensitivity) {
        if (!Float.isFinite(horizontalSensitivity) || !Float.isFinite(verticalSensitivity)
                || horizontalSensitivity < 0.1f || horizontalSensitivity > 3f
                || verticalSensitivity < 0.1f || verticalSensitivity > 3f) {
            throw new IllegalArgumentException("Sensitivity must be between 0.10 and 3.00");
        }
        this.horizontalSensitivity = horizontalSensitivity;
        this.verticalSensitivity = verticalSensitivity;
        sourceAxes = new int[] {xAxis, yAxis, zAxis};
        signs = new int[] {xSign, ySign, zSign};
        boolean[] seen = new boolean[3];
        for (int axis : sourceAxes) {
            if (axis < 0 || axis > 2 || seen[axis]) {
                throw new IllegalArgumentException("Axes must be a permutation of 0, 1, 2");
            }
            seen[axis] = true;
        }
        for (int sign : signs) {
            if (sign != 1 && sign != -1) {
                throw new IllegalArgumentException("Axis signs must be +1 or -1");
            }
        }
    }

    static GyroAxisTransformer identity() {
        return new GyroAxisTransformer(0, 1, 2, 1, 1, 1);
    }

    float[] transform(float x, float y, float z) {
        float[] input = {x, y, z};
        return new float[] {
                input[sourceAxes[0]] * signs[0] * verticalSensitivity,
                input[sourceAxes[1]] * signs[1] * horizontalSensitivity,
                input[sourceAxes[2]] * signs[2]
        };
    }
}
