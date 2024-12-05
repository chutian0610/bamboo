package info.victorchu.bamboo.segment;

import javax.annotation.Nullable;

import java.util.Arrays;

import static info.victorchu.bamboo.segment.SegmentUtils.checkReadablePosition;
import static info.victorchu.bamboo.utils.SizeOf.instanceSize;
import static info.victorchu.bamboo.utils.SizeOf.sizeOf;

public class IntSegment
        extends PrimitiveSegment
{

    public static final int SIZE_IN_BYTES_PER_POSITION = Integer.BYTES + Byte.BYTES;
    private static final int INSTANCE_SIZE = instanceSize(IntSegment.class);
    /* ============= storage =========== */
    private boolean[] valueIsNull = new boolean[0];
    private int[] values = new int[0];
    /* ============= storage =========== */

    public IntSegment(@Nullable SegmentStatus status, int expectedCapacity, SizeCalculator sizeCalculator)
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
        existNonNullValue(true);
        if (status != null) {
            status.addBytes(getSizePerPosition());
        }
        return this;
    }

    public boolean isNull(int position)
    {
        checkReadablePosition(this, position);
        return valueIsNull[position];
    }

    public IntSegment appendNull()
    {
        ensureCapacity(position + 1);
        valueIsNull[position] = true;
        position++;
        existNullValue(true);
        if (status != null) {
            status.addBytes(getSizePerPosition());
        }
        return this;
    }

    @Override
    public int getSizePerPosition()
    {
        return SIZE_IN_BYTES_PER_POSITION;
    }
}
