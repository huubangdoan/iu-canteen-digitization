package iu.canteen.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MenuItem {
    private int id;
    private String name;
    private String category;
    private boolean available;
    private LocalDateTime createdAt;
    private List<MenuItemSize> sizes = new ArrayList<>();

    public MenuItem() {
        this.available = true;
    }

    public MenuItem(int id, String name, String category, boolean available) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.available = available;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<MenuItemSize> getSizes() { return sizes; }
    public void setSizes(List<MenuItemSize> sizes) { this.sizes = sizes; }

    public MenuItemSize getSize(String label) {
        if (sizes == null) return null;
        for (MenuItemSize s : sizes) {
            if (s.getSizeLabel().equalsIgnoreCase(label)) return s;
        }
        return sizes.isEmpty() ? null : sizes.get(0);
    }

    public int getBasePrice() {
        MenuItemSize m = getSize("M");
        if (m != null) return m.getPrice();
        return sizes.isEmpty() ? 0 : sizes.get(0).getPrice();
    }

    @Override
    public String toString() {
        return name + " (" + category + ")";
    }
}
