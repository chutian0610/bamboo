package info.victorchu.bamboo.segment;

import info.victorchu.bamboo.RetainedSizeAware;

import static info.victorchu.bamboo.utils.SizeOf.instanceSize;

public class SegmentStatus
        implements RetainedSizeAware
{
    public static final int INSTANCE_SIZE = instanceSize(SegmentStatus.class);
    private int currentSize;
    private long memUsedSize;
    private boolean hasNullValue;
    private boolean hasNonNullValue;

    public void updateMemUsedSize(long retainedSize){
        this.memUsedSize = retainedSize;
    }

    public void addBytes(int bytes)
    {
        this.currentSize += bytes;
    }

    public void hasNullValue()
    {
        this.hasNullValue = true;
    }

    public void hasNonNullValue()
    {
        this.hasNonNullValue = true;
    }

    public boolean isHasNullValue()
    {
        return hasNullValue;
    }

    public boolean isHasNonNullValue()
    {
        return hasNonNullValue;
    }

    public int getCurrentSize()
    {
        return currentSize;
    }

    public long getMemUsedSize()
    {
        return memUsedSize;
    }

    @Override
    public long getRetainedSize()
    {
        return INSTANCE_SIZE;
    }
}
