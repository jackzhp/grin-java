package zede.util;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class Hex {

    public static byte[] toByteArray(String hex) {
        int len = hex.length();
        if (len % 2 > 0) {
            throw new NumberFormatException("Hexadecimal input string must have an even length.");
        }
        byte[] r = new byte[len / 2];
        final char[] chars = hex.toUpperCase().toCharArray();
        for (int i = 0; i < chars.length; i += 2) {
            char ch = chars[i], cl = chars[i + 1];
            int bh = ch < 'A' ? (ch - '0') : (ch - '7'), bl = cl < 'A' ? (cl - '0') : (cl - '7'); //if -'A', then we will have to add 10
            r[i / 2] = (byte) (bh * 16 + bl);
        }
        return r;
    }

    public static int toByteArray(String hex, byte[] r, int offset) {
        int len = hex.length();
        if (len % 2 > 0) {
            throw new NumberFormatException("Hexadecimal input string must have an even length.");
        }
        len >>>= 1;
        if (offset + len > r.length) {
            throw new IllegalStateException();
        }
        final char[] chars = hex.toUpperCase().toCharArray();
        for (int i = 0; i < chars.length; i += 2) {
            char ch = chars[i], cl = chars[i + 1];
            int bh = ch < 'A' ? (ch - '0') : (ch - '7'), bl = cl < 'A' ? (cl - '0') : (cl - '7'); //if -'A', then we will have to add 10
            r[offset + i / 2] = (byte) (bh * 16 + bl);
        }
        return len;
    }

    public static char[] acHex = "0123456789ABCDEF".toCharArray();

    public static StringBuilder appendTo(StringBuilder sb, byte b) {
        int b16 = b >>> 4;
        b16 &= 0x0F;
        sb.append(acHex[b16]);
        b16 = b & 0x0F;
        sb.append(acHex[b16]);
        return sb;
    }

    public static StringBuilder appendTo(StringBuilder sb, byte[] ab, int offset, int length) {
        int limit = offset + length;
        for (; offset < limit; offset++) {
            appendTo(sb, ab[offset]);
        }
        return sb;
    }

    public static String toHexString(byte[] ab) {
        if (ab == null) {
            return "null";
        }
        return toHexString(ab, 0, ab.length);
    }

    public static String toHexString(byte[] ab, int istart, int iend) {
        if (iend > ab.length) {
            throw new IllegalArgumentException("array length " + ab.length + "<" + iend);
        }
        int len = iend - istart;
        StringBuilder sb = new StringBuilder(len * 2);
        toHexString(ab, istart, iend, sb);
        return sb.toString();
    }

    public static void toHexString(byte[] ab, int istart, int iend, StringBuilder sb) {
        if (ab.length < iend) {
            throw new java.lang.IllegalArgumentException(istart + " -> " + iend + " > " + ab.length);
        }
        for (int i = istart; i < iend; i++) {
            appendTo(sb, ab[i]);
        }
    }

    public static String toHexString(ByteBuffer bb) {
        int len = bb.remaining();
        StringBuilder sb = new StringBuilder(len << 1); //*2
        for (int i = len; i > 0; i--) {
            appendTo(sb, bb.get());
        }
        return sb.toString();
    }

    public static String toHexString(ShortBuffer bb) {
        int len = bb.remaining();
        StringBuilder sb = new StringBuilder(len << 2); //*4
        for (int i = len; i > 0; i--) {
            short s = bb.get();
            appendTo(sb, (byte) (s >>> 8));
            appendTo(sb, (byte) s);
        }
        return sb.toString();
    }
}
