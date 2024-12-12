package info.victorchu.bamboo.segment.buffer;

import info.victorchu.bamboo.segment.RetainedSizeAware;
import info.victorchu.bamboo.segment.SizeCalculator;
import info.victorchu.bamboo.segment.utils.JvmUtils;

import java.util.Arrays;

import static info.victorchu.bamboo.segment.SizeCalculator.MAX_ARRAY_SIZE;
import static info.victorchu.bamboo.segment.utils.PreConditions.checkFromIndexSize;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_BYTE;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_DOUBLE;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_FLOAT;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_INT;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_LONG;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_SHORT;
import static info.victorchu.bamboo.segment.utils.SizeOf.instanceSize;
import static info.victorchu.bamboo.segment.utils.SizeOf.sizeOf;
import static info.victorchu.bamboo.segment.utils.SizeOf.sizeOfByteArray;
import static java.util.Objects.requireNonNull;

public class Buffer
        implements RetainedSizeAware
{
    static final Buffer EMPTY_BUFFER = new Buffer();
    private static final int INSTANCE_SIZE = instanceSize(Buffer.class);
    private final byte[] base;
    private final int baseOffset;
    private final int size;
    private final long retainedSize;
    private int hash;
    private SizeCalculator sizeCalculator;

    /**
     * only for Buffer.EMPTY_BUFFER
     */
    private Buffer()
    {
        this.base = new byte[0];
        this.baseOffset = 0;
        this.size = 0;
        this.retainedSize = INSTANCE_SIZE;
        this.sizeCalculator = DefaultBufferSizeCalculator.INSTANCE;
    }

    Buffer(byte[] base, SizeCalculator sizeCalculator)
    {
        requireNonNull(base, "base is null");
        if (base.length == 0) {
            throw new IllegalArgumentException("Empty array");
        }
        this.base = base;
        this.baseOffset = 0;
        this.size = base.length;
        this.retainedSize = INSTANCE_SIZE + sizeOf(base);
        this.sizeCalculator = sizeCalculator;
    }

    Buffer(byte[] base, int offset, int length, SizeCalculator sizeCalculator)
    {
        requireNonNull(base, "base is null");
        if (base.length == 0) {
            throw new IllegalArgumentException("Empty array");
        }
        checkFromIndexSize(offset, length, base.length);

        this.base = base;
        this.baseOffset = offset;
        this.size = length;
        this.retainedSize = INSTANCE_SIZE + sizeOf(base);
        this.sizeCalculator = sizeCalculator;
    }

    public static Buffer allocate(int capacity, SizeCalculator sizeCalculator)
    {
        if (capacity == 0) {
            return EMPTY_BUFFER;
        }
        if (capacity > MAX_ARRAY_SIZE) {
            throw new BufferTooLargeException(String.format("Cannot allocate buffer larger than %s bytes", MAX_ARRAY_SIZE * SIZE_OF_BYTE));
        }
        return new Buffer(new byte[capacity], sizeCalculator == null ? DefaultBufferSizeCalculator.INSTANCE : sizeCalculator);
    }

    public int length()
    {
        return size;
    }

    public long getRetainedSize()
    {
        return retainedSize;
    }

    public byte[] byteArray()
    {
        return base;
    }

    public int byteArrayOffset()
    {
        return baseOffset;
    }

    public boolean isCompact()
    {
        return baseOffset == 0 && size == base.length;
    }

    /* ====================== init operation =========================*/

    public void fill(byte value)
    {
        Arrays.fill(base, baseOffset, baseOffset + size, value);
    }

    public void clear(int offset, int length)
    {
        Arrays.fill(base, baseOffset, baseOffset + size, (byte) 0);
    }

    public void clear()
    {
        clear(0, size);
    }

    /* ====================== value read operation =========================*/

    public byte getByte(int index)
    {
        checkFromIndexSize(index, SIZE_OF_BYTE, length());
        return base[baseOffset + index];
    }

    public short getShort(int index)
    {
        checkFromIndexSize(index, SIZE_OF_SHORT, length());
        return JvmUtils.unsafe.getShort(base, sizeOfByteArray(baseOffset + index));
    }

    public int getInt(int index)
    {
        checkFromIndexSize(index, SIZE_OF_INT, length());
        return JvmUtils.unsafe.getInt(base, sizeOfByteArray(baseOffset + index));
    }

    public long getLong(int index)
    {
        checkFromIndexSize(index, SIZE_OF_LONG, length());
        return JvmUtils.unsafe.getInt(base, sizeOfByteArray(baseOffset + index));
    }

    public float getFloat(int index)
    {
        checkFromIndexSize(index, SIZE_OF_FLOAT, length());
        return JvmUtils.unsafe.getFloat(base, sizeOfByteArray(baseOffset + index));
    }

    public double getDouble(int index)
    {
        checkFromIndexSize(index, SIZE_OF_DOUBLE, length());
        return JvmUtils.unsafe.getDouble(base, sizeOfByteArray(baseOffset + index));
    }

    /* ====================== value set operation =========================*/
    public void setByte(int index, byte value)
    {
        checkFromIndexSize(index, SIZE_OF_BYTE, length());
        base[baseOffset + index] = value;
    }

    void setShort(int index, short value)
    {
        checkFromIndexSize(index, SIZE_OF_SHORT, length());
        JvmUtils.unsafe.putShort(base, sizeOfByteArray(baseOffset + index), value);
    }

    public void setInt(int index, int value)
    {
        checkFromIndexSize(index, SIZE_OF_INT, length());
        JvmUtils.unsafe.putInt(base, sizeOfByteArray(baseOffset + index), value);
    }

    public void setLong(int index, long value)
    {
        checkFromIndexSize(index, SIZE_OF_LONG, length());
        JvmUtils.unsafe.putLong(base, sizeOfByteArray(baseOffset + index), value);
    }

    public void setFloat(int index, float value)
    {
        checkFromIndexSize(index, SIZE_OF_FLOAT, length());
        JvmUtils.unsafe.putFloat(base, sizeOfByteArray(baseOffset + index), value);
    }

    public void setDouble(int index, double value)
    {
        checkFromIndexSize(index, SIZE_OF_DOUBLE, length());
        JvmUtils.unsafe.putDouble(base, sizeOfByteArray(baseOffset + index), value);
    }
    /* ========================= capacity operation ==============================*/

    public Buffer ensureSize(int minWritableBytes)
    {
        if (minWritableBytes < 0) {
            throw new IllegalArgumentException("minWritableBytes is negative");
        }
        if (minWritableBytes <= length()) {
            return this;
        }

        int newCapacity;
        if (length() == 0) {
            newCapacity = 1;
        }
        else {
            newCapacity = length();
        }
        newCapacity = sizeCalculator.calculateNewArraySize(newCapacity, newCapacity + minWritableBytes);
        byte[] bytes = byteArray();
        int offset = byteArrayOffset();
        byte[] copy = Arrays.copyOfRange(bytes, offset, offset + newCapacity);
        // 确保新数组的未初始化部分被设置为零，防止旧的数据泄露。
        Arrays.fill(copy, length(), bytes.length - offset, (byte) 0);
        return new Buffer(copy, sizeCalculator);
    }
}
