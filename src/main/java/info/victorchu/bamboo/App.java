package info.victorchu.bamboo;

import info.victorchu.bamboo.segment.utils.JvmUtils;

import java.util.Arrays;

import static info.victorchu.bamboo.segment.utils.ByteUtils.bytesToHex;
import static info.victorchu.bamboo.segment.utils.ByteUtils.intToBytes;
import static sun.misc.Unsafe.ARRAY_BOOLEAN_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_INT_INDEX_SCALE;

public class App
{
    public static void main(String[] args)
    {
        byte[] bytes = new byte[9];
        int s = Integer.MAX_VALUE;
        System.out.println(bytesToHex(intToBytes(s)));
        int s1 = 10;
        System.out.println(bytesToHex(intToBytes(s1)));

        System.out.println(Arrays.toString(bytes));
        System.out.println(JvmUtils.unsafe.getInt((Object) bytes,ARRAY_BOOLEAN_BASE_OFFSET+ARRAY_INT_INDEX_SCALE*2+0L));
        JvmUtils.unsafe.putInt((Object) bytes,ARRAY_BOOLEAN_BASE_OFFSET+0L,s);
        JvmUtils.unsafe.putInt((Object) bytes,ARRAY_BOOLEAN_BASE_OFFSET+ARRAY_INT_INDEX_SCALE+0L,s1);
        System.out.println(JvmUtils.unsafe.getInt((Object) bytes,ARRAY_BOOLEAN_BASE_OFFSET+0L));
        System.out.println(JvmUtils.unsafe.getInt((Object) bytes,ARRAY_BOOLEAN_BASE_OFFSET+ARRAY_INT_INDEX_SCALE+0L));
        System.out.println(JvmUtils.unsafe.getInt((Object) bytes,ARRAY_BOOLEAN_BASE_OFFSET+ARRAY_INT_INDEX_SCALE*2+0L));
        System.out.println(bytesToHex(bytes));
        System.out.println(JvmUtils.unsafe.arrayBaseOffset(byte[].class));
    }
}
