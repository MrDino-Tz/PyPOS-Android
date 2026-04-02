package com.dtcteam.pypos.model;

public class DashboardStats {
    private int totalItems;
    private int lowStockItems;
    private double todaySales;
    private int todayTransactions;

    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public int getLowStockItems() { return lowStockItems; }
    public void setLowStockItems(int lowStockItems) { this.lowStockItems = lowStockItems; }
    public double getTodaySales() { return todaySales; }
    public void setTodaySales(double todaySales) { this.todaySales = todaySales; }
    public int getTodayTransactions() { return todayTransactions; }
    public void setTodayTransactions(int todayTransactions) { this.todayTransactions = todayTransactions; }
}
