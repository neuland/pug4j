package de.neuland.pug4j.expression;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author hoang
 * @implNote IntelliJ IDEA
 * @since 19/04/2022 20:44
 */
public class BooleanUtilTest {

    @Test
    public void testNull() {
        assertFalse(BooleanUtil.convert(null));
    }

    @Test
    public void testList() {
        List<String> emptyList = new ArrayList<>();
        List<String> nonEmptyList = new ArrayList<>();
        nonEmptyList.add("Test string");
        assertFalse(BooleanUtil.convert(emptyList));
        assertTrue(BooleanUtil.convert(nonEmptyList));
    }

    @Test
    public void testBoolean() {
        Boolean booleanValue = true;
        assertTrue(BooleanUtil.convert(booleanValue));
    }

    @Test
    public void testIntArray() {
        int[] emptyIntArray = {};
        int[] nonEmptyIntArray = {0, 1};
        assertTrue(BooleanUtil.convert(nonEmptyIntArray));
        assertFalse(BooleanUtil.convert(emptyIntArray));
    }

    @Test
    public void testDoubleArray() {
        double[] emptyDoubleArray = {};
        double[] nonEmptyDoubleArray = {0.0, 1.0};
        assertTrue(BooleanUtil.convert(nonEmptyDoubleArray));
        assertFalse(BooleanUtil.convert(emptyDoubleArray));
    }

    @Test
    public void testFloatArray() {
        float[] emptyFloatArray = {};
        float[] nonEmptyFloatArray = {0f, 1f};
        assertTrue(BooleanUtil.convert(nonEmptyFloatArray));
        assertFalse(BooleanUtil.convert(emptyFloatArray));
    }

    @Test
    public void testObjectArray() {
        Object[] emptyObjectArray = {};
        Object[] nonEmptyObjectArray = {new Object()};
        assertTrue(BooleanUtil.convert(nonEmptyObjectArray));
        assertFalse(BooleanUtil.convert(emptyObjectArray));
    }

    @Test
    public void testNumber() {
        Number nonZeroNumber = 2;
        Number zeroNumber = 0;
        assertTrue(BooleanUtil.convert(nonZeroNumber));
        assertFalse(BooleanUtil.convert(zeroNumber));
    }

    @Test
    public void testString() {
        String emptyString = "";
        String nonEmptyString = "nonEmptyString";
        assertTrue(BooleanUtil.convert(nonEmptyString));
        assertFalse(BooleanUtil.convert(emptyString));
    }

    @Test
    public void testOthers() {
        Map<String, Number> testMap = new HashMap<>();
        assertTrue(BooleanUtil.convert(testMap));
    }
}
