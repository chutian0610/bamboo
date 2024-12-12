package info.victorchu.bamboo.segment;

import javax.annotation.Nullable;

import java.util.Arrays;

import static info.victorchu.bamboo.segment.SegmentUtils.checkReadablePosition;
import static info.victorchu.bamboo.segment.utils.SizeOf.instanceSize;
import static info.victorchu.bamboo.segment.utils.SizeOf.sizeOf;

public class Int128Segment
        extends FixedWidthSegment
{
    public static final int INT128_BYTES = Long.BYTES + Long.BYTES;
    public static final int SIZE_IN_BYTES_PER_ENTITY = INT128_BYTES + Byte.BYTES;
    private static final int INSTANCE_SIZE = instanceSize(ByteSegment.class);

    /* ============= storage =========== */
    private boolean[] valueIsNull = new boolean[0];
    private long[] values = new long[0];
    /* ============= storage =========== */

    public Int128Segment(@Nullable SegmentStatus status, int expectedCapacity, SizeCalculator sizeCalculator)
    {
        super(status, expectedCapacity, sizeCalculator);
    }

    @Override
    public void reAllocate(int newSize)
    {
        valueIsNull = Arrays.copyOf(valueIsNull, newSize);
        values = Arrays.copyOf(values, newSize * 2);
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
        return valueIsNull.length;
    }

    public boolean isNull(int position)
    {
        checkReadablePosition(this, position);
        return valueIsNull[position];
    }

    public Int128Segment appendNull()
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

//    public void appendEntity(Slice source)
//    {
//        ensureCapacity(position + 1);
//
//        int valueIndex = position * 2;
//        values[valueIndex] = source.getLong(0);
//        values[valueIndex + 1] =  source.getLong(1);
//
//        existNonNullValue(true);
//        position++;
//        if (status != null) {
//            status.addBytes(getEntitySize());
//        }
//    }
}
