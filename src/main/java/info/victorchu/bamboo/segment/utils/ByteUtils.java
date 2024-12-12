package info.victorchu.bamboo.segment.utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteUtils
{
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();


    public short toUnsignedByte(byte value)
    {
        return (short) (value & 0xFF);
    }
    public int toUnsignedShort(short value)
    {
        return value & 0xFFFF;
    }

    public static long toUnsignedInt(int value) {
        return value & 0xFFFFFFFFL;
    }

    public static BigInteger toUnsignedLong(long value) {
        if (value >= 0L)
            return BigInteger.valueOf(value);
        else {
            int upper = (int) (value >>> 32);
            int lower = (int) value;

            // return (upper << 32) + lower
            return (BigInteger.valueOf(Integer.toUnsignedLong(upper))).shiftLeft(32).
                    add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
        }
    }


    public static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(value).array();
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
