package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.Leftover;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LeftoverDao {

    private static final String BASE_SELECT =
            "SELECT l.id, l.menu_item_id, l.size_label, l.initial_quantity, l.remaining_quantity, " +
            "       l.discount_percent, l.status, l.date, " +
            "       m.name AS menu_item_name, " +
            "       COALESCE(s.price, 0) AS original_price " +
            "FROM leftovers l " +
            "JOIN menu_items m ON l.menu_item_id = m.id " +
            "LEFT JOIN menu_item_sizes s ON s.menu_item_id = l.menu_item_id AND s.size_label = l.size_label ";

    public Leftover insert(Leftover leftover) {
        String sql = "INSERT INTO leftovers (menu_item_id, size_label, initial_quantity, remaining_quantity, discount_percent, status, date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, leftover.getMenuItemId());
            ps.setString(2, leftover.getSizeLabel());
            ps.setInt(3, leftover.getInitialQuantity());
            ps.setInt(4, leftover.getRemainingQuantity());
            ps.setInt(5, leftover.getDiscountPercent());
            ps.setString(6, leftover.getStatus());
            ps.setString(7, leftover.getDate().toString());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    leftover.setId(rs.getInt(1));
                }
            }
            return leftover;
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting leftover item", e);
        }
    }

    public Optional<Leftover> findById(int id) {
        String sql = BASE_SELECT + "WHERE l.id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapLeftover(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding leftover by id: " + id, e);
        }
        return Optional.empty();
    }

    public List<Leftover> getActiveLeftovers(LocalDate date) {
        List<Leftover> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE l.status = 'ACTIVE' AND l.remaining_quantity > 0 AND l.date = ? ORDER BY l.id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLeftover(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting active leftovers for date: " + date, e);
        }
        return list;
    }

    public List<Leftover> getAllActiveLeftovers() {
        List<Leftover> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE l.status = 'ACTIVE' AND l.remaining_quantity > 0 ORDER BY l.date DESC, l.id DESC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapLeftover(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting all active leftovers", e);
        }
        return list;
    }

    public List<Leftover> getLeftoversByDate(LocalDate date) {
        List<Leftover> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE l.date = ? ORDER BY l.id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLeftover(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting leftovers for date: " + date, e);
        }
        return list;
    }

    public List<Leftover> getAll() {
        List<Leftover> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY l.date DESC, l.id DESC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapLeftover(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting all leftovers", e);
        }
        return list;
    }

    public void updateRemainingAndStatus(int id, int remainingQuantity, String status) {
        String sql = "UPDATE leftovers SET remaining_quantity = ?, status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, remainingQuantity);
            ps.setString(2, status);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating leftover remaining and status for id: " + id, e);
        }
    }

    public void updateStatus(int id, String status) {
        String sql = "UPDATE leftovers SET status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating status for leftover id: " + id, e);
        }
    }

    public int countByStatus(LocalDate date, String status) {
        String sql = "SELECT COUNT(*) FROM leftovers WHERE date = ? AND status = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting leftovers by status", e);
        }
        return 0;
    }

    /**
     * Tính tổng số phần thừa ghi nhận trong N ngày qua cho một món ăn cụ thể.
     */
    public int getTotalLeftoversForMenuItem(int menuItemId, LocalDate from, LocalDate to) {
        String sql = "SELECT COALESCE(SUM(initial_quantity), 0) FROM leftovers " +
                     "WHERE menu_item_id = ? AND date >= ? AND date <= ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            ps.setString(2, from.toString());
            ps.setString(3, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting total leftovers for menu item: " + menuItemId, e);
        }
        return 0;
    }

    private Leftover mapLeftover(ResultSet rs) throws SQLException {
        return new Leftover(
                rs.getInt("id"),
                rs.getInt("menu_item_id"),
                rs.getString("menu_item_name"),
                rs.getString("size_label"),
                rs.getInt("original_price"),
                rs.getInt("initial_quantity"),
                rs.getInt("remaining_quantity"),
                rs.getInt("discount_percent"),
                rs.getString("status"),
                LocalDate.parse(rs.getString("date"))
        );
    }
}
