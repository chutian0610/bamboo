package info.victorchu.bamboo.segment.buffer;

import info.victorchu.bamboo.segment.RetainedSizeAware;
import info.victorchu.bamboo.segment.SizeCalculator;
import info.victorchu.bamboo.segment.utils.ByteUtils;
import info.victorchu.bamboo.segment.utils.XxHash64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import static info.victorchu.bamboo.segment.utils.JvmUtils.unsafe;
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
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_DOUBLE_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_FLOAT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_INT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_LONG_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET;

public class Buffer
        implements RetainedSizeAware
{
    public static final Buffer EMPTY_BUFFER = new Buffer();
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

    /* subBuffer constructor
     * subBuffer retainedSize same as origin Buffer
     */
    Buffer(byte[] base, int offset, int length, SizeCalculator sizeCalculator, long retainedSize)
    {
        requireNonNull(base, "base is null");
        if (base.length == 0) {
            throw new IllegalArgumentException("Empty array");
        }
        checkFromIndexSize(offset, length, base.length);

        this.base = base;
        this.baseOffset = offset;
        this.size = length;
        this.retainedSize = retainedSize;
        this.sizeCalculator = sizeCalculator;
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

    /**
     * @param index byte[] 中的开始位置
     * @return byte[] 中的第index个字节
     */
    public byte getByte(int index)
    {
        checkFromIndexSize(index, SIZE_OF_BYTE, length());
        return getByteUnchecked(index);
    }

    public byte getByteUnchecked(int index)
    {
        return base[baseOffset + index];
    }

    public short getShort(int index)
    {
        checkFromIndexSize(index, SIZE_OF_SHORT, length());
        return unsafe.getShort(base, sizeOfByteArray(baseOffset + index));
    }

    public int getInt(int index)
    {
        checkFromIndexSize(index, SIZE_OF_INT, length());
        return unsafe.getInt(base, sizeOfByteArray(baseOffset + index));
    }

    public long getLong(int index)
    {
        checkFromIndexSize(index, SIZE_OF_LONG, length());
        return unsafe.getLong(base, sizeOfByteArray(baseOffset + index));
    }

    public float getFloat(int index)
    {
        checkFromIndexSize(index, SIZE_OF_FLOAT, length());
        return unsafe.getFloat(base, sizeOfByteArray(baseOffset + index));
    }

    public double getDouble(int index)
    {
        checkFromIndexSize(index, SIZE_OF_DOUBLE, length());
        return unsafe.getDouble(base, sizeOfByteArray(baseOffset + index));
    }

    public byte[] getBytes(int index, int length)
    {
        byte[] bytes = new byte[length];
        getBytes(index, bytes, 0, length);
        return bytes;
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
        unsafe.putShort(base, sizeOfByteArray(baseOffset + index), value);
    }

    public void setInt(int index, int value)
    {
        checkFromIndexSize(index, SIZE_OF_INT, length());
        unsafe.putInt(base, sizeOfByteArray(baseOffset + index), value);
    }

    public void setLong(int index, long value)
    {
        checkFromIndexSize(index, SIZE_OF_LONG, length());
        unsafe.putLong(base, sizeOfByteArray(baseOffset + index), value);
    }

    public void setFloat(int index, float value)
    {
        checkFromIndexSize(index, SIZE_OF_FLOAT, length());
        unsafe.putFloat(base, sizeOfByteArray(baseOffset + index), value);
    }

    public void setDouble(int index, double value)
    {
        checkFromIndexSize(index, SIZE_OF_DOUBLE, length());
        unsafe.putDouble(base, sizeOfByteArray(baseOffset + index), value);
    }

    /* ====================== values get operation =========================*/

    public byte[] getBytes()
    {
        return getBytes(0, length());
    }

    public void getBytes(int index, OutputStream out, int length)
            throws IOException
    {
        checkFromIndexSize(index, length, length());
        out.write(byteArray(), byteArrayOffset() + index, length);
    }

    public void getBytes(int index, Buffer target)
    {
        getBytes(index, target, 0, target.length());
    }

    public void getBytes(int index, Buffer target, int targetIndex, int length)
    {
        checkFromIndexSize(targetIndex, length, target.length());
        checkFromIndexSize(index, length, length());

        System.arraycopy(base, baseOffset + index, target.base, target.baseOffset + targetIndex, length);
    }

    public void getBytes(int index, byte[] target)
    {
        getBytes(index, target, 0, target.length);
    }

    public void getBytes(int index, byte[] target, int targetIndex, int length)
    {
        checkFromIndexSize(index, length, length());
        checkFromIndexSize(targetIndex, length, target.length);

        System.arraycopy(base, baseOffset + index, target, targetIndex, length);
    }

    public short[] getShorts(int index, int length)
    {
        short[] shorts = new short[length];
        getShorts(index, shorts, 0, length);
        return shorts;
    }

    public void getShorts(int index, short[] target)
    {
        getShorts(index, target, 0, target.length);
    }

    public void getShorts(int index, short[] target, int targetIndex, int length)
    {
        checkFromIndexSize(index, length * Short.BYTES, length());
        checkFromIndexSize(targetIndex, length, target.length);

        copyFromBase(index, target, ARRAY_SHORT_BASE_OFFSET + ((long) targetIndex * Short.BYTES), length * Short.BYTES);
    }

    public int[] getIntegers(int index, int length)
    {
        int[] ints = new int[length];
        getIntegers(index, ints, 0, length);
        return ints;
    }

    public void getIntegers(int index, int[] target)
    {
        getIntegers(index, target, 0, target.length);
    }

    public void getIntegers(int index, int[] destination, int destinationIndex, int length)
    {
        checkFromIndexSize(index, length * Integer.BYTES, length());
        checkFromIndexSize(destinationIndex, length, destination.length);

        copyFromBase(index, destination, ARRAY_INT_BASE_OFFSET + ((long) destinationIndex * Integer.BYTES), length * Integer.BYTES);
    }

    public long[] getLongs(int index, int length)
    {
        long[] longs = new long[length];
        getLongs(index, longs, 0, length);
        return longs;
    }

    public void getLongs(int index, long[] target)
    {
        getLongs(index, target, 0, target.length);
    }

    public void getLongs(int index, long[] destination, int destinationIndex, int length)
    {
        checkFromIndexSize(index, length * Long.BYTES, length());
        checkFromIndexSize(destinationIndex, length, destination.length);

        copyFromBase(index, destination, ARRAY_LONG_BASE_OFFSET + ((long) destinationIndex * Long.BYTES), length * Long.BYTES);
    }

    public float[] getFloats(int index, int length)
    {
        float[] floats = new float[length];
        getFloats(index, floats, 0, length);
        return floats;
    }

    public void getFloats(int index, float[] destination)
    {
        getFloats(index, destination, 0, destination.length);
    }

    public void getFloats(int index, float[] destination, int destinationIndex, int length)
    {
        checkFromIndexSize(index, length * Float.BYTES, length());
        checkFromIndexSize(destinationIndex, length, destination.length);

        copyFromBase(index, destination, ARRAY_FLOAT_BASE_OFFSET + ((long) destinationIndex * Float.BYTES), length * Float.BYTES);
    }

    public double[] getDoubles(int index, int length)
    {
        double[] doubles = new double[length];
        getDoubles(index, doubles, 0, length);
        return doubles;
    }

    public void getDoubles(int index, double[] destination)
    {
        getDoubles(index, destination, 0, destination.length);
    }

    public void getDoubles(int index, double[] destination, int destinationIndex, int length)
    {
        checkFromIndexSize(index, length * Double.BYTES, length());
        checkFromIndexSize(destinationIndex, length, destination.length);

        copyFromBase(index, destination, ARRAY_DOUBLE_BASE_OFFSET + ((long) destinationIndex * Double.BYTES), length * Double.BYTES);
    }

    private void copyFromBase(int index, Object target, long targetAddress, int length)
    {
        int baseAddress = ARRAY_BYTE_BASE_OFFSET + baseOffset + index;
        // The Unsafe Javadoc specifies that the transfer size is 8 iff length % 8 == 0
        // so ensure that we copy big chunks whenever possible, even at the expense of two separate copy operations
        int bytesToCopy = length - (length % 8);
        unsafe.copyMemory(base, baseAddress, target, targetAddress, bytesToCopy);
        unsafe.copyMemory(base, baseAddress + bytesToCopy, target, targetAddress + bytesToCopy, length - bytesToCopy);
    }

    /* ====================== values set operation =========================*/

    public void setBytes(int index, Buffer source)
    {
        setBytes(index, source, 0, source.length());
    }

    public void setBytes(int index, Buffer source, int sourceIndex, int length)
    {
        checkFromIndexSize(index, length, length());
        checkFromIndexSize(sourceIndex, length, source.length());
        System.arraycopy(source.base, source.baseOffset + sourceIndex, base, baseOffset + index, length);
    }

    public void setBytes(int index, byte[] source)
    {
        setBytes(index, source, 0, source.length);
    }

    public void setBytes(int index, byte[] source, int sourceIndex, int length)
    {
        checkFromIndexSize(index, length, length());
        checkFromIndexSize(sourceIndex, length, source.length);
        System.arraycopy(source, sourceIndex, base, baseOffset + index, length);
    }

    public void setBytes(int index, InputStream in, int length)
            throws IOException
    {
        checkFromIndexSize(index, length, length());
        byte[] bytes = byteArray();
        int offset = byteArrayOffset() + index;
        while (length > 0) {
            int bytesRead = in.read(bytes, offset, length);
            if (bytesRead < 0) {
                throw new IndexOutOfBoundsException("End of stream");
            }
            length -= bytesRead;
            offset += bytesRead;
        }
    }

    public void setShorts(int index, short[] source)
    {
        setShorts(index, source, 0, source.length);
    }

    public void setShorts(int index, short[] source, int sourceIndex, int length)
    {
        checkFromIndexSize(index, length, length());
        checkFromIndexSize(sourceIndex, length, source.length);
        copyToBase(index, source, ARRAY_SHORT_BASE_OFFSET + ((long) sourceIndex * Short.BYTES), length * Short.BYTES);
    }

    public void setIntegers(int index, int[] source)
    {
        setIntegers(index, source, 0, source.length);
    }

    public void setIntegers(int index, int[] source, int sourceIndex, int length)
    {
        checkFromIndexSize(index, length, length());
        checkFromIndexSize(sourceIndex, length, source.length);
        copyToBase(index, source, ARRAY_INT_BASE_OFFSET + ((long) sourceIndex * Integer.BYTES), length * Integer.BYTES);
    }

    public void setLongs(int index, long[] source)
    {
        setLongs(index, source, 0, source.length);
    }

    public void setLongs(int index, long[] source, int sourceIndex, int length)
    {
        checkFromIndexSize(index, length, length());
        checkFromIndexSize(sourceIndex, length, source.length);
        copyToBase(index, source, ARRAY_LONG_BASE_OFFSET + ((long) sourceIndex * Long.BYTES), length * Long.BYTES);
    }

    public void setFloats(int index, float[] source)
    {
        setFloats(index, source, 0, source.length);
    }

    public void setFloats(int index, float[] source, int sourceIndex, int length)
    {
        checkFromIndexSize(index, length, length());
        checkFromIndexSize(sourceIndex, length, source.length);
        copyToBase(index, source, ARRAY_FLOAT_BASE_OFFSET + ((long) sourceIndex * Float.BYTES), length * Float.BYTES);
    }

    public void setDoubles(int index, double[] source)
    {
        setDoubles(index, source, 0, source.length);
    }

    public void setDoubles(int index, double[] source, int sourceIndex, int length)
    {
        checkFromIndexSize(index, length, length());
        checkFromIndexSize(sourceIndex, length, source.length);
        copyToBase(index, source, ARRAY_DOUBLE_BASE_OFFSET + ((long) sourceIndex * Double.BYTES), length * Double.BYTES);
    }

    private void copyToBase(int index, Object src, long srcAddress, int length)
    {
        int baseAddress = ARRAY_BYTE_BASE_OFFSET + baseOffset + index;
        // The Unsafe Javadoc specifies that the transfer size is 8 iff length % 8 == 0
        // so ensure that we copy big chunks whenever possible, even at the expense of two separate copy operations
        int bytesToCopy = length - (length % 8);
        unsafe.copyMemory(src, srcAddress, base, baseAddress, bytesToCopy);
        unsafe.copyMemory(src, srcAddress + bytesToCopy, base, baseAddress + bytesToCopy, length - bytesToCopy);
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
    /* ========================= collection  ======================================= */

    public Buffer slice(int index, int length)
    {
        if ((index == 0) && (length == length())) {
            return this;
        }
        checkFromIndexSize(index, length, length());
        if (length == 0) {
            return Buffer.EMPTY_BUFFER;
        }

        return new Buffer(base, baseOffset + index, length, sizeCalculator, retainedSize);
    }

    public Buffer copy()
    {
        if (size == 0) {
            return Buffer.EMPTY_BUFFER;
        }
        return new Buffer(Arrays.copyOfRange(base, baseOffset, baseOffset + size), sizeCalculator);
    }

    public Buffer copy(int index, int length)
    {
        checkFromIndexSize(index, length, size);
        if (length == 0) {
            return Buffer.EMPTY_BUFFER;
        }
        return new Buffer(Arrays.copyOfRange(base, baseOffset + index, baseOffset + index + length), sizeCalculator);
    }

    public int compareTo(Buffer that)
    {
        if (this == that) {
            return 0;
        }
        return compareTo(0, size, that, 0, that.size);
    }

    public int compareTo(int offset, int length, Buffer that, int otherOffset, int otherLength)
    {
        if ((this == that) && (offset == otherOffset) && (length == otherLength)) {
            return 0;
        }

        checkFromIndexSize(offset, length, length());
        checkFromIndexSize(otherOffset, otherLength, that.length());

        return ByteUtils.compareUnsigned(
                base,
                baseOffset + offset,
                baseOffset + offset + length,
                that.base,
                that.baseOffset + otherOffset,
                that.baseOffset + otherOffset + otherLength);
    }

    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Buffer)) {
            return false;
        }
        Buffer that = (Buffer) o;
        if (length() != that.length()) {
            return false;
        }

        return equalsUnchecked(0, that, 0, length());
    }

    public boolean equals(int offset, int length, Buffer that, int otherOffset, int otherLength)
    {
        if (length != otherLength) {
            return false;
        }

        if ((this == that) && (offset == otherOffset)) {
            return true;
        }

        checkFromIndexSize(offset, length, length());
        checkFromIndexSize(otherOffset, otherLength, that.length());

        return equalsUnchecked(offset, that, otherOffset, length);
    }

    boolean equalsUnchecked(int offset, Buffer that, int otherOffset, int length)
    {
        return ByteUtils.equals(
                base,
                baseOffset + offset,
                baseOffset + offset + length,
                that.base,
                that.baseOffset + otherOffset,
                that.baseOffset + otherOffset + length);
    }

    public int hashCode()
    {
        if (hash != 0) {
            return hash;
        }

        hash = hashCode(0, size);
        return hash;
    }

    /**
     * Returns the hash code of a portion of this slice.
     */
    public int hashCode(int offset, int length)
    {
        return (int) XxHash64.hash(this, offset, length);
    }
}
