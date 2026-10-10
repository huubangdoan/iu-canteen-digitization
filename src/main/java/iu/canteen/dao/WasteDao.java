package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.WasteLog;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WasteDao {

    public WasteLog logWaste(WasteLog log) {
        String sql = "INSERT INTO waste_logs (ingredient_id, quantity, reason, date, recorded_by) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getIngredientId());
            ps.setDouble(2, log.getQuantity());
            ps.setString(3, log.getReason());
            ps.setString(4, log.getDate().toString());
            ps.setString(5, log.getRecordedBy());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getInt(1));
                    return log;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error logging waste for ingredient: " + log.getIngredientId(), e);
        }
        return null;
    }

    public List<WasteLog> getAllWasteLogs() {
        List<WasteLog> list = new ArrayList<>();
        String sql = "SELECT w.id, w.ingredient_id, w.quantity, w.reason, w.date, w.recorded_by, " +
                     "i.name as ing_name, i.unit as ing_unit " +
                     "FROM waste_logs w " +
                     "JOIN ingredients i ON w.ingredient_id = i.id " +
                     "ORDER BY w.date DESC, w.id DESC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                WasteLog wl = new WasteLog(
                        rs.getInt("id"),
                        rs.getInt("ingredient_id"),
                        rs.getDouble("quantity"),
                        rs.getString("reason"),
                        LocalDate.parse(rs.getString("date")),
                        rs.getString("recorded_by")
                );
                wl.setIngredientName(rs.getString("ing_name"));
                wl.setUnit(rs.getString("ing_unit"));
                list.add(wl);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting all waste logs", e);
        }
        return list;
    }

    public List<WasteLog> getWasteLogsByIngredient(int ingredientId) {
        List<WasteLog> list = new ArrayList<>();
        String sql = "SELECT w.id, w.ingredient_id, w.quantity, w.reason, w.date, w.recorded_by, " +
                     "i.name as ing_name, i.unit as ing_unit " +
                     "FROM waste_logs w " +
                     "JOIN ingredients i ON w.ingredient_id = i.id " +
                     "WHERE w.ingredient_id = ? " +
                     "ORDER BY w.date DESC, w.id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WasteLog wl = new WasteLog(
                            rs.getInt("id"),
                            rs.getInt("ingredient_id"),
                            rs.getDouble("quantity"),
                            rs.getString("reason"),
                            LocalDate.parse(rs.getString("date")),
                            rs.getString("recorded_by")
                    );
                    wl.setIngredientName(rs.getString("ing_name"));
                    wl.setUnit(rs.getString("ing_unit"));
                    list.add(wl);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting waste logs for ingredient: " + ingredientId, e);
        }
        return list;
    }
}
