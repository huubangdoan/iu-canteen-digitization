package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.InventoryBatch;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BatchDao {

    public List<InventoryBatch> getAllBatches() {
        List<InventoryBatch> list = new ArrayList<>();
        String sql = "SELECT b.id, b.ingredient_id, b.quantity, b.remaining_quantity, b.cost_per_unit, " +
                     "b.received_date, b.expiry_date, b.batch_code, i.name as ing_name, i.unit as ing_unit " +
                     "FROM inventory_batches b " +
                     "JOIN ingredients i ON b.ingredient_id = i.id " +
                     "ORDER BY b.expiry_date ASC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapBatch(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting all batches", e);
        }
        return list;
    }

    public List<InventoryBatch> getBatchesByIngredient(int ingredientId) {
        List<InventoryBatch> list = new ArrayList<>();
        String sql = "SELECT b.id, b.ingredient_id, b.quantity, b.remaining_quantity, b.cost_per_unit, " +
                     "b.received_date, b.expiry_date, b.batch_code, i.name as ing_name, i.unit as ing_unit " +
                     "FROM inventory_batches b " +
                     "JOIN ingredients i ON b.ingredient_id = i.id " +
                     "WHERE b.ingredient_id = ? " +
                     "ORDER BY b.expiry_date ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBatch(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting batches for ingredient: " + ingredientId, e);
        }
        return list;
    }

    public List<InventoryBatch> getAvailableBatchesFIFO(int ingredientId) {
        List<InventoryBatch> list = new ArrayList<>();
        String sql = "SELECT b.id, b.ingredient_id, b.quantity, b.remaining_quantity, b.cost_per_unit, " +
                     "b.received_date, b.expiry_date, b.batch_code, i.name as ing_name, i.unit as ing_unit " +
                     "FROM inventory_batches b " +
                     "JOIN ingredients i ON b.ingredient_id = i.id " +
                     "WHERE b.ingredient_id = ? AND b.remaining_quantity > 0 " +
                     "ORDER BY b.expiry_date ASC, b.received_date ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBatch(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting FIFO batches for ingredient: " + ingredientId, e);
        }
        return list;
    }

    public List<InventoryBatch> getExpiringBatches(int withinDays) {
        List<InventoryBatch> list = new ArrayList<>();
        String sql = "SELECT b.id, b.ingredient_id, b.quantity, b.remaining_quantity, b.cost_per_unit, " +
                     "b.received_date, b.expiry_date, b.batch_code, i.name as ing_name, i.unit as ing_unit " +
                     "FROM inventory_batches b " +
                     "JOIN ingredients i ON b.ingredient_id = i.id " +
                     "WHERE b.remaining_quantity > 0 AND date(b.expiry_date) <= date('now', '+' || ? || ' day') " +
                     "ORDER BY b.expiry_date ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, withinDays);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBatch(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting expiring batches within " + withinDays + " days", e);
        }
        return list;
    }

    public InventoryBatch createBatch(int ingredientId, double quantity, double costPerUnit,
                                     LocalDate receivedDate, LocalDate expiryDate, String batchCode) {
        String sql = "INSERT INTO inventory_batches (ingredient_id, quantity, remaining_quantity, cost_per_unit, received_date, expiry_date, batch_code) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ingredientId);
            ps.setDouble(2, quantity);
            ps.setDouble(3, quantity); // ban đầu remaining = quantity
            ps.setDouble(4, costPerUnit);
            ps.setString(5, receivedDate.toString());
            ps.setString(6, expiryDate.toString());
            ps.setString(7, batchCode);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new InventoryBatch(id, ingredientId, quantity, quantity, costPerUnit, receivedDate, expiryDate, batchCode);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creating batch for ingredient: " + ingredientId, e);
        }
        return null;
    }

    public java.util.Optional<InventoryBatch> getBatchById(int id) {
        String sql = "SELECT b.id, b.ingredient_id, b.quantity, b.remaining_quantity, b.cost_per_unit, " +
                     "b.received_date, b.expiry_date, b.batch_code, i.name as ing_name, i.unit as ing_unit " +
                     "FROM inventory_batches b " +
                     "JOIN ingredients i ON b.ingredient_id = i.id " +
                     "WHERE b.id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return java.util.Optional.of(mapBatch(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting batch by id: " + id, e);
        }
        return java.util.Optional.empty();
    }

    public void deductQuantity(int batchId, double amount) {
        String sql = "UPDATE inventory_batches SET remaining_quantity = MAX(0, remaining_quantity - ?) WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, batchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deducting batch: " + batchId, e);
        }
    }

    private InventoryBatch mapBatch(ResultSet rs) throws SQLException {
        InventoryBatch b = new InventoryBatch(
                rs.getInt("id"),
                rs.getInt("ingredient_id"),
                rs.getDouble("quantity"),
                rs.getDouble("remaining_quantity"),
                rs.getDouble("cost_per_unit"),
                LocalDate.parse(rs.getString("received_date")),
                LocalDate.parse(rs.getString("expiry_date")),
                rs.getString("batch_code")
        );
        b.setIngredientName(rs.getString("ing_name"));
        b.setUnit(rs.getString("ing_unit"));
        return b;
    }
}
