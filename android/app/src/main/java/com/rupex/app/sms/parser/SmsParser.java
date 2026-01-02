package com.rupex.app.sms.parser;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMS Parser for extracting transaction data from bank SMS messages.
 * 
 * Supports major Indian banks and UPI services.
 * PRIVACY: Only structured data is extracted - raw SMS is never stored.
 */
public class SmsParser {

    private static final String TAG = "SmsParser";

    // ============================================
    // DEBIT PATTERNS (Expenses)
    // ============================================
    
    private static final Pattern[] DEBIT_PATTERNS = {
            // IOB: "Your a/c XXXXX95 debited for payee P S GOVINDAS for Rs. 50.00 on 2025-09-10"
            // This captures merchant name properly
            Pattern.compile(
                    "(?:Your\\s+)?a/c\\s*[xX*]*(\\d{2,4})\\s*debited\\s*for\\s*payee\\s+(.+?)\\s+for\\s+Rs\\.?\\s*([\\d,]+\\.?\\d*)",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // HDFC: "Rs.499.00 debited from A/c **4532"
            Pattern.compile(
                    "Rs\\.?\\s*([\\d,]+\\.?\\d*)\\s*(?:debited|withdrawn|spent|paid)\\s*(?:from)?\\s*(?:A/c|a/c|Acct?)?\\s*\\*{0,2}(\\d{4})",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // SBI: "debited by Rs.500 from A/c XXXX1234"
            Pattern.compile(
                    "(?:debited|withdrawn)\\s*(?:by|for)?\\s*Rs\\.?\\s*([\\d,]+\\.?\\d*).*?(?:A/c|a/c)\\s*[xX*]+(\\d{4})",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // ICICI: "Rs 1,500 debited from your Account"
            Pattern.compile(
                    "Rs\\.?\\s*([\\d,]+\\.?\\d*)\\s*(?:debited|spent|paid)\\s*(?:from)?\\s*(?:your)?\\s*(?:Account|Card)",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // UPI: "Paid Rs.250 to merchant@upi"
            Pattern.compile(
                    "(?:Paid|Sent|Transferred)\\s*Rs\\.?\\s*([\\d,]+\\.?\\d*)\\s*(?:to|for)\\s*([^\\s]+)",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // Credit Card: "spent Rs.1234 at AMAZON"
            Pattern.compile(
                    "(?:spent|charged|transaction)\\s*(?:of)?\\s*Rs\\.?\\s*([\\d,]+\\.?\\d*).*?(?:at|on)\\s+([A-Za-z0-9\\s]+)",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // HDFC with merchant: "Rs.5999.00 debited from A/c **4532 on 01-01-26 to FLIPKART"
            Pattern.compile(
                    "Rs\\.?\\s*([\\d,]+\\.?\\d*)\\s*debited.*?(?:A/c|a/c)\\s*\\**(\\d{4}).*?(?:to|at|for)\\s+([A-Za-z0-9\\s]+?)(?:\\.\\s*|\\s+Avl)",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // Generic: "INR 500.00 debited"
            Pattern.compile(
                    "(?:INR|Rs\\.?)\\s*([\\d,]+\\.?\\d*)\\s*(?:has been)?\\s*(?:debited|deducted|withdrawn)",
                    Pattern.CASE_INSENSITIVE
            )
    };

    // ============================================
    // CREDIT PATTERNS (Income)
    // ============================================
    
    private static final Pattern[] CREDIT_PATTERNS = {
            // IOB UPI Credit: "Your a/c no. XXXXX95 is credited by Rs.1000.00 on DATE, from SENDER-upi@bank"
            Pattern.compile(
                    "a/c\\s*(?:no\\.?)?\\s*[xX*]*(\\d{2,4})\\s*is\\s*credited\\s*by\\s*Rs\\.?\\s*([\\d,]+\\.?\\d*).*?from\\s+([^(]+)",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // HDFC: "Rs.5000.00 credited to A/c **4532"
            Pattern.compile(
                    "Rs\\.?\\s*([\\d,]+\\.?\\d*)\\s*(?:credited|deposited|received)\\s*(?:to|in)?\\s*(?:A/c|a/c|Acct?)?\\s*\\*{0,2}(\\d{4})",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // UPI: "Received Rs.500 from sender@upi"
            Pattern.compile(
                    "(?:Received|Got|Credited)\\s*Rs\\.?\\s*([\\d,]+\\.?\\d*)\\s*(?:from)\\s*([^\\s]+)",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // Salary: "Salary of Rs.50000 credited"
            Pattern.compile(
                    "(?:Salary|Payment)\\s*(?:of)?\\s*Rs\\.?\\s*([\\d,]+\\.?\\d*)\\s*(?:has been)?\\s*(?:credited|deposited)",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // Generic: "credited with Rs.1000"
            Pattern.compile(
                    "(?:credited|deposited)\\s*(?:with)?\\s*(?:INR|Rs\\.?)\\s*([\\d,]+\\.?\\d*)",
                    Pattern.CASE_INSENSITIVE
            ),
            
            // Refund: "Refund of Rs.499 credited"
            Pattern.compile(
                    "(?:Refund|Cashback)\\s*(?:of)?\\s*Rs\\.?\\s*([\\d,]+\\.?\\d*)\\s*(?:has been)?\\s*(?:credited|processed)",
                    Pattern.CASE_INSENSITIVE
            )
    };

    // ============================================
    // EXTRACTION PATTERNS
    // ============================================
    
    // Reference ID patterns
    private static final Pattern[] REFERENCE_PATTERNS = {
            Pattern.compile("(?:UPI\\s*[Rr]ef|Ref(?:erence)?(?:\\s*No)?|Txn\\s*ID?)[\\s:]*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{12,})")  // 12+ digit number as fallback
    };

    // Balance pattern
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
            "(?:Avl?\\.?\\s*Bal(?:ance)?|Balance|Bal)[\\s:]*(?:INR|Rs\\.?)?\\s*([\\d,]+\\.?\\d*)",
            Pattern.CASE_INSENSITIVE
    );

    // Account last 4 digits
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(
            "(?:A/c|Acct?|Account|Card)\\s*[xX*]*\\s*(\\d{4})",
            Pattern.CASE_INSENSITIVE
    );

    // Merchant extraction
    private static final Pattern[] MERCHANT_PATTERNS = {
            Pattern.compile("(?:at|to|from|for|@)\\s+([A-Za-z0-9@._\\-]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Info:\\s*([^.]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("VPA:\\s*([^\\s]+)", Pattern.CASE_INSENSITIVE)
    };

    // ============================================
    // PUBLIC API
    // ============================================

    /**
     * Check if sender is a bank SMS
     */
    public static boolean isBankSms(String sender) {
        return BankConfig.isBankSender(sender);
    }

    /**
     * Parse SMS and extract transaction data
     */
    public static ParsedSms parse(String sender, String smsBody, long timestamp) {
        if (smsBody == null || smsBody.isEmpty()) {
            return null;
        }

        // Normalize Unicode styled text (some banks use fancy Unicode characters)
        String normalizedBody = normalizeUnicode(smsBody);

        ParsedSms result = new ParsedSms();
        result.setBankName(BankConfig.getBankName(sender));

        // Try to match debit patterns first
        boolean matched = tryMatchPatterns(normalizedBody, DEBIT_PATTERNS, result, "expense");

        // If no debit match, try credit patterns
        if (!matched) {
            matched = tryMatchPatterns(normalizedBody, CREDIT_PATTERNS, result, "income");
        }

        if (!matched) {
            Log.d(TAG, "No pattern matched for SMS");
            return null;
        }

        // Extract additional fields
        result.setReferenceId(extractReferenceId(smsBody));
        result.setBalance(extractBalance(smsBody));
        
        if (result.getLast4Digits() == null) {
            result.setLast4Digits(extractAccountLast4(smsBody));
        }
        
        if (result.getMerchant() == null) {
            result.setMerchant(extractMerchant(smsBody));
        }
        
        // Detect category from merchant name
        String category = CategoryDetector.detectCategory(result.getMerchant());
        result.setCategory(category);
        result.setCategoryIcon(CategoryDetector.getCategoryIcon(category));
        result.setCategoryColor(CategoryDetector.getCategoryColor(category));

        // Generate SMS hash for deduplication
        result.setSmsHash(generateSmsHash(sender, result.getAmount(), result.getReferenceId(), timestamp));
        result.setConfidence(0.90);

        return result;
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private static boolean tryMatchPatterns(String smsBody, Pattern[] patterns, ParsedSms result, String type) {
        for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = patterns[i];
            Matcher matcher = pattern.matcher(smsBody);
            if (matcher.find()) {
                result.setType(type);
                
                // IOB debit pattern (index 0 for debit) has special group order:
                // Group 1 = last digits, Group 2 = merchant, Group 3 = amount
                if (i == 0 && type.equals("expense") && matcher.groupCount() >= 3) {
                    result.setLast4Digits(matcher.group(1));
                    result.setMerchant(cleanMerchant(matcher.group(2)));
                    String amountStr = matcher.group(3);
                    if (amountStr != null) {
                        result.setAmount(parseAmount(amountStr));
                    }
                    return true;
                }
                
                // IOB credit UPI pattern (index 0 for income) has:
                // Group 1 = last digits, Group 2 = amount, Group 3 = sender (merchant)
                if (i == 0 && type.equals("income") && matcher.groupCount() >= 3) {
                    result.setLast4Digits(matcher.group(1));
                    String amountStr = matcher.group(2);
                    if (amountStr != null) {
                        result.setAmount(parseAmount(amountStr));
                    }
                    result.setMerchant(cleanMerchant(matcher.group(3)));
                    return true;
                }
                
                // HDFC with merchant pattern (index 6) has: amount, account, merchant
                if (i == 6 && type.equals("expense") && matcher.groupCount() >= 3) {
                    String amountStr = matcher.group(1);
                    if (amountStr != null) {
                        result.setAmount(parseAmount(amountStr));
                    }
                    result.setLast4Digits(matcher.group(2));
                    result.setMerchant(cleanMerchant(matcher.group(3)));
                    return true;
                }
                
                // Standard pattern: Group 1 = amount, Group 2 = account or merchant
                String amountStr = matcher.group(1);
                if (amountStr != null) {
                    result.setAmount(parseAmount(amountStr));
                }

                // Try to extract account/merchant from group 2 if available
                if (matcher.groupCount() >= 2) {
                    String group2 = matcher.group(2);
                    if (group2 != null) {
                        if (group2.matches("\\d{2,4}")) {
                            result.setLast4Digits(group2);
                        } else {
                            result.setMerchant(cleanMerchant(group2));
                        }
                    }
                }

                return true;
            }
        }
        return false;
    }

    private static double parseAmount(String amountStr) {
        try {
            // Remove commas and parse
            return Double.parseDouble(amountStr.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String extractReferenceId(String smsBody) {
        for (Pattern pattern : REFERENCE_PATTERNS) {
            Matcher matcher = pattern.matcher(smsBody);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static Double extractBalance(String smsBody) {
        Matcher matcher = BALANCE_PATTERN.matcher(smsBody);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String extractAccountLast4(String smsBody) {
        Matcher matcher = ACCOUNT_PATTERN.matcher(smsBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractMerchant(String smsBody) {
        for (Pattern pattern : MERCHANT_PATTERNS) {
            Matcher matcher = pattern.matcher(smsBody);
            if (matcher.find()) {
                return cleanMerchant(matcher.group(1));
            }
        }
        return null;
    }

    private static String cleanMerchant(String merchant) {
        if (merchant == null) return null;
        
        // Clean up merchant name
        return merchant
                .replaceAll("@[a-z]+$", "")  // Remove UPI handle suffix
                .replaceAll("[._-]+$", "")    // Remove trailing special chars
                .trim();
    }

    private static String generateSmsHash(String sender, double amount, String refId, long timestamp) {
        try {
            String data = sender + "-" + amount + "-" + (refId != null ? refId : String.valueOf(timestamp));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 32); // First 32 chars
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }

    /**
     * Normalize styled Unicode characters to ASCII equivalents.
     * Some SMS messages use mathematical/styled Unicode characters that look like
     * normal letters but are different code points (e.g., 𝖸𝗈𝗎𝗋 instead of Your).
     */
    private static String normalizeUnicode(String text) {
        if (text == null) return null;
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int codePoint = text.codePointAt(i);
            
            // Handle surrogate pairs for characters outside BMP
            if (Character.isHighSurrogate(c) && i + 1 < text.length()) {
                char low = text.charAt(i + 1);
                if (Character.isLowSurrogate(low)) {
                    codePoint = Character.toCodePoint(c, low);
                    i++; // Skip the low surrogate
                }
            }
            
            // Mathematical Sans-Serif Bold (𝗔-𝗭: U+1D5D4-U+1D5ED, 𝗮-𝘇: U+1D5EE-U+1D607)
            // Mathematical Sans-Serif (𝖠-𝖹: U+1D5A0-U+1D5B9, 𝖺-𝗓: U+1D5BA-U+1D5D3)
            char normalized = normalizeCodePoint(codePoint);
            result.append(normalized);
        }
        
        return result.toString();
    }

    private static char normalizeCodePoint(int codePoint) {
        // Mathematical Sans-Serif (U+1D5A0 to U+1D5D3)
        // Uppercase A-Z: U+1D5A0 to U+1D5B9
        if (codePoint >= 0x1D5A0 && codePoint <= 0x1D5B9) {
            return (char) ('A' + (codePoint - 0x1D5A0));
        }
        // Lowercase a-z: U+1D5BA to U+1D5D3
        if (codePoint >= 0x1D5BA && codePoint <= 0x1D5D3) {
            return (char) ('a' + (codePoint - 0x1D5BA));
        }
        
        // Mathematical Sans-Serif Bold (U+1D5D4 to U+1D607)
        // Uppercase A-Z: U+1D5D4 to U+1D5ED
        if (codePoint >= 0x1D5D4 && codePoint <= 0x1D5ED) {
            return (char) ('A' + (codePoint - 0x1D5D4));
        }
        // Lowercase a-z: U+1D5EE to U+1D607
        if (codePoint >= 0x1D5EE && codePoint <= 0x1D607) {
            return (char) ('a' + (codePoint - 0x1D5EE));
        }
        
        // Mathematical Sans-Serif Italic (U+1D608 to U+1D63B)
        if (codePoint >= 0x1D608 && codePoint <= 0x1D621) {
            return (char) ('A' + (codePoint - 0x1D608));
        }
        if (codePoint >= 0x1D622 && codePoint <= 0x1D63B) {
            return (char) ('a' + (codePoint - 0x1D622));
        }
        
        // Mathematical Bold (U+1D400 to U+1D433)
        if (codePoint >= 0x1D400 && codePoint <= 0x1D419) {
            return (char) ('A' + (codePoint - 0x1D400));
        }
        if (codePoint >= 0x1D41A && codePoint <= 0x1D433) {
            return (char) ('a' + (codePoint - 0x1D41A));
        }
        
        // If not a styled character, return as-is (or space for unmapped surrogates)
        if (codePoint > 0xFFFF) {
            return ' '; // Replace unmapped supplementary characters with space
        }
        return (char) codePoint;
    }
}
