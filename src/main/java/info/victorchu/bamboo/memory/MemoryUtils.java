package info.victorchu.bamboo.memory;

import info.victorchu.bamboo.utils.Preconditions;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MemoryUtils
{
    /** The "unsafe", which can be used to perform native memory accesses. */
    @SuppressWarnings({"restriction", "UseOfSunClasses"})
    public static final sun.misc.Unsafe UNSAFE = getUnsafe();

    /** The native byte order of the platform on which the system currently runs. */
    public static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();

    private static final long BUFFER_ADDRESS_FIELD_OFFSET =
            getClassFieldOffset(Buffer.class, "address");
    private static final long BUFFER_CAPACITY_FIELD_OFFSET =
            getClassFieldOffset(Buffer.class, "capacity");
    private static final Class<?> DIRECT_BYTE_BUFFER_CLASS =
            getClassByName("java.nio.DirectByteBuffer");

    @SuppressWarnings("restriction")
    private static sun.misc.Unsafe getUnsafe() {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (sun.misc.Unsafe) unsafeField.get(null);
        } catch (SecurityException e) {
            throw new Error(
                    "Could not access the sun.misc.Unsafe handle, permission denied by security manager.",
                    e);
        } catch (NoSuchFieldException e) {
            throw new Error("The static handle field in sun.misc.Unsafe was not found.", e);
        } catch (IllegalArgumentException e) {
            throw new Error("Bug: Illegal argument reflection access for static field.", e);
        } catch (IllegalAccessException e) {
            throw new Error("Access to sun.misc.Unsafe is forbidden by the runtime.", e);
        } catch (Throwable t) {
            throw new Error(
                    "Unclassified error while trying to access the sun.misc.Unsafe handle.", t);
        }
    }

    private static long getClassFieldOffset(
            @SuppressWarnings("SameParameterValue") Class<?> cl, String fieldName) {
        try {
            return UNSAFE.objectFieldOffset(cl.getDeclaredField(fieldName));
        } catch (SecurityException e) {
            throw new Error(
                    getClassFieldOffsetErrorMessage(cl, fieldName)
                            + ", permission denied by security manager.",
                    e);
        } catch (NoSuchFieldException e) {
            throw new Error(getClassFieldOffsetErrorMessage(cl, fieldName), e);
        } catch (Throwable t) {
            throw new Error(
                    getClassFieldOffsetErrorMessage(cl, fieldName) + ", unclassified error", t);
        }
    }

    private static String getClassFieldOffsetErrorMessage(Class<?> cl, String fieldName) {
        return "Could not get field '"
                + fieldName
                + "' offset in class '"
                + cl
                + "' for unsafe operations";
    }

    private static Class<?> getClassByName(
            @SuppressWarnings("SameParameterValue") String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new Error("Could not find class '" + className + "' for unsafe operations.", e);
        }
    }
    /**
     * Get native memory address wrapped by the given {@link ByteBuffer}.
     *
     * @param buffer {@link ByteBuffer} which wraps the native memory address to get
     * @return native memory address wrapped by the given {@link ByteBuffer}
     */
    public static long getByteBufferAddress(ByteBuffer buffer) {
        Preconditions.checkNotNull(buffer, "buffer is null");
        Preconditions.checkArgument(
                buffer.isDirect(), "Can't get address of a non-direct ByteBuffer.");

        long offHeapAddress;
        try {
            offHeapAddress = UNSAFE.getLong(buffer, BUFFER_ADDRESS_FIELD_OFFSET);
        } catch (Throwable t) {
            throw new Error("Could not access direct byte buffer address field.", t);
        }

        Preconditions.checkState(offHeapAddress > 0, "negative pointer or size");
        Preconditions.checkState(
                offHeapAddress < Long.MAX_VALUE - Integer.MAX_VALUE,
                "Segment initialized with too large address: "
                        + offHeapAddress
                        + " ; Max allowed address is "
                        + (Long.MAX_VALUE - Integer.MAX_VALUE - 1));

        return offHeapAddress;
    }
}
