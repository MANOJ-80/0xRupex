package com.rupex.app.notification;

import android.util.Log;

import com.rupex.app.sms.parser.CategoryDetector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses UPI payment notifications from various apps
 */
public class UpiNotificationParser {

    private static final String TAG = "UpiNotificationParser";

    // Common patterns
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:₹|Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern AMOUNT_PATTERN_2 = Pattern.compile(
            "([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:₹|Rs\\.?|INR)", Pattern.CASE_INSENSITIVE);

    // GPay patterns
    private static final Pattern GPAY_PAID = Pattern.compile(
            "(?:Paid|Sent)\\s+₹?([\\d,]+(?:\\.\\d{2})?)\\s+(?:to|for)\\s+(.+)", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern GPAY_RECEIVED = Pattern.compile(
            "(?:Received|Got)\\s+₹?([\\d,]+(?:\\.\\d{2})?)\\s+from\\s+(.+)", Pattern.CASE_INSENSITIVE);
    
    // GPay format: "NAME paid you ₹X.XX"
    private static final Pattern GPAY_PAID_YOU = Pattern.compile(
            "(.+?)\\s+paid you\\s+₹?([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);
    
    // GPay format: "You paid NAME ₹X.XX" or "Paid ₹X.XX to NAME"  
    private static final Pattern GPAY_YOU_PAID = Pattern.compile(
            "(?:You paid|Paid)\\s+(.+?)\\s+₹?([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);

    // PhonePe patterns
    private static final Pattern PHONEPE_PAID = Pattern.compile(
            "(?:Payment of|Paid)\\s+₹?([\\d,]+(?:\\.\\d{2})?)\\s+(?:to|successful)", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern PHONEPE_RECEIVED = Pattern.compile(
            "(?:Received|Credited)\\s+₹?([\\d,]+(?:\\.\\d{2})?)\\s+from\\s+(.+)", Pattern.CASE_INSENSITIVE);

    // Paytm patterns
    private static final Pattern PAYTM_PAID = Pattern.compile(
            "(?:Paid|Payment)\\s+(?:₹|Rs\\.?)\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);

    public static class ParsedNotification {
        public double amount;
        public String merchant;
        public boolean isIncome;
        public String category;
        public String originalText;

        public ParsedNotification(double amount, String merchant, boolean isIncome, String originalText) {
            this.amount = amount;
            this.merchant = merchant;
            this.isIncome = isIncome;
            this.originalText = originalText;
            this.category = CategoryDetector.detectCategory(merchant);
        }
    }

    public static ParsedNotification parse(String packageName, String title, String text) {
        String combined = title + " " + text;
        
        Log.d(TAG, "Parsing: " + combined);

        // Try GPay patterns
        if (packageName.contains("google") || packageName.contains("gpay")) {
            return parseGPay(combined);
        }

        // Try PhonePe patterns
        if (packageName.contains("phonepe")) {
            return parsePhonePe(combined);
        }

        // Try Paytm patterns
        if (packageName.contains("paytm")) {
            return parsePaytm(combined);
        }

        // Generic UPI parsing
        return parseGeneric(combined);
    }

    private static ParsedNotification parseGPay(String text) {
        // Check for "NAME paid you ₹X.XX" format (received money)
        Matcher paidYouMatcher = GPAY_PAID_YOU.matcher(text);
        if (paidYouMatcher.find()) {
            String sender = cleanMerchant(paidYouMatcher.group(1));
            double amount = parseAmount(paidYouMatcher.group(2));
            Log.d(TAG, "Matched GPAY_PAID_YOU: amount=" + amount + ", sender=" + sender);
            return new ParsedNotification(amount, sender, true, text);
        }
        
        // Check for "You paid NAME ₹X.XX" format (sent money)
        Matcher youPaidMatcher = GPAY_YOU_PAID.matcher(text);
        if (youPaidMatcher.find()) {
            String merchant = cleanMerchant(youPaidMatcher.group(1));
            double amount = parseAmount(youPaidMatcher.group(2));
            Log.d(TAG, "Matched GPAY_YOU_PAID: amount=" + amount + ", merchant=" + merchant);
            return new ParsedNotification(amount, merchant, false, text);
        }
        
        // Check for payment sent: "Paid ₹X to NAME" or "Sent ₹X to NAME"
        Matcher paidMatcher = GPAY_PAID.matcher(text);
        if (paidMatcher.find()) {
            double amount = parseAmount(paidMatcher.group(1));
            String merchant = cleanMerchant(paidMatcher.group(2));
            return new ParsedNotification(amount, merchant, false, text);
        }

        // Check for money received: "Received ₹X from NAME"
        Matcher receivedMatcher = GPAY_RECEIVED.matcher(text);
        if (receivedMatcher.find()) {
            double amount = parseAmount(receivedMatcher.group(1));
            String sender = cleanMerchant(receivedMatcher.group(2));
            return new ParsedNotification(amount, sender, true, text);
        }

        // Fallback: Check for any amount and determine type from keywords
        return parseGeneric(text);
    }

    private static ParsedNotification parsePhonePe(String text) {
        // Check for payment
        Matcher paidMatcher = PHONEPE_PAID.matcher(text);
        if (paidMatcher.find()) {
            double amount = parseAmount(paidMatcher.group(1));
            String merchant = extractMerchantFromText(text);
            return new ParsedNotification(amount, merchant, false, text);
        }

        // Check for received
        Matcher receivedMatcher = PHONEPE_RECEIVED.matcher(text);
        if (receivedMatcher.find()) {
            double amount = parseAmount(receivedMatcher.group(1));
            String sender = receivedMatcher.groupCount() > 1 ? 
                    cleanMerchant(receivedMatcher.group(2)) : "PhonePe";
            return new ParsedNotification(amount, sender, true, text);
        }

        return parseGeneric(text);
    }

    private static ParsedNotification parsePaytm(String text) {
        Matcher paidMatcher = PAYTM_PAID.matcher(text);
        if (paidMatcher.find()) {
            double amount = parseAmount(paidMatcher.group(1));
            String merchant = extractMerchantFromText(text);
            boolean isIncome = text.toLowerCase().contains("received") || 
                               text.toLowerCase().contains("credited");
            return new ParsedNotification(amount, merchant, isIncome, text);
        }

        return parseGeneric(text);
    }

    private static ParsedNotification parseGeneric(String text) {
        String lowerText = text.toLowerCase();
        
        // Determine if income or expense
        boolean isIncome = lowerText.contains("received") || 
                          lowerText.contains("credited") ||
                          lowerText.contains("got") ||
                          lowerText.contains("from");
        
        boolean isExpense = lowerText.contains("paid") || 
                           lowerText.contains("sent") ||
                           lowerText.contains("debited") ||
                           lowerText.contains("to ");

        // If both or neither found, skip (ambiguous)
        if ((isIncome && isExpense) || (!isIncome && !isExpense)) {
            // Try to determine from context
            if (lowerText.contains("payment successful") || lowerText.contains("transaction successful")) {
                isExpense = true;
                isIncome = false;
            } else {
                return null;
            }
        }

        // Extract amount
        double amount = 0;
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(text);
        if (amountMatcher.find()) {
            amount = parseAmount(amountMatcher.group(1));
        } else {
            amountMatcher = AMOUNT_PATTERN_2.matcher(text);
            if (amountMatcher.find()) {
                amount = parseAmount(amountMatcher.group(1));
            }
        }

        if (amount <= 0) {
            return null;
        }

        // Extract merchant
        String merchant = extractMerchantFromText(text);

        return new ParsedNotification(amount, merchant, isIncome && !isExpense, text);
    }

    private static double parseAmount(String amountStr) {
        if (amountStr == null) return 0;
        try {
            return Double.parseDouble(amountStr.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String cleanMerchant(String merchant) {
        if (merchant == null) return "UPI Payment";
        
        // Remove common suffixes
        merchant = merchant.trim()
                .replaceAll("\\s*via\\s+.*", "")
                .replaceAll("\\s*on\\s+.*", "")
                .replaceAll("\\s*using\\s+.*", "")
                .replaceAll("@.*", "") // Remove UPI IDs
                .trim();
        
        // Truncate if too long
        if (merchant.length() > 50) {
            merchant = merchant.substring(0, 47) + "...";
        }
        
        return merchant.isEmpty() ? "UPI Payment" : merchant;
    }

    private static String extractMerchantFromText(String text) {
        // Try to find merchant after "to" keyword
        Pattern toPattern = Pattern.compile("(?:to|at|for)\\s+([A-Za-z][A-Za-z0-9\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = toPattern.matcher(text);
        if (matcher.find()) {
            return cleanMerchant(matcher.group(1));
        }

        // Try to find merchant after "from" keyword
        Pattern fromPattern = Pattern.compile("from\\s+([A-Za-z][A-Za-z0-9\\s]+)", Pattern.CASE_INSENSITIVE);
        matcher = fromPattern.matcher(text);
        if (matcher.find()) {
            return cleanMerchant(matcher.group(1));
        }

        return "UPI Payment";
    }
}
