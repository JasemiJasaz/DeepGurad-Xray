package org.myplugin.deepGuardXray.utils;

import java.util.Base64;

/**
 * Provides basic obfuscation for sensitive data like webhook URLs
 */
public class WebhookSecurity {

    /**
     * Obfuscates a webhook URL for storage in config
     * This isn't true encryption but adds a layer of obfuscation
     */
    public static String obfuscateWebhookUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }


        String base64 = Base64.getEncoder().encodeToString(url.getBytes());
        return new StringBuilder(base64).reverse().toString();
    }

    /**
     * Deobfuscates a stored webhook URL
     */
    public static String deobfuscateWebhookUrl(String obfuscated) {
        if (obfuscated == null || obfuscated.isEmpty()) {
            return "";
        }

        try {

            String reversed = new StringBuilder(obfuscated).reverse().toString();
            byte[] decoded = Base64.getDecoder().decode(reversed);
            return new String(decoded);
        } catch (Exception e) {

            return "";
        }
    }
}