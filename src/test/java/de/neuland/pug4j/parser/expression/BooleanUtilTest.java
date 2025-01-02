package de.neuland.pug4j.parser.expression;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.neuland.pug4j.expression.BooleanUtil;

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
		trues.add(new int[] { 1, 2 });
		trues.add(new Object());

		for (Object object : falses) {
			assertFalse(object + " (" + object.getClass().getName() + ") should be false", BooleanUtil.convert(object));
		}
		for (Object object : trues) {
			assertTrue(object + " (" + object.getClass().getName() + ") should be true", BooleanUtil.convert(object));
		}
	}
}
