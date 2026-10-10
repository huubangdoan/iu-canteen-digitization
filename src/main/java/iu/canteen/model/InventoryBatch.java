package iu.canteen.model;

import java.time.LocalDate;

public class InventoryBatch {
    private int id;
    private int ingredientId;
    private String ingredientName;
    private String unit;
    private double quantity;
    private double remainingQuantity;
    private double costPerUnit;
    private LocalDate receivedDate;
    private LocalDate expiryDate;
    private String batchCode;

    public InventoryBatch() {}

    public InventoryBatch(int id, int ingredientId, double quantity, double remainingQuantity,
                          double costPerUnit, LocalDate receivedDate, LocalDate expiryDate, String batchCode) {
        this.id = id;
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.remainingQuantity = remainingQuantity;
        this.costPerUnit = costPerUnit;
        this.receivedDate = receivedDate;
        this.expiryDate = expiryDate;
        this.batchCode = batchCode;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public double getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(double remainingQuantity) { this.remainingQuantity = remainingQuantity; }

    public double getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(double costPerUnit) { this.costPerUnit = costPerUnit; }

    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon(int daysThreshold) {
        return expiryDate != null && !isExpired() && expiryDate.isBefore(LocalDate.now().plusDays(daysThreshold));
    }
}
