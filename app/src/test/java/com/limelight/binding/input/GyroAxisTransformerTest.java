package com.limelight.binding.input;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class GyroAxisTransformerTest {
    @Test
    public void identityLeavesValuesUnchanged() {
        assertArrayEquals(new float[] {1f, -2f, 3f},
                GyroAxisTransformer.identity().transform(1f, -2f, 3f), 0f);
    }

    @Test
    public void swapsAxesAndInvertsSelectedDirections() {
        GyroAxisTransformer transformer = new GyroAxisTransformer(1, 0, 2, -1, 1, -1);
        assertArrayEquals(new float[] {2f, 1f, -3f},
                transformer.transform(1f, -2f, 3f), 0f);
    }

    @Test
    public void rejectsDuplicateAxes() {
        assertThrows(IllegalArgumentException.class,
                () -> new GyroAxisTransformer(0, 0, 2, 1, 1, 1));
    }

    @Test
    public void rejectsInvalidSign() {
        assertThrows(IllegalArgumentException.class,
                () -> new GyroAxisTransformer(0, 1, 2, 1, 0, 1));
    }
}
