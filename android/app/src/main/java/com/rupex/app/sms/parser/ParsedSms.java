package com.rupex.app.sms.parser;

/**
 * Represents a parsed SMS transaction
 */
public class ParsedSms {
    
    private String type;           // "income" or "expense"
    private double amount;
    private String last4Digits;
    private String referenceId;
    private String merchant;
    private Double balance;
    private String bankName;
    private String smsHash;
    private double confidence;
    private String category;       // Auto-detected category
    private String categoryIcon;
    private String categoryColor;

    public ParsedSms() {
        this.confidence = 0.0;
    }

    public boolean isValid() {
        return type != null && amount > 0;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getLast4Digits() { return last4Digits; }
    public void setLast4Digits(String last4Digits) { this.last4Digits = last4Digits; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getSmsHash() { return smsHash; }
    public void setSmsHash(String smsHash) { this.smsHash = smsHash; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }
    
    public String getCategoryColor() { return categoryColor; }
    public void setCategoryColor(String categoryColor) { this.categoryColor = categoryColor; }

    @Override
    public String toString() {
        return "ParsedSms{" +
                "type='" + type + '\'' +
                ", amount=" + amount +
                ", last4Digits='" + last4Digits + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", merchant='" + merchant + '\'' +
                ", category='" + category + '\'' +
                ", balance=" + balance +
                ", bankName='" + bankName + '\'' +
                '}';
    }
}
