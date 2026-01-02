package com.rupex.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Monthly summary model
 */
public class MonthlySummary {
    
    @SerializedName("year")
    private int year;
    
    @SerializedName("month")
    private int month;
    
    @SerializedName("income")
    private double income;
    
    @SerializedName("expenses")
    private double expenses;
    
    @SerializedName("net_savings")
    private double netSavings;
    
    @SerializedName("transaction_count")
    private int transactionCount;
    
    public MonthlySummary() {}
    
    public MonthlySummary(double income, double expenses, int transactionCount) {
        this.income = income;
        this.expenses = expenses;
        this.transactionCount = transactionCount;
        this.netSavings = income - expenses;
    }

    // Getters
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public double getIncome() { return income; }
    public double getExpenses() { return expenses; }
    public double getNetSavings() { return netSavings; }
    public int getTransactionCount() { return transactionCount; }
    
    public double getSavingsRate() {
        if (income == 0) return 0;
        return (netSavings / income) * 100;
    }
}
