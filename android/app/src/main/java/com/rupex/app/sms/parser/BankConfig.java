package com.rupex.app.sms.parser;

import com.rupex.app.BuildConfig;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Bank configuration for SMS parsing
 */
public class BankConfig {

    // Debug mode test sender (for emulator testing)
    private static final String DEBUG_TEST_SENDER = "6505556789";

    // Known bank sender IDs (case-insensitive matching)
    private static final Set<String> BANK_SENDERS = new HashSet<>(Arrays.asList(
            // Major Banks
            "HDFCBK", "HDFCBN", "HDFC",
            "SBIINB", "SBIPSG", "SBISMS", "SBIUPI",
            "ICICIB", "ICICIT", "ICICI",
            "AXISBK", "AXISBN",
            "KOTAKB", "KOTAK",
            "PNBSMS", "PUNBNK",
            "BOIIND", "BOBANK",
            "CANBNK", "CANARA",
            "IABORB", "INDBNK",
            "IOBCHN", "IOB", "IOBIND",  // Indian Overseas Bank
            "UNIONB",
            "YESBNK", "YESBK",
            "IDBIBNK",
            "FEDBNK", "FEDSMS",
            
            // UPI / Wallets
            "PAYTMB", "PYTM",
            "PHONEPE", "PHNEPE",
            "GPAY", "GOOGLEPAY",
            "AMAZONPAY", "AMZPAY",
            "MOBIKWIK",
            "FREECHARGE",
            
            // Credit Cards
            "HDFCCC", "SBICRD", "ICICCC", "AXISCC",
            "AMEX", "CITI",
            
            // Generic UPI
            "UPIBNK", "NPCIUPI"
    ));

    // Prefixes to check if sender contains these
    private static final String[] BANK_PREFIXES = {
            "BK", "BNK", "BANK", "UPI", "PAY", "CC", "CRD"
    };

    /**
     * Check if sender ID is from a known bank or payment service
     */
    public static boolean isBankSender(String sender) {
        if (sender == null || sender.isEmpty()) {
            return false;
        }

        // Debug mode: Allow test phone number from emulator (only in debug builds)
        if (BuildConfig.DEBUG) {
            String cleanSender = sender.replaceAll("[^0-9]", "");
            if (cleanSender.endsWith(DEBUG_TEST_SENDER)) {
                return true;
            }
        }

        // Normalize sender ID
        String normalized = sender.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .replaceAll("^(AD|BZ|DM|TD|TM|VM|VD)-?", ""); // Remove carrier prefixes

        // Direct match
        if (BANK_SENDERS.contains(normalized)) {
            return true;
        }

        // Check if any known sender is contained in the ID
        for (String known : BANK_SENDERS) {
            if (normalized.contains(known) || known.contains(normalized)) {
                return true;
            }
        }

        // Check common bank prefixes/suffixes
        for (String prefix : BANK_PREFIXES) {
            if (normalized.endsWith(prefix) || normalized.contains(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get bank name from sender ID
     */
    public static String getBankName(String sender) {
        if (sender == null) return "Unknown";

        // Debug mode: Return test bank name for emulator testing
        if (BuildConfig.DEBUG) {
            String cleanSender = sender.replaceAll("[^0-9]", "");
            if (cleanSender.endsWith(DEBUG_TEST_SENDER)) {
                return "Indian Overseas Bank"; // Default to IOB for testing
            }
        }

        String normalized = sender.toUpperCase();

        if (normalized.contains("HDFC")) return "HDFC Bank";
        if (normalized.contains("SBI")) return "SBI";
        if (normalized.contains("ICICI")) return "ICICI Bank";
        if (normalized.contains("AXIS")) return "Axis Bank";
        if (normalized.contains("KOTAK")) return "Kotak Bank";
        if (normalized.contains("PNB") || normalized.contains("PUNB")) return "PNB";
        if (normalized.contains("BOI")) return "Bank of India";
        if (normalized.contains("CAN")) return "Canara Bank";
        if (normalized.contains("IOB")) return "Indian Overseas Bank";
        if (normalized.contains("UNION")) return "Union Bank";
        if (normalized.contains("YES")) return "Yes Bank";
        if (normalized.contains("IDBI")) return "IDBI Bank";
        if (normalized.contains("FED")) return "Federal Bank";
        if (normalized.contains("PAYTM")) return "Paytm";
        if (normalized.contains("PHONE") || normalized.contains("PHNE")) return "PhonePe";
        if (normalized.contains("GPAY") || normalized.contains("GOOGLE")) return "Google Pay";
        if (normalized.contains("AMAZON") || normalized.contains("AMZ")) return "Amazon Pay";
        if (normalized.contains("MOBIKWIK")) return "MobiKwik";
        if (normalized.contains("AMEX")) return "American Express";
        if (normalized.contains("CITI")) return "Citibank";

        return "Bank";
    }
}
