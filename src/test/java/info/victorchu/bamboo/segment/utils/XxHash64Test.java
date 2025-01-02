package info.victorchu.bamboo.segment.utils;

import info.victorchu.bamboo.segment.buffer.Buffer;
import info.victorchu.bamboo.segment.buffer.Buffers;
import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static info.victorchu.bamboo.segment.utils.XxHash64.hash;
import static java.lang.Math.min;
import static org.assertj.core.api.Assertions.assertThat;

class XxHash64Test
{
    private static final byte[] EMPTY_BYTES = {};

    private static final long PRIME = 2654435761L;

    private final Buffer buffer;

    public XxHash64Test()
    {
        buffer = Buffers.allocate(101);

        long value = PRIME;
        for (int i = 0; i < buffer.length(); i++) {
            buffer.setByte(i, (byte) (value >> 24));
            value *= value;
        }
    }

    @Test
    public void testSanity()
            throws Exception
    {
        assertHash(0, buffer, 1, 0x4FCE394CC88952D8L);
        assertHash(PRIME, buffer, 1, 0x739840CB819FA723L);

        assertHash(0, buffer, 4, 0x9256E58AA397AEF1L);
        assertHash(PRIME, buffer, 4, 0x9D5FFDFB928AB4BL);

        assertHash(0, buffer, 8, 0xF74CB1451B32B8CFL);
        assertHash(PRIME, buffer, 8, 0x9C44B77FBCC302C5L);

        assertHash(0, buffer, 14, 0xCFFA8DB881BC3A3DL);
        assertHash(PRIME, buffer, 14, 0x5B9611585EFCC9CBL);

        assertHash(0, buffer, 32, 0xAF5753D39159EDEEL);
        assertHash(PRIME, buffer, 32, 0xDCAB9233B8CA7B0FL);

        assertHash(0, buffer, buffer.length(), 0x0EAB543384F878ADL);
        assertHash(PRIME, buffer, buffer.length(), 0xCAA65939306F1E21L);
    }

    private static void assertHash(long seed, Buffer data, int length, long expected)
            throws IOException
    {
        assertThat(hash(seed, data, 0, length)).isEqualTo(expected);
        assertThat(hash(seed, data.slice(0, length))).isEqualTo(expected);

        assertThat(new XxHash64(seed).update(data.slice(0, length)).hash()).isEqualTo(expected);
        assertThat(new XxHash64(seed).update(data, 0, length).hash()).isEqualTo(expected);
        assertThat(new XxHash64(seed).update(data.getBytes(0, length)).hash()).isEqualTo(expected);
        assertThat(new XxHash64(seed).update(data.getBytes(), 0, length).hash()).isEqualTo(expected);

        assertThat(hash(seed, new ByteArrayInputStream(data.getBytes(0, length)))).isEqualTo(expected);

        for (int chunkSize = 1; chunkSize <= length; chunkSize++) {
            XxHash64 hash = new XxHash64(seed);
            for (int i = 0; i < length; i += chunkSize) {
                int updateSize = min(length - i, chunkSize);
                hash.update(data.slice(i, updateSize));
                assertThat(hash.hash()).isEqualTo(hash(seed, data, 0, i + updateSize));
            }
            assertThat(hash.hash()).isEqualTo(expected);
        }
    }

    @Test
    public void testMultipleLengths()
            throws Exception
    {
        XXHash64 jpountz = XXHashFactory.fastestInstance().hash64();
        for (int i = 0; i < 20_000; i++) {
            byte[] data = new byte[i];
            long expected = jpountz.hash(data, 0, data.length, 0);

            Buffer buffer = Buffers.wrappedBuffer(data);
            assertThat(hash(buffer)).isEqualTo(expected);
            assertThat(new XxHash64().update(buffer).hash()).isEqualTo(expected);
            assertThat(new XxHash64().update(data).hash()).isEqualTo(expected);

            assertThat(hash(new ByteArrayInputStream(data))).isEqualTo(expected);
        }
    }

    @Test
    public void testEmpty()
    {
        long expected = 0xEF46DB3751D8E999L;

        assertThat(hash(Buffer.EMPTY_BUFFER())).isEqualTo(expected);
        assertThat(hash(Buffer.EMPTY_BUFFER(), 0, 0)).isEqualTo(expected);

        assertThat(hash(0, Buffer.EMPTY_BUFFER())).isEqualTo(expected);
        assertThat(hash(0, Buffer.EMPTY_BUFFER(), 0, 0)).isEqualTo(expected);

        assertThat(new XxHash64().update(Buffer.EMPTY_BUFFER()).hash()).isEqualTo(expected);
        assertThat(new XxHash64().update(Buffer.EMPTY_BUFFER(), 0, 0).hash()).isEqualTo(expected);

        assertThat(new XxHash64().update(EMPTY_BYTES).hash()).isEqualTo(expected);
        assertThat(new XxHash64().update(EMPTY_BYTES, 0, 0).hash()).isEqualTo(expected);

        assertThat(
                new XxHash64()
                        .update(EMPTY_BYTES)
                        .update(EMPTY_BYTES, 0, 0)
                        .update(Buffer.EMPTY_BUFFER())
                        .update(Buffer.EMPTY_BUFFER(), 0, 0)
                        .hash())
                .isEqualTo(expected);
    }

    @Test
    public void testHashLong()
    {
        // Different seed
        assertThat(hash(42, buffer.getLong(0))).isNotEqualTo(hash(buffer, 0, SizeOf.SIZE_OF_LONG));
        // Matching seed
        assertThat(hash(buffer.getLong(0))).isEqualTo(hash(buffer, 0, SizeOf.SIZE_OF_LONG));
        assertThat(hash(42, buffer.getLong(0))).isEqualTo(hash(42, buffer, 0, SizeOf.SIZE_OF_LONG));
    }
}