package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.Order;
import iu.canteen.model.OrderItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDao {

    public Order createOrder(Order order) {
        String insertOrderSql = "INSERT INTO orders (user_id, customer_email, pickup_time, note, total_amount, status, created_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO order_items (order_id, menu_item_id, size_label, quantity, unit_price, portion_multiplier, note) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int orderId;
                try (PreparedStatement psOrder = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                    if (order.getUserId() != null) psOrder.setInt(1, order.getUserId());
                    else psOrder.setNull(1, Types.INTEGER);

                    psOrder.setString(2, order.getCustomerEmail());
                    psOrder.setString(3, order.getPickupTime());
                    psOrder.setString(4, order.getNote());
                    psOrder.setInt(5, order.getTotalAmount());
                    psOrder.setString(6, order.getStatus().name());
                    psOrder.setString(7, (order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now()).toString());
                    psOrder.executeUpdate();

                    try (ResultSet rs = psOrder.getGeneratedKeys()) {
                        if (rs.next()) {
                            orderId = rs.getInt(1);
                            order.setId(orderId);
                        } else {
                            throw new SQLException("Creating order failed, no ID obtained.");
                        }
                    }
                }

                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    try (PreparedStatement psItem = conn.prepareStatement(insertItemSql, Statement.RETURN_GENERATED_KEYS)) {
                        for (OrderItem item : order.getItems()) {
                            psItem.setInt(1, orderId);
                            psItem.setInt(2, item.getMenuItemId());
                            psItem.setString(3, item.getSizeLabel());
                            psItem.setInt(4, item.getQuantity());
                            psItem.setInt(5, item.getUnitPrice());
                            psItem.setDouble(6, item.getPortionMultiplier());
                            psItem.setString(7, item.getNote());
                            psItem.executeUpdate();

                            try (ResultSet rs = psItem.getGeneratedKeys()) {
                                if (rs.next()) {
                                    item.setId(rs.getInt(1));
                                    item.setOrderId(orderId);
                                }
                            }
                        }
                    }
                }

                conn.commit();
                return order;
            } catch (Exception ex) {
                conn.rollback();
                throw new RuntimeException("Error executing order transaction", ex);
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error creating order", e);
        }
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, user_id, customer_email, pickup_time, note, total_amount, status, created_at " +
                     "FROM orders ORDER BY created_at DESC, id DESC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                orders.add(mapOrder(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting all orders", e);
        }

        for (Order o : orders) {
            o.setItems(getItemsByOrderId(o.getId()));
        }
        return orders;
    }

    public List<Order> getOrdersByCustomer(String email) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, user_id, customer_email, pickup_time, note, total_amount, status, created_at " +
                     "FROM orders WHERE lower(customer_email) = lower(?) ORDER BY created_at DESC, id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting orders for email: " + email, e);
        }

        for (Order o : orders) {
            o.setItems(getItemsByOrderId(o.getId()));
        }
        return orders;
    }

    public Optional<Order> getOrderById(int orderId) {
        String sql = "SELECT id, user_id, customer_email, pickup_time, note, total_amount, status, created_at " +
                     "FROM orders WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order o = mapOrder(rs);
                    o.setItems(getItemsByOrderId(o.getId()));
                    return Optional.of(o);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting order by id: " + orderId, e);
        }
        return Optional.empty();
    }

    public void updateOrderStatus(int orderId, Order.Status status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating status for order: " + orderId, e);
        }
    }

    public List<OrderItem> getItemsByOrderId(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT oi.id, oi.order_id, oi.menu_item_id, oi.size_label, oi.quantity, " +
                     "oi.unit_price, oi.portion_multiplier, oi.note, m.name as dish_name " +
                     "FROM order_items oi " +
                     "JOIN menu_items m ON oi.menu_item_id = m.id " +
                     "WHERE oi.order_id = ? ORDER BY oi.id ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem it = new OrderItem(
                            rs.getInt("id"),
                            rs.getInt("order_id"),
                            rs.getInt("menu_item_id"),
                            rs.getString("dish_name"),
                            rs.getString("size_label"),
                            rs.getInt("quantity"),
                            rs.getInt("unit_price"),
                            rs.getDouble("portion_multiplier"),
                            rs.getString("note")
                    );
                    items.add(it);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting items for order: " + orderId, e);
        }
        return items;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int uid = rs.getInt("user_id");
        Integer userId = rs.wasNull() ? null : uid;
        String email = rs.getString("customer_email");
        String pickup = rs.getString("pickup_time");
        String note = rs.getString("note");
        int total = rs.getInt("total_amount");
        Order.Status st = Order.Status.fromString(rs.getString("status"));
        LocalDateTime created = LocalDateTime.now();
        String createdStr = rs.getString("created_at");
        if (createdStr != null) {
            try {
                created = LocalDateTime.parse(createdStr.replace(" ", "T"));
            } catch (Exception ignored) {}
        }
        return new Order(id, userId, email, pickup, note, total, st, created);
    }
}
