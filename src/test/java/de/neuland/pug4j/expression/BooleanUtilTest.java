package de.neuland.pug4j.expression;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.*;
import org.junit.Test;

/**
 * @author hoang
 * @implNote IntelliJ IDEA
 * @since 19/04/2022 20:44
 */
public class BooleanUtilTest {
  @Test
  public void convert() {
    List<Object> falses = new ArrayList<Object>();
    List<Object> trues = new ArrayList<Object>();

    falses.add(Integer.valueOf(0));
    falses.add(Double.valueOf(0.0));
    falses.add("");
    falses.add(Boolean.FALSE);
    falses.add(new ArrayList<String>());
    falses.add(new int[] {});

    trues.add(Integer.valueOf(1));
    trues.add(Double.valueOf(0.5));
    trues.add("a");
    trues.add(" ");
    trues.add(Boolean.TRUE);
    trues.add(Collections.singletonList("a"));
    trues.add(new int[] {1, 2});
    trues.add(new Object());

    for (Object object : falses) {
      assertFalse(
          object + " (" + object.getClass().getName() + ") should be false",
          BooleanUtil.convert(object));
    }
    for (Object object : trues) {
      assertTrue(
          object + " (" + object.getClass().getName() + ") should be true",
          BooleanUtil.convert(object));
    }
  }

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
