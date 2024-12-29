package info.victorchu.bamboo.segment.utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static info.victorchu.bamboo.segment.utils.PreConditions.checkFromToIndex;

public class ByteUtils
{
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static byte[] intToBytes(int value)
    {
        return ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(value).array();
    }

    public static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static int compareUnsignedBytes(byte thisByte, byte thatByte)
    {
        return unsignedByteToInt(thisByte) - unsignedByteToInt(thatByte);
    }

    public static int unsignedByteToInt(byte thisByte)
    {
        return thisByte & 0xFF;
    }

    public static short unsignedByteToShort(byte value)
    {
        return (short) (value & 0xFF);
    }

    public static int unsignedShortToInt(short value)
    {
        return value & 0xFFFF;
    }

    public static byte[] unsignedShortToBytes(short value)
    {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) ((value >> 8) & 0xFF); // Higher-order byte
        bytes[1] = (byte) (value & 0xFF);       // Lower-order byte
        return bytes;
    }

    public static long unsignedIntToLong(int value)
    {
        return value & 0xFFFFFFFFL;
    }

    public static BigInteger unsignedLongToBigInt(long value)
    {
        if (value >= 0L) {
            return BigInteger.valueOf(value);
        }
        else {
            int upper = (int) (value >>> 32);
            int lower = (int) value;

            // return (upper << 32) + lower
            return (BigInteger.valueOf(Integer.toUnsignedLong(upper))).shiftLeft(32).
                    add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
        }
    }

    public static int compareUnsigned(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex)
    {
        checkFromToIndex(aFromIndex, aToIndex, a.length);
        checkFromToIndex(bFromIndex, bToIndex, b.length);
        int aLen = aToIndex - aFromIndex;
        int bLen = bToIndex - bFromIndex;
        int len = Math.min(aLen, bLen);
        for (int i = 0; i < len; i++) {
            int aByte = unsignedByteToInt(a[i + aFromIndex]);
            int bByte = unsignedByteToInt(b[i + bFromIndex]);
            int diff = aByte - bByte;
            if (diff != 0) {
                return diff;
            }
        }
        // One is a prefix of the other, or, they are equal:
        return aLen - bLen;
    }

    public static boolean equals(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex)
    {
        checkFromToIndex(aFromIndex, aToIndex, a.length);
        checkFromToIndex(bFromIndex, bToIndex, b.length);
        int aLen = aToIndex - aFromIndex;
        int bLen = bToIndex - bFromIndex;
        // lengths differ: cannot be equal
        if (aLen != bLen) {
            return false;
        }
        for (int i = 0; i < aLen; i++) {
            if (a[i + aFromIndex] != b[i + bFromIndex]) {
                return false;
            }
        }
        return true;
    }

}
