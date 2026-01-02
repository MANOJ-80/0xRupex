package com.rupex.app.sms.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Detects transaction category from merchant name using keyword matching.
 * 
 * Categories match the backend category IDs/names.
 */
public class CategoryDetector {

    // Category name constants (matching backend)
    public static final String FOOD_DINING = "Food & Dining";
    public static final String GROCERIES = "Groceries";
    public static final String TRANSPORT = "Transport";
    public static final String SHOPPING = "Shopping";
    public static final String ENTERTAINMENT = "Entertainment";
    public static final String BILLS_UTILITIES = "Bills & Utilities";
    public static final String HEALTH = "Health";
    public static final String PERSONAL_CARE = "Personal Care";
    public static final String EDUCATION = "Education";
    public static final String TRAVEL = "Travel";
    public static final String TRANSFERS = "Transfers";
    public static final String OTHER = "Other";

    // Keyword patterns for each category
    private static final Map<String, Pattern> CATEGORY_PATTERNS = new HashMap<>();

    static {
        // Food & Dining - Restaurants, food delivery, cafes
        CATEGORY_PATTERNS.put(FOOD_DINING, Pattern.compile(
                "SWIGGY|ZOMATO|DOMINOS|PIZZA|MCDONALDS|KFC|BURGER|STARBUCKS|CAFE|" +
                "RESTAURANT|FOOD|DINE|DINING|BIRYANI|CHAAYOS|SUBWAY|DUNKIN|" +
                "BARBEQUE|BBQ|HALDIRAM|SARAVANA|BHAVAN|MESS|CANTEEN|EATERY|" +
                "BASKIN|ICE.?CREAM|NATURALS|AMUL|CHAAT|BAKERY|SWEET|MITHAI",
                Pattern.CASE_INSENSITIVE
        ));

        // Groceries - Supermarkets, grocery stores, kirana
        CATEGORY_PATTERNS.put(GROCERIES, Pattern.compile(
                "BIGBASKET|BLINKIT|ZEPTO|DUNZO|GROFERS|JIOMART|DMART|RELIANCE.?FRESH|" +
                "MORE|SPAR|STAR.?BAZAAR|NATURE.?BASKET|EASYDAY|SUPER.?MARKET|" +
                "SPENCER|KIRANA|GROCERY|PROVISION|VEGETABLES|FRUITS|MILK|DAIRY|" +
                "RATNADEEP|METRO.?CASH|COSTCO|LULU",
                Pattern.CASE_INSENSITIVE
        ));

        // Transport - Cabs, auto, metro, fuel, parking
        CATEGORY_PATTERNS.put(TRANSPORT, Pattern.compile(
                "UBER|OLA|RAPIDO|METRO|IRCTC|REDBUS|ABSBUS|MAKEMYTRIP|" +
                "PETROL|DIESEL|FUEL|HP.?PETROL|BHARAT.?PETROL|INDIAN.?OIL|" +
                "FASTAG|TOLL|PARKING|GARAGE|AUTO|TAXI|CAB|" +
                "GOIBIBO|CLEARTRIP|YATRA|BUS|TRAIN|RAILWAY",
                Pattern.CASE_INSENSITIVE
        ));

        // Shopping - E-commerce, retail, fashion
        CATEGORY_PATTERNS.put(SHOPPING, Pattern.compile(
                "AMAZON|FLIPKART|MYNTRA|AJIO|NYKAA|MEESHO|SNAPDEAL|SHOPCLUES|" +
                "TATA.?CLQ|FIRST.?CRY|CROMA|RELIANCE.?DIGITAL|VIJAY.?SALES|" +
                "DECATHLON|PUMA|NIKE|ADIDAS|ZARA|H.?M|UNIQLO|LIFESTYLE|" +
                "WESTSIDE|PANTALOONS|MAX|TRENDS|SHOPPERS.?STOP|CENTRAL|" +
                "LENSKART|TITAN|TANISHQ|KALYAN|MALABAR|JEWEL|WATCH",
                Pattern.CASE_INSENSITIVE
        ));

        // Entertainment - Movies, OTT, games, events
        CATEGORY_PATTERNS.put(ENTERTAINMENT, Pattern.compile(
                "NETFLIX|HOTSTAR|PRIME.?VIDEO|SPOTIFY|GAANA|WYNK|JIOSAVN|" +
                "BOOKMYSHOW|PVR|INOX|CINEPOLIS|MOVIE|CINEMA|THEATRE|" +
                "PLAYSTATION|XBOX|STEAM|GOOGLE.?PLAY|APP.?STORE|" +
                "DREAM11|MPL|GAMES|GAMING|CONCERT|EVENT|TICKET",
                Pattern.CASE_INSENSITIVE
        ));

        // Bills & Utilities - Electricity, water, gas, phone, internet
        CATEGORY_PATTERNS.put(BILLS_UTILITIES, Pattern.compile(
                "ELECTRICITY|BESCOM|CESC|TATA.?POWER|ADANI.?POWER|RELIANCE.?ENERGY|" +
                "JIO.?FIBER|AIRTEL|VODAFONE|BSNL|ACT.?FIBERNET|HATHWAY|TATA.?SKY|" +
                "GAS|INDANE|BHARAT.?GAS|HP.?GAS|WATER|SEWAGE|" +
                "BILL.?PAYMENT|RECHARGE|DTH|BROADBAND|INTERNET|POSTPAID|PREPAID",
                Pattern.CASE_INSENSITIVE
        ));

        // Health - Pharmacy, hospital, doctor, insurance
        CATEGORY_PATTERNS.put(HEALTH, Pattern.compile(
                "APOLLO|MEDPLUS|NETMEDS|PHARMEASY|1MG|TATA.?1MG|" +
                "HOSPITAL|CLINIC|DOCTOR|DIAGNOSTIC|LAB|PATHOLOGY|" +
                "PHARMACY|MEDICAL|MEDICINE|HEALTH|WELLNESS|" +
                "GYM|FITNESS|CULT|GOLD.?GYM|YOGA|INSURANCE|" +
                "PRACTO|LYBRATE|MFINE|THYROCARE",
                Pattern.CASE_INSENSITIVE
        ));

        // Personal Care - Salon, spa, beauty
        CATEGORY_PATTERNS.put(PERSONAL_CARE, Pattern.compile(
                "SALON|SPA|PARLOUR|BEAUTY|BARBER|HAIRCUT|" +
                "LAKME|NATURALS|JAWED.?HABIB|LOOKS|BODYCRAFT|" +
                "URBAN.?COMPANY|URBAN.?CLAP|GROOMING",
                Pattern.CASE_INSENSITIVE
        ));

        // Education - Schools, courses, books
        CATEGORY_PATTERNS.put(EDUCATION, Pattern.compile(
                "SCHOOL|COLLEGE|UNIVERSITY|TUITION|COACHING|" +
                "BYJU|UNACADEMY|VEDANTU|COURSERA|UDEMY|" +
                "BOOKS|STATIONERY|LIBRARY|EDUCATION|ACADEMIC|" +
                "UPGRAD|SIMPLILEARN|GREAT.?LEARNING",
                Pattern.CASE_INSENSITIVE
        ));

        // Travel - Hotels, flights, vacation
        CATEGORY_PATTERNS.put(TRAVEL, Pattern.compile(
                "HOTEL|OYO|TREEBO|FABHOTEL|TAJ|OBEROI|ITC|MARRIOTT|" +
                "AIRBNB|HOSTEL|RESORT|LODGE|BOOKING.?COM|" +
                "INDIGO|SPICEJET|AIRINDIA|VISTARA|AKASA|" +
                "FLIGHT|AIRLINE|AIRPORT|VISA|PASSPORT",
                Pattern.CASE_INSENSITIVE
        ));

        // Transfers - Person-to-person, bank transfers
        CATEGORY_PATTERNS.put(TRANSFERS, Pattern.compile(
                "IMPS|NEFT|RTGS|UPI|TRANSFER|SENT.?TO|PAID.?TO",
                Pattern.CASE_INSENSITIVE
        ));
    }

    /**
     * Detect category from merchant name
     * 
     * @param merchant The merchant/payee name
     * @return Category name or "Other" if not detected
     */
    public static String detectCategory(String merchant) {
        if (merchant == null || merchant.isEmpty()) {
            return OTHER;
        }

        // Normalize for matching
        String normalized = merchant.toUpperCase().replaceAll("[^A-Z0-9]", " ");

        // Check each category pattern
        for (Map.Entry<String, Pattern> entry : CATEGORY_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(normalized).find()) {
                return entry.getKey();
            }
        }

        // Special case: UPI payments to individuals (names typically have 2-3 parts)
        if (merchant.matches("^[A-Z\\s]{2,30}$") && merchant.split("\\s+").length >= 2) {
            return TRANSFERS;
        }

        return OTHER;
    }

    /**
     * Get category icon name (Material icon name)
     */
    public static String getCategoryIcon(String category) {
        switch (category) {
            case FOOD_DINING: return "restaurant";
            case GROCERIES: return "local_grocery_store";
            case TRANSPORT: return "directions_car";
            case SHOPPING: return "shopping_bag";
            case ENTERTAINMENT: return "movie";
            case BILLS_UTILITIES: return "receipt";
            case HEALTH: return "local_hospital";
            case PERSONAL_CARE: return "spa";
            case EDUCATION: return "school";
            case TRAVEL: return "flight";
            case TRANSFERS: return "swap_horiz";
            default: return "category";
        }
    }

    /**
     * Get category color (hex)
     */
    public static String getCategoryColor(String category) {
        switch (category) {
            case FOOD_DINING: return "#EF4444";     // Red
            case GROCERIES: return "#84CC16";       // Lime
            case TRANSPORT: return "#F59E0B";       // Amber
            case SHOPPING: return "#8B5CF6";        // Purple
            case ENTERTAINMENT: return "#EC4899";   // Pink
            case BILLS_UTILITIES: return "#3B82F6"; // Blue
            case HEALTH: return "#10B981";          // Emerald
            case PERSONAL_CARE: return "#F472B6";   // Pink
            case EDUCATION: return "#6366F1";       // Indigo
            case TRAVEL: return "#14B8A6";          // Teal
            case TRANSFERS: return "#64748B";       // Slate
            default: return "#6B7280";              // Gray
        }
    }
}
