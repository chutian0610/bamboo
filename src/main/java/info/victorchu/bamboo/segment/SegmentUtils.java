package info.victorchu.bamboo.segment;

import static java.lang.String.format;

public class SegmentUtils
{
    static void checkValidPosition(int position, int positionCount)
    {
        if (position < 0 || position >= positionCount) {
            throw new IllegalArgumentException(format("Invalid position %s in block with %s positions", position, positionCount));
        }
    }

    static void checkReadablePosition(Segment segment, int position)
    {
        checkValidPosition(position, segment.getPosition());
    }
}
