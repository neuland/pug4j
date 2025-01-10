package de.neuland.pug4j.jexl3;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.jexl3.JexlArithmetic;

import java.util.Collection;

import static org.apache.commons.lang3.StringUtils.isNumeric;

public class PugJexlArithmetic extends JexlArithmetic {

    public PugJexlArithmetic(boolean astrict) {
        super(astrict);
    }

    /**
     * Prioritize String comparison
     *
     * @param left     the left operand
     * @param right    the right operator
     * @param operator the operator
     * @return int
     */
    @Override
    protected int compare(final Object left, final Object right, final String operator) {
        if (left != null && right != null) {
            if (left instanceof String || right instanceof String) {
                return toString(left).compareTo(toString(right));
            }
        }
        return super.compare(left,right,operator);
    }

    /**
     * using the original implementation
     * added check for empty lists
     * defaulting to "true"
     */
    @Override
    public boolean toBoolean(final Object val) {
        if (val instanceof Collection) {
            return CollectionUtils.isNotEmpty((Collection) val);
        }
        return super.toBoolean(val);
    }
    @Override
    public Object subtract(final Object left, final Object right) {
        if ((left instanceof String && !isNumeric((String)left) && !isFloatingPointNumber(left)) || (right instanceof String&& !isNumeric((String)right) && !isFloatingPointNumber(right))) {
            return "null";
        }
        return super.subtract(left, right);
    }
    @Override
    public Object add(Object left, Object right) {
        if (left instanceof String || right instanceof String) {
            return (left == null ? "" : toString(left)).concat(right == null ? "" : toString(right));
        }
        return super.add(left, right);
    }

    @Override
    public MapBuilder mapBuilder(int size) {
        return this.mapBuilder(size, false);
    }

    @Override
    public MapBuilder mapBuilder(int size, boolean extended) {
        return new de.neuland.pug4j.jexl3.internal.MapBuilder(size, extended);
    }

}
