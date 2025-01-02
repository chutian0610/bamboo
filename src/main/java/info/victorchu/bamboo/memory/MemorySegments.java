package info.victorchu.bamboo.memory;

import static info.victorchu.bamboo.utils.Preconditions.checkArgument;

public class MemorySegments
{
    /**
     * Creates a new memory segment that targets the given heap memory region.
     *
     * <p>This method should be used to turn short lived byte arrays into memory segments.
     *
     * @param buffer The heap memory region.
     * @return A new memory segment that targets the given heap memory region.
     */
    public static MemorySegment wrap(byte[] buffer) {
        return new MemorySegment(buffer);
    }

    /**
     * Copies the given heap memory region and creates a new memory segment wrapping it.
     *
     * @param bytes The heap memory region.
     * @param start starting position, inclusive
     * @param end end position, exclusive
     * @return A new memory segment that targets a copy of the given heap memory region.
     * @throws IllegalArgumentException if start > end or end > bytes.length
     */
    public static MemorySegment wrapCopy(byte[] bytes, int start, int end)
            throws IllegalArgumentException {
        checkArgument(end >= start);
        checkArgument(end <= bytes.length);
        MemorySegment copy = allocate(end - start);
        copy.put(0, bytes, start, copy.size());
        return copy;
    }


    /**
     * Allocates some memory and creates a new memory segment that represents that memory.
     *
     * @param size The size of the memory segment to allocate.
     * @return A new memory segment, backed by unpooled heap memory.
     */
    public static MemorySegment allocate(int size) {
        return new MemorySegment(new byte[size]);
    }
}
