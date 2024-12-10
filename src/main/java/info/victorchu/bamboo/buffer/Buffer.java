package info.victorchu.bamboo.buffer;

import info.victorchu.bamboo.RetainedSizeAware;
import info.victorchu.bamboo.utils.PreConditions;

import static info.victorchu.bamboo.utils.SizeOf.instanceSize;
import static info.victorchu.bamboo.utils.SizeOf.sizeOf;
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

    /**
     * only for Buffer.EMPTY_BUFFER
     */
    private Buffer()
    {
        this.base = new byte[0];
        this.baseOffset = 0;
        this.size = 0;
        this.retainedSize = INSTANCE_SIZE;
    }

    Buffer(byte[] base)
    {
        requireNonNull(base, "base is null");
        if (base.length == 0) {
            throw new IllegalArgumentException("Empty array");
        }
        this.base = base;
        this.baseOffset = 0;
        this.size = base.length;
        this.retainedSize = INSTANCE_SIZE + sizeOf(base);
    }

    Buffer(byte[] base, int offset, int length)
    {
        requireNonNull(base, "base is null");
        if (base.length == 0) {
            throw new IllegalArgumentException("Empty array");
        }
        PreConditions.checkFromIndexSize(offset, length, base.length);

        this.base = base;
        this.baseOffset = offset;
        this.size = length;
        this.retainedSize = INSTANCE_SIZE + sizeOf(base);
    }

    public int length()
    {
        return size;
    }

    public long getRetainedSize()
    {
        return retainedSize;
    }
}
