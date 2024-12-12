package info.victorchu.bamboo.segment;

public interface SizeCalculator
{
    /**
     * @see java.util.ArrayList#MAX_ARRAY_SIZE for explanation
     */
    int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    int calculateNewArraySize(int currentSize, int targetSize);
}
