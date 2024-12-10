package info.victorchu.bamboo.segment;

import info.victorchu.bamboo.RetainedSizeAware;

public class VariableWidthSegment
        implements Segment, RetainedSizeAware
{

    @Override
    public long getRetainedSize()
    {
        return 0;
    }

    @Override
    public int getPosition()
    {
        return 0;
    }

    @Override
    public int getCapacity()
    {
        return 0;
    }
}