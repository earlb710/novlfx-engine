package com.eb.javafx.util;

import static com.eb.javafx.util.UtilString.lpad;

public final class UtilConvert {

    private UtilConvert() { // static class
    }

    public static String bytesToRadix(byte[] b, int pRadix) {
        String baseDigits = base256Digits(pRadix);
        StringBuilder s = new StringBuilder();
        String digit;

        for (int i = 0; i < b.length; i++) {
            digit = Integer.toString(((int) b[i]) & 0xff, pRadix);
            s.append(digit);
        }
        if (baseDigits.length() > s.length()) {
            return baseDigits.substring(0, baseDigits.length() - s.length()) + s.toString();
        } else {
            return s.toString();
        }
    }

    public static String intToRadix(int val, int pRadix) {
        return Integer.toString(val, pRadix).toUpperCase();
    }
    
    public static double posIntPow(final double pVal, final int pPow) {
        double ret = 1;
        double v1, v2;
        int n = pPow;
        v1 = pVal;
        if ((n & 1) == 1) {
            ret = pVal;
        }
        n = n >>> 1;
        while (n > 0) {
            v2 = v1 * v1;
            if ((n & 1) == 1) {
                ret = ret * v2;
            }
            v1 = v2;
            n = n >>> 1;
        }
        return ret;
    }

    public static double intPow(final double pVal, final int pPow) {
        if (pPow >= 0) {
            return posIntPow(pVal, pPow);
        } else {
            return 1 / posIntPow(pVal, -pPow);
        }
    }

    public static String intToRadix(int val, int pRadix, int pDigits) {
        int range = (int) intPow(pRadix, pDigits) - 1;
        val = val & range;
        String ret = Integer.toString(val, pRadix).toUpperCase();
        if (ret.length() < pDigits) {
            ret = lpad(ret, pDigits, '0');
        }
        return ret;
    }

    public static String bytesToRadixFormatted(byte[] b, int pRadix, int pDigits, String pDiv, String pPrefix) {
        String baseDigits = base256Digits(pRadix);
        StringBuilder s = new StringBuilder(pPrefix);
        pPrefix = pDiv + pPrefix;
        String currDigit;
        for (int i = 0; i < b.length; i++) {
            if (i > 0 && pDigits > 0 && i % pDigits == 0) {
                s.append(pPrefix);
            }
            currDigit = Integer.toString((b[i] & 0xff), pRadix);
            s.append(baseDigits.substring(currDigit.length())).append(currDigit);
        }
        return s.toString();
    }

    public static String bytesToHex(byte[] b) {
        return bytesToRadix(b, 16);
    }

    public static String charToHex(char c) {
        return bytesToRadix(new byte[]{0, (byte) c}, 16);
    }

    public static char hexToChar(String s) {
        byte[] b = hexToBytes(s);

        return (char) ((short) b[1] & 0x00ff);
    }

    public static String bytesToHexFormatted(byte[] b, int pDigits) {
        //StringBuilder s = new StringBuilder("0x");
        return bytesToRadixFormatted(b, 16, pDigits, " ", "0x");
    }

    public static byte[] hexToBytes(String s) {
        byte[] b = new byte[(s.length() + 1) / 2];
        for (int i = 0; i < s.length(); i += 2) {
            b[i / 2] = (byte) ((UtilConvert.getHexToInt(s.charAt(i)) << 4) + UtilConvert.getHexToInt(s.charAt(i + 1)));
        }
        return b;
    }

    public static long bytesToLong(byte[] pVal) {
        return ((pVal[0] & 255L) << 56) | ((pVal[1] & 255L) << 48) | ((pVal[2] & 255L) << 40) | ((pVal[3] & 255L) << 32) | ((pVal[4] & 255L) << 24) | ((pVal[5] & 255L) << 16) | ((pVal[6] & 255L) << 8) | (pVal[7] & 255L);
    }

    public static byte[] longToBytes(long pVal) {
        byte[] b = new byte[8];
        b[0] = (byte) (pVal >>> 56);
        b[1] = (byte) (pVal >>> 48);
        b[2] = (byte) (pVal >>> 40);
        b[3] = (byte) (pVal >>> 32);
        b[4] = (byte) (pVal >>> 24);
        b[5] = (byte) (pVal >>> 16);
        b[6] = (byte) (pVal >>> 8);
        b[7] = (byte) (pVal);
        return b;
    }

    public static int bytesToInt(byte pVal0, byte pVal1, byte pVal2, byte pVal3) {
        return ((pVal0 & 255) << 24) | ((pVal1 & 255) << 16) | ((pVal2 & 255) << 8) | (pVal3 & 255);
    }

    public static int bytesToInt(byte[] pVal) {
        return ((pVal[0] & 255) << 24) | ((pVal[1] & 255) << 16) | ((pVal[2] & 255) << 8) | (pVal[3] & 255);
    }

    public static byte[] intToBytes(int pVal) {
        byte[] b = new byte[4];
        b[0] = (byte) (pVal >>> 24);
        b[1] = (byte) (pVal >>> 16);
        b[2] = (byte) (pVal >>> 8);
        b[3] = (byte) (pVal);
        return b;
    }

    public static boolean[] byteToBits(byte b) {
        boolean[] bits = new boolean[8];
        for (int i = 0; i < 8; i++) {
            bits[7 - i] = ((b & (1 << i)) != 0);
        }
        return bits;
    }

    public static char[] byteToBitsString(byte b) {
        char[] bits = new char[8];
        for (int i = 0; i < 8; i++) {
            bits[7 - i] = (char) (((b & (1 << i)) == 0) ? 48 : 49);
        }
        return bits;
    }

    public static char[] intToBitsString(int b) {
        char[] bits = new char[32];
        for (int i = 0; i < 32; i++) {
            bits[31 - i] = (char) (((b & (1 << i)) == 0) ? 48 : 49);
        }
        return bits;
    }

    public static char[] longToBitsString(long b) {
        char[] bits = new char[64];
        for (int i = 0; i < 64; i++) {
            bits[63 - i] = (char) (((b & (1 << i)) == 0) ? 48 : 49);
        }
        return bits;
    }

    public static char[] longToBitsString(long[] b) {
        if (b != null && b.length > 0) {
            char[] bits = new char[64 * b.length];
            for (int idx = 0; idx < b.length; idx++) {
                for (int i = 0; i < 64; i++) {
                    bits[idx * 64 + 63 - i] = (char) (((b[idx] & (1 << i)) == 0) ? 48 : 49);
                }
            }
            return bits;
        } else {
            return new char[0];
        }
    }

    public static byte[] bitsTo8Bytes(boolean[] bits) {
        byte[] b = new byte[8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                b[i] += (((bits[(8 * i) + j]) ? 1 : 0) << (7 - j));
            }
        }
        return b;
    }

    private static int base256DigitCount(int pBase) {
        int[] digits = new int[]{0, 8, 6, 5, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2};
        if (pBase >= 16) {
            return 2;
        } else if (pBase > 0) {
            return digits[pBase - 1];
        }
        return -1;
    }

    private static String base256Digits(int pBase) {
        String[] digits = new String[]{"", "00000000", "000000", "00000", "0000", "0000", "0000", "000", "000", "000", "000", "000", "000", "000", "000", "000", "00"};
        if (pBase >= 16) {
            return "00";
        } else if (pBase > 0) {
            return digits[pBase - 1];
        }
        return "";
    }

    public static byte[] getHexToBytes(String pValue) {
        if (pValue != null) {
            char[] ca = pValue.toCharArray();
            byte[] ret = new byte[(ca.length >> 1)];
            for (int idx = 0; idx < ca.length - 1; idx = idx + 2) {
                int b1 = getHexToByte(ca[idx]);
                b1 = b1 << 4;
                if (idx < ca.length - 1) {
                    b1 = b1 | getHexToByte(ca[idx + 1]);
                }
                ret[idx >> 1] = (byte) b1;
            }
            return ret;
        } else {
            return null;
        }
    }

    public static byte[] getHexToBytes(String pValue, int length, int offset) {
        if (pValue != null) {
            char[] ca = pValue.toCharArray();
            byte[] ret = new byte[length];
            for (int idx = 0; idx < ca.length - 1; idx = idx + 2) {
                int b1 = getHexToByte(ca[idx]);
                b1 = b1 << 4;
                if (idx < ca.length - 1) {
                    b1 = b1 | getHexToByte(ca[idx + 1]);
                }
                ret[offset + idx >> 1] = (byte) b1;
            }
            return ret;
        } else {
            return null;
        }
    }

    public static byte getHexToByte(char pValue) {
        if (pValue >= '0' && pValue <= '9') {
            return (byte) (pValue - 48);
        }
        pValue = (char) (pValue & -33);
        if (pValue >= 'A' && pValue <= 'F') {
            return (byte) (pValue - 55);
        }
        return 0;
    }

    public static byte getRawHexToByte(byte pValue) {
        if (pValue >= '0' && pValue <= '9') {
            return (byte) (pValue - 48);
        }
        pValue = (byte) (pValue & -33);
        if (pValue >= 'A' && pValue <= 'F') {
            return (byte) (pValue - 55);
        }
        return 0;
    }

    public static int getHexToInt(char pValue) {
        if (pValue >= '0' && pValue <= '9') {
            return pValue - 48;
        }
        pValue = (char) (pValue & -33);
        if (pValue >= 'A' && pValue <= 'F') {
            return pValue - 55;
        }
        return 0;
    }

    public static int getRawHexToInt(byte pValue) {
        if (pValue >= '0' && pValue <= '9') {
            return pValue - 48;
        }
        pValue = (byte) (pValue & -33);
        if (pValue >= 'A' && pValue <= 'F') {
            return pValue - 55;
        }
        return 0;
    }

    public static byte[] getRawHexToBytes(byte[] pArray) {
        return getRawHexToBytes(pArray, 0, pArray.length - 1);
    }

    public static byte[] getRawHexToBytes(byte[] pArray, int pStart, int pEnd) {
        byte[] ret = new byte[pArray.length / 2 + pArray.length % 2];
        for (int idx = 0; idx <= pEnd - pStart; idx++) {
            ret[idx] = getRawHexToByte(pArray[pStart + idx]);
        }
        return ret;
    }

    public static int getRawHexToInt(byte[] pArray) {
        return getRawHexToInt(pArray, 0, pArray.length - 1);
    }

    public static int getRawHexToInt(byte[] pArray, int pStart, int pEnd) {

        int ret = 0;
        for (int idx = 0; idx <= pEnd - pStart; idx++) {
            ret = ret << 4;
            ret = ret | getRawHexToByte(pArray[pStart + idx]);
        }
        return ret;
    }

    public static int getHexToInt(char[] pArray) {
        return UtilConvert.getHexToInt(pArray, 0, pArray.length - 1);
    }

    public static int getHexToInt(char[] pArray, int pStart, int pEnd) {

        int ret = 0;
        for (int idx = 0; idx <= pEnd - pStart; idx++) {
            ret = ret << 4;
            ret = ret | getHexToByte(pArray[pStart + idx]);
        }
        return ret;
    }

    public static long getRawHexToLong(byte[] pArray) {
        return getRawHexToInt(pArray, 0, pArray.length - 1);
    }

    public static long getRawHexToLong(byte[] pArray, int pStart, int pEnd) {

        long ret = 0;
        for (int idx = 0; idx <= pEnd - pStart; idx++) {
            ret = ret << 4;
            ret = ret | getRawHexToByte(pArray[pStart + idx]);
        }
        return ret;
    }

    public static long getHexToLong(char[] pArray) {
        return UtilConvert.getHexToInt(pArray, 0, pArray.length - 1);
    }

    public static long getHexToLong(char[] pArray, int pStart, int pEnd) {

        long ret = 0;
        for (int idx = 0; idx <= pEnd - pStart; idx++) {
            ret = ret << 4;
            ret = ret | getHexToByte(pArray[pStart + idx]);
        }
        return ret;
    }

    public static byte[] avgArray(byte[] b1, byte[] b2) {
        byte[] ret = new byte[b1.length];
        for (int idx = 0; idx < b1.length; idx++) {
            ret[idx] = (byte) (((b1[idx] & 0xFF) + (b2[idx] & 0xFF)) / 2);
        }
        return ret;
    }

}
