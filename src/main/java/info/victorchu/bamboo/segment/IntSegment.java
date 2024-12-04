package info.victorchu.bamboo.segment;

import static info.victorchu.bamboo.segment.SegmentUtils.checkReadablePosition;
import static info.victorchu.bamboo.utils.SizeOf.instanceSize;
import static info.victorchu.bamboo.utils.SizeOf.sizeOf;
import static java.lang.Math.max;

import info.victorchu.bamboo.RetainedSizeAware;

import java.util.Arrays;

import javax.annotation.Nullable;

public class IntSegment
        implements Segment, RetainedSizeAware
{

    private static final int INSTANCE_SIZE = instanceSize(IntSegment.class);
    public static final int SIZE_IN_BYTES_PER_POSITION = Integer.BYTES + Byte.BYTES;

    /* ============= values =========== */
    private boolean[] valueIsNull = new boolean[0];
    private int[] values = new int[0];
    /* ============= values =========== */

    private final int initialCount;
    private boolean initialized;
    private int position;

    private SegmentStatus status;
    private final SizeCalculator sizeCalculator;

    public IntSegment(@Nullable SegmentStatus status, int expectedEntries, SizeCalculator sizeCalculator)
    {
        this.initialCount = max(expectedEntries, 1);
        this.status = status;
        this.sizeCalculator = sizeCalculator;
        if(status != null){
            status.updateMemUsedSize(getRetainedSize());
        }
    }

    private void ensureCapacity(int capacity)
    {
        if (values.length >= capacity) {
            return;
        }
        int newSize;
        if (initialized) {
            newSize = sizeCalculator.calculateNewArraySize(capacity);
        }
        else {
            newSize = initialCount;
            initialized = true;
        }
        newSize = max(newSize, capacity);

        valueIsNull = Arrays.copyOf(valueIsNull, newSize);
        values = Arrays.copyOf(values, newSize);
        if(status != null){
            status.updateMemUsedSize(getRetainedSize());
        }
    }

    @Override
    public long getRetainedSize()
    {
        long retainedSize = INSTANCE_SIZE + sizeOf(valueIsNull) + sizeOf(values);
        // sizeCalculator is shared, so we don't count it‘s Size here
        if (status != null) {
            retainedSize += status.getRetainedSize();
        }
        return retainedSize;
    }

    @Override
    public int getPosition()
    {
        return position;
    }

    @Override
    public int getCapacity()
    {
        return values.length;
    }

    public int getInt(int position)
    {
        checkReadablePosition(this, position);
        return values[position];
    }

    public IntSegment appendInt(int value)
    {
        ensureCapacity(position + 1);
        values[position] = value;
        position++;
        if (status != null) {
            status.hasNonNullValue();
            status.addBytes(SIZE_IN_BYTES_PER_POSITION);
        }
        return this;
    }

    public boolean isNull(int position)
    {
        checkReadablePosition(this,position);
        return valueIsNull[position];
    }

    public IntSegment appendNull()
    {
        ensureCapacity(position + 1);
        valueIsNull[position] = true;
        position++;
        if (status != null) {
            status.hasNullValue();
            status.addBytes(Byte.BYTES + Integer.BYTES);
        }
        return this;
    }
}
