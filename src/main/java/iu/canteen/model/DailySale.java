package iu.canteen.model;

import java.time.LocalDate;

public class DailySale {
    private int id;
    private LocalDate date;
    private int menuItemId;
    private String menuItemName;
    private String sizeLabel;
    private int quantitySold;
    private int revenue;

    public DailySale() {}

    public DailySale(int id, LocalDate date, int menuItemId, String menuItemName, String sizeLabel, int quantitySold, int revenue) {
        this.id = id;
        this.date = date;
        this.menuItemId = menuItemId;
        this.menuItemName = menuItemName;
        this.sizeLabel = sizeLabel;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getMenuItemId() { return menuItemId; }
    public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

    public String getMenuItemName() { return menuItemName; }
    public void setMenuItemName(String menuItemName) { this.menuItemName = menuItemName; }

    public String getSizeLabel() { return sizeLabel; }
    public void setSizeLabel(String sizeLabel) { this.sizeLabel = sizeLabel; }

    public int getQuantitySold() { return quantitySold; }
    public void setQuantitySold(int quantitySold) { this.quantitySold = quantitySold; }

    public int getRevenue() { return revenue; }
    public void setRevenue(int revenue) { this.revenue = revenue; }
}
