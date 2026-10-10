package iu.canteen.model;

public class OrderItem {
    private int id;
    private int orderId;
    private int menuItemId;
    private String menuItemName;
    private String sizeLabel; // 'S', 'M', 'L'
    private int quantity;
    private int unitPrice;
    private double portionMultiplier;
    private String note;

    public OrderItem() {
        this.sizeLabel = "M";
        this.quantity = 1;
        this.portionMultiplier = 1.0;
        this.note = "";
    }

    public OrderItem(int id, int orderId, int menuItemId, String menuItemName, String sizeLabel,
                     int quantity, int unitPrice, double portionMultiplier, String note) {
        this.id = id;
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.menuItemName = menuItemName;
        this.sizeLabel = sizeLabel;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.portionMultiplier = portionMultiplier;
        this.note = note != null ? note : "";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getMenuItemId() { return menuItemId; }
    public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

    public String getMenuItemName() { return menuItemName; }
    public void setMenuItemName(String menuItemName) { this.menuItemName = menuItemName; }

    public String getSizeLabel() { return sizeLabel; }
    public void setSizeLabel(String sizeLabel) { this.sizeLabel = sizeLabel; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getUnitPrice() { return unitPrice; }
    public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }

    public double getPortionMultiplier() { return portionMultiplier; }
    public void setPortionMultiplier(double portionMultiplier) { this.portionMultiplier = portionMultiplier; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getSubtotal() { return unitPrice * quantity; }

    @Override
    public String toString() {
        return menuItemName + " (Size " + sizeLabel + ") x" + quantity;
    }
}
