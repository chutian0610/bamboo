package info.victorchu.bamboo.segment;

import info.victorchu.bamboo.segment.buffer.Buffer;
import info.victorchu.bamboo.segment.buffer.Buffers;
import info.victorchu.bamboo.segment.buffer.DefaultBufferSizeCalculator;

import javax.annotation.Nullable;

import java.util.Arrays;

import static info.victorchu.bamboo.segment.SegmentUtils.checkReadablePosition;
import static info.victorchu.bamboo.segment.utils.SizeOf.instanceSize;
import static info.victorchu.bamboo.segment.utils.SizeOf.sizeOf;
import static java.lang.Math.min;

public class VariableWidthSegment
        implements Segment, RetainedSizeAware
{
    private static final int INSTANCE_SIZE = instanceSize(VariableWidthSegment.class);
    private static final Segment NULL_VALUE_SEGMENT = new VariableWidthSegment(null, 0, 0,DefaultSizeCalculator.INSTANCE,
            DefaultBufferSizeCalculator.INSTANCE);
    private static final int SIZE_IN_BYTES_PER_POSITION = Integer.BYTES + Byte.BYTES;

    protected final int initialEntityCount;
    private final int initialBufferOutputSize;
    protected final SegmentStatus status;
    protected final SizeCalculator sizeCalculator;
    protected final SizeCalculator bufferSizeCalculator;

    protected int position;

    /* ============= storage =========== */
    private boolean[] valueIsNull = new boolean[0];
    private int[] offsets = new int[1];
    private Buffer buffer;
    /* ============= storage =========== */

    private boolean hasNullValue;
    private boolean hasNonNullValue;

    public VariableWidthSegment(@Nullable SegmentStatus status, int expectedEntries, int expectedBytes,SizeCalculator sizeCalculator,SizeCalculator bufferSizeCalculator)
    {
        this.status = status;
        this.initialEntityCount = expectedEntries;
        this.initialBufferOutputSize = min(expectedBytes, SizeCalculator.MAX_ARRAY_SIZE);
        this.sizeCalculator = sizeCalculator;
        this.bufferSizeCalculator = bufferSizeCalculator;
        buffer = Buffers.allocate(initialBufferOutputSize, bufferSizeCalculator);
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

    @Override
    public long getRetainedSize()
    {
        long size = INSTANCE_SIZE + sizeOf(valueIsNull) + sizeOf(offsets) + buffer.getRetainedSize();;
        if (status != null) {
            size += SegmentStatus.INSTANCE_SIZE;
        }
        return size;
    }

    public VariableWidthSegment appendEntry(Buffer source)
    {
        return appendEntry(source, 0, source.length());
    }
    public VariableWidthSegment appendEntry(Buffer source, int sourceIndex, int length)
    {
        ensureFreeSpace(length);
        source.getBytes(sourceIndex, buffer, offsets[position], length);
        entryAdded(length, false);
        return this;
    }

    public VariableWidthSegment appendEntry(byte[] source, int sourceIndex, int length)
    {
        ensureFreeSpace(length);
        buffer.setBytes( offsets[position] ,source,sourceIndex,length);
        entryAdded(length, false);
        return this;
    }

    private void ensureCapacity(int capacity)
    {
        if (valueIsNull.length >= capacity) {
            return;
        }

        int newSize = sizeCalculator.calculateNewArraySize(capacity, initialEntityCount);
        valueIsNull = Arrays.copyOf(valueIsNull, newSize);
        offsets = Arrays.copyOf(offsets, newSize + 1);
    }

    private void ensureFreeSpace(int extraBytesCapacity)
    {
        buffer = buffer.ensureSize(extraBytesCapacity);
    }
    public VariableWidthSegment appendNull()
    {
        hasNullValue = true;
        entryAdded(0, true);
        return this;
    }
    private void entryAdded(int bytesWritten, boolean isNull)
    {
        ensureCapacity(position + 1);

        valueIsNull[position] = isNull;
        offsets[position + 1] = offsets[position] + bytesWritten;

        position++;
        hasNonNullValue |= !isNull;

        if (status != null) {
            status.addBytes(SIZE_IN_BYTES_PER_POSITION + bytesWritten);
        }
    }
    public boolean isNull(int position)
    {
        checkReadablePosition(this, position);
        return valueIsNull[position];
    }

    public Buffer getEntry(int position)
    {
        checkReadablePosition(this, position);
        int offset = offsets[position];
        int length = offsets[position + 1] - offset;
        return buffer.slice(offset, length);
    }
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("VariableWidthBlockBuilder{");
        sb.append("position=").append(getPosition());
        sb.append(", size=").append(offsets[position]);
        sb.append('}');
        return sb.toString();
    }
}
