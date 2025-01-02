package info.victorchu.bamboo.memory;

import java.io.IOException;

public interface MemorySegmentWritable
{
    /**
     * Writes {@code len} bytes from memory segment {@code segment} starting at offset {@code off},
     * in order, to the output.
     *
     * @param segment memory segment to copy the bytes from.
     * @param off the start offset in the memory segment.
     * @param len The number of bytes to copy.
     * @throws IOException if an I/O error occurs.
     */
    void write(MemorySegment segment, int off, int len) throws IOException;
}
