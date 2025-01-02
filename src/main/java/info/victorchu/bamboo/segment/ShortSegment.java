package info.victorchu.bamboo.segment;

import javax.annotation.Nullable;

import java.util.Arrays;

import static info.victorchu.bamboo.segment.SegmentUtils.checkReadablePosition;
import static info.victorchu.bamboo.segment.utils.SizeOf.instanceSize;
import static info.victorchu.bamboo.segment.utils.SizeOf.sizeOf;

public class ShortSegment
        extends FixedWidthSegment
{

    public static final int SIZE_IN_BYTES_PER_ENTITY = Byte.BYTES + Short.BYTES;
    private static final int INSTANCE_SIZE = instanceSize(ShortSegment.class);

    /* ============= storage =========== */
    private boolean[] valueIsNull = new boolean[0];
    private short[] values = new short[0];
    /* ============= storage =========== */

    public ShortSegment(@Nullable SegmentStatus status, int expectedCapacity, SizeCalculator sizeCalculator)
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

    public short getShort(int position)
    {
        checkReadablePosition(this, position);
        return values[position];
    }

    public ShortSegment appendShort(short value)
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

    public ShortSegment appendNull()
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
        StringBuilder sb = new StringBuilder("ShortSegment{");
        sb.append("position=").append(getPosition());
        sb.append('}');
        return sb.toString();
    }
}
