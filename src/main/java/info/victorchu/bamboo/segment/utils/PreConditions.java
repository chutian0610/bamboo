package info.victorchu.bamboo.segment.utils;

public final class PreConditions
{
    private PreConditions() {}

    public static void checkFromIndexSize(int fromIndex, int size, int length)
    {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException(String.format("Range [%d, %<d + %d) out of bounds for length %d",
                    fromIndex, size, length));
        }
    }

    public static void checkFromToIndex(int fromIndex, int toIndex, int length)
    {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex " + fromIndex + " > toIndex " + toIndex);
        }
        if (fromIndex < 0 || toIndex > length) {
            throw new IndexOutOfBoundsException(String.format("Range [%d, %d) out of bounds for length %d",
                    fromIndex, toIndex, length));
        }
    }

    public static void checkArgument(boolean expression, String errorMessage)
    {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void verify(boolean condition)
    {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
