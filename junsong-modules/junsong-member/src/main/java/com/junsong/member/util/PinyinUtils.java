package com.junsong.member.util;

public class PinyinUtils {

    private static final int[] SECTION_POSITION = {
        45217, 45253, 45761, 46318, 46826, 47010, 47297, 47614, 48119, 49062,
        49324, 49896, 50371, 50614, 50622, 50906, 51387, 51446, 52218, 52698,
        52980, 53689, 54481, 55290
    };

    private static final char[] INITIALS = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
        'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'W',
        'X', 'Y', 'Z'
    };

    public static char getFirstPinyinChar(char ch) {
        if (ch >= 'a' && ch <= 'z') return (char) (ch - 32);
        if (ch >= 'A' && ch <= 'Z') return ch;
        if (ch >= '0' && ch <= '9') return ch;

        try {
            byte[] bytes = String.valueOf(ch).getBytes("GB2312");
            if (bytes.length < 2) return '#';
            int code = ((bytes[0] & 0xFF) * 256) + (bytes[1] & 0xFF);
            if (code < 45217 || code > 55289) return '#';
            for (int i = SECTION_POSITION.length - 1; i >= 0; i--) {
                if (code >= SECTION_POSITION[i]) {
                    return INITIALS[i];
                }
            }
        } catch (Exception e) {
            return '#';
        }
        return '#';
    }

    public static String getFirstLetter(String str) {
        if (str == null || str.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            char pinyin = getFirstPinyinChar(ch);
            if (pinyin != '#') {
                sb.append(pinyin);
            }
        }
        return sb.toString();
    }

    public static String getFirstTwoLetters(String str) {
        String firstLetters = getFirstLetter(str);
        if (firstLetters.length() >= 2) {
            return firstLetters.substring(0, 2);
        }
        if (firstLetters.length() == 1) {
            return firstLetters + "X";
        }
        return "XX";
    }
}
