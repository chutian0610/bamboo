package info.victorchu.bamboo.segment;

import static java.lang.String.format;

public class DefaultSizeCalculator
        implements SizeCalculator
{

    private static final int DEFAULT_CAPACITY = 64;
    /**
     * @see java.util.ArrayList#MAX_ARRAY_SIZE for explanation
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    public int calculateNewArraySize(int currentSize)
    {
        // grow array by 50%
        long newSize = (long) currentSize + (currentSize >> 1);

        if (newSize < DEFAULT_CAPACITY) {
            newSize = DEFAULT_CAPACITY;
        }
        else if (newSize > MAX_ARRAY_SIZE) {
            newSize = MAX_ARRAY_SIZE;
            if (newSize == currentSize) {
                throw new IllegalArgumentException(format("Can not grow array beyond '%s'", MAX_ARRAY_SIZE));
            }
        }
        return (int) newSize;
    }
}
