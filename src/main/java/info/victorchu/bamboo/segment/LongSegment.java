package info.victorchu.bamboo.segment;

import javax.annotation.Nullable;

import java.util.Arrays;

import static info.victorchu.bamboo.segment.SegmentUtils.checkReadablePosition;
import static info.victorchu.bamboo.segment.utils.SizeOf.instanceSize;
import static info.victorchu.bamboo.segment.utils.SizeOf.sizeOf;

public class LongSegment
        extends FixedWidthSegment
{
    public static final int SIZE_IN_BYTES_PER_ENTITY = Long.BYTES + Byte.BYTES;
    private static final int INSTANCE_SIZE = instanceSize(LongSegment.class);
    /* ============= storage =========== */
    private boolean[] valueIsNull = new boolean[0];
    private long[] values = new long[0];
    /* ============= storage =========== */

    public LongSegment(@Nullable SegmentStatus status, int expectedCapacity, SizeCalculator sizeCalculator)
    {
        super(status, expectedCapacity, sizeCalculator);
    }

    @Override
    public void reAllocate(int newSize)
    {
        valueIsNull = Arrays.copyOf(valueIsNull, newSize);
        values = Arrays.copyOf(values, newSize);
    }

    @Override
    public int getEntitySize()
    {
        return SIZE_IN_BYTES_PER_ENTITY;
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

    public long getLong(int position)
    {
        checkReadablePosition(this, position);
        return values[position];
    }

    public LongSegment appendLong(long value)
    {
        ensureCapacity(position + 1);
        values[position] = value;
        position++;
        existNonNullValue(true);
        if (status != null) {
            status.addBytes(getEntitySize());
        }
        return this;
    }

    public boolean isNull(int position)
    {
        checkReadablePosition(this, position);
        return valueIsNull[position];
    }

    public LongSegment appendNull()
    {
        ensureCapacity(position + 1);
        valueIsNull[position] = true;
        position++;
        existNullValue(true);
        if (status != null) {
            status.addBytes(getEntitySize());
        }
        return this;
    }
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("LongSegment{");
        sb.append("position=").append(position);
        sb.append('}');
        return sb.toString();
    }
}
