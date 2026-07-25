package com.easybeach.shared.id;

import java.security.SecureRandom;
import java.time.Clock;

/**
 * ULID (Universally Unique Lexicographically Sortable Identifier):
 * 48-bit timestamp (ms) + 80-bit randomness, encoded as 26 Crockford-Base32
 * chars. Usado como {@code public_id} para entidades expuestas en URLs
 * (etapa 03 §1 convenciones) - evita enumeración secuencial de IDs ajenos.
 */
public final class UlidGenerator {

    private static final char[] ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int TIMESTAMP_LEN = 10;
    private static final int RANDOMNESS_LEN = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UlidGenerator() {
    }

    public static String generate() {
        return generate(Clock.systemUTC().millis());
    }

    static String generate(long timestampMillis) {
        char[] ulid = new char[TIMESTAMP_LEN + RANDOMNESS_LEN];
        long time = timestampMillis;
        for (int i = TIMESTAMP_LEN - 1; i >= 0; i--) {
            ulid[i] = ENCODING[(int) (time & 0x1F)];
            time >>>= 5;
        }
        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);
        long randHigh = ((long) (randomBytes[0] & 0xFF) << 32)
                | ((long) (randomBytes[1] & 0xFF) << 24)
                | ((long) (randomBytes[2] & 0xFF) << 16)
                | ((long) (randomBytes[3] & 0xFF) << 8)
                | (randomBytes[4] & 0xFF);
        long randLow = ((long) (randomBytes[5] & 0xFF) << 32)
                | ((long) (randomBytes[6] & 0xFF) << 24)
                | ((long) (randomBytes[7] & 0xFF) << 16)
                | ((long) (randomBytes[8] & 0xFF) << 8)
                | (randomBytes[9] & 0xFF);
        for (int i = TIMESTAMP_LEN + 7; i >= TIMESTAMP_LEN; i--) {
            ulid[i] = ENCODING[(int) (randHigh & 0x1F)];
            randHigh >>>= 5;
        }
        for (int i = TIMESTAMP_LEN + RANDOMNESS_LEN - 1; i >= TIMESTAMP_LEN + 8; i--) {
            ulid[i] = ENCODING[(int) (randLow & 0x1F)];
            randLow >>>= 5;
        }
        return new String(ulid);
    }
}
