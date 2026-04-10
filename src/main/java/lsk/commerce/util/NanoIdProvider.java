package lsk.commerce.util;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import static lombok.AccessLevel.PRIVATE;

@Component
@NoArgsConstructor(access = PRIVATE)
public class NanoIdProvider {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] NUMBER_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnoqprstuvwxyz".toCharArray();
    private static final int DEFAULT_SIZE = 12;
    private static final Pattern NANOID_PATTERN = Pattern.compile("^[" + String.valueOf(NUMBER_ALPHABET) + "]{" + DEFAULT_SIZE + "}$");

    public static String createNanoId() {
        return NanoIdUtils.randomNanoId(SECURE_RANDOM, NUMBER_ALPHABET, DEFAULT_SIZE);
    }

    public static boolean validateNanoId(String nanoId) {
        return NANOID_PATTERN.matcher(nanoId).matches();
    }
}
