package info.victorchu.bamboo.memory;

import java.io.DataOutput;
import java.io.IOException;

public interface DataOutputView extends DataOutput
{

    /**
     * Skips {@code numBytes} bytes memory. If some program reads the memory that was skipped over,
     * the results are undefined.
     *
     * @param numBytes The number of bytes to skip.
     * @throws IOException Thrown, if any I/O related problem occurred such that the view could not
     *     be advanced to the desired position.
     */
    void skipBytesToWrite(int numBytes) throws IOException;

    /**
     * Copies {@code numBytes} bytes from the source to this view.
     *
     * @param source The source to copy the bytes from.
     * @param numBytes The number of bytes to copy.
     * @throws IOException Thrown, if any I/O related problem occurred, such that either the input
     *     view could not be read, or the output could not be written.
     */
    void write(DataInputView source, int numBytes) throws IOException;
}
