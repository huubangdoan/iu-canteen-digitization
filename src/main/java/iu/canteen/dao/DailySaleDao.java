package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.DailySale;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailySaleDao {

    public void upsertSale(DailySale sale) {
        String sql = "INSERT INTO daily_sales (date, menu_item_id, size_label, quantity_sold, revenue) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT(date, menu_item_id, size_label) DO UPDATE SET " +
                     "quantity_sold = quantity_sold + excluded.quantity_sold, " +
                     "revenue = revenue + excluded.revenue";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sale.getDate().toString());
            ps.setInt(2, sale.getMenuItemId());
            ps.setString(3, sale.getSizeLabel());
            ps.setInt(4, sale.getQuantitySold());
            ps.setInt(5, sale.getRevenue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error upserting daily sale", e);
        }
    }

    public List<DailySale> getSalesByMenuItem(int menuItemId, int limitDays) {
        List<DailySale> list = new ArrayList<>();
        String sql = "SELECT s.id, s.date, s.menu_item_id, s.size_label, s.quantity_sold, s.revenue, m.name as dish_name " +
                     "FROM daily_sales s " +
                     "JOIN menu_items m ON s.menu_item_id = m.id " +
                     "WHERE s.menu_item_id = ? " +
                     "ORDER BY s.date DESC LIMIT ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            ps.setInt(2, limitDays);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDailySale(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting sales for menu item: " + menuItemId, e);
        }
        return list;
    }

    public List<DailySale> getSalesBetween(LocalDate from, LocalDate to) {
        List<DailySale> list = new ArrayList<>();
        String sql = "SELECT s.id, s.date, s.menu_item_id, s.size_label, s.quantity_sold, s.revenue, m.name as dish_name " +
                     "FROM daily_sales s " +
                     "JOIN menu_items m ON s.menu_item_id = m.id " +
                     "WHERE s.date >= ? AND s.date <= ? " +
                     "ORDER BY s.date ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDailySale(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting sales between dates", e);
        }
        return list;
    }

    /**
     * Tự động tổng hợp số lượng bán từ các đơn hàng thực tế (orders & order_items)
     * của một ngày vào bảng daily_sales.
     */
    public int aggregateSalesFromOrders(LocalDate date) {
        String sql = "INSERT INTO daily_sales (date, menu_item_id, size_label, quantity_sold, revenue) " +
                     "SELECT date(o.created_at) as sale_date, oi.menu_item_id, oi.size_label, " +
                     "       SUM(oi.quantity), SUM(oi.quantity * oi.unit_price) " +
                     "FROM orders o " +
                     "JOIN order_items oi ON o.id = oi.order_id " +
                     "WHERE date(o.created_at) = ? AND o.status != 'CANCELLED' " +
                     "GROUP BY oi.menu_item_id, oi.size_label " +
                     "ON CONFLICT(date, menu_item_id, size_label) DO UPDATE SET " +
                     "quantity_sold = excluded.quantity_sold, revenue = excluded.revenue";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error aggregating sales from orders for date: " + date, e);
        }
    }

    private DailySale mapDailySale(ResultSet rs) throws SQLException {
        DailySale s = new DailySale(
                rs.getInt("id"),
                LocalDate.parse(rs.getString("date")),
                rs.getInt("menu_item_id"),
                rs.getString("dish_name"),
                rs.getString("size_label"),
                rs.getInt("quantity_sold"),
                rs.getInt("revenue")
        );
        return s;
    }
}
