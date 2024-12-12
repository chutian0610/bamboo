package info.victorchu.bamboo.segment.buffer;

import info.victorchu.bamboo.segment.SizeCalculator;

public class DefaultBufferSizeCalculator
        implements SizeCalculator
{
    public static final DefaultBufferSizeCalculator INSTANCE = new DefaultBufferSizeCalculator();

    static final int SLICE_ALLOC_THRESHOLD = 524_288; // 2^19
    static final double SLICE_ALLOW_SKEW = 1.25; // must be > 1!

    @Override
    public int calculateNewArraySize(int currentSize, int targetSize)
    {
        while (currentSize < targetSize) {
            if (currentSize < SLICE_ALLOC_THRESHOLD) {
                currentSize <<= 1;
            }
            else {
                currentSize *= SLICE_ALLOW_SKEW; // double to int cast is saturating
                if (currentSize > MAX_ARRAY_SIZE && targetSize <= MAX_ARRAY_SIZE) {
                    currentSize = MAX_ARRAY_SIZE;
                }
            }
        }
        return currentSize;
    }
}
