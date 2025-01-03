package info.victorchu.bamboo.memory;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MemorySegmentTest
{
    private final Random random = new Random();
    private final int PAGE_SIZE = 1024*1024;

    MemorySegment createSegment(int size) {
        return MemorySegments.allocate(size);
    }
    @Test
    void testByteAccess() {
        int pageSize = PAGE_SIZE;
        final MemorySegment segment = createSegment(pageSize);

        // test exceptions
        // put() method
        assertThatThrownBy(() -> segment.put(-1, (byte) 0))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> segment.put(pageSize, (byte) 0))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> segment.put(Integer.MAX_VALUE, (byte) 0))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> segment.put(Integer.MIN_VALUE, (byte) 0))
                .isInstanceOf(IndexOutOfBoundsException.class);

        // get() method
        assertThatThrownBy(() -> segment.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> segment.get(pageSize))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> segment.get(Integer.MAX_VALUE))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> segment.get(Integer.MIN_VALUE))
                .isInstanceOf(IndexOutOfBoundsException.class);
        // test expected correct behavior, sequential access

        long seed = random.nextLong();

        random.setSeed(seed);
        for (int i = 0; i < pageSize; i++) {
            segment.put(i, (byte) random.nextInt());
        }

        random.setSeed(seed);
        for (int i = 0; i < pageSize; i++) {
            assertThat(segment.get(i)).isEqualTo((byte) random.nextInt());
        }

        // test expected correct behavior, random access

        random.setSeed(seed);
        boolean[] occupied = new boolean[pageSize];

        for (int i = 0; i < 1000; i++) {
            int pos = random.nextInt(pageSize);

            if (occupied[pos]) {
                continue;
            } else {
                occupied[pos] = true;
            }

            segment.put(pos, (byte) random.nextInt());
        }

        random.setSeed(seed);
        occupied = new boolean[pageSize];

        for (int i = 0; i < 1000; i++) {
            int pos = random.nextInt(pageSize);

            if (occupied[pos]) {
                continue;
            } else {
                occupied[pos] = true;
            }

            assertThat(segment.get(pos)).isEqualTo((byte) random.nextInt());
        }
    }
}