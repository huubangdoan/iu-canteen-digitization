package iu.canteen.model;

public class Recipe {
    private int id;
    private int menuItemId;
    private int ingredientId;
    private String ingredientName;
    private String unit;
    private double quantity;

    public Recipe() {}

    public Recipe(int id, int menuItemId, int ingredientId, double quantity) {
        this.id = id;
        this.menuItemId = menuItemId;
        this.ingredientId = ingredientId;
        this.quantity = quantity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMenuItemId() { return menuItemId; }
    public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return (ingredientName != null ? ingredientName : "Ingredient #" + ingredientId) + ": " + quantity + " " + (unit != null ? unit : "");
    }
}
