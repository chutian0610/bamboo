package info.victorchu.bamboo.segment.utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static sun.misc.Unsafe.ARRAY_BOOLEAN_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_BYTE_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_DOUBLE_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_FLOAT_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_INT_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_LONG_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_SHORT_INDEX_SCALE;

public class JvmUtils
{
    public static final Unsafe unsafe;

    private static void assertArrayIndexScale(final String name, int actualIndexScale, int expectedIndexScale)
    {
        if (actualIndexScale != expectedIndexScale) {
            throw new IllegalStateException(name + " array index scale must be " + expectedIndexScale + ", but is " + actualIndexScale);
        }
    }

    static {
        try {
            // fetch theUnsafe object
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            if (unsafe == null) {
                throw new RuntimeException("Unsafe access not available");
            }

            // verify the stride of arrays matches the width of primitives
            assertArrayIndexScale("Boolean", ARRAY_BOOLEAN_INDEX_SCALE, 1);
            assertArrayIndexScale("Byte", ARRAY_BYTE_INDEX_SCALE, 1);
            assertArrayIndexScale("Short", ARRAY_SHORT_INDEX_SCALE, 2);
            assertArrayIndexScale("Int", ARRAY_INT_INDEX_SCALE, 4);
            assertArrayIndexScale("Long", ARRAY_LONG_INDEX_SCALE, 8);
            assertArrayIndexScale("Float", ARRAY_FLOAT_INDEX_SCALE, 4);
            assertArrayIndexScale("Double", ARRAY_DOUBLE_INDEX_SCALE, 8);
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
