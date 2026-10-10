package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.Ingredient;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IngredientDao {

    public List<Ingredient> getAll() {
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT id, name, unit, shelf_life_days, min_stock, current_stock, created_at FROM ingredients ORDER BY name ASC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapIngredient(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting all ingredients", e);
        }
        return list;
    }

    public Optional<Ingredient> getById(int id) {
        String sql = "SELECT id, name, unit, shelf_life_days, min_stock, current_stock, created_at FROM ingredients WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapIngredient(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting ingredient by id: " + id, e);
        }
        return Optional.empty();
    }

    public Optional<Ingredient> findByName(String name) {
        String sql = "SELECT id, name, unit, shelf_life_days, min_stock, current_stock, created_at FROM ingredients WHERE lower(name) = lower(?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapIngredient(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding ingredient by name: " + name, e);
        }
        return Optional.empty();
    }

    public Ingredient create(String name, String unit, int shelfLifeDays, double minStock, double initialStock) {
        String sql = "INSERT INTO ingredients (name, unit, shelf_life_days, min_stock, current_stock) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, unit);
            ps.setInt(3, shelfLifeDays);
            ps.setDouble(4, minStock);
            ps.setDouble(5, initialStock);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new Ingredient(id, name, unit, shelfLifeDays, minStock, initialStock);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creating ingredient: " + name, e);
        }
        return null;
    }

    public void updateStock(int id, double newStock) {
        String sql = "UPDATE ingredients SET current_stock = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newStock);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating stock for ingredient: " + id, e);
        }
    }

    public void addStock(int id, double deltaStock) {
        String sql = "UPDATE ingredients SET current_stock = current_stock + ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, deltaStock);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error adding stock for ingredient: " + id, e);
        }
    }

    private Ingredient mapIngredient(ResultSet rs) throws SQLException {
        Ingredient ing = new Ingredient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("unit"),
                rs.getInt("shelf_life_days"),
                rs.getDouble("min_stock"),
                rs.getDouble("current_stock")
        );
        String createdStr = rs.getString("created_at");
        if (createdStr != null) {
            try {
                ing.setCreatedAt(LocalDateTime.parse(createdStr.replace(" ", "T")));
            } catch (Exception ignored) {}
        }
        return ing;
    }
}
