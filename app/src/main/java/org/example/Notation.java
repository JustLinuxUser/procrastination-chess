package org.example;

public class Notation {
    public static byte toIdx(String notation) {
        if (notation.length() != 2) {
            throw new RuntimeException("The legth of notation string is not 2, str: " + notation);
        }
        char letter = notation.charAt(0);
        char number = notation.charAt(1);
        if (letter < 'a' || letter > 'h' || number < '1' || number > '8') {
            throw new RuntimeException("Invalid notation, str: " + notation);
        }
        return (byte) ((number - '1') * 8 + letter - 'a');
    }

    public static String fromIdx(int idx) {
        if (idx < 0 || idx > 63) {
            throw new RuntimeException("Idx out of range: " + idx);
        }
        return "" + (char) (idx % 8 + 'a') + (char) (idx / 8 + '1');
    }
}
