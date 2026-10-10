package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.Recipe;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecipeDao {

    public List<Recipe> getRecipesByMenuItemId(int menuItemId) {
        List<Recipe> list = new ArrayList<>();
        String sql = "SELECT r.id, r.menu_item_id, r.ingredient_id, r.quantity, i.name as ing_name, i.unit as ing_unit " +
                     "FROM recipes r " +
                     "JOIN ingredients i ON r.ingredient_id = i.id " +
                     "WHERE r.menu_item_id = ? ORDER BY i.name ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Recipe r = new Recipe(
                            rs.getInt("id"),
                            rs.getInt("menu_item_id"),
                            rs.getInt("ingredient_id"),
                            rs.getDouble("quantity")
                    );
                    r.setIngredientName(rs.getString("ing_name"));
                    r.setUnit(rs.getString("ing_unit"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting recipes for dish: " + menuItemId, e);
        }
        return list;
    }

    public void saveRecipe(int menuItemId, int ingredientId, double quantity) {
        String sql = "INSERT INTO recipes (menu_item_id, ingredient_id, quantity) VALUES (?, ?, ?) " +
                     "ON CONFLICT(menu_item_id, ingredient_id) DO UPDATE SET quantity = excluded.quantity";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            ps.setInt(2, ingredientId);
            ps.setDouble(3, quantity);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving recipe for dish: " + menuItemId + ", ingredient: " + ingredientId, e);
        }
    }

    public void deleteRecipe(int menuItemId, int ingredientId) {
        String sql = "DELETE FROM recipes WHERE menu_item_id = ? AND ingredient_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            ps.setInt(2, ingredientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting recipe", e);
        }
    }
}
