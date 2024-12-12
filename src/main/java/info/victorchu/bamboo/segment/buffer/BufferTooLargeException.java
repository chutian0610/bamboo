package info.victorchu.bamboo.segment.buffer;

public class BufferTooLargeException
        extends IllegalArgumentException
{
    public BufferTooLargeException(String message)
    {
        super(message);
    }
}
