package com.eb.javafx.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static com.eb.javafx.util.UtilConvert.charToHex;
import static com.eb.javafx.util.UtilConvert.hexToChar;

/**
 * Generic string utility methods Note : mostly static utility methods
 *
 * @author Earl Bosch
 *
 */
public class UtilString extends UtilUnicode {

    protected UtilString() {
    }

    public static final char[] EMPTY_CHAR_ARRAY = new char[0];

    public static final String QUOTE_COMMA_SPLIT
            = "(?x)" //           # enable comments                                    
            + "(\"[^\"]*\")" //   # quoted data, and store in group #1                 
            + "|" //              # OR                                                   
            + "([^,]+)" //        # one or more chars other than ',', and store it in #2 
            + "|" //              # OR                                                  
            + "\\s*,\\s*";//      # a ',' optionally surrounded by space-chars           

    public static final String COMMA_SPLIT
            = "([^,]+)" //        # one or more chars other than ',', and store it in #2 
            + "|" //              # OR                                                  
            + "\\s*,\\s*";//      # a ',' optionally surrounded by space-chars           

    public static final short MASK_UPPERCASE = -33;
    public static final short MASK_LOWERCASE = 32;
    public static final short MASK_WHITESPACES_CHAR = 1;
    public static final short MASK_SPECIAL_CHAR = 2;
    public static final short MASK_ALPHA_CHAR = 4;
    public static final short MASK_NUMERIC_CHAR = 8;
    public static final short MASK_NAME_CHAR = 16;
    public static final short MASK_DIV_CHAR = 32;
    public static final short MASK_OPERAND_CHAR = 64;
    public static final short MASK_ALPHA_UPPER_CHAR = 128;
    public static final short MASK_CONST_CHAR = 256;
    public static final short MASK_NUMERIC_COMMA_CHAR = 512;
    public static final short MASK_COMPLEX_NAME_CHAR = 1024;
    public static final short MASK_BRACKET_CHAR = 2048;

    private static String div = ",";

    private static final short[] CHAR_mapping = new short[128];

    private static final char[] _WHITESPACES_CHAR = new char[]{
        0x9, 0xA, 0xB, 0xC, 0xD, 0x1C, 0x1D, 0x1E, 0x1F, 0x20
    };
    public static final String WHITESPACES_CHAR = new String(_WHITESPACES_CHAR);
    public static final String[] WHITESPACES_STRING_ARRAY = new String[_WHITESPACES_CHAR.length];

    private static final char[] _SPECIAL_CHAR = new char[]{
        '(', ')', '[', ']', '{', '}', '*', '+', '-', '/', '\\', '=', '<', '>', ',', '@', '#', '$', '%', '&', '|', ':', ';', '?', '!', '`', '~', '\'', '"'
    };
    public static final String SPECIAL_CHAR = new String(_SPECIAL_CHAR);
    private static final char[] _BRACKET_CHAR = new char[]{
        '(', ')', '[', ']', '{', '}'};
    public static final String BRACKET_CHAR = new String(_BRACKET_CHAR);

    private static final char[] _DIV_CHAR = new char[]{
        ' ', '(', ')', '[', ']', '{', '}', '\n', '\r', '\t', '"', '\\', ',', '.', ':', ';', '?', '!'
    };
    public static final String DIV_CHAR = new String(_DIV_CHAR);

    private static final char[] _OPERAND_CHAR = new char[]{
        '+', '-', '/', '*', '=', '>', '<', '!', '&', '|'
    };
    public static final String OPERAND_CHAR = new String(_OPERAND_CHAR);

    private static final char[] _CONST_CHAR = new char[]{
        'B', 'C', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Z',
        'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'z'
    };
    public static final String CONST_CHAR = new String(_CONST_CHAR);

    private static final char[] _NAME_CHAR = new char[65];
    public static final String NAME_CHAR;

    private static final char[] _COMPLEX_NAME_CHAR = new char[73];
    public static final String COMPLEX_NAME_CHAR;

    private static final char[] _ALPHA_CHAR = new char[52];
    public static final String ALPHA_CHAR;

    private static final char[] _ALPHA_UPPER_CHAR = new char[26];
    public static final String ALPHA_UPPER_CHAR;

    private static final char[] _NUMERIC_CHAR = new char[12];
    public static final String NUMERIC_CHAR;

    private static final char[] _NUMERIC_COMMA_CHAR = new char[14];
    public static final String NUMERIC_COMMA_CHAR;

    static {
        for (int i = 0; i < _WHITESPACES_CHAR.length; i++) {
            int c = _WHITESPACES_CHAR[i];
            CHAR_mapping[c] = (short) (CHAR_mapping[c] | MASK_WHITESPACES_CHAR);
            WHITESPACES_STRING_ARRAY[i] = String.valueOf((char) c);
        }
        for (int c : _SPECIAL_CHAR) {
            CHAR_mapping[c] = (short) (CHAR_mapping[c] | MASK_SPECIAL_CHAR);
        }
        for (int c : _BRACKET_CHAR) {
            CHAR_mapping[c] = (short) (CHAR_mapping[c] | MASK_BRACKET_CHAR);
        }
        for (int c = 'A'; c <= 'Z'; c++) {
            _ALPHA_UPPER_CHAR[c - 'A'] = (char) c;
            _ALPHA_CHAR[c - 'A'] = (char) c;
            _NAME_CHAR[c - 'A'] = (char) c;
            _COMPLEX_NAME_CHAR[c - 'A'] = (char) c;
            CHAR_mapping[c] = (short) (CHAR_mapping[c] | MASK_ALPHA_CHAR | MASK_NAME_CHAR | MASK_ALPHA_UPPER_CHAR | MASK_COMPLEX_NAME_CHAR);
        }
        for (int c = 'a'; c <= 'z'; c++) {
            _ALPHA_CHAR[c - 'a' + 26] = (char) c;
            _NAME_CHAR[c - 'a' + 26] = (char) c;
            _COMPLEX_NAME_CHAR[c - 'a' + 26] = (char) c;
            CHAR_mapping[c] = (short) (CHAR_mapping[c] | MASK_ALPHA_CHAR | MASK_NAME_CHAR | MASK_COMPLEX_NAME_CHAR);
        }
        for (int c = '0'; c <= '9'; c++) {
            _NAME_CHAR[c - '0' + 52] = (char) c;
            _COMPLEX_NAME_CHAR[c - '0' + 52] = (char) c;
            _NUMERIC_CHAR[c - '0'] = (char) c;
            _NUMERIC_COMMA_CHAR[c - '0'] = (char) c;
            CHAR_mapping[c] = (short) (CHAR_mapping[c] | MASK_NUMERIC_CHAR | MASK_NAME_CHAR | MASK_NUMERIC_COMMA_CHAR | MASK_COMPLEX_NAME_CHAR);
        }
        _NUMERIC_CHAR[10] = '+';
        _NUMERIC_CHAR[11] = '-';
        _NUMERIC_COMMA_CHAR[10] = '+';
        _NUMERIC_COMMA_CHAR[11] = '-';
        _NUMERIC_COMMA_CHAR[12] = '.';
        _NUMERIC_COMMA_CHAR[13] = ',';

        CHAR_mapping[(int) '+'] = (short) (CHAR_mapping[(int) '+'] | MASK_NUMERIC_CHAR);
        CHAR_mapping[(int) '-'] = (short) (CHAR_mapping[(int) '-'] | MASK_NUMERIC_CHAR);
        CHAR_mapping[(int) '.'] = (short) (CHAR_mapping[(int) '.'] | MASK_NUMERIC_COMMA_CHAR);
        CHAR_mapping[(int) ','] = (short) (CHAR_mapping[(int) ','] | MASK_NUMERIC_COMMA_CHAR);
        CHAR_mapping[(int) '+'] = (short) (CHAR_mapping[(int) '+'] | MASK_NUMERIC_COMMA_CHAR);
        CHAR_mapping[(int) '-'] = (short) (CHAR_mapping[(int) '-'] | MASK_NUMERIC_COMMA_CHAR);

        _NAME_CHAR[62] = '_';
        _NAME_CHAR[63] = '$';
        _NAME_CHAR[64] = '.';
        CHAR_mapping[(int) '_'] = (short) (CHAR_mapping[(int) '_'] | MASK_NAME_CHAR);
        CHAR_mapping[(int) '$'] = (short) (CHAR_mapping[(int) '$'] | MASK_NAME_CHAR);
        CHAR_mapping[(int) '.'] = (short) (CHAR_mapping[(int) '.'] | MASK_NAME_CHAR);

        _COMPLEX_NAME_CHAR[62] = '!';
        _COMPLEX_NAME_CHAR[63] = '@';
        _COMPLEX_NAME_CHAR[64] = '#';
        _COMPLEX_NAME_CHAR[65] = '$';
        _COMPLEX_NAME_CHAR[66] = '%';
        _COMPLEX_NAME_CHAR[67] = '^';
        _COMPLEX_NAME_CHAR[68] = '&';
        _COMPLEX_NAME_CHAR[69] = '_';
        _COMPLEX_NAME_CHAR[70] = '.';
        _COMPLEX_NAME_CHAR[71] = ':';
        _COMPLEX_NAME_CHAR[72] = '-';
        CHAR_mapping[(int) '!'] = (short) (CHAR_mapping[(int) '!'] | MASK_COMPLEX_NAME_CHAR);
        CHAR_mapping[(int) '@'] = (short) (CHAR_mapping[(int) '@'] | MASK_COMPLEX_NAME_CHAR);
        CHAR_mapping[(int) '#'] = (short) (CHAR_mapping[(int) '#'] | MASK_COMPLEX_NAME_CHAR);
        CHAR_mapping[(int) '$'] = (short) (CHAR_mapping[(int) '$'] | MASK_COMPLEX_NAME_CHAR);
        CHAR_mapping[(int) '%'] = (short) (CHAR_mapping[(int) '%'] | MASK_COMPLEX_NAME_CHAR);
        CHAR_mapping[(int) '^'] = (short) (CHAR_mapping[(int) '^'] | MASK_COMPLEX_NAME_CHAR);
        CHAR_mapping[(int) '&'] = (short) (CHAR_mapping[(int) '&'] | MASK_COMPLEX_NAME_CHAR);
        CHAR_mapping[(int) '_'] = (short) (CHAR_mapping[(int) '_'] | MASK_COMPLEX_NAME_CHAR);
        CHAR_mapping[(int) '.'] = (short) (CHAR_mapping[(int) '.'] | MASK_COMPLEX_NAME_CHAR);
        CHAR_mapping[(int) ':'] = (short) (CHAR_mapping[(int) ':'] | MASK_COMPLEX_NAME_CHAR);
        CHAR_mapping[(int) '-'] = (short) (CHAR_mapping[(int) '-'] | MASK_COMPLEX_NAME_CHAR);

        for (int c : _DIV_CHAR) {
            CHAR_mapping[c] = (short) (CHAR_mapping[c] | MASK_DIV_CHAR);
        }
        for (int c : _OPERAND_CHAR) {
            CHAR_mapping[c] = (short) (CHAR_mapping[c] | MASK_OPERAND_CHAR);
        }

        for (int c : _CONST_CHAR) {
            CHAR_mapping[c] = (short) (CHAR_mapping[c] | MASK_CONST_CHAR);
        }

        NAME_CHAR = new String(_NAME_CHAR);

        ALPHA_CHAR = new String(_ALPHA_CHAR);

        ALPHA_UPPER_CHAR = new String(_ALPHA_UPPER_CHAR);

        NUMERIC_CHAR = new String(_NUMERIC_CHAR);

        NUMERIC_COMMA_CHAR = NUMERIC_CHAR + new String(_NUMERIC_COMMA_CHAR);
        COMPLEX_NAME_CHAR = new String(_COMPLEX_NAME_CHAR);
    }

    public static UUID generateUUID(String... pName) {
        StringBuilder str = new StringBuilder();
        for (String n : pName) {
            str = str.append(n).append(';');
        }
        return UUID.nameUUIDFromBytes(str.toString().getBytes());
    }

    public static boolean isChar(ECharMappings pCharType, char pChar) {
        return switch (pCharType) {
            case WHITESPACES_CHAR ->(CHAR_mapping[pChar] & MASK_WHITESPACES_CHAR) == MASK_WHITESPACES_CHAR;
            case SPECIAL_CHAR -> (CHAR_mapping[pChar] & MASK_SPECIAL_CHAR) == MASK_SPECIAL_CHAR;
            case ALPHA_CHAR -> (CHAR_mapping[pChar] & MASK_ALPHA_CHAR) == MASK_ALPHA_CHAR;
            case NUMERIC_CHAR -> (CHAR_mapping[pChar] & MASK_NUMERIC_CHAR) == MASK_NUMERIC_CHAR;
            case NUMERIC_COMMA_CHAR ->(CHAR_mapping[pChar] & (MASK_NUMERIC_CHAR + MASK_NUMERIC_COMMA_CHAR)) > 0;
            case NAME_CHAR -> (CHAR_mapping[pChar] & MASK_NAME_CHAR) == MASK_NAME_CHAR;
            case COMPLEX_NAME_CHAR ->(CHAR_mapping[pChar] & MASK_COMPLEX_NAME_CHAR) == MASK_COMPLEX_NAME_CHAR;
            case DIV_CHAR -> (CHAR_mapping[pChar] & MASK_DIV_CHAR) == MASK_DIV_CHAR;
            case OPERAND_CHAR -> (CHAR_mapping[pChar] & MASK_OPERAND_CHAR) == MASK_OPERAND_CHAR;
            case ALPHA_UPPER_CHAR ->(CHAR_mapping[pChar] & MASK_ALPHA_UPPER_CHAR) == MASK_ALPHA_UPPER_CHAR;
            case CONST_CHAR -> (CHAR_mapping[pChar] & MASK_CONST_CHAR) == MASK_CONST_CHAR;
            case CONST_UPPER_CHAR ->(CHAR_mapping[pChar] & (MASK_CONST_CHAR + MASK_ALPHA_UPPER_CHAR)) == MASK_CONST_CHAR + MASK_ALPHA_UPPER_CHAR;
        };
    }

    public static char[] getCharArray(ECharMappings pCharType) {
        return switch (pCharType) {
            case WHITESPACES_CHAR -> WHITESPACES_CHAR.toCharArray();
            case SPECIAL_CHAR -> SPECIAL_CHAR.toCharArray();
            case ALPHA_CHAR -> ALPHA_CHAR.toCharArray();
            case NUMERIC_CHAR -> NUMERIC_CHAR.toCharArray();
            case NAME_CHAR -> NAME_CHAR.toCharArray();
            case COMPLEX_NAME_CHAR -> COMPLEX_NAME_CHAR.toCharArray();
            case DIV_CHAR -> DIV_CHAR.toCharArray();
            case OPERAND_CHAR -> OPERAND_CHAR.toCharArray();
            case ALPHA_UPPER_CHAR -> ALPHA_UPPER_CHAR.toCharArray();
            case CONST_CHAR -> CONST_CHAR.toCharArray();
            default -> new char[0];
        };
    }

    public static boolean checkChar(char pChar, char[] pTestChar) {
        for (char tc : pTestChar) {
            if (tc == pChar) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWhitespaceChar(char pChar) {
        if (pChar <= 127) {
            return (CHAR_mapping[pChar] & MASK_WHITESPACES_CHAR) == MASK_WHITESPACES_CHAR;
        } else {
            return false;
        }
    }

    public static boolean containsWhitespaceChar(String pString) {
        return containsWhitespaceChar(pString.toCharArray(), 0, pString.length() - 1);
    }

    public static boolean containsWhitespaceChar(char[] pChar) {
        return containsWhitespaceChar(pChar, 0, pChar.length - 1);
    }

    public static boolean containsWhitespaceChar(char[] pChar, int pStart, int pEnd) {
        if (pChar != null && pChar.length > 0) {
            for (int idx = pStart; idx <= pEnd; idx++) {
                if (pChar[idx] <= 127) {
                    if ((CHAR_mapping[pChar[idx]] & MASK_WHITESPACES_CHAR) == MASK_WHITESPACES_CHAR) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSpecialChar(char pChar) {
        if (pChar <= 127) {
            return (CHAR_mapping[pChar] & MASK_SPECIAL_CHAR) == MASK_SPECIAL_CHAR;
        } else {
            return true;
        }
    }

    public static boolean containsOnlyNumericChar(String pString) {
        return containsOnlyNumericChar(pString.toCharArray(), 0, pString.length() - 1);
    }

    public static boolean containsOnlyNumericChar(char[] pChar) {
        return containsOnlyNumericChar(pChar, 0, pChar.length - 1);
    }

    public static boolean containsOnlyNumericChar(char[] pString, int pStart, int pEnd) {
        Character vWrong = null;
        boolean vFound = false;
        for (int idx2 = pStart; idx2 <= pEnd; idx2++) {
            vFound = (CHAR_mapping[pString[idx2]] & MASK_NUMERIC_COMMA_CHAR) == MASK_NUMERIC_COMMA_CHAR;
            if (!vFound) {
                vWrong = pString[idx2];
                break;
            }
        }
        return vWrong == null;
    }

    public static boolean containsSpecialChar(String pString) {
        return containsSpecialChar(pString.toCharArray(), 0, pString.length() - 1);
    }

    public static boolean containsSpecialChar(char[] pChar) {
        return containsSpecialChar(pChar, 0, pChar.length - 1);
    }

    public static boolean containsSpecialChar(char[] pChar, int pStart, int pEnd) {
        if (pChar != null && pChar.length > 0) {
            for (int idx = pStart; idx <= pEnd; idx++) {
                int i = pChar[idx];
                if (i <= 127) {
                    if ((CHAR_mapping[i] & MASK_SPECIAL_CHAR) == MASK_SPECIAL_CHAR) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isBracketChar(char pChar) {
        if (pChar <= 127) {
            boolean ret = (CHAR_mapping[pChar] & MASK_BRACKET_CHAR) == MASK_BRACKET_CHAR;
            return ret;
        } else {
            return true;
        }
    }

    public static boolean scanForCharInNumeric(char[] pData, int pStart, char pChar) {
        for (int idx = pStart; idx < pData.length; idx++) {
            char c = pData[idx];
            if (c == pChar) {
                return true;
            }
            if (isWhitespaceChar(c) || isNumericCommaChar(c)) {
                continue;
            }
            return false;
        }
        return false;
    }

    public static boolean isAlphaChar(char pChar) {
        if (pChar <= 127) {
            return (CHAR_mapping[pChar] & MASK_ALPHA_CHAR) == MASK_ALPHA_CHAR;
        } else {
            return false;
        }
    }

    public static boolean isNumericChar(char pChar) {
        if (pChar <= 127) {
            return (CHAR_mapping[pChar] & MASK_NUMERIC_CHAR) == MASK_NUMERIC_CHAR;
        } else {
            return false;
        }
    }

    public static boolean isNumericCommaChar(char pChar) {
        if (pChar <= 127) {
            return (CHAR_mapping[pChar] & (MASK_NUMERIC_COMMA_CHAR + MASK_NUMERIC_CHAR)) > 0;
        } else {
            return false;
        }
    }

    public static boolean isNameChar(char pChar) {
        if (pChar <= 127) {
            return (CHAR_mapping[pChar] & MASK_NAME_CHAR) == MASK_NAME_CHAR;
        } else {
            return false;
        }
    }

    public static boolean isComplexNameChar(char pChar) {
        if (pChar <= 127) {
            return (CHAR_mapping[pChar] & MASK_COMPLEX_NAME_CHAR) == MASK_COMPLEX_NAME_CHAR;
        } else {
            return false;
        }
    }

    public static boolean isDivChar(char pChar) {
        if (pChar <= 127) {
            return (CHAR_mapping[pChar] & MASK_DIV_CHAR) == MASK_DIV_CHAR;
        } else {
            return false;
        }
    }

    public static boolean isOperandChar(char pChar) {
        if (pChar <= 127) {
            return (CHAR_mapping[pChar] & MASK_OPERAND_CHAR) == MASK_OPERAND_CHAR;
        } else {
            return false;
        }
    }

    public static boolean isName(String pString) {
        return isName(pString.toCharArray(), 0, pString.length() - 1);
    }

    public static boolean isName(char[] pChar) {
        return isName(pChar, 0, pChar.length - 1);
    }

    public static boolean isName(char[] pChar, int pStart, int pEnd) {
        if (pChar != null && pChar.length > 0 && isAlphaChar(pChar[pStart])) {
            for (int idx = pStart + 1; idx <= pEnd; idx++) {
                if (pChar[idx] <= 127) {
                    if ((CHAR_mapping[pChar[idx]] & MASK_NAME_CHAR) == 0) {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /*
     * Sets the default token divider, can be more than one charater
     */
    public static void setTokenDivider(String pDiv) {
        div = pDiv;
    }

    /*
     * Get the token divider, can be more than one charater
     */
    public static String getTokenDivider() {
        return div;
    }

    /*
     * Add string token to a string using set divider. If the string is empty ony the token is returned.
     *
     * returns String
     */
    public static String addTokenString(String pString, String pToken) {
        if (pString == null || pString.equals("")) {
            return pToken;
        } else {
            return new StringBuilder(pString).append(div).append(pToken).toString();
        }
    }

    /*
     * Add string token to a StringBuilder using set divider. If the string is empty ony the token is returned.
     *
     * returns StringBuilder
     */
    public static StringBuilder addTokenStringBuilder(StringBuilder pString, String pToken) {
        if (pString == null || pString.length() == 0) {
            return new StringBuilder(pToken);
        } else {
            return pString.append(div).append(pToken);
        }
    }

    public static String getLastTokenString(String pString) {
        String lastStr = pString.substring(pString.lastIndexOf(div) + 1);
        return lastStr;
    }

    public static String getLastTokenString(String pString, String pDiv) {
        String lastStr = pString.substring(pString.lastIndexOf(pDiv) + 1);
        return lastStr;
    }

    public static String getFirstTokenString(int pStartToken, String pString, String pDiv) {
        int idx = pString.indexOf(pDiv, pStartToken);
        if (idx >= 0) {
            String lastStr = pString.substring(pStartToken, idx);
            return lastStr;
        } else {
            return null;
        }
    }

    public static String getFirstTokenString(int pStartToken, String pString) {
        int idx = pString.indexOf(div, pStartToken);
        if (idx >= 0) {
            String lastStr = pString.substring(pStartToken, idx);
            return lastStr;
        } else {
            return null;
        }
    }

    public static String getFirstTokenString(String pString, String pDiv) {
        int idx = pString.indexOf(pDiv);
        if (idx >= 0) {
            String lastStr = pString.substring(0, idx);
            return lastStr;
        } else {
            return null;
        }
    }

    public static String getFirstTokenString(String pString) {
        int idx = pString.indexOf(div);
        if (idx >= 0) {
            String lastStr = pString.substring(0, idx);
            return lastStr;
        } else {
            return null;
        }
    }

    public static String removeLastTokenString(String pString, String pDiv) {
        String lastStr = pString.substring(0, pString.lastIndexOf(pDiv));
        return lastStr;
    }

    public static String removeLastTokenString(String pString) {
        String lastStr = pString.substring(0, pString.lastIndexOf(div));
        return lastStr;
    }

    public static int findReplaceAll(StringBuilder pText, String pFrom, String pTo) {
        return findReplaceAll(pText, pFrom, pTo, false);
    }

    public static int findReplaceAllIgnoreCase(StringBuilder pText, String pFrom, String pTo) {
        return findReplaceAll(pText, pFrom, pTo, true);
    }

    public static int findReplaceAll(StringBuilder pText, String pFrom, String pTo, boolean pIgnoreCase) {
        int count = 0;
        int vIdxDif = 0;
        int fromLength = pFrom.length();
        int toLength = pTo.length();
        int vItemIdx = Math.min(toLength, fromLength);
        if (!pIgnoreCase) {
            int vIdx = pText.indexOf(pFrom);
            while (vIdx >= 0) {
                pText.replace(vIdx, vIdx + fromLength, pTo);
                vIdx = pText.indexOf(pFrom, vIdx + vItemIdx);
                count++;
            }
        } else {
            //String pTextLower = pText.toString().toLowerCase();
            String pTextLower = pText.toString().toLowerCase();
            String pFromLower = pFrom.toLowerCase(LOCALE);
            int vIdx = pTextLower.indexOf(pFromLower);
            while (vIdx >= 0) {
                pText.replace(vIdx + vIdxDif, vIdx + fromLength + vIdxDif, pTo);
                vIdx = pTextLower.indexOf(pFromLower, vIdx + fromLength);
                vIdxDif = vIdxDif + toLength - fromLength;
                count++;
            }
        }
        return count;
    }

    /**
     * Changes base string by replacing all occurrences of find string with
     * replace string
     *
     * @param pText Base string to manipulate
     * @param pFrom String to find in base string
     * @param pTo String to replace find string in base string
     * @return StringBuilder
     */
    public static String findReplaceAllString(String pText, String pFrom, String pTo) {
        StringBuilder retString = new StringBuilder(pText);
        findReplaceAll(retString, pFrom, pTo, false);
        return retString.toString();
    }

    public static String findReplaceAllString(StringBuilder pText, String pFrom, String pTo) {
        StringBuilder retString = new StringBuilder(pText);
        findReplaceAll(retString, pFrom, pTo, false);
        return retString.toString();
    }

    public static String findReplaceAllStringIgnoreCase(String pText, String pFrom, String pTo) {
        StringBuilder retString = new StringBuilder(pText);
        findReplaceAll(retString, pFrom, pTo, true);
        return retString.toString();
    }

    public static String findReplaceAllStringIgnoreCase(StringBuilder pText, String pFrom, String pTo) {
        StringBuilder retString = new StringBuilder(pText);
        findReplaceAll(retString, pFrom, pTo, true);
        return retString.toString();
    }

    public static String findDeleteAllString(String pText, String... pFind) {
        return findDeleteAllString(new StringBuilder(pText), pFind).toString();
    }

    /**
     * Changes base string by deleting all occurrences of find string
     *
     * @param pText Base string to manipulate
     * @param pFind String to find in base string
     * @return StringBuilder
     */
    public static StringBuilder findDeleteAllString(StringBuilder pText, String... pFind) {
        for (String f : pFind) {
            int vIdx = pText.indexOf(f);
            while (vIdx >= 0) {
                pText.delete(vIdx, vIdx + f.length());
                vIdx = pText.indexOf(f, vIdx);
            }
        }
        return pText;
    }

    public static String findReplaceString(String pText, String pFrom, String pTo) {
        return findReplaceString(new StringBuilder(pText), pFrom, pTo).toString();
    }

    public static StringBuilder findReplaceString(StringBuilder pText, String pFrom, String pTo) {
        int vIdx = pText.indexOf(pFrom);
        if (vIdx >= 0) {
            pText.replace(vIdx, vIdx + pFrom.length(), pTo);
        }
        return pText;
    }

    public static int findStringIndex(String[] pArray, String pFind) {
        int retIdx = -1, idx = 0;
        for (String str : pArray) {
            if (str.equals(pFind)) {
                retIdx = idx;
                break;
            }
            idx++;
        }
        return retIdx;
    }

    public static int findStringIndexIgnoreCase(String[] pArray, String pFind) {
        int retIdx = -1, idx = 0;
        for (String str : pArray) {
            if (str.equalsIgnoreCase(pFind)) {
                retIdx = idx;
                break;
            }
            idx++;
        }
        return retIdx;
    }

    /**
     * For Arguments that uses 'arg=xyz'
     *
     * @param pArgs
     * @param pFind
     * @return
     */
    public static String getArgEquals(String[] pArgs, String pFind) {
        for (String idxArg : pArgs) {
            if (idxArg.toLowerCase(LOCALE).startsWith(pFind.toLowerCase(LOCALE))) {
                return idxArg.substring(idxArg.indexOf('=') + 1);
            }
        }
        return null;
    }

    public static boolean containsAnyString(String[] pString, String[] pContains) {
        boolean vFound = false;

        for (int vIdx = 0; vIdx < pString.length; vIdx++) {
            for (int vIdx2 = 0; vIdx2 < pContains.length; vIdx2++) {
                if (pString[vIdx].equals(pContains[vIdx2])) {
                    vFound = true;
                    break;
                }
            }
            if (vFound) {
                break;
            }
        }
        return vFound;
    }

    public static boolean containsAnyChar(CharSequence pString, CharSequence pContains) {
        boolean vFound = false;

        for (int vIdx = 0; vIdx < pString.length(); vIdx++) {
            for (int vIdx2 = 0; vIdx2 < pContains.length(); vIdx2++) {
                if (pString.charAt(vIdx) == pContains.charAt(vIdx2)) {
                    vFound = true;
                    break;
                }
            }
            if (vFound) {
                break;
            }
        }
        return vFound;
    }

    public static boolean containsAllChar(CharSequence pString, CharSequence pContains) {
        boolean vFound = false;
        boolean vFoundAll = false;

        for (int idx = 0; idx < pContains.length(); idx++) {
            vFound = false;
            for (int v_idx2 = 0; v_idx2 < pString.length(); v_idx2++) {
                if (pString.charAt(v_idx2) == pContains.charAt(idx)) {
                    vFound = true;
                    break;
                }
            }
            if (!vFound) {
                vFoundAll = false;
                break;
            } else {
                vFoundAll = true;
            }
        }
        return vFoundAll;
    }

    public static Character containsOnlyChar(char[] pString, char[] pContains) {
        Character vWrong = null;
        boolean vFound = false;
        for (int idx2 = 0; idx2 < pString.length; idx2++) {
            vFound = false;
            for (int v_idx = 0; v_idx < pContains.length; v_idx++) {
                if (pString[idx2] != pContains[v_idx]) {
                    vFound = true;
                    break;
                }
            }
            if (!vFound) {
                vWrong = pString[idx2];
                break;
            }
        }
        return vWrong;
    }

    public static Character containsOnlyNameChar(char[] pString) {
        Character vWrong = null;
        boolean vFound = false;
        for (int idx2 = 0; idx2 < pString.length; idx2++) {
            vFound = (CHAR_mapping[pString[idx2]] & MASK_NAME_CHAR) == MASK_NAME_CHAR;
            if (!vFound) {
                vWrong = pString[idx2];
                break;
            }
        }
        return vWrong;
    }

    public static Character validateNameString(String pName) {
        if (pName.length() > 0) {
            //char[] carray = pName.toCharArray();
            char[] carray = pName.toCharArray();
            if (carray[0] > '9' || carray[0] == '$') {
                return containsOnlyNameChar(carray);
            } else {
                return carray[0];
            }
        }
        return null;
    }

    public static String[] convertCharArrayToStringArray(char[] pChar) {
        String[] vRet = new String[pChar.length];
        for (int vIdx = 0; vIdx < pChar.length; vIdx++) {
            vRet[vIdx] = new String(new char[]{pChar[vIdx]});
        }
        return vRet;
    }

    public static int findIndexInStringArray(String pParam, String[] pParamList) {
        int vIdx = 0;
        if (pParamList != null && pParamList.length > 0) {
            for (String pl : pParamList) {
                if (pl.equalsIgnoreCase(pParam)) {
                    return vIdx;
                } else {
                    vIdx++;
                }

            }
        }
        return -1;
    }

    public static String quoteString(String pText, String pQuote) {
        return quoteString(new StringBuffer(pText), pQuote);
    }

    public static String quoteString(StringBuffer pText, String pQuote) {
        pText = pText.insert(0, pQuote).append(pQuote);
        return pText.toString();
    }

    public static char[] copyCharArray(char[] pArray, int pStart, int pEnd) {
        char[] charArray = new char[pEnd - pStart + 1];
        for (int idx = pStart; idx <= pEnd; idx++) {
            charArray[idx] = pArray[idx];
        }
        return charArray;
    }

    public static byte[] stringToBytes(String pStr) {
        if (pStr != null) {
            char[] str = pStr.toCharArray();
            return stringToBytes(str);
        } else {
            return null;
        }

    }

    public static byte[] stringToBytes(char[] pChar) {
        byte[] b = new byte[pChar.length << 1];

        for (int i = 0; i < pChar.length; i++) {
            int bpos = i << 1;
            b[bpos] = (byte) ((pChar[i] & 0xFF00) >> 8);
            b[bpos + 1] = (byte) (pChar[i] & 0x00FF);
        }

        return b;
    }

    public static String bytesToString(byte[] pBytes) {

        char[] buffer = new char[pBytes.length >> 1];
        for (int i = 0; i < buffer.length; i++) {
            int bpos = i << 1;
            char c = (char) (((pBytes[bpos] & 0x00FF) << 8) + (pBytes[bpos + 1] & 0x00FF));
            buffer[i] = c;
        }
        return new String(buffer);

    }

    public static String escapeXML(String pTextXml) {
        char[] carray = pTextXml.toCharArray();
        return escapeXML(carray);
    }

    public static String escapeXML(char[] pCharXml) {
        StringBuilder newString = new StringBuilder(pCharXml.length);
        for (int idx = 0; idx < pCharXml.length; idx++) {
            char c = pCharXml[idx];
            if (c == '<') {
                newString.append("&lt;");
            } else if (c == '>') {
                newString.append("&gt;");
            } else if (c == '\'') {
                newString.append("&apos;");
            } else if (c == '"') {
                newString.append("&quot;");
            } else if (c == '&') {
                newString.append("&amp;");
            } else if (c > '\u00A0') {
                newString.append("&#x").append(charToHex(c)).append(";");
            } else {
                newString.append(c);
            }
        }
        return newString.toString();
    }

    public static String unescapeXML(String pTextXml) {
        char[] carray = pTextXml.toCharArray();
        return unescapeXML(carray);
    }

    public static String unescapeXML(char[] pCharXml) {
        StringBuilder newString = new StringBuilder(pCharXml.length);
        for (int idx = 0; idx < pCharXml.length; idx++) {
            if (pCharXml[idx] == '&') {
                if (idx < pCharXml.length - 3) {
                    if (pCharXml[idx + 1] == 'l' && pCharXml[idx + 2] == 't' && pCharXml[idx + 3] == ';') {
                        newString.append("<");
                        idx = idx + 3;
                        continue;
                    } else if (pCharXml[idx + 1] == 'g' && pCharXml[idx + 2] == 't' && pCharXml[idx + 3] == ';') {
                        newString.append(">");
                        idx = idx + 3;
                        continue;
                    }
                }
                if (idx < pCharXml.length - 4) {
                    if (pCharXml[idx + 1] == 'a' && pCharXml[idx + 2] == 'm' && pCharXml[idx + 3] == 'p' && pCharXml[idx + 4] == ';') {
                        newString.append("&");
                        idx = idx + 4;
                        continue;
                    }
                }
                if (idx < pCharXml.length - 5) {
                    if (pCharXml[idx + 1] == 'a' && pCharXml[idx + 2] == 'p' && pCharXml[idx + 3] == 'o' && pCharXml[idx + 4] == 's' && pCharXml[idx + 5] == ';') {
                        newString.append("'");
                        idx = idx + 5;
                        continue;
                    } else if (pCharXml[idx + 1] == 'q' && pCharXml[idx + 2] == 'u' && pCharXml[idx + 3] == 'o' && pCharXml[idx + 4] == 't' && pCharXml[idx + 5] == ';') {
                        newString.append("\"");
                        idx = idx + 5;
                        continue;
                    }
                }
                if (idx < pCharXml.length - 6) {
                    if (pCharXml[idx + 1] == '#' && pCharXml[idx + 6] == ';') {
                        newString.append(String.valueOf((char) ((byte) Byte.valueOf(String.copyValueOf(pCharXml, idx + 2, 4)))));
                        idx = idx + 6;
                        continue;
                    }
                }
                if (idx < pCharXml.length - 7) {
                    if (pCharXml[idx + 1] == '#' && pCharXml[idx + 2] == 'x' && pCharXml[idx + 7] == ';') {
                        newString.append(String.valueOf(hexToChar(String.copyValueOf(pCharXml, idx + 3, 4))));
                        idx = idx + 7;
                        continue;
                    }
                }
            } else {
                newString.append(pCharXml[idx]);
            }
        }
        return newString.toString();
    }

    public static int firstIndexChar(String pString) {
        return firstIndexChar(pString, 0);
    }

    public static int firstIndexChar(String pString, int pIdx) {
        char[] charString = pString.toCharArray();
        return firstIndexChar(charString, pIdx);
    }

    public static int firstIndexChar(char[] pCharString) {
        return firstIndexChar(pCharString, 0);
    }

    public static int firstIndexChar(char[] pCharString, char[] pTrimChar) {
        return firstIndexChar(pCharString, pTrimChar, 0);
    }

    public static int firstIndexChar(char[] pCharString, int pIdx) {
        int findIdx = pIdx;
        do {
            if (UtilString.isWhitespaceChar(pCharString[findIdx])) {
                findIdx++;
            } else {
                break;
            }
        } while (findIdx < pCharString.length);
        return findIdx;
    }

    public static int firstIndexChar(char[] pCharString, char[] pTrimChar, int pIdx) {
        int findIdx = pIdx;
        do {
            if (UtilString.checkChar(pCharString[findIdx], pTrimChar)) {
                findIdx++;
            } else {
                break;
            }
        } while (findIdx < pCharString.length);
        return findIdx;
    }

    public static int lastIndexChar(String pString) {
        return lastIndexChar(pString, pString.length() - 1);
    }

    public static int lastIndexChar(String pString, int pIdx) {
        char[] charString = pString.toCharArray();
        return lastIndexChar(charString, pIdx);
    }

    public static int lastIndexChar(char[] pCharString) {
        return lastIndexChar(pCharString, pCharString.length - 1);
    }

    public static int lastIndexChar(char[] pCharString, char[] pTrimChar) {
        return lastIndexChar(pCharString, pTrimChar, pCharString.length - 1);
    }

    public static int lastIndexChar(char[] pCharString, int pIdx) {
        int findIdx = pIdx;
        do {
            if (UtilString.isWhitespaceChar(pCharString[findIdx])) {
                findIdx--;
            } else {
                break;
            }
        } while (findIdx >= 0);
        return findIdx;
    }

    public static int lastIndexChar(char[] pCharString, char[] pTrimChar, int pIdx) {
        int findIdx = pIdx;
        do {
            if (UtilString.checkChar(pCharString[findIdx], pTrimChar)) {
                findIdx--;
            } else {
                break;
            }
        } while (findIdx >= 0);
        return findIdx;
    }

    public static int firstIndexAlphaNumeric(char[] pCharString, int pIdx) {
        int findIdx = pIdx;
        do {
            if (!(UtilString.isAlphaChar(pCharString[findIdx]) || UtilString.isNumericChar(pCharString[findIdx]))) {
                findIdx++;
            } else {
                break;
            }
        } while (findIdx < pCharString.length);
        return findIdx;
    }

    public static int lastIndexAlphaNumeric(char[] pCharString, int pIdx) {
        int findIdx = pIdx;
        do {
            if (!(UtilString.isAlphaChar(pCharString[findIdx]) || UtilString.isNumericChar(pCharString[findIdx]))) {
                findIdx--;
            } else {
                break;
            }
        } while (findIdx >= 0);
        return findIdx;
    }

    public static int firstIndexDiv(String pString) {
        char[] charString = pString.toCharArray();
        return firstIndexDiv(charString, 0);
    }

    public static int firstIndexDiv(char[] pString, int pIdx) {
        int findIdx = pIdx;
        do {
            if (UtilString.isNameChar(pString[findIdx])) {
                findIdx++;
            } else {
                break;
            }
        } while (findIdx < pString.length);
        return findIdx;
    }

    public static int getNextCheckChar(String pString, int pIdx, char pCheck) {
        char[] charString = pString.toCharArray();
        return getNextCheckChar(charString, pIdx, pCheck);
    }

    public static int getNextCheckChar(char[] pString, int pIdx, char pCheck) {
        int findIdx = pIdx;
        do {
            if (UtilString.isWhitespaceChar(pString[findIdx])) {
                findIdx++;
            } else if (pString[findIdx] == pCheck) {
                break;
            } else {
                return -1;
            }
        } while (findIdx < pString.length);
        return findIdx;
    }

    public static int findNextChar(String pString, int pIdx, char pChar) {
        char[] charString = pString.toCharArray();
        return UtilString.findNextChar(charString, pIdx, pChar);
    }

    public static int findNextChar(char[] pString, int pIdx, char pChar) {
        int findIdx = pIdx;
        do {
            if (pString[findIdx] == pChar) {
                return findIdx;
            } else {
                findIdx++;
            }
        } while (findIdx < pString.length);
        return -1;
    }

    public static String trimSubstr(String pString, int pStartIdx, int pEndIdx) {
        char[] charString = pString.toCharArray();
        char[] newCharString = trimSubstr(charString, pStartIdx, pEndIdx);
        if (newCharString.length != charString.length) {
            return new String(newCharString);
        } else {
            return pString;
        }
    }

    public static char[] trimSubstr(char[] pStringChar, int pStartIdx, int pEndIdx) {
        int firstIdx = firstIndexChar(pStringChar, pStartIdx);
        int lastIdx = lastIndexChar(pStringChar, pEndIdx - 1);
        if (firstIdx > pEndIdx) {
            firstIdx = pStartIdx;
        }
        if (lastIdx < pStartIdx) {
            lastIdx = pEndIdx - 1;
        }
        if (firstIdx <= pEndIdx - 1 && lastIdx >= pStartIdx) {
            if (firstIdx != 0 || lastIdx != pStringChar.length - 1) {
                return Arrays.copyOfRange(pStringChar, firstIdx, lastIdx + 1);
            } else {
                return pStringChar;
            }
        } else {
            return EMPTY_CHAR_ARRAY;
        }
    }

    public static String trimChar(String pStringChar, int pStartIdx, int pEndIdx, char pTrimChar) {
        char[] sc = pStringChar.toCharArray();
        char[] retChar = trimChar(sc, pStartIdx, pEndIdx, pTrimChar);
        if (sc == retChar) {
            return pStringChar;
        } else {
            return new String(retChar);
        }
    }

    public static char[] trimChar(char[] pStringChar, int pStartIdx, int pEndIdx, char pTrimChar) {
        if (pStringChar != null && pStringChar.length > 0) {
            pEndIdx--;
            int start = pStartIdx, end = pEndIdx;
            while (start < pEndIdx && pStringChar[start] == pTrimChar) {
                start++;
            }
            while (end > pStartIdx && pStringChar[end] == pTrimChar) {
                end--;
            }
            if (end < start) {
                return EMPTY_CHAR_ARRAY;
            } else if (pStartIdx == start && pEndIdx == end) {
                return pStringChar;
            } else {
                return Arrays.copyOfRange(pStringChar, start, end + 1);
            }
        }
        return EMPTY_CHAR_ARRAY;

    }

    public static String trim(String pString) {
        if (pString != null) {
            char[] charString = pString.toCharArray();
            String ret = trim(charString);
            if (ret.length() == pString.length()) {
                ret = pString;
            }
            return ret;
        } else {
            return null;
        }
    }

    public static String[] trim(String[] pString) {
        if (pString != null) {
            String[] ret = new String[pString.length];
            for (int idx = 0; idx < pString.length; idx++) {
                char[] charString = pString[idx].toCharArray();
                ret[idx] = trim(charString);
                if (ret[idx].length() == pString[idx].length()) {
                    ret[idx] = pString[idx];
                }
            }
            return ret;
        } else {
            return null;
        }
    }

    public static String[] trim(String[] pString, char... pTrimChar) {
        String[] ret = new String[pString.length];
        for (int idx = 0; idx < pString.length; idx++) {
            char[] charString = pString[idx].toCharArray();
            ret[idx] = trim(charString, pTrimChar);
            if (ret[idx].length() == pString[idx].length()) {
                ret[idx] = pString[idx];
            }
        }
        return ret;
    }

    public static String trim(String pString, char... pTrimChar) {
        char[] charString = pString.toCharArray();
        String ret = trim(charString, pTrimChar);
        if (ret.length() == pString.length()) {
            ret = pString;
        }
        return ret;
    }

    public static String trim(char[] pString) {
        if (pString.length > 0) {
            int firstIdx = firstIndexChar(pString);
            int lastIdx = lastIndexChar(pString);
            if (firstIdx < pString.length && lastIdx > -1) {
                return new String(pString, firstIdx, lastIdx + 1 - firstIdx);
            }
        }
        return "";
    }

    public static String trim(char[] pString, char[] pTrimChar) {
        if (pTrimChar == null || pTrimChar.length == 0) {
            pTrimChar = new char[]{' '};
        }
        if (pString.length > 0) {
            int firstIdx = firstIndexChar(pString, pTrimChar);
            int lastIdx = lastIndexChar(pString, pTrimChar);
            if (firstIdx < pString.length && lastIdx > -1) {
                return new String(pString, firstIdx, lastIdx + 1 - firstIdx);
            }
        }
        return "";
    }

    public static char[] trimChar(char[] pStringChar) {
        if (pStringChar != null) {
            return trimSubstr(pStringChar, 0, pStringChar.length - 1);
        } else {
            return EMPTY_CHAR_ARRAY;
        }
    }

    public static String trimBetweem(String pSource, String pStartFind, String pEndFind) {
        int sidx = pSource.indexOf(pStartFind);
        int eidx = pSource.lastIndexOf(pEndFind);
        return pSource.substring(sidx + pStartFind.length(), eidx);
    }

    public static String lpad(String pString, int pTotalLength, char pPad) {
        if (pString.length() < pTotalLength) {
            char[] charString = pString.toCharArray();
            return new String(lpad(charString, pTotalLength, pPad));
        } else {
            return pString;
        }
    }

    public static char[] lpad(char[] pStringChar, int pTotalLength, char pPad) {
        if (pStringChar.length < pTotalLength) {
            char[] retChar = new char[pTotalLength];
            int padIdx = pTotalLength - pStringChar.length;
            Arrays.fill(retChar, 0, padIdx, pPad);
            System.arraycopy(pStringChar, 0, retChar, padIdx, pStringChar.length);
            return retChar;
        } else {
            return pStringChar;
        }
    }

    public static String rpad(String pString, int pTotalLength, char pPad) {
        if (pString.length() < pTotalLength) {
            char[] charString = pString.toCharArray();
            return new String(rpad(charString, pTotalLength, pPad));
        } else {
            return pString;
        }
    }

    public static char[] rpad(char[] pStringChar, int pTotalLength, char pPad) {
        if (pStringChar.length < pTotalLength) {
            char[] retChar = new char[pTotalLength];
            int padIdx = pStringChar.length;
            System.arraycopy(pStringChar, 0, retChar, 0, pStringChar.length);
            Arrays.fill(retChar, padIdx, pTotalLength, pPad);
            return retChar;
        } else {
            return pStringChar;
        }
    }

    public static String ltrim(String pString) {
        char[] charString = pString.toCharArray();
        return ltrim(charString);
    }

    public static String ltrim(char[] pString) {
        int firstIdx = firstIndexChar(pString);
        if (firstIdx < pString.length) {
            return new String(pString, firstIdx, pString.length - 1 - firstIdx);
        } else {
            return "";
        }
    }

    public static String rtrim(String pString) {
        char[] charString = pString.toCharArray();
        return rtrim(charString);
    }

    public static String rtrim(char[] pString) {
        int lastIdx = lastIndexChar(pString);
        if (lastIdx > 0) {
            return new String(pString, 0, lastIdx);
        } else {
            return "";
        }
    }

    public static String firstUpperToLowerString(String pString) {
        if (pString != null) {
            char[] strC = pString.toCharArray();
            return new String(UtilString.firstUpperToLowerString(strC));
        } else {
            return null;
        }
    }

    public static char[] firstUpperToLowerString(char[] pCharString) {
        if (pCharString.length > 0) {
            char[] retChar = new char[pCharString.length];
            retChar[0] = toUpperChar(pCharString[0]);
            toLowerCharZero(pCharString, 1, retChar, 1, pCharString.length - 1);
            return retChar;
        } else {
            return new char[0];
        }
    }

    public static String divToCamelCaseString(String pString, char pDiv) {
        int didx = 0;
        char[] cstr = firstUpperToLowerString(pString.toCharArray());
        char[] retChar = new char[cstr.length];
        for (int sidx = 0; sidx < cstr.length; sidx++) {
            char c = cstr[sidx];
            if (c == pDiv) {
                do {
                    sidx++;
                } while (sidx < cstr.length && cstr[sidx] == pDiv);
                if (sidx < cstr.length) {
                    retChar[didx] = (char) (cstr[sidx] & MASK_UPPERCASE);
                }
            } else {
                retChar[didx] = cstr[sidx];
            }
            didx++;
        }
        return new String(retChar, 0, didx);
    }

    public static String camelCaseToDivString(String pString, String pDiv) {
        if (pString != null && !pString.equals("")) {
            char[] cstr = pString.toCharArray();
            StringBuilder sb = new StringBuilder();
            sb.append(UtilString.toLowerChar(cstr[0]));
            for (int idx = 1; idx < cstr.length; idx++) {
                if (isUpperChar(cstr[idx])) {
                    sb.append(pDiv).append(UtilString.toLowerChar(cstr[idx]));
                } else {
                    sb.append(cstr[idx]);
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    public static String camelCaseString(String pString, String[] pCamelContains, String[] pCamelEndWith) {
        char[] cstr = pString.toCharArray();
        char[] retChar = UtilString.toLowerChar(cstr);
        boolean found;
        for (int idx = 0; idx < retChar.length; idx++) {
            int fidx = 0;
            char c = retChar[idx];
            found = false;
            for (String s : pCamelContains) {
                fidx = 0;
                while (idx + fidx < retChar.length && s.charAt(fidx) == retChar[idx + fidx]) {
                    fidx++;
                    if (fidx >= s.length()) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    retChar[idx] = (char) (c & MASK_UPPERCASE);
                    idx = idx + fidx - 1;
                    break;
                }
            }
        }
        int idx = 0, fidx = 0;
        for (String s : pCamelEndWith) {
            fidx = s.length() - 1;
            idx = retChar.length - 1;
            found = false;
            if (idx > fidx) {
                while (s.charAt(fidx) == retChar[idx]) {
                    fidx--;
                    if (fidx < 0) {
                        found = true;
                        break;
                    }
                    idx--;
                }
                if (found) {
                    retChar[idx] = (char) (retChar[idx] & MASK_UPPERCASE);
                    //idx = idx + fidx - 1;
                    break;
                }
            }
        }
        return new String(UtilString.firstUpperString(retChar));
    }

    public static String defaultCamelCaseString(String pString) {
        String[] contains = new String[]{"type", "decode", "encode", "precode", "subcode", "uncode", "code", "new", "old", "util", "core", "stand", "email", "mail",
            "xml", "xsd", "outdate", "predate", "update", "valid", "date", "amnt", "restart", "start", "ver", "rerun", "crun", "trun", "run",
            "encount", "rediscount", "discount", "recount", "account", "uncount", "count", "case", "string", "collect", "common", "prod", "model", "proj", "web",
            "pref", "prev", "next", "implant", "transp", "plan", "flag", "status", "member", "client", "branch", "bank", "address",
            "sms", "title", "nonexcl", "excl", "text", "source", "scheme", "from", "setup", "group", "time", "tag", "data", "auth",
            "redisp", "undisp", "disp", "tran", "defi", "base", "creat", "host", "enabl", "message", "msg", "sequence", "seq", "notify", "susp",
            "indesc", "undesc", "desc", "year", "month", "response", "infreq", "freq", "preq", "req", "detail", "monopoli", "poli",
            "watt", "attach", "atta", "element", "elem", "reinf", "info", "letter", "menu", "job", "scan", "search", "surname", "name",
            "blue", "green", "yellow", "gray", "pink", "orange", "anti", "gold", "brown", "black", "white", "purple", "backup",
            "imprint", "preprint", "reprint", "print", "copro", "reproc", "subprocedure", "subproc", "proc", "procedure", "system", "sys", "user", "benefit", "root"};
        String[] endswith = new String[]{"num", "id", "amt", "no", "ind", "end"};

        if (pString != null) {
            return camelCaseString(pString, contains, endswith);
        } else {
            return null;
        }
    }

    public static String firstUpperString(String pString) {
        if (pString != null) {
            char[] str = pString.toCharArray();
            return new String(firstUpperString(str));
        } else {
            return null;
        }
    }

    public static char[] firstUpperString(char[] pString) {
        if (pString.length > 0) {
            char[] retChar = new char[pString.length];
            retChar[0] = (char) (pString[0] & MASK_UPPERCASE);
            for (int idx = 1; idx < pString.length; idx++) {
                retChar[idx] = pString[idx];
            }
            return retChar;
        } else {
            return new char[0];
        }
    }

    public static String firstLowerString(String pString) {
        if (pString != null) {
            char[] str = pString.toCharArray();
            return new String(firstLowerString(str));
        } else {
            return null;
        }
    }

    public static char[] firstLowerString(char[] pString) {
        if (pString.length > 0) {
            char[] retChar = new char[pString.length];
            retChar[0] = (char) (pString[0] | MASK_LOWERCASE);
            for (int idx = 1; idx < pString.length; idx++) {
                retChar[idx] = pString[idx];
            }
            return retChar;
        } else {
            return new char[0];
        }
    }

    public static boolean isLowerChar(final char pChar) {
        if (pChar == toLowerChar(pChar)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isUpperChar(final char pChar) {
        if (pChar == toUpperChar(pChar)) {
            return true;
        } else {
            return false;
        }
    }

    public static int indexOf(char[] pString, String pFind, int pStart) {
        char[] charFind = pFind.toCharArray();
        for (int idx = pStart; idx < pString.length; idx++) {
            if (pString[idx] == charFind[0]) {
                for (int fidx = 1; fidx < charFind.length; fidx++) {
                    if (pString[idx + fidx] != charFind[fidx]) {
                        idx = idx + fidx;
                        break;
                    }
                }
                return idx;
            }
        }
        return -1;
    }

    public static int getTokenStringIndex(String pToken, String pStringList) {
        if (pStringList != null && pStringList.length() > 1) {
            int vRet = 0;
            int vIdx = 0, vIdx2 = 1;
            int vCount = 0;

            if (pStringList.charAt(0) != ':') {
                pStringList = ":" + pStringList;
            }
            if (pStringList.charAt(pStringList.length() - 1) != ':') {
                pStringList = pStringList + ":";
            }
            String vParam = pToken.toUpperCase(LOCALE);
            String vParamList = pStringList.toUpperCase(LOCALE);
            while (vIdx >= 0 && vIdx < vParamList.length() - 1) {
                vCount++;
                vIdx = vParamList.indexOf(':', vIdx);
                if (vIdx >= 0 && vIdx < vParamList.length() - 1) {
                    vIdx2 = vParamList.indexOf(':', vIdx + 1);
                    if (vIdx2 < 0) {
                        vIdx2 = vParamList.length() - 1;
                    }
                    if (vParamList.substring(vIdx + 1, vIdx2).equals(vParam)) {
                        vRet = vCount;
                        break;
                    }
                    vIdx = vIdx2;
                } else {
                    break;
                }
            }

            return vRet;
        } else {
            return 0;
        }
    }

    public static char[] combineCharArray(char[]... pCharArrays) {
        char[] ret = EMPTY_CHAR_ARRAY;
        if (pCharArrays != null && pCharArrays.length > 0) {
            int length = 0;
            for (char[] ca : pCharArrays) {
                length = length + ca.length;
            }
            ret = new char[length];
            int pos = 0;
            for (char[] ca : pCharArrays) {
                System.arraycopy(ca, 0, ret, pos, ca.length);
                pos = pos + ca.length;
            }
        }
        return ret;
    }

    public static char[] combineCharArray(char pDivider, char[]... pCharArrays) {
        char[] ret = EMPTY_CHAR_ARRAY;
        if (pCharArrays != null && pCharArrays.length > 0) {
            int length = 0;
            for (char[] ca : pCharArrays) {
                length = length + ca.length;
            }
            length = length + (pCharArrays.length - 1);
            ret = new char[length];
            int pos = 0;
            for (char[] ca : pCharArrays) {
                System.arraycopy(ca, 0, ret, pos, ca.length);
                pos = pos + ca.length;
                if (pos < ret.length) {
                    ret[pos] = pDivider;
                    pos = pos + 1;
                }
            }
        }
        return ret;
    }

    public static String combineString(String pDivider, String... pStrings) {
        if (pStrings != null && pStrings.length > 0) {
            StringBuilder buffer = new StringBuilder(pStrings[0]);
            for (int idx = 1; idx < pStrings.length; idx++) {
                String s = pStrings[idx];
                buffer.append(pDivider).append(s);
            }
            return buffer.toString();
        }
        return "";
    }

    public static int compareOptionalString(String pStr1, String pStr2) {
        if (pStr1 != null) {
            if (pStr2 != null) {
                return pStr1.compareTo(pStr2);
            } else {
                return -1;
            }
        } else if (pStr2 == null) {
            return 0;
        } else {
            return 1;
        }
    }

    public static int findStringInCharArray(char[] pData, int pStart, int pEnd, String pFindString) {
        char[] scomp = pFindString.toCharArray();
        //char[] scomp = pFindString.toCharArray();
        for (int idx = pStart; idx <= pEnd; idx++) {
            if (pData[idx] == scomp[0]) {
                for (int idx2 = 1; idx2 < scomp.length; idx2++) {
                    if (pData[idx + idx2] != scomp[idx2]) {
                        break;
                    }
                }
                return idx;
            }
        }
        return -1;
    }

    public static String firstDotNamePart(String pName) {
        if (pName != null) {
            for (int idx = 0; idx < pName.length(); idx++) {
                char c = pName.charAt(idx);
                if (c == '.') {
                    return pName.substring(0, idx);
                } else if (!isNameChar(c)) {
                    return null;
                }
            }
        }
        return null;
    }

    public static String firstNamePart(String pName) {
        if (pName != null) {
            for (int idx = 0; idx < pName.length(); idx++) {
                char c = pName.charAt(idx);
                if (!isNameChar(c)) {
                    return pName.substring(0, idx);
                }
            }
        }
        return pName;
    }

    public static String lastNamePart(String pName) {
        if (pName != null) {
            for (int idx = pName.length() - 1; idx >= 0; idx--) {
                char c = pName.charAt(idx);
                if (c != '"' && !isNameChar(c) || c == '.') {
                    return pName.substring(idx + 1);
                }
            }
        }
        return pName;
    }

    public static String[] splitNameOff(String pName) {
        if (pName != null) {
            int idx = pName.lastIndexOf('.');
            if (idx > 0) {
                String n = pName.substring(idx + 1);
                String p = pName.substring(0, idx);
                return new String[]{n, p};
            } else {
                return new String[]{pName, null};
            }
        } else {
            return new String[]{null, null};
        }
    }

    public static String removeDuplicateChars(String pString) {
        if (pString != null && !pString.isEmpty()) {
            char[] ret = new char[pString.length()];
            int idx = 0;
            char c, lastc;
            lastc = pString.charAt(0);
            for (int sidx = 1; sidx < pString.length(); sidx++) {
                c = pString.charAt(sidx);
                if (c != lastc) {
                    ret[idx] = c;
                    idx++;
                }
                lastc = c;
            }
            return new String(ret, 0, idx);
        }
        return "";
    }

    public static char[] removeDuplicateChars(char[] pString) {
        if (pString != null && pString.length > 0) {
            char[] ret = new char[pString.length];
            int idx = 0;
            char c, lastc;
            lastc = pString[0];
            for (int sidx = 1; sidx < pString.length; sidx++) {
                c = pString[sidx];
                if (c != lastc) {
                    ret[idx] = c;
                    idx++;
                }
                lastc = c;
            }
            return Arrays.copyOfRange(ret, 0, idx);
        }
        return new char[0];
    }

    public static char[] removeDuplicateChars(char[] pString, int pOffset, int pLength) {
        if (pString != null && pString.length > 0 && pLength > 0) {
            char[] ret = new char[pLength];
            int idx = 1;
            char c, lastc;
            lastc = pString[pOffset];
            ret[0] = lastc;
            for (int sidx = 1; sidx < pLength; sidx++) {
                c = pString[sidx + pOffset];
                if (c != lastc) {
                    ret[idx] = c;
                    idx++;
                }
                lastc = c;
            }
            return Arrays.copyOfRange(ret, 0, idx);
        }
        return new char[0];
    }

    public static String inputStreamToString(InputStream pInString) throws IOException {
        final int bufLength = 64 * 1024;
        int readCount = 0;
        byte[] buf = new byte[bufLength];
        String ret = null;
        try (ByteArrayOutputStream bufStream = new ByteArrayOutputStream();) {
            do {
                readCount = pInString.read(buf, 0, bufLength - 1);
                if (readCount > 0) {
                    bufStream.write(buf, 0, readCount);
                }
            } while (readCount > 0);
            bufStream.flush();
            ret = bufStream.toString();
        }
        return ret;
    }

    public static String inputStreamToString(InputStreamReader pInString) throws IOException {
        final int bufLength = 64 * 1024;
        int readCount = 0;
        char[] buf = new char[bufLength];
        StringBuilder stringBuf = new StringBuilder();
        do {
            readCount = pInString.read(buf, 0, bufLength - 1);
            if (readCount > 0) {
                stringBuf.append(buf, 0, readCount);
            }
        } while (readCount > 0);
        return stringBuf.toString();
    }

    public static String substringBetween(String inString, String fromString, String toString) {
        int idx1 = inString.indexOf(fromString);
        if (idx1 < 0) {
            idx1 = 0;
        }
        int idx2 = inString.indexOf(toString, idx1);
        if (idx2 < 0) {
            idx2 = inString.length();
        }
        return inString.substring(idx1, idx2);
    }

    public static String defaultIfBlank(String pString, String pDefault) {
        if (pString == null || pString.isEmpty()) {
            pString = pDefault;
        }
        return pString;
    }

    public static boolean isBlank(String pString) {
        if (pString == null || pString.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public static String arrayToString(Object[] pStringArray, String pDefault) {
        if (pStringArray == null || pStringArray.length == 0) {
            return pDefault;
        }
        StringBuilder ret = new StringBuilder();
        for (Object s : pStringArray) {
            if (ret.length() > 0) {
                ret.append(";");
            }
            ret.append(s);
        }
        return ret.toString();
    }

    public static String removeEnd(String inString, String fromString) {
        int idx1 = inString.indexOf(fromString);
        if (idx1 < 0) {
            idx1 = inString.length();
        }
        return inString.substring(0, idx1);
    }

    public static int compareCharArray(char[] pSource, int pSourceStartIdx, char[] pSearch, int pSearchStartIdx, int pLength) {
        if (pSource == null || pSource.length == 0 || (pSourceStartIdx + pLength) >= pSource.length) {
            return -1;
        } else if (pSearch == null || pSearch.length == 0 || (pSearchStartIdx + pLength) > pSearch.length) {
            return +1;
        } else {
            int idx = 0;
            while (idx < pLength) {
                char c1 = pSource[idx + pSourceStartIdx];
                char c2 = pSearch[idx + pSearchStartIdx];
                if (c1 != c2) {
                    return c1 - c2;
                }
                idx++;
            }
            return 0;
        }
    }

    public static int compareIgnoreCaseCharArray(char[] pSource, int pSourceStartIdx, char[] pSearch, int pSearchStartIdx, int pLength) {
        if (pSource == null || pSource.length == 0 || (pSourceStartIdx + pLength) >= pSource.length) {
            return -1;
        } else if (pSearch == null || pSearch.length == 0 || (pSearchStartIdx + pLength) > pSearch.length) {
            return +1;
        } else {
            int idx = 0;
            while (idx < pLength) {
                char c1 = UtilUnicode.toUpperChar(pSource[idx + pSourceStartIdx]);
                char c2 = UtilUnicode.toUpperChar(pSearch[idx + pSearchStartIdx]);
                if (c1 != c2) {
                    return c1 - c2;
                }
                idx++;
            }
            return 0;
        }
    }

    public static String[] splitUnscoped(String pSource, String pFind, boolean pTrim) {
        if (pSource != null && pFind != null) {
            return splitUnscoped(pSource.toCharArray(), pFind.toCharArray(), pTrim);
        } else {
            return new String[0];
        }
    }

    public static String[] splitUnscoped(String pSource, String pFindStart, String pFindEnd, boolean pTrim) {
        if (pSource != null && pFindStart != null && pFindEnd != null) {
            return splitUnscoped(pSource.toCharArray(), pFindStart.toCharArray(), pFindEnd.toCharArray(), pTrim);
        } else {
            return new String[0];
        }
    }

    public static String[] splitUnscoped(char[] pSource, char[] pFind, boolean pTrim) {
        String[] ret = new String[0];
        if (pSource != null && pFind != null) {
            List<String> retList = new ArrayList();
            int sidx = 0;
            int idx = 0;
            while (idx < pSource.length) {
                int fidx = findNextUnscopedChar(pSource, idx, pFind);
                if (fidx > -1) {
                    if (sidx < fidx) {
                        String s;
                        if (pTrim) {
                            s = new String(trimSubstr(pSource, sidx, fidx));
                        } else {
                            s = new String(pSource, sidx, fidx - sidx);
                        }
                        if (s.length() > 0) {
                            retList.add(s);
                        }
                    }
                    idx = fidx + pFind.length;
                    sidx = idx;
                } else {
                    idx = pSource.length;
                }
            }
            if (sidx < idx) {
                String s;
                if (pTrim) {
                    s = new String(trimSubstr(pSource, sidx, idx));
                } else {
                    s = new String(pSource, sidx, idx - sidx);
                }
                retList.add(s);
            }
            ret = retList.toArray(ret);
        }
        return ret;
    }

    public static String[] splitUnscoped(char[] pSource, char[] pFindStart, char[] pFindEnd, boolean pTrim) {
        String[] ret = new String[0];
        if (pSource != null && pFindStart != null && pFindEnd != null) {
            List<String> retList = new ArrayList();
            int sidx = 0;
            int idx = 0;
            String s;
            while (idx < pSource.length) {
                int fidx1 = findNextUnscopedChar(pSource, idx, pFindStart);
                if (fidx1 > -1) {
                    if (sidx < fidx1) {
                        if (pTrim) {
                            s = new String(trimSubstr(pSource, sidx, fidx1));
                        } else {
                            s = new String(pSource, sidx, fidx1 - sidx);
                        }
                        if (s.length() > 0) {
                            retList.add(s);
                        }
                    }
                    int fidx2 = findNextUnscopedChar(pSource, fidx1 + 1, pFindEnd);
                    if (fidx2 > -1) {

                        if (pTrim) {
                            s = new String(trimSubstr(pSource, fidx1 + pFindStart.length, fidx2));
                        } else {
                            s = new String(pSource, fidx1 + pFindStart.length, fidx2 - fidx1 - pFindStart.length);
                        }
                        if (s.length() > 0) {
                            retList.add(s);
                        }
                        idx = fidx2 + pFindEnd.length;
                        sidx = idx;
                    } else {
                        idx = pSource.length;
                    }
                } else {
                    idx = pSource.length;
                }
            }
            if (sidx < pSource.length) {
                if (pTrim) {
                    s = new String(trimSubstr(pSource, sidx, pSource.length));
                } else {
                    s = new String(pSource, sidx, pSource.length - sidx);
                }
                retList.add(s);
            }
            ret = retList.toArray(ret);
        }
        return ret;
    }

    public static String[] splitWhitespaceUnscoped(String pSource) {
        if (pSource != null) {
            return splitWhitespaceUnscoped(pSource.toCharArray());
        } else {
            return new String[0];
        }
    }

    public static String[] splitWhitespaceUnscoped(char[] pSource) {
        String[] ret = new String[0];
        if (pSource != null) {
            List<String> retList = new ArrayList();
            int sidx = 0;
            int idx = 0;
            while (idx < pSource.length) {
                int fidx = findNextWhitespaceUnscopedChar(pSource, idx);
                if (fidx > -1) {
                    if (sidx < fidx) {
                        String s;
                        s = new String(trimSubstr(pSource, sidx, fidx));
                        if (s != null && s.length() > 0) {
                            retList.add(s);
                        }
                    }
                    idx = fidx + 1;
                    if (idx < pSource.length) {
                        idx = firstIndexChar(pSource, fidx + 1);
                    }
                    sidx = idx;
                } else {
                    idx = pSource.length;
                }
            }
            if (sidx < idx) {
                String s;
                s = new String(trimSubstr(pSource, sidx, idx));
                retList.add(s);
            }
            ret = retList.toArray(ret);
        }
        return ret;
    }

    public static char[][] splitWhitespaceUnscopedChar(char[] pSource) {
        char[][] ret = new char[0][0];
        if (pSource != null) {
            List<char[]> retList = new ArrayList();
            int sidx = 0;
            int idx = 0;
            while (idx < pSource.length) {
                int fidx = findNextWhitespaceUnscopedChar(pSource, idx);
                if (fidx > -1) {
                    if (sidx < fidx) {
                        char[] s;
                        s = trimSubstr(pSource, sidx, fidx);
                        if (s != null && s.length > 0) {
                            retList.add(s);
                        }
                    }
                    idx = fidx + 1;
                    if (idx < pSource.length) {
                        idx = firstIndexChar(pSource, fidx + 1);
                    }
                    sidx = idx;
                } else {
                    idx = pSource.length;
                }
            }
            if (sidx < idx) {
                char[] s = trimSubstr(pSource, sidx, idx);
                retList.add(s);
            }
            ret = retList.toArray(ret);
        }
        return ret;
    }

    public static String[] splitOperandUnscoped(String pSource) {
        if (pSource != null) {
            return splitOperandUnscoped(pSource.toCharArray());
        } else {
            return new String[0];
        }
    }

    public static String[] splitOperandUnscoped(char[] pSource) {
        String[] ret = new String[0];
        if (pSource != null) {
            List<String> retList = new ArrayList();
            int sidx = 0;
            int idx = 0;
            while (idx < pSource.length) {
                int fidx = findNextOperandUnscopedChar(pSource, idx);
                if (fidx > -1) {
                    if (sidx < fidx) {
                        String s = new String(trimSubstr(pSource, sidx, fidx));
                        if (s.length() > 0) {
                            retList.add(s);
                        }
                    }
                    idx = fidx + 1;
                    if (fidx + 1 < pSource.length && isOperandChar(pSource[fidx + 1])) {
                        idx++;
                    }
                    String s = new String(trimSubstr(pSource, fidx, idx));
                    if (s.length() > 0) {
                        retList.add(s);
                    }

                    sidx = idx;
                } else {
                    idx = pSource.length;
                }
            }
            if (sidx < idx) {
                String s;
                s = new String(trimSubstr(pSource, sidx, idx));
                retList.add(s);
            }
            ret = retList.toArray(ret);
        }
        return ret;
    }

    public static String[] splitFromTo(String pSource, String pStartFind, String pEndFind, boolean pTrim) {
        List<String> ret = new ArrayList();
        char[] ca = pSource.toCharArray();
        char[] cstart = pStartFind.toCharArray();
        char[] cend = pEndFind.toCharArray();
        int sidx = 0, eidx = 0;
        boolean found = false;
        int idx;
        for (idx = 0; idx < ca.length; idx++) {
            if (!found) {
                if (ca[idx] == cstart[0]) {
                    if (cstart.length == 1 || compareCharArray(ca, idx, cstart, 0, cstart.length) == 0) {
                        if (eidx < idx) {
                            String s;
                            if (pTrim) {
                                s = new String(trimSubstr(ca, eidx, idx - 1));
                                if (s != null && !s.isEmpty()) {
                                    ret.add(s);
                                }
                            } else {
                                s = new String(ca, eidx, idx - eidx);
                                ret.add(s);
                            }
                        }
                        sidx = idx;
                        idx = idx + cstart.length;
                        found = true;
                        idx = findNextUnscopedChar(ca, idx, cend);
                    }
                }
            }
            if (found && idx > -1 && ca[idx] == cend[0]) {
                if (cend.length == 1 || compareCharArray(ca, idx, cend, 0, cend.length) == 0) {
                    idx = idx + cend.length;
                    eidx = idx;
                    String s;
                    if (pTrim) {
                        s = new String(trimSubstr(ca, sidx, idx - 1));
                        if (s != null && !s.isEmpty()) {
                            ret.add(s);
                        }
                    } else {
                        s = new String(ca, sidx, idx - sidx);
                        ret.add(s);
                    }
                    found = false;
                }
            } else if (idx == -1) {
                idx = ca.length;
            }
        }
        if (idx > ca.length) {
            idx = ca.length;
        }
        if (found && sidx < idx) {
            String s;
            if (pTrim) {
                s = new String(trimSubstr(ca, sidx, idx - 1));
                if (s != null && !s.isEmpty()) {
                    ret.add(s);
                }
            } else {
                s = new String(ca, sidx, idx - sidx);
                ret.add(s);
            }
        } else if (!found && eidx < idx) {
            String s;
            if (pTrim) {
                s = new String(trimSubstr(ca, eidx, idx - 1));
                if (s != null && !s.isEmpty()) {
                    ret.add(s);
                }
            } else {
                s = new String(ca, eidx, idx - eidx);
                ret.add(s);
            }
        }
        return ret.toArray(new String[0]);
    }

    public static int findNextUnscopedChar(char[] pSource, int pStartIndex, char... pFind) {
        if (pSource != null && pFind != null && pFind.length > 0 && pSource.length > 0) {
            int idx;
            boolean q1 = false, q2 = false;
            int b1 = 0, b2 = 0, b3 = 0;
            mailLoop:
            for (idx = pStartIndex; idx < pSource.length; idx++) {
                if (pSource[idx] == pFind[0]) {
                    if (!(q1 || q2) && b1 == 0 && b2 == 0 && b3 == 0) {
                        for (int fidx = 1; fidx < pFind.length; fidx++) {
                            if (pSource[idx + fidx] != pFind[fidx]) {
                                break mailLoop;
                            }
                        }
                        return idx;
                    }
                }
                if (pSource[idx] == '\'') {
                    q1 = !(q1);
                } else if (pSource[idx] == '"') {
                    q2 = !(q2);
                } else if (pSource[idx] == '(') {
                    if (!(q1 || q2)) {
                        b1++;
                    }
                } else if (pSource[idx] == '[') {
                    if (!(q1 || q2)) {
                        b2++;
                    }
                } else if (pSource[idx] == '{') {
                    if (!(q1 || q2)) {
                        b3++;
                    }
                } else if (pSource[idx] == ')') {
                    if (!(q1 || q2)) {
                        b1--;
                    }
                } else if (pSource[idx] == ']') {
                    if (!(q1 || q2)) {
                        b2--;
                    }
                } else if (pSource[idx] == '}') {
                    if (!(q1 || q2)) {
                        b3--;
                    }
                }
            }
        }
        return -1;
    }

    public static int findNextWhitespaceUnscopedChar(char[] pSource, int pStartIndex) {
        if (pSource != null && pSource.length > 0) {
            int idx;
            boolean q1 = false, q2 = false;
            int b1 = 0, b2 = 0, b3 = 0;
            mailLoop:
            for (idx = pStartIndex; idx < pSource.length; idx++) {
                if (pSource[idx] == '\'') {
                    q1 = !(q1);
                } else if (pSource[idx] == '"') {
                    q2 = !(q2);
                } else if (pSource[idx] == '(') {
                    if (!(q1 || q2)) {
                        b1++;
                    }
                } else if (pSource[idx] == '[') {
                    if (!(q1 || q2)) {
                        b2++;
                    }
                } else if (pSource[idx] == '{') {
                    if (!(q1 || q2)) {
                        b3++;
                    }
                } else if (pSource[idx] == ')') {
                    if (!(q1 || q2)) {
                        b1--;
                    }
                } else if (pSource[idx] == ']') {
                    if (!(q1 || q2)) {
                        b2--;
                    }
                } else if (pSource[idx] == '}') {
                    if (!(q1 || q2)) {
                        b3--;
                    }
                } else if (isWhitespaceChar(pSource[idx])) {
                    if (!(q1 || q2) && b1 == 0 && b2 == 0 && b3 == 0) {
                        return idx;
                    }
                }
            }
        }
        return -1;
    }

    public static int findNextOperandUnscopedChar(char[] pSource, int pStartIndex) {
        if (pSource != null && pSource.length > 0) {
            int idx;
            boolean q1 = false, q2 = false;
            int b1 = 0, b2 = 0, b3 = 0;
            mailLoop:
            for (idx = pStartIndex; idx < pSource.length; idx++) {
                if (pSource[idx] == '\'') {
                    q1 = !(q1);
                } else if (pSource[idx] == '"') {
                    q2 = !(q2);
                } else if (pSource[idx] == '(') {
                    if (!(q1 || q2)) {
                        b1++;
                    }
                } else if (pSource[idx] == '[') {
                    if (!(q1 || q2)) {
                        b2++;
                    }
                } else if (pSource[idx] == '{') {
                    if (!(q1 || q2)) {
                        b3++;
                    }
                } else if (pSource[idx] == ')') {
                    if (!(q1 || q2)) {
                        b1--;
                    }
                } else if (pSource[idx] == ']') {
                    if (!(q1 || q2)) {
                        b2--;
                    }
                } else if (pSource[idx] == '}') {
                    if (!(q1 || q2)) {
                        b3--;
                    }
                } else if (isOperandChar(pSource[idx])) {
                    if (!(q1 || q2) && b1 == 0 && b2 == 0 && b3 == 0) {
                        return idx;
                    }
                }
            }
        }
        return -1;
    }

    public static int findNextAphaNumericUnscopedChar(char[] pSource, int pStartIndex) {
        if (pSource != null && pSource.length > 0) {
            int idx;
            boolean q1 = false, q2 = false;
            int b1 = 0, b2 = 0, b3 = 0;
            mailLoop:
            for (idx = pStartIndex; idx < pSource.length; idx++) {
                if (pSource[idx] == '\'') {
                    q1 = !(q1);
                } else if (pSource[idx] == '"') {
                    q2 = !(q2);
                } else if (pSource[idx] == '(') {
                    if (!(q1 || q2)) {
                        b1++;
                    }
                } else if (pSource[idx] == '[') {
                    if (!(q1 || q2)) {
                        b2++;
                    }
                } else if (pSource[idx] == '{') {
                    if (!(q1 || q2)) {
                        b3++;
                    }
                } else if (pSource[idx] == ')') {
                    if (!(q1 || q2)) {
                        b1--;
                    }
                } else if (pSource[idx] == ']') {
                    if (!(q1 || q2)) {
                        b2--;
                    }
                } else if (pSource[idx] == '}') {
                    if (!(q1 || q2)) {
                        b3--;
                    }
                } else if (isAlphaChar(pSource[idx]) || isNumericChar(pSource[idx])) {
                    if (!(q1 || q2) && b1 == 0 && b2 == 0 && b3 == 0) {
                        return idx;
                    }
                }
            }
        }
        return -1;
    }

    public static int findPreviousAphaNumericUnscopedChar(char[] pSource, int pStartIndex) {
        if (pSource != null && pSource.length > 0) {
            int idx;
            boolean q1 = false, q2 = false;
            int b1 = 0, b2 = 0, b3 = 0;
            mailLoop:
            for (idx = pStartIndex; idx > 0; idx--) {
                if (pSource[idx] == '\'') {
                    q1 = !(q1);
                } else if (pSource[idx] == '"') {
                    q2 = !(q2);
                } else if (pSource[idx] == '(') {
                    if (!(q1 || q2)) {
                        b1++;
                    }
                } else if (pSource[idx] == '[') {
                    if (!(q1 || q2)) {
                        b2++;
                    }
                } else if (pSource[idx] == '{') {
                    if (!(q1 || q2)) {
                        b3++;
                    }
                } else if (pSource[idx] == ')') {
                    if (!(q1 || q2)) {
                        b1--;
                    }
                } else if (pSource[idx] == ']') {
                    if (!(q1 || q2)) {
                        b2--;
                    }
                } else if (pSource[idx] == '}') {
                    if (!(q1 || q2)) {
                        b3--;
                    }
                } else if (isAlphaChar(pSource[idx]) || isNumericChar(pSource[idx])) {
                    if (!(q1 || q2) && b1 == 0 && b2 == 0 && b3 == 0) {
                        return idx;
                    }
                }
            }
        }
        return -1;
    }

    public static int countLines(String pString) {
        char[] str = pString.toCharArray();
        return countLines(str);
    }

    public static int countLines(char[] pString) {
        if (pString != null && pString.length > 0) {
            int count = 1;
            for (char c : pString) {
                if (c == '\n') {
                    count++;
                }
            }
            return count;
        } else {
            return 0;
        }
    }

    public static int countLines(String pString, int pUptoIdx) {
        char[] str = pString.toCharArray();
        return countLines(str, pUptoIdx);
    }

    public static int countLines(char[] pString, int pUptoIdx) {
        if (pString != null && pString.length > 0) {
            int count = 1, idx = 0;
            for (char c : pString) {
                if (c == '\n') {
                    count++;
                }
                idx++;
                if (idx > pUptoIdx) {
                    break;
                }
            }
            return count;
        } else {
            return 0;
        }
    }

    public static int getLinePosition(char[] pString, int pIdx) {
        for (int idx = pIdx; idx > 0; idx--) {
            if (pString[idx] == '\n') {
                return pIdx - idx;
            }
        }
        return pIdx;
    }

    public static int editDistance(String s, String t) {
        int m = s.length();
        int n = t.length();
        int[][] d = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            d[0][j] = j;
        }
        for (int j = 1; j <= n; j++) {
            for (int i = 1; i <= m; i++) {
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    int v1 = d[i - 1][j] + 1;
                    int v2 = d[i][j - 1] + 1;
                    int v3 = d[i - 1][j - 1] + 1;
                    d[i][j] = min(v1, v2, v3);
                }
            }
        }
        return (d[m][n]);
    }

    public static int min(int pVal1, int pVal2, int pVal3) {
        //java.lang.Math.max
        return (pVal1 < pVal2)
                ? (pVal1 < pVal3) ? pVal1 : pVal3
                : (pVal2 < pVal3) ? pVal2 : pVal3;
    }

}
