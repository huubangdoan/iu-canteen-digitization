package iu.canteen.model;

import java.time.LocalDateTime;

public class Ingredient {
    private int id;
    private String name;
    private String unit;
    private int shelfLifeDays;
    private double minStock;
    private double currentStock;
    private LocalDateTime createdAt;

    public Ingredient() {}

    public Ingredient(int id, String name, String unit, int shelfLifeDays, double minStock, double currentStock) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.shelfLifeDays = shelfLifeDays;
        this.minStock = minStock;
        this.currentStock = currentStock;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(int shelfLifeDays) { this.shelfLifeDays = shelfLifeDays; }

    public double getMinStock() { return minStock; }
    public void setMinStock(double minStock) { this.minStock = minStock; }

    public double getCurrentStock() { return currentStock; }
    public void setCurrentStock(double currentStock) { this.currentStock = currentStock; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isLowStock() {
        return currentStock <= minStock;
    }

    @Override
    public String toString() {
        return name + " (" + unit + ")";
    }
}
