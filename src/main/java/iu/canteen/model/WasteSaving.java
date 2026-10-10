package iu.canteen.model;

import java.time.LocalDate;

/**
 * Đại diện cho bản ghi thống kê tiết kiệm thức ăn và giảm lãng phí theo ngày.
 */
public class WasteSaving {
    private int id;
    private LocalDate date;
    private int totalLeftoversSold;
    private double foodWasteReducedKg;
    private int costSavedVnd;

    public WasteSaving() {
        this.date = LocalDate.now();
    }

    public WasteSaving(int id, LocalDate date, int totalLeftoversSold, double foodWasteReducedKg, int costSavedVnd) {
        this.id = id;
        this.date = date;
        this.totalLeftoversSold = totalLeftoversSold;
        this.foodWasteReducedKg = foodWasteReducedKg;
        this.costSavedVnd = costSavedVnd;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getTotalLeftoversSold() { return totalLeftoversSold; }
    public void setTotalLeftoversSold(int totalLeftoversSold) { this.totalLeftoversSold = totalLeftoversSold; }

    public double getFoodWasteReducedKg() { return foodWasteReducedKg; }
    public void setFoodWasteReducedKg(double foodWasteReducedKg) { this.foodWasteReducedKg = foodWasteReducedKg; }

    public int getCostSavedVnd() { return costSavedVnd; }
    public void setCostSavedVnd(int costSavedVnd) { this.costSavedVnd = costSavedVnd; }

    @Override
    public String toString() {
        return "WasteSaving{" +
                "id=" + id +
                ", date=" + date +
                ", totalLeftoversSold=" + totalLeftoversSold +
                ", foodWasteReducedKg=" + foodWasteReducedKg +
                ", costSavedVnd=" + costSavedVnd +
                '}';
    }
}
