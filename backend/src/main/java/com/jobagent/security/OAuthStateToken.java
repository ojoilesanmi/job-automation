package com.jobagent.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class OAuthStateToken {

    private static final String SECRET = System.getenv().getOrDefault("JWT_SECRET", "default-secret");
    private static final String HMAC_ALGO = "HmacSHA256";

    public static String generate(java.util.UUID userId) {
        String payload = userId.toString() + ":" + System.currentTimeMillis();
        String signature = sign(payload);
        return Base64.getUrlEncoder().encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
    }

    public static java.util.UUID validate(String state) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 3) return null;

            String payload = parts[0] + ":" + parts[1];
            String expectedSig = sign(payload);
            if (!expectedSig.equals(parts[2])) return null;

            long timestamp = Long.parseLong(parts[1]);
            if (System.currentTimeMillis() - timestamp > 600_000) return null;

            return java.util.UUID.fromString(parts[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign OAuth state", e);
        }
    }
}
