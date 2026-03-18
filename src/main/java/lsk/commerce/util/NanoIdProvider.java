package lsk.commerce.util;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

import static lombok.AccessLevel.PRIVATE;

@Component
@NoArgsConstructor(access = PRIVATE)
public class NanoIdProvider {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] NUMBER_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnoqprstuvwxyz".toCharArray();
    private static final int DEFAULT_SIZE = 12;

    public static String createNanoId() {
        return NanoIdUtils.randomNanoId(SECURE_RANDOM, NUMBER_ALPHABET, DEFAULT_SIZE);
    }
}
