package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.MenuItem;
import iu.canteen.model.MenuItemSize;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuDao {

    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT id, name, category, available, created_at FROM menu_items ORDER BY id ASC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapMenuItem(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting all menu items", e);
        }

        for (MenuItem item : items) {
            item.setSizes(getSizesByMenuItemId(item.getId()));
        }
        return items;
    }

    public Optional<MenuItem> getMenuItemById(int id) {
        String sql = "SELECT id, name, category, available, created_at FROM menu_items WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MenuItem item = mapMenuItem(rs);
                    item.setSizes(getSizesByMenuItemId(item.getId()));
                    return Optional.of(item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting menu item by id: " + id, e);
        }
        return Optional.empty();
    }

    public List<MenuItemSize> getSizesByMenuItemId(int menuItemId) {
        List<MenuItemSize> sizes = new ArrayList<>();
        String sql = "SELECT id, menu_item_id, size_label, price, portion_multiplier, daily_limit " +
                     "FROM menu_item_sizes WHERE menu_item_id = ? ORDER BY price ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sizes.add(mapMenuItemSize(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting sizes for menu item: " + menuItemId, e);
        }
        return sizes;
    }

    public MenuItem createMenuItem(String name, String category, boolean available) {
        String sql = "INSERT INTO menu_items (name, category, available) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setBoolean(3, available);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    MenuItem item = new MenuItem(id, name, category, available);
                    item.setCreatedAt(LocalDateTime.now());
                    return item;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creating menu item: " + name, e);
        }
        return null;
    }

    public MenuItemSize addMenuItemSize(int menuItemId, String sizeLabel, int price, double portionMultiplier, int dailyLimit) {
        String sql = "INSERT INTO menu_item_sizes (menu_item_id, size_label, price, portion_multiplier, daily_limit) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, menuItemId);
            ps.setString(2, sizeLabel.toUpperCase());
            ps.setInt(3, price);
            ps.setDouble(4, portionMultiplier);
            ps.setInt(5, dailyLimit);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new MenuItemSize(id, menuItemId, sizeLabel.toUpperCase(), price, portionMultiplier, dailyLimit);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error adding size " + sizeLabel + " for item: " + menuItemId, e);
        }
        return null;
    }

    public void updateMenuItemAvailability(int id, boolean available) {
        String sql = "UPDATE menu_items SET available = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, available);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating availability for menu item: " + id, e);
        }
    }

    public void updateMenuItemSizePrice(int sizeId, int price) {
        String sql = "UPDATE menu_item_sizes SET price = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, price);
            ps.setInt(2, sizeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating price for size id: " + sizeId, e);
        }
    }

    private MenuItem mapMenuItem(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String category = rs.getString("category");
        boolean available = rs.getBoolean("available");
        MenuItem m = new MenuItem(id, name, category, available);
        String createdStr = rs.getString("created_at");
        if (createdStr != null) {
            try {
                m.setCreatedAt(LocalDateTime.parse(createdStr.replace(" ", "T")));
            } catch (Exception ignored) {}
        }
        return m;
    }

    private MenuItemSize mapMenuItemSize(ResultSet rs) throws SQLException {
        return new MenuItemSize(
                rs.getInt("id"),
                rs.getInt("menu_item_id"),
                rs.getString("size_label"),
                rs.getInt("price"),
                rs.getDouble("portion_multiplier"),
                rs.getInt("daily_limit")
        );
    }
}
