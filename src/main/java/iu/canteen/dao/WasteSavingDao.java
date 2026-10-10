package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.WasteSaving;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WasteSavingDao {

    public void upsert(WasteSaving ws) {
        String sql = "INSERT INTO waste_savings (date, total_leftovers_sold, food_waste_reduced_kg, cost_saved_vnd) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(date) DO UPDATE SET " +
                     "total_leftovers_sold = excluded.total_leftovers_sold, " +
                     "food_waste_reduced_kg = excluded.food_waste_reduced_kg, " +
                     "cost_saved_vnd = excluded.cost_saved_vnd";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ws.getDate().toString());
            ps.setInt(2, ws.getTotalLeftoversSold());
            ps.setDouble(3, ws.getFoodWasteReducedKg());
            ps.setInt(4, ws.getCostSavedVnd());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error upserting waste saving", e);
        }
    }

    public void recordSale(LocalDate date, int quantity, double wasteReducedKg, int costSavedVnd) {
        String sql = "INSERT INTO waste_savings (date, total_leftovers_sold, food_waste_reduced_kg, cost_saved_vnd) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(date) DO UPDATE SET " +
                     "total_leftovers_sold = total_leftovers_sold + excluded.total_leftovers_sold, " +
                     "food_waste_reduced_kg = food_waste_reduced_kg + excluded.food_waste_reduced_kg, " +
                     "cost_saved_vnd = cost_saved_vnd + excluded.cost_saved_vnd";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, quantity);
            ps.setDouble(3, wasteReducedKg);
            ps.setInt(4, costSavedVnd);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error recording waste saving sale for date: " + date, e);
        }
    }

    public Optional<WasteSaving> findByDate(LocalDate date) {
        String sql = "SELECT id, date, total_leftovers_sold, food_waste_reduced_kg, cost_saved_vnd " +
                     "FROM waste_savings WHERE date = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapWasteSaving(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding waste saving by date: " + date, e);
        }
        return Optional.empty();
    }

    public List<WasteSaving> getBetween(LocalDate from, LocalDate to) {
        List<WasteSaving> list = new ArrayList<>();
        String sql = "SELECT id, date, total_leftovers_sold, food_waste_reduced_kg, cost_saved_vnd " +
                     "FROM waste_savings WHERE date >= ? AND date <= ? ORDER BY date ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapWasteSaving(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting waste savings between dates", e);
        }
        return list;
    }

    public WasteSaving getSummary(LocalDate from, LocalDate to) {
        String sql = "SELECT COALESCE(SUM(total_leftovers_sold), 0) AS sum_sold, " +
                     "       COALESCE(SUM(food_waste_reduced_kg), 0.0) AS sum_kg, " +
                     "       COALESCE(SUM(cost_saved_vnd), 0) AS sum_vnd " +
                     "FROM waste_savings WHERE date >= ? AND date <= ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new WasteSaving(
                            0,
                            from,
                            rs.getInt("sum_sold"),
                            Math.round(rs.getDouble("sum_kg") * 100.0) / 100.0,
                            rs.getInt("sum_vnd")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculating waste savings summary", e);
        }
        return new WasteSaving(0, from, 0, 0.0, 0);
    }

    private WasteSaving mapWasteSaving(ResultSet rs) throws SQLException {
        return new WasteSaving(
                rs.getInt("id"),
                LocalDate.parse(rs.getString("date")),
                rs.getInt("total_leftovers_sold"),
                rs.getDouble("food_waste_reduced_kg"),
                rs.getInt("cost_saved_vnd")
        );
    }
}
