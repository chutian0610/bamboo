package info.victorchu.bamboo.segment.buffer;

import info.victorchu.bamboo.segment.SizeCalculator;

import static info.victorchu.bamboo.segment.SizeCalculator.MAX_ARRAY_SIZE;
import static info.victorchu.bamboo.segment.buffer.Buffer.EMPTY_BUFFER;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_BYTE;

public class Buffers
{
    public static Buffer wrappedBuffer(byte[] array)
    {
        if (array.length == 0) {
            return EMPTY_BUFFER;
        }
        return new Buffer(array, DefaultBufferSizeCalculator.INSTANCE);
    }
    public static Buffer wrappedBuffer(byte[] array,SizeCalculator sizeCalculator)
    {
        if (array.length == 0) {
            return EMPTY_BUFFER;
        }
        return new Buffer(array, sizeCalculator == null ? DefaultBufferSizeCalculator.INSTANCE : sizeCalculator);
    }
    public static Buffer allocate(int capacity)
    {
        if (capacity == 0) {
            return EMPTY_BUFFER;
        }
        if (capacity > MAX_ARRAY_SIZE) {
            throw new BufferTooLargeException(String.format("Cannot allocate buffer larger than %s bytes", MAX_ARRAY_SIZE * SIZE_OF_BYTE));
        }
        return new Buffer(new byte[capacity], DefaultBufferSizeCalculator.INSTANCE);
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
}
