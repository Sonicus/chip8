package util;

public final class CpuUtil {

    public static short shortFromBytes(byte b1, byte b2) {
        return (short) (b1 << 8 | b2 & 0xFF);
    }

    public static byte[] nibblesFromShort(short s) {
        byte[] nibbles = new byte[4];
        nibbles[3] = (byte) (s & 0x000F);
        nibbles[2] = (byte) (s >> 4 & 0x000F);
        nibbles[1] = (byte) (s >> 8 & 0x000F);
        nibbles[0] = (byte) (s >> 12 & 0x000F);
        return nibbles;
    }

    public static byte byteFromNibbles(byte n1, byte n2) {
        return (byte) (n1 << 4 | n2);
    }

    public static short addressFromNibbles(byte n1, byte n2, byte n3) {
        return (short) (n1 << 8 | n2 << 4 | n3);
    }
}
