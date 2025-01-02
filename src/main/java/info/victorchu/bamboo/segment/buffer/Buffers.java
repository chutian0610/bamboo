package info.victorchu.bamboo.segment.buffer;

import info.victorchu.bamboo.segment.SizeCalculator;

import java.nio.charset.Charset;

import static info.victorchu.bamboo.segment.SizeCalculator.MAX_ARRAY_SIZE;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_BYTE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class Buffers
{
    public static Buffer wrappedBuffer(byte[] array)
    {
        if (array.length == 0) {
            return Buffer.EMPTY_BUFFER();
        }
        return new Buffer(array, DefaultBufferSizeCalculator.INSTANCE);
    }

    public static Buffer wrappedBuffer(byte[] array, SizeCalculator sizeCalculator)
    {
        sizeCalculator = sizeCalculator == null ? DefaultBufferSizeCalculator.INSTANCE : sizeCalculator;
        if (array.length == 0) {
            return Buffer.EMPTY_BUFFER(sizeCalculator);
        }
        return new Buffer(array,sizeCalculator);
    }

    public static Buffer wrappedBuffer(byte[] array, int offset, int length)
    {
        if (length == 0) {
            return Buffer.EMPTY_BUFFER();
        }
        return new Buffer(array, offset, length, DefaultBufferSizeCalculator.INSTANCE);
    }

    public static Buffer wrappedBuffer(byte[] array, int offset, int length, SizeCalculator sizeCalculator)
    {
        sizeCalculator = sizeCalculator == null ? DefaultBufferSizeCalculator.INSTANCE : sizeCalculator;
        if (length == 0) {
            return Buffer.EMPTY_BUFFER(sizeCalculator);
        }
        return new Buffer(array, offset, length,sizeCalculator);
    }

    public static Buffer copiedBuffer(String string, Charset charset)
    {
        requireNonNull(string, "string is null");
        requireNonNull(charset, "charset is null");

        return wrappedBuffer(string.getBytes(charset));
    }

    public static Buffer utf8Buffer(String string)
    {
        return copiedBuffer(string, UTF_8);
    }

    public static Buffer allocate(int capacity)
    {
        if (capacity == 0) {
            return Buffer.EMPTY_BUFFER();
        }
        if (capacity > MAX_ARRAY_SIZE) {
            throw new BufferTooLargeException(String.format("Cannot allocate buffer larger than %s bytes", MAX_ARRAY_SIZE * SIZE_OF_BYTE));
        }
        return new Buffer(new byte[capacity], DefaultBufferSizeCalculator.INSTANCE);
    }

    public static Buffer allocate(int capacity, SizeCalculator sizeCalculator)
    {
        sizeCalculator = sizeCalculator == null ? DefaultBufferSizeCalculator.INSTANCE : sizeCalculator;
        if (capacity == 0) {
            return Buffer.EMPTY_BUFFER(sizeCalculator);
        }
        if (capacity > MAX_ARRAY_SIZE) {
            throw new BufferTooLargeException(String.format("Cannot allocate buffer larger than %s bytes", MAX_ARRAY_SIZE * SIZE_OF_BYTE));
        }
        return new Buffer(new byte[capacity], sizeCalculator);
    }
}
