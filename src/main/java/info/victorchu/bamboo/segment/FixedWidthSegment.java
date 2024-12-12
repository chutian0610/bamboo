package info.victorchu.bamboo.segment;

import javax.annotation.Nullable;

import static java.lang.Math.max;

public abstract class FixedWidthSegment
        implements Segment, RetainedSizeAware
{
    protected final int initialEntityCount;
    protected final SegmentStatus status;
    protected final SizeCalculator sizeCalculator;
    protected boolean initialized;
    protected int position;
    private boolean hasNullValue;
    private boolean hasNonNullValue;

    public FixedWidthSegment(@Nullable SegmentStatus status, int expectedCapacity, SizeCalculator sizeCalculator)
    {
        this.initialEntityCount = max(expectedCapacity, 1);
        this.status = status;
        this.sizeCalculator = sizeCalculator;
        if (status != null) {
            status.updateMemUsedSize(getRetainedSize());
        }
    }

    protected void ensureCapacity(int capacity)
    {
        if (getCapacity() >= capacity) {
            return;
        }
        int newSize;
        if (initialized) {
            newSize = sizeCalculator.calculateNewArraySize(getCapacity(), capacity);
        }
        else {
            newSize = initialEntityCount;
            initialized = true;
        }
        newSize = max(newSize, capacity);
        reAllocate(newSize);
        if (status != null) {
            status.updateMemUsedSize(getRetainedSize());
        }
    }

    public void existNullValue(boolean hasNullValue)
    {
        this.hasNullValue = hasNullValue;
    }

    public void existNonNullValue(boolean hasNonNullValue)
    {
        this.hasNonNullValue = hasNonNullValue;
    }

    public boolean hasNullValue()
    {
        return hasNullValue;
    }

    public boolean hasNonNullValue()
    {
        return hasNonNullValue;
    }

    public abstract void reAllocate(int newSize);

    public abstract int getEntitySize();
}
