package util;

public final class CpuUtil {

    public static short shortFromBytes(byte b1, byte b2) {
        return (short) (b1 << 8 | b2 & 0xFF);
    }
}
