package de.neuland.pug4j.expression;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class BooleanUtil {

  public static Boolean convert(Object in) {
    if (in == null) {
      return Boolean.FALSE;
    } else if (in instanceof List<?> list) {
      return !list.isEmpty();
    } else if (in instanceof Boolean b) {
      return b;
    } else if (in instanceof int[] arr) {
      return arr.length != 0;
    } else if (in instanceof double[] arr) {
      return arr.length != 0;
    } else if (in instanceof float[] arr) {
      return arr.length != 0;
    } else if (in instanceof Object[] arr) {
      return arr.length != 0;
    } else if (in instanceof Number n) {
      return n.doubleValue() != 0;
    } else if (in instanceof String s) {
      return !StringUtils.isEmpty(s);
    } else {
      return true;
    }
  }
}
