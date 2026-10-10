package iu.canteen.model;

import java.time.LocalDate;

public class WasteLog {
    private int id;
    private int ingredientId;
    private String ingredientName;
    private String unit;
    private double quantity;
    private String reason;
    private LocalDate date;
    private String recordedBy;

    public WasteLog() {}

    public WasteLog(int id, int ingredientId, double quantity, String reason, LocalDate date, String recordedBy) {
        this.id = id;
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.reason = reason;
        this.date = date;
        this.recordedBy = recordedBy;
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

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    @Override
    public String toString() {
        return (ingredientName != null ? ingredientName : "Nguyên liệu #" + ingredientId) + ": " + quantity + " (" + reason + ")";
    }
}
