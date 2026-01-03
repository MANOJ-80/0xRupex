package com.rupex.app.data.remote.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Update transaction request - only includes fields that are actually being changed.
 * This prevents overwriting existing values on the backend.
 */
public class UpdateTransactionRequest extends HashMap<String, Object> {
    
    public UpdateTransactionRequest() {
        super();
    }
    
    /**
     * Set type only if provided and not empty
     */
    public UpdateTransactionRequest setType(String type) {
        if (type != null && !type.isEmpty()) {
            put("type", type);
        }
        return this;
    }
    
    /**
     * Set category name only if provided and not empty
     */
    public UpdateTransactionRequest setCategoryName(String categoryName) {
        if (categoryName != null && !categoryName.isEmpty()) {
            put("categoryName", categoryName);
        }
        return this;
    }
    
    /**
     * Set notes - allow empty string to clear notes
     */
    public UpdateTransactionRequest setNotes(String notes) {
        if (notes != null) {
            put("notes", notes);
        }
        return this;
    }
    
    /**
     * Set amount only if positive
     */
    public UpdateTransactionRequest setAmount(double amount) {
        if (amount > 0) {
            put("amount", amount);
        }
        return this;
    }
    
    /**
     * Set merchant only if provided and not empty
     */
    public UpdateTransactionRequest setMerchant(String merchant) {
        if (merchant != null && !merchant.isEmpty()) {
            put("merchant", merchant);
        }
        return this;
    }
    
    /**
     * Set description only if provided and not empty
     */
    public UpdateTransactionRequest setDescription(String description) {
        if (description != null && !description.isEmpty()) {
            put("description", description);
        }
        return this;
    }
    
    /**
     * Set transaction date/time (as ISO 8601 string for backend)
     */
    public UpdateTransactionRequest setTransactionAt(long timestamp) {
        if (timestamp > 0) {
            // Convert to ISO 8601 format for backend
            java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            put("transactionAt", isoFormat.format(new java.util.Date(timestamp)));
        }
        return this;
    }
}
