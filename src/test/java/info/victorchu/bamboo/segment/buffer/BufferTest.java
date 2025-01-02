package info.victorchu.bamboo.segment.buffer;

import info.victorchu.bamboo.segment.utils.SizeOf;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static info.victorchu.bamboo.segment.buffer.Buffer.EMPTY_BUFFER;
import static info.victorchu.bamboo.segment.buffer.Buffers.utf8Buffer;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_BYTE;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_DOUBLE;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_FLOAT;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_LONG;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_SHORT;
import static info.victorchu.bamboo.segment.utils.SizeOf.SIZE_OF_INT;
import static info.victorchu.bamboo.segment.utils.SizeOf.sizeOfByteArray;
import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BufferTest
{
    @Test
    public void testFillAndClear()
    {
        for (byte size = 0; size < 100; size++) {
            Buffer buffer = allocate(size);
            for (int i = 0; i < buffer.length(); i++) {
                assertThat(buffer.getByte(i)).isEqualTo((byte) 0);
            }
            buffer.fill((byte) 0xA5);
            for (int i = 0; i < buffer.length(); i++) {
                assertThat(buffer.getByte(i)).isEqualTo((byte) 0xA5);
            }
            buffer.clear();
            for (int i = 0; i < buffer.length(); i++) {
                assertThat(buffer.getByte(i)).isEqualTo((byte) 0);
            }
        }
    }

    @Test
    public void testEqualsHashCodeCompare()
    {
        for (int size = 0; size < 100; size++) {
            // self equals
            Buffer buffer = allocate(size);
            assertSlicesEquals(buffer, buffer);

            // equals other all zero
            Buffer other = allocate(size);
            assertSlicesEquals(buffer, other);

            // equals self fill pattern
            buffer = allocate(size); // create a new slice since slices cache the hash code value
            buffer.fill((byte) 0xA5);
            assertSlicesEquals(buffer, buffer);

            // equals other fill pattern
            other = allocate(size); // create a new slice since slices cache the hash code value
            other.fill((byte) 0xA5);
            assertSlicesEquals(buffer, other);

            // different types
            assertThat(buffer).isNotEqualTo(new Object());
            assertThat(new Object()).isNotEqualTo(buffer);

            // different sizes
            Buffer oneBigger = allocate(size + 1);
            oneBigger.fill((byte) 0xA5);
            assertThat(buffer).isNotEqualTo(oneBigger);
            assertThat(oneBigger).isNotEqualTo(buffer);
            assertThat(buffer.compareTo(oneBigger) < 0).isTrue();
            assertThat(oneBigger.compareTo(buffer) > 0).isTrue();
            assertThat(buffer.equals(0, size, oneBigger, 0, size + 1)).isFalse();
            assertThat(oneBigger.equals(0, size + 1, buffer, 0, size)).isFalse();
            assertThat(buffer.compareTo(0, size, oneBigger, 0, size + 1) < 0).isTrue();
            assertThat(oneBigger.compareTo(0, size + 1, buffer, 0, size) > 0).isTrue();

            // different in one byte
            for (int i = 1; i < buffer.length(); i++) {
                buffer.setByte(i - 1, 0xA5);
                assertThat(buffer.equals(i - 1, size - i, other, i - 1, size - i)).isTrue();
                buffer.setByte(i, 0xFF);
                assertThat(buffer).isNotEqualTo(other);
                assertThat(buffer.equals(i, size - i, other, i, size - i)).isFalse();
                assertThat(buffer.compareTo(0, size, oneBigger, 0, size + 1) > 0).isTrue();
            }

            // compare with empty slice
            if (buffer.length() > 0) {
                testCompareWithEmpty(size, buffer);
            }
        }
    }

    private static void testCompareWithEmpty(int size, Buffer slice)
    {
        assertThat(slice).isNotEqualTo(EMPTY_BUFFER());
        assertThat(EMPTY_BUFFER()).isNotEqualTo(slice);

        assertThat(slice.equals(0, size, EMPTY_BUFFER(), 0, 0)).isFalse();
        assertThat(EMPTY_BUFFER().equals(0, 0, slice, 0, size)).isFalse();

        assertThat(slice.compareTo(0, size, EMPTY_BUFFER(), 0, 0) > 0).isTrue();
        assertThat(EMPTY_BUFFER().compareTo(0, 0, slice, 0, size) < 0).isTrue();

        assertThatThrownBy(() -> slice.equals(0, size, EMPTY_BUFFER(), 0, size))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> EMPTY_BUFFER().equals(0, size, slice, 0, size))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> slice.compareTo(0, size, EMPTY_BUFFER(), 0, size))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> EMPTY_BUFFER().compareTo(0, size, slice, 0, size))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testBackingByteArray()
    {
        byte[] bytes = new byte[Byte.MAX_VALUE];
        for (int i = 0; i < bytes.length; i++) {
            Buffer buffer = Buffers.wrappedBuffer(bytes, i, bytes.length - i);
            assertThat(buffer.byteArrayOffset()).isEqualTo(i);
            assertThat(buffer.byteArray()).isSameAs(bytes);
            bytes[i] = (byte) i;
            assertThat(buffer.getByte(0)).isEqualTo((byte) i);
        }
    }

    private static void assertSlicesEquals(Buffer slice, Buffer other)
    {
        int size = slice.length();

        assertThat(slice).isEqualTo(other);
        assertThat(slice.equals(0, size, other, 0, size)).isTrue();
        assertThat(slice.hashCode()).isEqualTo(other.hashCode());
        assertThat(slice.hashCode()).isEqualTo(other.hashCode(0, size));
        assertThat(slice.compareTo(other)).isEqualTo(0);
        assertThat(slice.compareTo(0, size, other, 0, size)).isEqualTo(0);
        for (int i = 0; i < slice.length(); i++) {
            assertThat(slice.equals(i, size - i, other, i, size - i)).isTrue();
            assertThat(slice.hashCode(i, size - i)).isEqualTo(other.hashCode(i, size - i));
            assertThat(slice.compareTo(i, size - i, other, i, size - i)).isEqualTo(0);
        }
        for (int i = 0; i < slice.length(); i++) {
            assertThat(slice.equals(0, size - i, other, 0, size - i)).isTrue();
            assertThat(slice.hashCode(0, size - i)).isEqualTo(other.hashCode(0, size - i));
            assertThat(slice.compareTo(0, size - i, other, 0, size - i)).isEqualTo(0);
        }
    }

    @Test
    public void testToString()
    {
        assertThat(Buffers.copiedBuffer("apple", UTF_8).toString(UTF_8)).isEqualTo("apple");

        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < size; index++) {
                assertToStrings(allocate(size), index);
            }
        }
    }

    @Test
    public void testUtf8Conversion()
    {
        String s = "apple \u2603 snowman";
        Buffer buffer = Buffers.copiedBuffer(s, UTF_8);
        assertThat(utf8Buffer(s)).isEqualTo(buffer);
        assertThat(buffer.toStringUtf8()).isEqualTo(s);
        assertThat(utf8Buffer(s).toStringUtf8()).isEqualTo(s);
    }

    @SuppressWarnings("CharUsedInArithmeticContext")
    private static void assertToStrings(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        // set and get the value
        char[] chars = new char[(buffer.length() - index) / 2];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) ('a' + (i % 26));
        }
        String string = new String(chars);
        Buffer value = Buffers.copiedBuffer(string, UTF_8);
        buffer.setBytes(index, value);
        assertThat(buffer.toString(index, value.length(), UTF_8)).isEqualTo(string);

        for (int length = 0; length < value.length(); length++) {
            buffer.fill((byte) 0xFF);
            buffer.setBytes(index, value, 0, length);
            assertThat(buffer.toString(index, length, UTF_8)).isEqualTo(string.substring(0, length));
        }
    }

    @Test
    public void testByte()
    {
        for (byte size = 0; size < 100; size++) {
            for (byte index = 0; index < (size - SIZE_OF_BYTE); index++) {
                assertByte(allocate(size), index);
            }
        }
    }

    private static void assertByte(Buffer buffer, byte index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        // set and get unsigned value
        buffer.setByte(index, 0xA5);
        assertThat(buffer.getUnsignedByte(index)).isEqualTo((short) 0x0000_00A5);

        // set and get the value
        buffer.setByte(index, 0xA5);
        assertThat(buffer.getByte(index)).isEqualTo((byte) 0xA5);

        assertThatThrownBy(() -> buffer.getByte(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> buffer.getByte((buffer.length() - SIZE_OF_BYTE) + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> buffer.getByte(buffer.length()))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> buffer.getByte(buffer.length() + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testShort()
    {
        for (short size = 0; size < 100; size++) {
            for (short index = 0; index < (size - SIZE_OF_SHORT); index++) {
                assertShort(allocate(size), index);
            }
        }
    }

    private static void assertShort(Buffer buffer, short index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        // set and get the value
        buffer.setShort(index, 0xAA55);
        assertThat(buffer.getShort(index)).isEqualTo((short) 0xAA55);

        assertThatThrownBy(() -> buffer.getShort(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getShort((buffer.length() - SIZE_OF_SHORT) + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getShort(buffer.length()))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getShort(buffer.length() + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testInt()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < (size - SIZE_OF_INT); index++) {
                assertInt(allocate(size), index);
            }
        }
    }

    private static void assertInt(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        // set and get the value
        buffer.setInt(index, 0xAAAA_5555);
        assertThat(buffer.getInteger(index)).isEqualTo(0xAAAA_5555);

        assertThatThrownBy(() -> buffer.getInteger(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getInteger((buffer.length() - SIZE_OF_INT) + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getInteger(buffer.length()))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getInteger(buffer.length() + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testLong()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < (size - SIZE_OF_LONG); index++) {
                assertLong(allocate(size), index);
            }
        }
    }

    private static void assertLong(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        // set and get the value
        buffer.setLong(index, 0xAAAA_AAAA_5555_5555L);
        assertThat(buffer.getLong(index)).isEqualTo(0xAAAA_AAAA_5555_5555L);

        assertThatThrownBy(() -> buffer.getLong(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getLong((buffer.length() - SIZE_OF_LONG) + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getLong(buffer.length()))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getLong(buffer.length() + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testFloat()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < (size - SIZE_OF_FLOAT); index++) {
                assertFloat(allocate(size), index);
            }
        }
    }

    private static void assertFloat(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        // set and get the value
        buffer.setFloat(index, intBitsToFloat(0xAAAA_5555));
        assertThat(floatToIntBits(buffer.getFloat(index))).isEqualTo(0xAAAA_5555);

        assertThatThrownBy(() -> buffer.getFloat(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getFloat((buffer.length() - SIZE_OF_FLOAT) + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getFloat(buffer.length()))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getFloat(buffer.length() + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testDouble()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < (size - SIZE_OF_DOUBLE); index++) {
                assertDouble(allocate(size), index);
            }
        }
    }

    private static void assertDouble(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        // set and get the value
        buffer.setDouble(index, longBitsToDouble(0xAAAA_AAAA_5555_5555L));
        assertThat(doubleToLongBits(buffer.getDouble(index))).isEqualTo(0xAAAA_AAAA_5555_5555L);

        assertThatThrownBy(() -> buffer.getDouble(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getDouble((buffer.length() - SIZE_OF_DOUBLE) + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getDouble(buffer.length()))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getDouble(buffer.length() + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testBytesArray()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < size; index++) {
                assertBytesArray(allocate(size), index);
            }
        }
    }

    private static void assertBytesArray(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        byte[] value = new byte[buffer.length()];
        Arrays.fill(value, (byte) 0xFF);
        assertThat(buffer.getBytes()).isEqualTo(value);

        // set and get the value
        value = new byte[(buffer.length() - index) / 2];
        for (int i = 0; i < value.length; i++) {
            value[i] = (byte) i;
        }
        buffer.setBytes(index, value);
        assertThat(buffer.getBytes(index, value.length)).isEqualTo(value);

        for (int length = 0; length < value.length; length++) {
            buffer.fill((byte) 0xFF);
            buffer.setBytes(index, value, 0, length);
            assertThat(buffer.getBytes(index, length)).isEqualTo(Arrays.copyOf(value, length));
        }

        assertThatThrownBy(() -> buffer.setBytes(buffer.length() - 1, new byte[10]))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.setBytes(buffer.length() - 1, new byte[20], 1, 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testShortsArray()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < size; index++) {
                assertShortsArray(allocate(size), index);
            }
        }
    }

    private static void assertShortsArray(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        short[] value = new short[(buffer.length() - index) / 2];
        buffer.getShorts(index, value);
        for (short v : value) {
            assertThat(v).isEqualTo((short) -1);
        }
        Arrays.fill(value, (short) -1);
        assertThat(buffer.getShorts(index, value.length)).isEqualTo(value);

        // set and get the value
        value = new short[value.length / 2];
        for (int i = 0; i < value.length; i++) {
            value[i] = (short) i;
        }
        buffer.setShorts(index, value);
        assertThat(buffer.getShorts(index, value.length)).isEqualTo(value);

        for (int length = 0; length < value.length; length++) {
            buffer.fill((byte) 0xFF);
            buffer.setShorts(index, value, 0, length);
            assertThat(buffer.getShorts(index, length)).isEqualTo(Arrays.copyOf(value, length));
        }

        assertThatThrownBy(() -> buffer.setShorts(buffer.length() - 1, new short[10]))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.setShorts(buffer.length() - 1, new short[20], 1, 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testIntsArray()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < size; index++) {
                assertIntegersArray(allocate(size), index);
            }
        }
    }

    private static void assertIntegersArray(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        int[] value = new int[(buffer.length() - index) / 4];
        buffer.getIntegers(index, value);
        for (int v : value) {
            assertThat(v).isEqualTo(-1);
        }
        Arrays.fill(value, -1);
        assertThat(buffer.getIntegers(index, value.length)).isEqualTo(value);

        // set and get the value
        value = new int[value.length / 2];
        for (int i = 0; i < value.length; i++) {
            value[i] = i;
        }
        buffer.setIntegers(index, value);
        assertThat(buffer.getIntegers(index, value.length)).isEqualTo(value);

        for (int length = 0; length < value.length; length++) {
            buffer.fill((byte) 0xFF);
            buffer.setIntegers(index, value, 0, length);
            assertThat(buffer.getIntegers(index, length)).isEqualTo(Arrays.copyOf(value, length));
        }

        assertThatThrownBy(() -> buffer.setIntegers(buffer.length() - 1, new int[10]))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.setIntegers(buffer.length() - 1, new int[20], 1, 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testLongsArray()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < size; index++) {
                assertLongsArray(allocate(size), index);
            }
        }
    }

    private static void assertLongsArray(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        long[] value = new long[(buffer.length() - index) / 8];
        buffer.getLongs(index, value);
        for (long v : value) {
            assertThat(v).isEqualTo(-1);
        }
        Arrays.fill(value, -1L);
        assertThat(buffer.getLongs(index, value.length)).isEqualTo(value);

        // set and get the value
        value = new long[value.length / 2];
        for (int i = 0; i < value.length; i++) {
            value[i] = i;
        }
        buffer.setLongs(index, value);
        assertThat(buffer.getLongs(index, value.length)).isEqualTo(value);

        for (int length = 0; length < value.length; length++) {
            buffer.fill((byte) 0xFF);
            buffer.setLongs(index, value, 0, length);
            assertThat(buffer.getLongs(index, length)).isEqualTo(Arrays.copyOf(value, length));
        }

        assertThatThrownBy(() -> buffer.setLongs(buffer.length() - 1, new long[10]))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.setLongs(buffer.length() - 1, new long[20], 1, 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testFloatsArray()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < size; index++) {
                assertFloatsArray(allocate(size), index);
            }
        }
    }

    private static void assertFloatsArray(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        float[] value = new float[(buffer.length() - index) / 4];
        buffer.getFloats(index, value);
        for (float v : value) {
            assertThat(v).isNaN();
        }
        Arrays.fill(value, intBitsToFloat(-1));
        assertThat(buffer.getFloats(index, value.length)).isEqualTo(value);

        // set and get the value
        value = new float[value.length / 2];
        for (int i = 0; i < value.length; i++) {
            value[i] = i;
        }
        buffer.setFloats(index, value);
        assertThat(buffer.getFloats(index, value.length)).isEqualTo(value);

        for (int length = 0; length < value.length; length++) {
            buffer.fill((byte) 0xFF);
            buffer.setFloats(index, value, 0, length);
            assertThat(buffer.getFloats(index, length)).isEqualTo(Arrays.copyOf(value, length));
        }

        assertThatThrownBy(() -> buffer.setFloats(buffer.length() - 1, new float[10]))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.setFloats(buffer.length() - 1, new float[20], 1, 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testDoublesArray()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < size; index++) {
                assertDoublesArray(allocate(size), index);
            }
        }
    }

    private static void assertDoublesArray(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        double[] value = new double[(buffer.length() - index) / 8];
        buffer.getDoubles(index, value);
        for (double v : value) {
            assertThat(v).isNaN();
        }
        Arrays.fill(value, longBitsToDouble(-1));
        assertThat(buffer.getDoubles(index, value.length)).isEqualTo(value);

        // set and get the value
        value = new double[value.length / 2];
        for (int i = 0; i < value.length; i++) {
            value[i] = i;
        }
        buffer.setDoubles(index, value);
        assertThat(buffer.getDoubles(index, value.length)).isEqualTo(value);

        for (int length = 0; length < value.length; length++) {
            buffer.fill((byte) 0xFF);
            buffer.setDoubles(index, value, 0, length);
            assertThat(buffer.getDoubles(index, length)).isEqualTo(Arrays.copyOf(value, length));
        }

        assertThatThrownBy(() -> buffer.setDoubles(buffer.length() - 1, new double[10]))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.setDoubles(buffer.length() - 1, new double[20], 1, 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testBytesSlice()
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < size; index++) {
                assertBytesSlice(allocate(size), index);
            }
        }
    }

    private void assertBytesSlice(Buffer buffer, int index)
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        // compare to self slice
        assertThat(buffer.slice(0, buffer.length())).isEqualTo(buffer);
        Buffer value = allocate(buffer.length());
        buffer.getBytes(0, value, 0, buffer.length());
        assertThat(value).isEqualTo(buffer);

        // set and get the value
        value = allocate((buffer.length() - index) / 2);
        for (int i = 0; i < value.length(); i++) {
            value.setByte(i, i);
        }

        // check by slicing out the region
        buffer.setBytes(index, value);
        assertThat(value).isEqualTo(buffer.slice(index, value.length()));

        // check by getting out the region
        Buffer tempValue = allocate(value.length());
        buffer.getBytes(index, tempValue, 0, tempValue.length());
        assertThat(tempValue).isEqualTo(buffer.slice(index, tempValue.length()));
        assertThat(tempValue.equals(0, tempValue.length(), buffer, index, tempValue.length())).isTrue();

        for (int length = 0; length < value.length(); length++) {
            buffer.fill((byte) 0xFF);
            buffer.setBytes(index, value, 0, length);

            // check by slicing out the region
            assertThat(value.slice(0, length)).isEqualTo(buffer.slice(index, length));
            assertThat(value.equals(0, length, buffer, index, length)).isTrue();

            // check by getting out the region
            tempValue = allocate(length);
            buffer.getBytes(index, tempValue);
            assertThat(tempValue).isEqualTo(buffer.slice(index, length));
            assertThat(tempValue.equals(0, length, buffer, index, length)).isTrue();
        }

        assertThatThrownBy(() -> buffer.getBytes(buffer.length() - 1, 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getBytes(buffer.length() - 1, new byte[20]))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> buffer.getBytes(buffer.length() - 1, new byte[20], 1, 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testBytesStreams()
            throws Exception
    {
        for (int size = 0; size < 100; size++) {
            for (int index = 0; index < size; index++) {
                assertBytesStreams(allocate(size), index);
            }
        }
        assertBytesStreams(allocate(16 * 1024), 3);
    }

    private static void assertBytesStreams(Buffer buffer, int index)
            throws Exception
    {
        // fill slice with FF
        buffer.fill((byte) 0xFF);

        byte[] value = new byte[buffer.length()];
        Arrays.fill(value, (byte) 0xFF);
        assertThat(buffer.getBytes()).isEqualTo(value);

        // set and get the value
        value = new byte[(buffer.length() - index) / 2];
        for (int i = 0; i < value.length; i++) {
            value[i] = (byte) i;
        }
        buffer.setBytes(index, new ByteArrayInputStream(value), value.length);
        assertThat(buffer.getBytes(index, value.length)).isEqualTo(value);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        buffer.getBytes(index, out, value.length);
        assertThat(buffer.getBytes(index, value.length)).isEqualTo(out.toByteArray());

        for (int length = 0; length < value.length; length++) {
            buffer.fill((byte) 0xFF);
            buffer.setBytes(index, new ByteArrayInputStream(value), length);
            assertThat(buffer.getBytes(index, length)).isEqualTo(Arrays.copyOf(value, length));

            out = new ByteArrayOutputStream();
            buffer.getBytes(index, out, length);
            assertThat(buffer.getBytes(index, length)).isEqualTo(out.toByteArray());
        }
    }

    @Test
    public void testRetainedSize()
            throws Exception
    {
        int sliceInstanceSize = SizeOf.instanceSize(Buffer.class);
        Buffer buffer = Buffers.allocate(10);
        assertThat(buffer.getRetainedSize()).isEqualTo(sizeOfByteArray(10) + sliceInstanceSize);
        assertThat(buffer.length()).isEqualTo(10);
        Buffer subBuffer = buffer.slice(0, 1);
        assertThat(subBuffer.getRetainedSize()).isEqualTo(sizeOfByteArray(10) + sliceInstanceSize);
        assertThat(subBuffer.length()).isEqualTo(1);
    }

    @Test
    public void testCopyOf()
            throws Exception
    {
        // slightly stronger guarantees for empty slice
        assertThat(EMPTY_BUFFER().copy().equals(EMPTY_BUFFER())).isTrue();
        assertThat(utf8Buffer("hello world").copy(1, 0).equals(EMPTY_BUFFER())).isTrue();

        Buffer buffer = utf8Buffer("hello world");
        assertThat(buffer.copy()).isEqualTo(buffer);
        assertThat(buffer.copy(1, 3)).isEqualTo(buffer.slice(1, 3));

        // verify it's an actual copy
        Buffer original = utf8Buffer("hello world");
        Buffer copy = original.copy();

        original.fill((byte) 0);
        assertThat(copy).isEqualTo(utf8Buffer("hello world"));

        // read before beginning
        assertThatThrownBy(() -> buffer.copy(-1, buffer.length()))
                .isInstanceOf(IndexOutOfBoundsException.class);

        // read after end
        assertThatThrownBy(() -> buffer.copy(buffer.length() + 1, 1))
                .isInstanceOf(IndexOutOfBoundsException.class);

        // start before but extend past end
        assertThatThrownBy(() -> buffer.copy(1, buffer.length()))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    public void testIndexOf()
    {
        assertIndexOf(utf8Buffer("no-match-bigger"), utf8Buffer("test"));
        assertIndexOf(utf8Buffer("no"), utf8Buffer("test"));

        assertIndexOf(utf8Buffer("test"), utf8Buffer("test"));
        assertIndexOf(utf8Buffer("test-start"), utf8Buffer("test"));
        assertIndexOf(utf8Buffer("end-test"), utf8Buffer("test"));
        assertIndexOf(utf8Buffer("a-test-middle"), utf8Buffer("test"));
        assertIndexOf(utf8Buffer("this-test-is-a-test"), utf8Buffer("test"));

        assertIndexOf(utf8Buffer("test"), EMPTY_BUFFER(), 0, 0);
        assertIndexOf(EMPTY_BUFFER(), utf8Buffer("test"), 0, -1);

        assertIndexOf(utf8Buffer("test"), utf8Buffer("no"), 4, -1);
        assertIndexOf(utf8Buffer("test"), utf8Buffer("no"), 5, -1);
        assertIndexOf(utf8Buffer("test"), utf8Buffer("no"), -1, -1);
    }

    public static void assertIndexOf(Buffer data, Buffer pattern, int offset, int expected)
    {
        assertThat(data.indexOf(pattern, offset)).isEqualTo(expected);
        assertThat(data.indexOfBruteForce(pattern, offset)).isEqualTo(expected);
    }

    public static void assertIndexOf(Buffer data, Buffer pattern)
    {
        int index;

        List<Integer> bruteForce = new ArrayList<>();
        index = 0;
        while (index >= 0 && index < data.length()) {
            index = data.indexOfBruteForce(pattern, index);
            if (index >= 0) {
                bruteForce.add(index);
                index++;
            }
        }

        List<Integer> indexOf = new ArrayList<>();
        index = 0;
        while (index >= 0 && index < data.length()) {
            index = data.indexOf(pattern, index);
            if (index >= 0) {
                indexOf.add(index);
                index++;
            }
        }

        assertThat(bruteForce).isEqualTo(indexOf);
    }

    @Test
    public void testIndexOfByte()
    {
        Buffer buffer = utf8Buffer("apple");

        assertThat(buffer.indexOfByte((byte) 'a')).isEqualTo(0);
        assertThat(buffer.indexOfByte((byte) 'p')).isEqualTo(1);
        assertThat(buffer.indexOfByte((byte) 'e')).isEqualTo(4);
        assertThat(buffer.indexOfByte((byte) 'x')).isEqualTo(-1);

        assertThat(buffer.indexOfByte('a')).isEqualTo(0);
        assertThat(buffer.indexOfByte('p')).isEqualTo(1);
        assertThat(buffer.indexOfByte('e')).isEqualTo(4);
        assertThat(buffer.indexOfByte('x')).isEqualTo(-1);

        assertThatThrownBy(() -> buffer.indexOfByte(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> buffer.indexOfByte(-123)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> buffer.indexOfByte(256)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> buffer.indexOfByte(500)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testToByteBuffer()
    {
        byte[] original = "hello world".getBytes(UTF_8);

        Buffer buffer = allocate(original.length);
        buffer.setBytes(0, original);
        assertThat(buffer.getBytes()).isEqualTo(original);

        assertThat(getBytes(buffer.toByteBuffer())).isEqualTo(original);

        assertToByteBuffer(buffer, original);
    }

    @Test
    public void testToByteBufferEmpty()
    {
        ByteBuffer buffer = allocate(0).toByteBuffer();
        assertThat(buffer.position()).isEqualTo(0);
        assertThat(buffer.remaining()).isEqualTo(0);
    }

    private static void assertToByteBuffer(Buffer buffer, byte[] original)
    {
        for (int index = 0; index < original.length; index++) {
            for (int length = 0; length < (original.length - index); length++) {
                byte[] actual = getBytes(buffer.toByteBuffer(index, length));
                byte[] expected = Arrays.copyOfRange(original, index, index + length);
                assertThat(actual).isEqualTo(expected);
            }
        }
    }

    private static byte[] getBytes(ByteBuffer buffer)
    {
        assertThat(buffer.position()).isEqualTo(0);
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return data;
    }

    protected Buffer allocate(int size)
    {
        return Buffers.allocate(size);
    }
}