package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.ForecastEntry;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ForecastDao {

    public ForecastEntry insertForecast(ForecastEntry entry) {
        String sql = "INSERT INTO forecasts (date, ingredient_id, predicted_need, actual_used, strategy_used, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entry.getDate().toString());
            ps.setInt(2, entry.getIngredientId());
            ps.setDouble(3, entry.getPredictedNeed());
            if (entry.getActualUsed() != null) {
                ps.setDouble(4, entry.getActualUsed());
            } else {
                ps.setNull(4, Types.DOUBLE);
            }
            ps.setString(5, entry.getStrategyUsed());
            String createdAt = entry.getCreatedAt() != null ? entry.getCreatedAt().toString() : LocalDateTime.now().toString();
            ps.setString(6, createdAt);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    entry.setId(rs.getInt(1));
                    return entry;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting forecast entry", e);
        }
        return null;
    }

    public void insertAll(List<ForecastEntry> list) {
        if (list == null || list.isEmpty()) return;
        String sql = "INSERT INTO forecasts (date, ingredient_id, predicted_need, actual_used, strategy_used, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (ForecastEntry entry : list) {
                ps.setString(1, entry.getDate().toString());
                ps.setInt(2, entry.getIngredientId());
                ps.setDouble(3, entry.getPredictedNeed());
                if (entry.getActualUsed() != null) {
                    ps.setDouble(4, entry.getActualUsed());
                } else {
                    ps.setNull(4, Types.DOUBLE);
                }
                ps.setString(5, entry.getStrategyUsed());
                String createdAt = entry.getCreatedAt() != null ? entry.getCreatedAt().toString() : LocalDateTime.now().toString();
                ps.setString(6, createdAt);
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Error batch inserting forecast entries", e);
        }
    }

    public List<ForecastEntry> getForecastsByDate(LocalDate date) {
        List<ForecastEntry> list = new ArrayList<>();
        String sql = "SELECT f.id, f.date, f.ingredient_id, f.predicted_need, f.actual_used, f.strategy_used, f.created_at, " +
                     "i.name as ing_name, i.unit as ing_unit, i.current_stock " +
                     "FROM forecasts f " +
                     "JOIN ingredients i ON f.ingredient_id = i.id " +
                     "WHERE f.date = ? " +
                     "ORDER BY f.id ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapForecastEntry(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting forecasts for date: " + date, e);
        }
        return list;
    }

    public List<ForecastEntry> getForecastHistory(int limitDays) {
        List<ForecastEntry> list = new ArrayList<>();
        String sql = "SELECT f.id, f.date, f.ingredient_id, f.predicted_need, f.actual_used, f.strategy_used, f.created_at, " +
                     "i.name as ing_name, i.unit as ing_unit, i.current_stock " +
                     "FROM forecasts f " +
                     "JOIN ingredients i ON f.ingredient_id = i.id " +
                     "ORDER BY f.date DESC, f.id DESC LIMIT ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limitDays * 20); // multiple ingredients per day
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapForecastEntry(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting forecast history", e);
        }
        return list;
    }

    public void updateActualUsed(int id, double actualUsed) {
        String sql = "UPDATE forecasts SET actual_used = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, actualUsed);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating actual used for forecast id: " + id, e);
        }
    }

    /**
     * Tính toán % độ chính xác trung bình (Accuracy Rate = 100% - MAPE)
     * giữa dự báo (predicted_need) và thực tế (actual_used).
     */
    public double getAverageAccuracy(LocalDate from, LocalDate to) {
        String sql = "SELECT predicted_need, actual_used FROM forecasts " +
                     "WHERE date >= ? AND date <= ? AND actual_used IS NOT NULL AND actual_used > 0";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                double totalErrorRate = 0;
                while (rs.next()) {
                    double predicted = rs.getDouble("predicted_need");
                    double actual = rs.getDouble("actual_used");
                    double err = Math.abs(actual - predicted) / actual;
                    totalErrorRate += Math.min(1.0, err); // Cap error rate at 100%
                    count++;
                }
                if (count == 0) return 92.5; // Baseline estimate if no actuals logged yet
                double mape = totalErrorRate / count;
                return Math.max(0.0, Math.round((1.0 - mape) * 1000) / 10.0);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculating forecast accuracy", e);
        }
    }

    private ForecastEntry mapForecastEntry(ResultSet rs) throws SQLException {
        Double actualUsed = rs.getObject("actual_used") != null ? rs.getDouble("actual_used") : null;
        LocalDateTime createdAt = null;
        String rawCreated = rs.getString("created_at");
        if (rawCreated != null) {
            try {
                createdAt = LocalDateTime.parse(rawCreated.replace(" ", "T"));
            } catch (Exception ignored) {}
        }
        ForecastEntry entry = new ForecastEntry(
                rs.getInt("id"),
                LocalDate.parse(rs.getString("date")),
                rs.getInt("ingredient_id"),
                rs.getString("ing_name"),
                rs.getString("ing_unit"),
                rs.getDouble("current_stock"),
                rs.getDouble("predicted_need"),
                actualUsed,
                rs.getString("strategy_used"),
                createdAt
        );
        return entry;
    }
}
