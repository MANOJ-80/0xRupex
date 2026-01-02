package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Paginated API response wrapper for list endpoints
 * Format: { success: true, data: [...], pagination: {...} }
 */
public class PaginatedApiResponse<T> {
    
    @SerializedName("success")
    public boolean success;
    
    @SerializedName("data")
    public List<T> data;
    
    @SerializedName("pagination")
    public Pagination pagination;
    
    @SerializedName("message")
    public String message;
    
    @SerializedName("error")
    public String error;

    public boolean isSuccess() { return success; }
    public List<T> getData() { return data; }
    public Pagination getPagination() { return pagination; }
    public String getMessage() { return message; }
    public String getError() { return error; }
    
    public static class Pagination {
        @SerializedName("page")
        private int page;
        
        @SerializedName("limit")
        private int limit;
        
        @SerializedName("total")
        private int total;
        
        @SerializedName("totalPages")
        private int totalPages;

        public int getPage() { return page; }
        public int getLimit() { return limit; }
        public int getTotal() { return total; }
        public int getTotalPages() { return totalPages; }
    }
}
