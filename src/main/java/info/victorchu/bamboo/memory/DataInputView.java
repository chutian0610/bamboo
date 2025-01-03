package info.victorchu.bamboo.memory;

import java.io.DataInput;
import java.io.IOException;

public interface DataInputView extends DataInput
{
    /**
     * Skips {@code numBytes} bytes of memory. In contrast to the {@link #skipBytes(int)} method,
     * this method always skips the desired number of bytes or throws an {@link
     * java.io.EOFException}.
     *
     * @param numBytes The number of bytes to skip.
     * @throws IOException Thrown, if any I/O related problem occurred such that the input could not
     *     be advanced to the desired position.
     */
    void skipBytesToRead(int numBytes) throws IOException;

    /**
     * Reads up to {@code len} bytes of memory and stores it into {@code b} starting at offset
     * {@code off}.
     *
     * <p>If <code>len</code> is zero, then no bytes are read and <code>0</code> is returned;
     * otherwise, there is an attempt to read at least one byte. If there is no more data left, the
     * value <code>-1</code> is returned; otherwise, at least one byte is read and stored into
     * <code>b</code>.
     *
     * @param b byte array to store the data to
     * @param off offset into byte array
     * @param len byte length to read
     * @return the number of actually read bytes of -1 if there is no more data left
     * @throws IOException
     */
    int read(byte[] b, int off, int len) throws IOException;

    /**
     * Tries to fill the given byte array {@code b}. Returns the actually number of read bytes or -1
     * if there is no more data.
     *
     * @param b byte array to store the data to
     * @return the number of read bytes or -1 if there is no more data left
     * @throws IOException
     */
    int read(byte[] b) throws IOException;
}
