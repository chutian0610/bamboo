package info.victorchu.bamboo.memory;

import info.victorchu.bamboo.utils.Preconditions;

import javax.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static info.victorchu.bamboo.memory.MemoryUtils.getByteBufferAddress;

public class MemorySegment
{

    @SuppressWarnings("restriction")
    private static final sun.misc.Unsafe UNSAFE = MemoryUtils.UNSAFE;

    /** The beginning of the byte array contents, relative to the byte array object. */
    @SuppressWarnings("restriction")
    private static final long BYTE_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

    /**
     * Constant that flags the byte order. Because this is a boolean constant, the JIT compiler can
     * use this well to aggressively eliminate the non-applicable code paths.
     */
    private static final boolean LITTLE_ENDIAN =
            (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);

    // ------------------------------------------------------------------------

    /**
     * The heap byte array object relative to which we access the memory.
     */
    @Nonnull
    private final byte[] heapMemory;

    /**
     * The address to the data, relative to the heap memory byte array. If the heap memory byte
     * array is <tt>null</tt>, this becomes an absolute memory address outside the heap.
     */
    private long address;

    /**
     * The address one byte after the last addressable byte, i.e. <tt>address + size</tt> while the
     * segment is not disposed.
     */
    private final long addressLimit;

    /** The size in bytes of the memory segment. */
    private final int size;

    private final AtomicBoolean isFreedAtomic;

    /**
     * Creates a new memory segment that represents the memory of the byte array.
     *
     * <p>Since the byte array is backed by on-heap memory, this memory segment holds its data on
     * heap. The buffer must be at least of size 8 bytes.
     *
     * <p>The memory segment references the given owner.
     *
     * @param buffer The byte array whose memory is represented by this memory segment.
     */
    MemorySegment(@Nonnull byte[] buffer) {
        this.heapMemory = buffer;
        this.size = buffer.length;
        this.address = BYTE_ARRAY_BASE_OFFSET;
        this.addressLimit = this.address + this.size;
        this.isFreedAtomic = new AtomicBoolean(false);
    }

    // ------------------------------------------------------------------------
    // Memory Segment Operations
    // ------------------------------------------------------------------------

    /**
     * Gets the size of the memory segment, in bytes.
     *
     * @return The size of the memory segment.
     */
    public int size() {
        return size;
    }

    /**
     * Checks whether the memory segment was freed.
     *
     * @return <tt>true</tt>, if the memory segment has been freed, <tt>false</tt> otherwise.
     */
    public boolean isFreed() {
        return address > addressLimit;
    }

    /**
     * Frees this memory segment.
     *
     */
    public void free() {
        if (!isFreedAtomic.getAndSet(true)) {
            // this ensures we can place no more data and trigger
            // the checks for the freed segment
            address = addressLimit + 1;
        }
    }

    /**
     * Returns the byte array of on-heap memory segments.
     *
     * @return underlying byte array
     * @throws IllegalStateException if the memory segment does not represent on-heap memory
     */
    public byte[] getArray() {
        return heapMemory;
    }

    public ByteBuffer wrap(int offset, int length) {
        return wrapInternal(offset, length);
    }

    private ByteBuffer wrapInternal(int offset, int length) {
        if (address <= addressLimit) {
            return ByteBuffer.wrap(heapMemory, offset, length);
        } else {
            throw new IllegalStateException("segment has been freed");
        }
    }

    // ------------------------------------------------------------------------
    //                    Random Access get() and put() methods
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Notes on the implementation: We try to collapse as many checks as
    // possible. We need to obey the following rules to make this safe
    // against segfaults:
    //
    //  - Grab mutable fields onto the stack before checking and using. This
    //    guards us against concurrent modifications which invalidate the
    //    pointers
    //  - Use subtractions for range checks, as they are tolerant
    // ------------------------------------------------------------------------

    public byte get(int index) {
        final long pos = address + index;
        if (index >= 0 && pos < addressLimit) {
            return UNSAFE.getByte(heapMemory, pos);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }
    public void put(int index, byte b) {
        final long pos = address + index;
        if (index >= 0 && pos < addressLimit) {
            UNSAFE.putByte(heapMemory, pos, b);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }
    public void get(int index, byte[] dst) {
        get(index, dst, 0, dst.length);
    }
    public void put(int index, byte[] src) {
        put(index, src, 0, src.length);
    }
    public void get(int index, byte[] dst, int offset, int length) {
        // check the byte array offset and length and the status
        if ((offset | length | (offset + length) | (dst.length - (offset + length))) < 0) {
            throw new IndexOutOfBoundsException();
        }

        final long pos = address + index;
        if (index >= 0 && pos <= addressLimit - length) {
            final long arrayAddress = BYTE_ARRAY_BASE_OFFSET + offset;
            UNSAFE.copyMemory(heapMemory, pos, dst, arrayAddress, length);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "pos: %d, length: %d, index: %d, offset: %d",
                            pos, length, index, offset));
        }
    }
    public void put(int index, byte[] src, int offset, int length) {
        // check the byte array offset and length
        if ((offset | length | (offset + length) | (src.length - (offset + length))) < 0) {
            throw new IndexOutOfBoundsException();
        }

        final long pos = address + index;

        if (index >= 0 && pos <= addressLimit - length) {
            final long arrayAddress = BYTE_ARRAY_BASE_OFFSET + offset;
            UNSAFE.copyMemory(src, arrayAddress, heapMemory, pos, length);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }
    public boolean getBoolean(int index) {
        return get(index) != 0;
    }
    public void putBoolean(int index, boolean value) {
        put(index, (byte) (value ? 1 : 0));
    }
    @SuppressWarnings("restriction")
    public char getChar(int index) {
        final long pos = address + index;
        if (index >= 0 && pos <= addressLimit - 2) {
            return UNSAFE.getChar(heapMemory, pos);
        } else if (address > addressLimit) {
            throw new IllegalStateException("This segment has been freed.");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }
    public char getCharLittleEndian(int index) {
        if (LITTLE_ENDIAN) {
            return getChar(index);
        } else {
            return Character.reverseBytes(getChar(index));
        }
    }
    public char getCharBigEndian(int index) {
        if (LITTLE_ENDIAN) {
            return Character.reverseBytes(getChar(index));
        } else {
            return getChar(index);
        }
    }
    @SuppressWarnings("restriction")
    public void putChar(int index, char value) {
        final long pos = address + index;
        if (index >= 0 && pos <= addressLimit - 2) {
            UNSAFE.putChar(heapMemory, pos, value);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }

    public void putCharLittleEndian(int index, char value) {
        if (LITTLE_ENDIAN) {
            putChar(index, value);
        } else {
            putChar(index, Character.reverseBytes(value));
        }
    }
    public void putCharBigEndian(int index, char value) {
        if (LITTLE_ENDIAN) {
            putChar(index, Character.reverseBytes(value));
        } else {
            putChar(index, value);
        }
    }
    public short getShort(int index) {
        final long pos = address + index;
        if (index >= 0 && pos <= addressLimit - 2) {
            return UNSAFE.getShort(heapMemory, pos);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }
    public short getShortLittleEndian(int index) {
        if (LITTLE_ENDIAN) {
            return getShort(index);
        } else {
            return Short.reverseBytes(getShort(index));
        }
    }
    public short getShortBigEndian(int index) {
        if (LITTLE_ENDIAN) {
            return Short.reverseBytes(getShort(index));
        } else {
            return getShort(index);
        }
    }
    public void putShort(int index, short value) {
        final long pos = address + index;
        if (index >= 0 && pos <= addressLimit - 2) {
            UNSAFE.putShort(heapMemory, pos, value);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }
    public void putShortLittleEndian(int index, short value) {
        if (LITTLE_ENDIAN) {
            putShort(index, value);
        } else {
            putShort(index, Short.reverseBytes(value));
        }
    }
    public void putShortBigEndian(int index, short value) {
        if (LITTLE_ENDIAN) {
            putShort(index, Short.reverseBytes(value));
        } else {
            putShort(index, value);
        }
    }
    public int getInt(int index) {
        final long pos = address + index;
        if (index >= 0 && pos <= addressLimit - 4) {
            return UNSAFE.getInt(heapMemory, pos);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }
    public int getIntLittleEndian(int index) {
        if (LITTLE_ENDIAN) {
            return getInt(index);
        } else {
            return Integer.reverseBytes(getInt(index));
        }
    }
    public int getIntBigEndian(int index) {
        if (LITTLE_ENDIAN) {
            return Integer.reverseBytes(getInt(index));
        } else {
            return getInt(index);
        }
    }

    public void putInt(int index, int value) {
        final long pos = address + index;
        if (index >= 0 && pos <= addressLimit - 4) {
            UNSAFE.putInt(heapMemory, pos, value);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }
    public void putIntLittleEndian(int index, int value) {
        if (LITTLE_ENDIAN) {
            putInt(index, value);
        } else {
            putInt(index, Integer.reverseBytes(value));
        }
    }
    public void putIntBigEndian(int index, int value) {
        if (LITTLE_ENDIAN) {
            putInt(index, Integer.reverseBytes(value));
        } else {
            putInt(index, value);
        }
    }
    public long getLong(int index) {
        final long pos = address + index;
        if (index >= 0 && pos <= addressLimit - 8) {
            return UNSAFE.getLong(heapMemory, pos);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }
    public long getLongLittleEndian(int index) {
        if (LITTLE_ENDIAN) {
            return getLong(index);
        } else {
            return Long.reverseBytes(getLong(index));
        }
    }
    public long getLongBigEndian(int index) {
        if (LITTLE_ENDIAN) {
            return Long.reverseBytes(getLong(index));
        } else {
            return getLong(index);
        }
    }
    public void putLong(int index, long value) {
        final long pos = address + index;
        if (index >= 0 && pos <= addressLimit - 8) {
            UNSAFE.putLong(heapMemory, pos, value);
        } else if (address > addressLimit) {
            throw new IllegalStateException("segment has been freed");
        } else {
            // index is in fact invalid
            throw new IndexOutOfBoundsException();
        }
    }
    public void putLongLittleEndian(int index, long value) {
        if (LITTLE_ENDIAN) {
            putLong(index, value);
        } else {
            putLong(index, Long.reverseBytes(value));
        }
    }
    public void putLongBigEndian(int index, long value) {
        if (LITTLE_ENDIAN) {
            putLong(index, Long.reverseBytes(value));
        } else {
            putLong(index, value);
        }
    }
    public float getFloat(int index) {
        return Float.intBitsToFloat(getInt(index));
    }
    public float getFloatLittleEndian(int index) {
        return Float.intBitsToFloat(getIntLittleEndian(index));
    }

    public float getFloatBigEndian(int index) {
        return Float.intBitsToFloat(getIntBigEndian(index));
    }
    public void putFloat(int index, float value) {
        putInt(index, Float.floatToRawIntBits(value));
    }
    public void putFloatLittleEndian(int index, float value) {
        putIntLittleEndian(index, Float.floatToRawIntBits(value));
    }
    public void putFloatBigEndian(int index, float value) {
        putIntBigEndian(index, Float.floatToRawIntBits(value));
    }
    public double getDouble(int index) {
        return Double.longBitsToDouble(getLong(index));
    }
    public double getDoubleLittleEndian(int index) {
        return Double.longBitsToDouble(getLongLittleEndian(index));
    }
    public double getDoubleBigEndian(int index) {
        return Double.longBitsToDouble(getLongBigEndian(index));
    }
    public void putDouble(int index, double value) {
        putLong(index, Double.doubleToRawLongBits(value));
    }
    public void putDoubleLittleEndian(int index, double value) {
        putLongLittleEndian(index, Double.doubleToRawLongBits(value));
    }

    public void putDoubleBigEndian(int index, double value) {
        putLongBigEndian(index, Double.doubleToRawLongBits(value));
    }

    // -------------------------------------------------------------------------
    //                     Bulk Read and Write Methods
    // -------------------------------------------------------------------------

    public void get(DataOutput out, int offset, int length) throws IOException
    {
        if (address <= addressLimit) {
            if (heapMemory != null) {
                out.write(heapMemory, offset, length);
            } else {
                while (length >= 8) {
                    out.writeLong(getLongBigEndian(offset));
                    offset += 8;
                    length -= 8;
                }

                while (length > 0) {
                    out.writeByte(get(offset));
                    offset++;
                    length--;
                }
            }
        } else {
            throw new IllegalStateException("segment has been freed");
        }
    }

    /**
     * Bulk put method. Copies length memory from the given DataInput to the memory starting at
     * position offset.
     *
     * @param in The DataInput to get the data from.
     * @param offset The position in the memory segment to copy the chunk to.
     * @param length The number of bytes to get.
     * @throws IOException Thrown, if the DataInput encountered a problem upon reading, such as an
     *     End-Of-File.
     */
    public void put(DataInput in, int offset, int length) throws IOException {
        if (address <= addressLimit) {
            if (heapMemory != null) {
                in.readFully(heapMemory, offset, length);
            } else {
                while (length >= 8) {
                    putLongBigEndian(offset, in.readLong());
                    offset += 8;
                    length -= 8;
                }
                while (length > 0) {
                    put(offset, in.readByte());
                    offset++;
                    length--;
                }
            }
        } else {
            throw new IllegalStateException("segment has been freed");
        }
    }

    /**
     * Bulk get method. Copies {@code numBytes} bytes from this memory segment, starting at position
     * {@code offset} to the target {@code ByteBuffer}. The bytes will be put into the target buffer
     * starting at the buffer's current position. If this method attempts to write more bytes than
     * the target byte buffer has remaining (with respect to {@link ByteBuffer#remaining()}), this
     * method will cause a {@link java.nio.BufferOverflowException}.
     *
     * @param offset The position where the bytes are started to be read from in this memory
     *     segment.
     * @param target The ByteBuffer to copy the bytes to.
     * @param numBytes The number of bytes to copy.
     * @throws IndexOutOfBoundsException If the offset is invalid, or this segment does not contain
     *     the given number of bytes (starting from offset), or the target byte buffer does not have
     *     enough space for the bytes.
     * @throws ReadOnlyBufferException If the target buffer is read-only.
     */
    public void get(int offset, ByteBuffer target, int numBytes) {
        // check the byte array offset and length
        if ((offset | numBytes | (offset + numBytes)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (target.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }

        final int targetOffset = target.position();
        final int remaining = target.remaining();

        if (remaining < numBytes) {
            throw new BufferOverflowException();
        }

        if (target.isDirect()) {
            // copy to the target memory directly
            final long targetPointer = getByteBufferAddress(target) + targetOffset;
            final long sourcePointer = address + offset;

            if (sourcePointer <= addressLimit - numBytes) {
                UNSAFE.copyMemory(heapMemory, sourcePointer, null, targetPointer, numBytes);
                target.position(targetOffset + numBytes);
            } else if (address > addressLimit) {
                throw new IllegalStateException("segment has been freed");
            } else {
                throw new IndexOutOfBoundsException();
            }
        } else if (target.hasArray()) {
            // move directly into the byte array
            get(offset, target.array(), targetOffset + target.arrayOffset(), numBytes);

            // this must be after the get() call to ensue that the byte buffer is not
            // modified in case the call fails
            target.position(targetOffset + numBytes);
        } else {
            // other types of byte buffers
            throw new IllegalArgumentException(
                    "The target buffer is not direct, and has no array.");
        }
    }

    /**
     * Bulk put method. Copies {@code numBytes} bytes from the given {@code ByteBuffer}, into this
     * memory segment. The bytes will be read from the target buffer starting at the buffer's
     * current position, and will be written to this memory segment starting at {@code offset}. If
     * this method attempts to read more bytes than the target byte buffer has remaining (with
     * respect to {@link ByteBuffer#remaining()}), this method will cause a {@link
     * java.nio.BufferUnderflowException}.
     *
     * @param offset The position where the bytes are started to be written to in this memory
     *     segment.
     * @param source The ByteBuffer to copy the bytes from.
     * @param numBytes The number of bytes to copy.
     * @throws IndexOutOfBoundsException If the offset is invalid, or the source buffer does not
     *     contain the given number of bytes, or this segment does not have enough space for the
     *     bytes (counting from offset).
     */
    public void put(int offset, ByteBuffer source, int numBytes) {
        // check the byte array offset and length
        if ((offset | numBytes | (offset + numBytes)) < 0) {
            throw new IndexOutOfBoundsException();
        }

        final int sourceOffset = source.position();
        final int remaining = source.remaining();

        if (remaining < numBytes) {
            throw new BufferUnderflowException();
        }

        if (source.isDirect()) {
            // copy to the target memory directly
            final long sourcePointer = getByteBufferAddress(source) + sourceOffset;
            final long targetPointer = address + offset;

            if (targetPointer <= addressLimit - numBytes) {
                UNSAFE.copyMemory(null, sourcePointer, heapMemory, targetPointer, numBytes);
                source.position(sourceOffset + numBytes);
            } else if (address > addressLimit) {
                throw new IllegalStateException("segment has been freed");
            } else {
                throw new IndexOutOfBoundsException();
            }
        } else if (source.hasArray()) {
            // move directly into the byte array
            put(offset, source.array(), sourceOffset + source.arrayOffset(), numBytes);

            // this must be after the get() call to ensue that the byte buffer is not
            // modified in case the call fails
            source.position(sourceOffset + numBytes);
        } else {
            // other types of byte buffers
            for (int i = 0; i < numBytes; i++) {
                put(offset++, source.get());
            }
        }
    }

    /**
     * Bulk copy method. Copies {@code numBytes} bytes from this memory segment, starting at
     * position {@code offset} to the target memory segment. The bytes will be put into the target
     * segment starting at position {@code targetOffset}.
     *
     * @param offset The position where the bytes are started to be read from in this memory
     *     segment.
     * @param target The memory segment to copy the bytes to.
     * @param targetOffset The position in the target memory segment to copy the chunk to.
     * @param numBytes The number of bytes to copy.
     * @throws IndexOutOfBoundsException If either of the offsets is invalid, or the source segment
     *     does not contain the given number of bytes (starting from offset), or the target segment
     *     does not have enough space for the bytes (counting from targetOffset).
     */
    public void copyTo(int offset, MemorySegment target, int targetOffset, int numBytes) {
        final byte[] thisHeapRef = this.heapMemory;
        final byte[] otherHeapRef = target.heapMemory;
        final long thisPointer = this.address + offset;
        final long otherPointer = target.address + targetOffset;

        if ((numBytes | offset | targetOffset) >= 0
                && thisPointer <= this.addressLimit - numBytes
                && otherPointer <= target.addressLimit - numBytes) {
            UNSAFE.copyMemory(thisHeapRef, thisPointer, otherHeapRef, otherPointer, numBytes);
        } else if (this.address > this.addressLimit) {
            throw new IllegalStateException("this memory segment has been freed.");
        } else if (target.address > target.addressLimit) {
            throw new IllegalStateException("target memory segment has been freed.");
        } else {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "offset=%d, targetOffset=%d, numBytes=%d, address=%d, targetAddress=%d",
                            offset, targetOffset, numBytes, this.address, target.address));
        }
    }

    /**
     * Bulk copy method. Copies {@code numBytes} bytes to target unsafe object and pointer. NOTE:
     * This is an unsafe method, no check here, please be careful.
     *
     * @param offset The position where the bytes are started to be read from in this memory
     *     segment.
     * @param target The unsafe memory to copy the bytes to.
     * @param targetPointer The position in the target unsafe memory to copy the chunk to.
     * @param numBytes The number of bytes to copy.
     * @throws IndexOutOfBoundsException If the source segment does not contain the given number of
     *     bytes (starting from offset).
     */
    public void copyToUnsafe(int offset, Object target, int targetPointer, int numBytes) {
        final long thisPointer = this.address + offset;
        if (thisPointer + numBytes > addressLimit) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "offset=%d, numBytes=%d, address=%d", offset, numBytes, this.address));
        }
        UNSAFE.copyMemory(this.heapMemory, thisPointer, target, targetPointer, numBytes);
    }

    /**
     * Bulk copy method. Copies {@code numBytes} bytes from source unsafe object and pointer. NOTE:
     * This is an unsafe method, no check here, please be careful.
     *
     * @param offset The position where the bytes are started to be write in this memory segment.
     * @param source The unsafe memory to copy the bytes from.
     * @param sourcePointer The position in the source unsafe memory to copy the chunk from.
     * @param numBytes The number of bytes to copy.
     * @throws IndexOutOfBoundsException If this segment can not contain the given number of bytes
     *     (starting from offset).
     */
    public void copyFromUnsafe(int offset, Object source, int sourcePointer, int numBytes) {
        final long thisPointer = this.address + offset;
        if (thisPointer + numBytes > addressLimit) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "offset=%d, numBytes=%d, address=%d", offset, numBytes, this.address));
        }
        UNSAFE.copyMemory(source, sourcePointer, this.heapMemory, thisPointer, numBytes);
    }

    // -------------------------------------------------------------------------
    //                      Comparisons & Swapping
    // -------------------------------------------------------------------------

    /**
     * Compares two memory segment regions.
     *
     * @param seg2 Segment to compare this segment with
     * @param offset1 Offset of this segment to start comparing
     * @param offset2 Offset of seg2 to start comparing
     * @param len Length of the compared memory region
     * @return 0 if equal, -1 if seg1 &lt; seg2, 1 otherwise
     */
    public int compare(MemorySegment seg2, int offset1, int offset2, int len) {
        while (len >= 8) {
            long l1 = this.getLongBigEndian(offset1);
            long l2 = seg2.getLongBigEndian(offset2);

            if (l1 != l2) {
                return (l1 < l2) ^ (l1 < 0) ^ (l2 < 0) ? -1 : 1;
            }

            offset1 += 8;
            offset2 += 8;
            len -= 8;
        }
        while (len > 0) {
            int b1 = this.get(offset1) & 0xff;
            int b2 = seg2.get(offset2) & 0xff;
            int cmp = b1 - b2;
            if (cmp != 0) {
                return cmp;
            }
            offset1++;
            offset2++;
            len--;
        }
        return 0;
    }

    /**
     * Compares two memory segment regions with different length.
     *
     * @param seg2 Segment to compare this segment with
     * @param offset1 Offset of this segment to start comparing
     * @param offset2 Offset of seg2 to start comparing
     * @param len1 Length of this memory region to compare
     * @param len2 Length of seg2 to compare
     * @return 0 if equal, -1 if seg1 &lt; seg2, 1 otherwise
     */
    public int compare(MemorySegment seg2, int offset1, int offset2, int len1, int len2) {
        final int minLength = Math.min(len1, len2);
        int c = compare(seg2, offset1, offset2, minLength);
        return c == 0 ? (len1 - len2) : c;
    }

    /**
     * Swaps bytes between two memory segments, using the given auxiliary buffer.
     *
     * @param tempBuffer The auxiliary buffer in which to put data during triangle swap.
     * @param seg2 Segment to swap bytes with
     * @param offset1 Offset of this segment to start swapping
     * @param offset2 Offset of seg2 to start swapping
     * @param len Length of the swapped memory region
     */
    public void swapBytes(
            byte[] tempBuffer, MemorySegment seg2, int offset1, int offset2, int len) {
        if ((offset1 | offset2 | len | (tempBuffer.length - len)) >= 0) {
            final long thisPos = this.address + offset1;
            final long otherPos = seg2.address + offset2;

            if (thisPos <= this.addressLimit - len && otherPos <= seg2.addressLimit - len) {
                // this -> temp buffer
                UNSAFE.copyMemory(
                        this.heapMemory, thisPos, tempBuffer, BYTE_ARRAY_BASE_OFFSET, len);

                // other -> this
                UNSAFE.copyMemory(seg2.heapMemory, otherPos, this.heapMemory, thisPos, len);

                // temp buffer -> other
                UNSAFE.copyMemory(
                        tempBuffer, BYTE_ARRAY_BASE_OFFSET, seg2.heapMemory, otherPos, len);
                return;
            } else if (this.address > this.addressLimit) {
                throw new IllegalStateException("this memory segment has been freed.");
            } else if (seg2.address > seg2.addressLimit) {
                throw new IllegalStateException("other memory segment has been freed.");
            }
        }

        // index is in fact invalid
        throw new IndexOutOfBoundsException(
                String.format(
                        "offset1=%d, offset2=%d, len=%d, bufferSize=%d, address1=%d, address2=%d",
                        offset1, offset2, len, tempBuffer.length, this.address, seg2.address));
    }

    /**
     * Equals two memory segment regions.
     *
     * @param seg2 Segment to equal this segment with
     * @param offset1 Offset of this segment to start equaling
     * @param offset2 Offset of seg2 to start equaling
     * @param length Length of the equaled memory region
     * @return true if equal, false otherwise
     */
    public boolean equalTo(MemorySegment seg2, int offset1, int offset2, int length) {
        int i = 0;

        // we assume unaligned accesses are supported.
        // Compare 8 bytes at a time.
        while (i <= length - 8) {
            if (getLong(offset1 + i) != seg2.getLong(offset2 + i)) {
                return false;
            }
            i += 8;
        }

        // cover the last (length % 8) elements.
        while (i < length) {
            if (get(offset1 + i) != seg2.get(offset2 + i)) {
                return false;
            }
            i += 1;
        }

        return true;
    }

    /**
     * Applies the given process function on a {@link ByteBuffer} that represents this entire
     * segment.
     *
     * <p>Note: The {@link ByteBuffer} passed into the process function is temporary and could
     * become invalid after the processing. Thus, the process function should not try to keep any
     * reference of the {@link ByteBuffer}.
     *
     * @param processFunction to be applied to the segment as {@link ByteBuffer}.
     * @return the value that the process function returns.
     */
    public <T> T processAsByteBuffer(Function<ByteBuffer, T> processFunction) {
        return Preconditions.checkNotNull(processFunction).apply(wrapInternal(0, size));
    }

    /**
     * Supplies a {@link ByteBuffer} that represents this entire segment to the given process
     * consumer.
     *
     * <p>Note: The {@link ByteBuffer} passed into the process consumer is temporary and could
     * become invalid after the processing. Thus, the process consumer should not try to keep any
     * reference of the {@link ByteBuffer}.
     *
     * @param processConsumer to accept the segment as {@link ByteBuffer}.
     */
    public void processAsByteBuffer(Consumer<ByteBuffer> processConsumer) {
        Preconditions.checkNotNull(processConsumer).accept(wrapInternal(0, size));
    }
}
