package iu.canteen.model;

public class MenuItemSize {
    private int id;
    private int menuItemId;
    private String sizeLabel; // 'S', 'M', 'L'
    private int price;
    private double portionMultiplier;
    private int dailyLimit;

    public MenuItemSize() {
        this.portionMultiplier = 1.0;
    }

    public MenuItemSize(int id, int menuItemId, String sizeLabel, int price, double portionMultiplier, int dailyLimit) {
        this.id = id;
        this.menuItemId = menuItemId;
        this.sizeLabel = sizeLabel;
        this.price = price;
        this.portionMultiplier = portionMultiplier;
        this.dailyLimit = dailyLimit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMenuItemId() { return menuItemId; }
    public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

    public String getSizeLabel() { return sizeLabel; }
    public void setSizeLabel(String sizeLabel) { this.sizeLabel = sizeLabel; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public double getPortionMultiplier() { return portionMultiplier; }
    public void setPortionMultiplier(double portionMultiplier) { this.portionMultiplier = portionMultiplier; }

    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }

    @Override
    public String toString() {
        return "Size " + sizeLabel + " (" + String.format("%,d đ", price) + ")";
    }
}
