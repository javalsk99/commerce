package lsk.commerce.util;

public class InitialExtractor {
    private static final char[] Initial = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    public static String extract(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int base = c - 0xAC00;
                int initialIndex = base / (21 * 28);
                sb.append(Initial[initialIndex]);
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
