package com.dtcteam.pypos.model;

public class CartItem {
    private int itemId;
    private String name;
    private String sku;
    private double unitPrice;
    private int quantity;
    private double subtotal;
    private int maxQty;
    private boolean isService;

    public CartItem(int itemId, String name, String sku, double unitPrice, int quantity, double subtotal, int maxQty) {
        this.itemId = itemId;
        this.name = name;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.maxQty = maxQty;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public int getMaxQty() { return maxQty; }
    public void setMaxQty(int maxQty) { this.maxQty = maxQty; }
    public boolean isService() { return isService; }
    public void setService(boolean service) { isService = service; }
}
