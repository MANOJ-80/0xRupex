package com.rupex.app.sms.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Unit tests for SmsParser - verifies IOB and other bank SMS parsing
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SmsParserTest {

    @Test
    public void testIobDebitWithMerchant() {
        String sender = "IOBCHN";
        String body = "Your a/c XXXXX95 debited for payee P S GOVINDAS for Rs. 50.00 on 2025-09-10 11:57:35.297-IOB Avl. Bal is Rs.16,017.62";
        long timestamp = System.currentTimeMillis();
        
        ParsedSms result = SmsParser.parse(sender, body, timestamp);
        
        assertNotNull("Should parse IOB debit SMS", result);
        assertTrue("Should be valid", result.isValid());
        assertEquals("expense", result.getType());
        assertEquals(50.00, result.getAmount(), 0.01);
        assertEquals("95", result.getLast4Digits());
        assertEquals("P S GOVINDAS", result.getMerchant());
    }

    @Test
    public void testIobCreditUpi() {
        String sender = "IOBCHN";
        String body = "Your a/c no. XXXXX95 is credited by Rs.1000.00 on 2025-06-27 10:12:18.193, from GANESAN-vinayagamwater-1@okaxis(UPI Ref no 536198947755).Payer Remark - UPI -IOB";
        long timestamp = System.currentTimeMillis();
        
        ParsedSms result = SmsParser.parse(sender, body, timestamp);
        
        assertNotNull("Should parse IOB credit SMS", result);
        assertTrue("Should be valid", result.isValid());
        assertEquals("income", result.getType());
        assertEquals(1000.00, result.getAmount(), 0.01);
        assertEquals("95", result.getLast4Digits());
        // Should extract sender name
        assertNotNull("Should extract sender", result.getMerchant());
        assertTrue("Merchant should contain GANESAN", result.getMerchant().contains("GANESAN"));
    }

    @Test
    public void testIobDebitWithSwiggy() {
        String sender = "IOBCHN";
        String body = "Your a/c XXXXX95 debited for payee SWIGGY for Rs. 350.00 on 2025-01-01 14:30:00.000-IOB Avl. Bal is Rs.15,667.62";
        long timestamp = System.currentTimeMillis();
        
        ParsedSms result = SmsParser.parse(sender, body, timestamp);
        
        assertNotNull("Should parse IOB SWIGGY SMS", result);
        assertEquals("expense", result.getType());
        assertEquals(350.00, result.getAmount(), 0.01);
        assertEquals("SWIGGY", result.getMerchant());
        assertEquals("Food & Dining", result.getCategory());
    }

    @Test
    public void testIobDebitWithAmazon() {
        String sender = "IOBCHN";
        String body = "Your a/c XXXXX95 debited for payee AMAZON PAY INDIA PVT LT for Rs. 1499.00 on 2025-01-01";
        long timestamp = System.currentTimeMillis();
        
        ParsedSms result = SmsParser.parse(sender, body, timestamp);
        
        assertNotNull("Should parse IOB AMAZON SMS", result);
        assertEquals("expense", result.getType());
        assertEquals(1499.00, result.getAmount(), 0.01);
        assertTrue("Merchant should contain AMAZON", result.getMerchant().contains("AMAZON"));
        assertEquals("Shopping", result.getCategory());
    }

    @Test
    public void testUnicodeNormalization() {
        String sender = "IOBCHN";
        // Unicode styled text: 𝖸𝗈𝗎𝗋 𝖺/𝖼 XXXXX95 𝖽𝖾𝖻𝗂𝗍𝖾𝖽 𝖿𝗈𝗋 𝗉𝖺𝗒𝖾𝖾 Kalaimagal stores 𝖿𝗈𝗋 𝖱𝗌. 10.00
        // These are Mathematical Sans-Serif characters
        String body = "\uD835\uDDF8\uD835\uDDFC\uD835\uDE02\uD835\uDDFF \uD835\uDDEE/\uD835\uDDf0 XXXXX95 \uD835\uDDF1\uD835\uDDF2\uD835\uDDEF\uD835\uDDF6\uD835\uDE01\uD835\uDDF2\uD835\uDDF1 for payee Kalaimagal stores for Rs. 10.00 on 2025-01-01";
        long timestamp = System.currentTimeMillis();
        
        ParsedSms result = SmsParser.parse(sender, body, timestamp);
        
        assertNotNull("Should parse Unicode SMS after normalization", result);
        assertTrue("Should be valid", result.isValid());
        assertEquals("expense", result.getType());
        assertEquals(10.00, result.getAmount(), 0.01);
    }

    @Test
    public void testHdfcDebit() {
        String sender = "HDFC-BANK";
        String body = "Rs.5999.00 debited from A/c **4532 on 01-01-26 to FLIPKART. Avl bal Rs 25000";
        long timestamp = System.currentTimeMillis();
        
        ParsedSms result = SmsParser.parse(sender, body, timestamp);
        
        assertNotNull("Should parse HDFC debit SMS", result);
        assertEquals("expense", result.getType());
        assertEquals(5999.00, result.getAmount(), 0.01);
        assertEquals("4532", result.getLast4Digits());
        // HDFC simple pattern doesn't extract merchant from this format
        // That's ok - the transaction still parses with amount and account
    }

    @Test
    public void testCategoryDetection() {
        // Test various merchants and their categories
        String[][] testCases = {
            {"SWIGGY", "Food & Dining"},
            {"ZOMATO", "Food & Dining"},
            {"UBER INDIA", "Transport"},
            {"OLA CABS", "Transport"},
            {"AMAZON", "Shopping"},
            {"FLIPKART", "Shopping"},
            {"NETFLIX", "Entertainment"},
            {"SPOTIFY", "Entertainment"},
            {"APOLLO PHARMACY", "Health"},
            {"IRCTC", "Transport"},
            {"ELECTRICITY BILL", "Bills & Utilities"},
        };
        
        CategoryDetector detector = new CategoryDetector();
        
        for (String[] testCase : testCases) {
            String merchant = testCase[0];
            String expected = testCase[1];
            String actual = detector.detectCategory(merchant);
            assertEquals("Category for " + merchant, expected, actual);
        }
    }

    @Test
    public void testRealIobDebitVendolite() {
        String sender = "IOBCHN";
        String body = "Your a/c XXX8795 debited for payee VendoliteIndia05 for Rs. 40.00 on 2025-07-21, ref 520259501995.If not you, report to your bank immediately-IOB";
        long timestamp = System.currentTimeMillis();
        
        ParsedSms result = SmsParser.parse(sender, body, timestamp);
        
        assertNotNull("Should parse IOB Vendolite debit SMS", result);
        assertTrue("Should be valid", result.isValid());
        assertEquals("expense", result.getType());
        assertEquals(40.00, result.getAmount(), 0.01);
        assertEquals("VendoliteIndia05", result.getMerchant());
    }

    @Test
    public void testRealIobCreditArivazhagan() {
        String sender = "IOBCHN";
        String body = "Your a/c no. XXX8795 is credited by Rs.130.00 on 21-Jun-2025 09:15:10 PM, from ARIVAZHAGAN  KARTHIK-karivazhagan46@oksbi(UPI Ref no 517279781954).Payer Remark - UPI -IOB";
        long timestamp = System.currentTimeMillis();
        
        ParsedSms result = SmsParser.parse(sender, body, timestamp);
        
        assertNotNull("Should parse IOB credit SMS", result);
        assertTrue("Should be valid", result.isValid());
        assertEquals("income", result.getType());
        assertEquals(130.00, result.getAmount(), 0.01);
        // Should extract sender name
        assertNotNull("Should extract sender", result.getMerchant());
        assertTrue("Merchant should contain ARIVAZHAGAN", result.getMerchant().contains("ARIVAZHAGAN"));
    }
}
