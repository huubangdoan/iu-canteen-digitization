package iu.canteen.model;

import java.time.LocalDate;

public class Leftover {
    private int id;
    private int menuItemId;
    private String menuItemName;
    private String sizeLabel;
    private int originalPrice;
    private int initialQuantity;
    private int remainingQuantity;
    private int discountPercent; // Ví dụ: 30 = giảm 30%
    private String status; // ACTIVE, SOLD_OUT, DONATED, EXPIRED
    private LocalDate date;

    public Leftover() {
        this.sizeLabel = "M";
        this.discountPercent = 30;
        this.status = "ACTIVE";
        this.date = LocalDate.now();
    }

    public Leftover(int id, int menuItemId, String menuItemName, String sizeLabel, int originalPrice,
                    int initialQuantity, int remainingQuantity, int discountPercent, String status, LocalDate date) {
        this.id = id;
        this.menuItemId = menuItemId;
        this.menuItemName = menuItemName;
        this.sizeLabel = sizeLabel;
        this.originalPrice = originalPrice;
        this.initialQuantity = initialQuantity;
        this.remainingQuantity = remainingQuantity;
        this.discountPercent = discountPercent;
        this.status = status;
        this.date = date;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMenuItemId() { return menuItemId; }
    public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

    public String getMenuItemName() { return menuItemName; }
    public void setMenuItemName(String menuItemName) { this.menuItemName = menuItemName; }

    public String getSizeLabel() { return sizeLabel; }
    public void setSizeLabel(String sizeLabel) { this.sizeLabel = sizeLabel; }

    public int getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(int originalPrice) { this.originalPrice = originalPrice; }

    public int getInitialQuantity() { return initialQuantity; }
    public void setInitialQuantity(int initialQuantity) { this.initialQuantity = initialQuantity; }

    public int getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(int remainingQuantity) { this.remainingQuantity = remainingQuantity; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getDiscountedPrice() {
        return (int) Math.round(originalPrice * (100 - discountPercent) / 100.0);
    }
}
