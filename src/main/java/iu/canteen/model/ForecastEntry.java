package iu.canteen.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ForecastEntry {
    private int id;
    private LocalDate date;
    private int ingredientId;
    private String ingredientName;
    private String unit;
    private double currentStock;
    private double predictedNeed;
    private Double actualUsed;
    private String strategyUsed;
    private LocalDateTime createdAt;

    public ForecastEntry() {}

    public ForecastEntry(int id, LocalDate date, int ingredientId, String ingredientName, String unit,
                         double currentStock, double predictedNeed, Double actualUsed, String strategyUsed, LocalDateTime createdAt) {
        this.id = id;
        this.date = date;
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.unit = unit;
        this.currentStock = currentStock;
        this.predictedNeed = predictedNeed;
        this.actualUsed = actualUsed;
        this.strategyUsed = strategyUsed;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getCurrentStock() { return currentStock; }
    public void setCurrentStock(double currentStock) { this.currentStock = currentStock; }

    public double getPredictedNeed() { return predictedNeed; }
    public void setPredictedNeed(double predictedNeed) { this.predictedNeed = predictedNeed; }

    public Double getActualUsed() { return actualUsed; }
    public void setActualUsed(Double actualUsed) { this.actualUsed = actualUsed; }

    public String getStrategyUsed() { return strategyUsed; }
    public void setStrategyUsed(String strategyUsed) { this.strategyUsed = strategyUsed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public double getSuggestedBuy() {
        return Math.max(0, Math.round((predictedNeed - currentStock) * 10) / 10.0);
    }
}
