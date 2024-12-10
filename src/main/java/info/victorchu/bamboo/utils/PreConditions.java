package info.victorchu.bamboo.utils;

public class PreConditions
{
    public static void checkFromIndexSize(int fromIndex, int size, int length)
    {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException(String.format("Range [%d, %<d + %d) out of bounds for length %d",
                    fromIndex, size, length));
        }
    }
}
