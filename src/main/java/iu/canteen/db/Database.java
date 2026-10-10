package iu.canteen.db;

import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý kết nối và khởi tạo cơ sở dữ liệu SQLite.
 */
public final class Database {
    private static final Logger logger = Logger.getLogger(Database.class.getName());
    private static String dbUrl = "jdbc:sqlite:iu_canteen.db";
    private static Connection connection;

    private Database() {}

    /** Cấu hình DB URL (hữu ích cho unit test với in-memory db: jdbc:sqlite::memory:) */
    public static synchronized void setDbUrl(String url) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
            connection = null;
        }
        dbUrl = url;
    }

    /** Lấy kết nối duy nhất hoặc tạo mới */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return connection;
    }

    /** Khởi tạo schema và nạp dữ liệu mẫu ban đầu nếu DB mới */
    public static synchronized void init() {
        try {
            Connection conn = getConnection();
            // 1. Chạy schema.sql
            executeScript(conn, "/schema.sql");

            // 2. Kiểm tra nếu chưa có user thì nạp seed.sql và tạo tài khoản mặc định
            if (isTableEmpty(conn, "users")) {
                executeScript(conn, "/seed.sql");
                seedDefaultUsers(conn);
                seedSampleOrders(conn);
                logger.info("IU Canteen Database initialized and seeded successfully.");
            } else {
                logger.info("IU Canteen Database connected. Tables already exist.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize SQLite Database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static boolean isTableEmpty(Connection conn, String tableName) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            return true;
        }
    }

    private static void executeScript(Connection conn, String resourcePath) throws Exception {
        try (InputStream is = Database.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.warning("Resource script not found: " + resourcePath);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    int commentIdx = line.indexOf("--");
                    if (commentIdx >= 0) {
                        line = line.substring(0, commentIdx);
                    }
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    sb.append(line).append(" ");
                    if (line.endsWith(";")) {
                        String sql = sb.toString().trim();
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(sql);
                        } catch (SQLException ex) {
                            logger.log(Level.WARNING, "Error executing SQL: " + sql, ex);
                        }
                        sb.setLength(0);
                    }
                }
            }
        }
    }

    private static void seedDefaultUsers(Connection conn) throws SQLException {
        String insertSql = "INSERT INTO users (full_name, email, student_id, password, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            // Admin
            ps.setString(1, "IU Canteen Admin");
            ps.setString(2, "admin@iu.edu.vn");
            ps.setString(3, "ADM001");
            ps.setString(4, BCrypt.hashpw("admin123", BCrypt.gensalt(10)));
            ps.setString(5, "ADMIN");
            ps.executeUpdate();

            // Vendor
            ps.setString(1, "Bếp trưởng Canteen");
            ps.setString(2, "vendor@iu.edu.vn");
            ps.setString(3, "VEN001");
            ps.setString(4, BCrypt.hashpw("vendor123", BCrypt.gensalt(10)));
            ps.setString(5, "VENDOR");
            ps.executeUpdate();

            // Student 1
            ps.setString(1, "Nguyễn Văn Sinh Viên");
            ps.setString(2, "student@iu.edu.vn");
            ps.setString(3, "ITCSIU21001");
            ps.setString(4, BCrypt.hashpw("student123", BCrypt.gensalt(10)));
            ps.setString(5, "STUDENT");
            ps.executeUpdate();

            // Student 2
            ps.setString(1, "Nguyễn Văn An");
            ps.setString(2, "an.nguyen@iu.edu.vn");
            ps.setString(3, "ITCSIU21002");
            ps.setString(4, BCrypt.hashpw("student123", BCrypt.gensalt(10)));
            ps.setString(5, "STUDENT");
            ps.executeUpdate();

            // Student 3
            ps.setString(1, "Trần Văn Bình");
            ps.setString(2, "binh.tran@iu.edu.vn");
            ps.setString(3, "ITCSIU21003");
            ps.setString(4, BCrypt.hashpw("student123", BCrypt.gensalt(10)));
            ps.setString(5, "STUDENT");
            ps.executeUpdate();
        }
    }

    private static void seedSampleOrders(Connection conn) throws SQLException {
        // Đơn mẫu tương thích với Store.java seed
        String insertOrder = "INSERT INTO orders (user_id, customer_email, pickup_time, note, total_amount, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, datetime('now', ?))";
        String insertItem = "INSERT INTO order_items (order_id, menu_item_id, size_label, quantity, unit_price, portion_multiplier, note) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pOrder = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pItem = conn.prepareStatement(insertItem)) {

            // Đơn 1: Completed
            pOrder.setInt(1, 3);
            pOrder.setString(2, "student@iu.edu.vn");
            pOrder.setString(3, "11:30");
            pOrder.setString(4, "Ít cơm");
            pOrder.setInt(5, 60000);
            pOrder.setString(6, "COMPLETED");
            pOrder.setString(7, "-50 minute");
            pOrder.executeUpdate();
            int o1 = getGeneratedKey(pOrder);
            addItem(pItem, o1, 1, "M", 1, 35000, 1.0, "Ít cơm");
            addItem(pItem, o1, 6, "M", 1, 25000, 1.0, "");

            // Đơn 2: Pending
            pOrder.setInt(1, 4);
            pOrder.setString(2, "an.nguyen@iu.edu.vn");
            pOrder.setString(3, "11:45");
            pOrder.setString(4, "Không hành");
            pOrder.setInt(5, 90000);
            pOrder.setString(6, "PENDING");
            pOrder.setString(7, "-12 minute");
            pOrder.executeUpdate();
            int o2 = getGeneratedKey(pOrder);
            addItem(pItem, o2, 3, "M", 2, 45000, 1.0, "Không hành");

            // Đơn 3: Accepted
            pOrder.setInt(1, 5);
            pOrder.setString(2, "binh.tran@iu.edu.vn");
            pOrder.setString(3, "11:50");
            pOrder.setString(4, "Gọi giúp mình khi xong");
            pOrder.setInt(5, 60000);
            pOrder.setString(6, "ACCEPTED");
            pOrder.setString(7, "-9 minute");
            pOrder.executeUpdate();
            int o3 = getGeneratedKey(pOrder);
            addItem(pItem, o3, 2, "M", 1, 38000, 1.0, "");
            addItem(pItem, o3, 7, "M", 1, 22000, 1.0, "Ít đá");

            // Đơn 4: Preparing
            pOrder.setInt(1, 3);
            pOrder.setString(2, "student@iu.edu.vn");
            pOrder.setString(3, "12:00");
            pOrder.setString(4, "Thêm tương ớt");
            pOrder.setInt(5, 40000);
            pOrder.setString(6, "PREPARING");
            pOrder.setString(7, "-6 minute");
            pOrder.executeUpdate();
            int o4 = getGeneratedKey(pOrder);
            addItem(pItem, o4, 5, "M", 2, 20000, 1.0, "Thêm tương ớt");

            // Đơn 5: Ready
            pOrder.setInt(1, 4);
            pOrder.setString(2, "an.nguyen@iu.edu.vn");
            pOrder.setString(3, "12:10");
            pOrder.setString(4, "");
            pOrder.setInt(5, 42000);
            pOrder.setString(6, "READY");
            pOrder.setString(7, "-3 minute");
            pOrder.executeUpdate();
            int o5 = getGeneratedKey(pOrder);
            addItem(pItem, o5, 4, "M", 1, 42000, 1.0, "");
        }
    }

    private static void addItem(PreparedStatement pItem, int orderId, int dishId, String size,
                                int qty, int price, double portion, String note) throws SQLException {
        pItem.setInt(1, orderId);
        pItem.setInt(2, dishId);
        pItem.setString(3, size);
        pItem.setInt(4, qty);
        pItem.setInt(5, price);
        pItem.setDouble(6, portion);
        pItem.setString(7, note);
        pItem.executeUpdate();
    }

    private static int getGeneratedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }
}
