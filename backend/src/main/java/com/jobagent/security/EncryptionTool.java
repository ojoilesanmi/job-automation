package com.jobagent.security;

public final class EncryptionTool {

    private EncryptionTool() {
    }

    public static void main(String[] args) {
        if (args.length != 2 || !("encrypt".equals(args[0]) || "decrypt".equals(args[0]))) {
            System.err.println("Usage: EncryptionTool encrypt|decrypt <value>");
            System.exit(1);
        }

        String masterKey = System.getenv("ENCRYPTION_KEY");
        if (masterKey == null || masterKey.isBlank()) {
            System.err.println("ENCRYPTION_KEY environment variable is required");
            System.exit(1);
        }

        String result = "encrypt".equals(args[0])
                ? EncryptionService.encrypt(args[1], masterKey)
                : EncryptionService.decrypt(args[1], masterKey);
        System.out.println(result);
    }
}
