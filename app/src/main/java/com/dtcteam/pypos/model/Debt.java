package com.dtcteam.pypos.model;

public class Debt {
    private int id;
    private Integer saleId;
    private String personName;
    private String phoneNumber;
    private String description;
    private double amount;
    private double remainingAmount;
    private String status;
    private String type;
    private String dueDate;
    private String createdAt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getSaleId() { return saleId; }
    public void setSaleId(Integer saleId) { this.saleId = saleId; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(double remainingAmount) { this.remainingAmount = remainingAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isOverdue() {
        if (dueDate == null || dueDate.isEmpty() || "paid".equals(status)) return false;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date due = sdf.parse(dueDate);
            java.util.Date today = new java.util.Date();
            return due != null && due.before(today);
        } catch (Exception e) {
            return false;
        }
    }
}
