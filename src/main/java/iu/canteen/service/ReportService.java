package iu.canteen.service;

import iu.canteen.dao.ForecastDao;
import iu.canteen.db.Database;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

/**
 * Dịch vụ tổng hợp báo cáo và thống kê kinh doanh (Module 7 - Phase 6).
 */
public class ReportService {
    private static final Logger logger = Logger.getLogger(ReportService.class.getName());

    private final ForecastDao forecastDao;

    public ReportService() {
        this.forecastDao = new ForecastDao();
    }

    public ReportService(ForecastDao forecastDao) {
        this.forecastDao = forecastDao;
    }

    /**
     * Dữ liệu tóm tắt kinh doanh tổng quan.
     */
    public record SalesSummary(
            int totalOrders,
            int totalRevenue,
            String bestSellingDish,
            int bestSellingQty,
            int peakHour,
            int peakHourOrders
    ) {}

    /**
     * Món ăn bán chạy theo số lượng và doanh thu.
     */
    public record TopSellingItem(
            int menuItemId,
            String menuItemName,
            String sizeLabel,
            int quantitySold,
            int revenue
    ) {}

    /**
     * Tóm tắt chỉ số bán hàng trong khoảng ngày [from, to].
     */
    public SalesSummary getSalesSummary(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now();
        if (to == null) to = LocalDate.now();

        int totalOrders = 0;
        int totalRevenue = 0;

        // 1. Thống kê tổng số đơn và doanh thu (loại bỏ CANCELLED)
        String orderSql = "SELECT COUNT(id) AS cnt, COALESCE(SUM(total_amount), 0) AS rev " +
                          "FROM orders " +
                          "WHERE status != 'CANCELLED' " +
                          "  AND date(created_at) >= ? AND date(created_at) <= ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(orderSql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalOrders = rs.getInt("cnt");
                    totalRevenue = rs.getInt("rev");
                }
            }
        } catch (SQLException e) {
            logger.warning("Lỗi truy vấn tổng quan đơn hàng: " + e.getMessage());
        }

        // 2. Món bán chạy nhất
        String bestName = "-";
        int bestQty = 0;
        List<TopSellingItem> topList = getTopSelling(from, to, 1);
        if (!topList.isEmpty()) {
            bestName = topList.get(0).menuItemName() + " (" + topList.get(0).sizeLabel() + ")";
            bestQty = topList.get(0).quantitySold();
        }

        // 3. Khung giờ cao điểm
        Map<Integer, Integer> hourly = getHourlyDistribution(from, to);
        int peakHour = 11;
        int peakHourOrders = 0;
        for (Map.Entry<Integer, Integer> entry : hourly.entrySet()) {
            if (entry.getValue() > peakHourOrders) {
                peakHourOrders = entry.getValue();
                peakHour = entry.getKey();
            }
        }

        return new SalesSummary(totalOrders, totalRevenue, bestName, bestQty, peakHour, peakHourOrders);
    }

    /**
     * Lấy danh sách các món bán chạy nhất trong khoảng ngày.
     */
    public List<TopSellingItem> getTopSelling(LocalDate from, LocalDate to, int limit) {
        if (from == null) from = LocalDate.now();
        if (to == null) to = LocalDate.now();
        if (limit <= 0) limit = 10;

        List<TopSellingItem> result = new ArrayList<>();
        String sql = "SELECT oi.menu_item_id, m.name, oi.size_label, " +
                     "       SUM(oi.quantity) AS sum_qty, " +
                     "       SUM(oi.quantity * oi.unit_price) AS sum_rev " +
                     "FROM orders o " +
                     "JOIN order_items oi ON o.id = oi.order_id " +
                     "JOIN menu_items m ON oi.menu_item_id = m.id " +
                     "WHERE o.status != 'CANCELLED' " +
                     "  AND date(o.created_at) >= ? AND date(o.created_at) <= ? " +
                     "GROUP BY oi.menu_item_id, oi.size_label " +
                     "ORDER BY sum_qty DESC " +
                     "LIMIT ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new TopSellingItem(
                            rs.getInt("menu_item_id"),
                            rs.getString("name"),
                            rs.getString("size_label"),
                            rs.getInt("sum_qty"),
                            rs.getInt("sum_rev")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.warning("Lỗi truy vấn món bán chạy: " + e.getMessage());
        }

        return result;
    }

    /**
     * Phân bổ số lượng đơn hàng theo các khung giờ trong ngày (7h đến 17h).
     */
    public Map<Integer, Integer> getHourlyDistribution(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now();
        if (to == null) to = LocalDate.now();

        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (int h = 7; h <= 17; h++) {
            map.put(h, 0);
        }

        String sql = "SELECT CAST(strftime('%H', created_at) AS INTEGER) AS hr, COUNT(id) AS cnt " +
                     "FROM orders " +
                     "WHERE status != 'CANCELLED' " +
                     "  AND date(created_at) >= ? AND date(created_at) <= ? " +
                     "GROUP BY hr";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int hr = rs.getInt("hr");
                    if (hr >= 7 && hr <= 17) {
                        map.put(hr, rs.getInt("cnt"));
                    }
                }
            }
        } catch (SQLException e) {
            logger.warning("Lỗi truy vấn phân bổ theo giờ: " + e.getMessage());
        }

        return map;
    }

    /**
     * Doanh thu theo ngày trong khoảng thời gian.
     */
    public Map<LocalDate, Integer> getDailyRevenueTrend(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(7);
        if (to == null) to = LocalDate.now();

        Map<LocalDate, Integer> trend = new LinkedHashMap<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            trend.put(cur, 0);
            cur = cur.plusDays(1);
        }

        String sql = "SELECT date(created_at) AS dt, COALESCE(SUM(total_amount), 0) AS rev " +
                     "FROM orders " +
                     "WHERE status != 'CANCELLED' " +
                     "  AND date(created_at) >= ? AND date(created_at) <= ? " +
                     "GROUP BY dt ORDER BY dt ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate d = LocalDate.parse(rs.getString("dt"));
                    trend.put(d, rs.getInt("rev"));
                }
            }
        } catch (SQLException e) {
            logger.warning("Lỗi truy vấn doanh thu theo ngày: " + e.getMessage());
        }

        return trend;
    }

    /**
     * Độ chính xác dự báo trung bình trong khoảng thời gian.
     */
    public double getAverageForecastAccuracy(LocalDate from, LocalDate to) {
        return forecastDao.getAverageAccuracy(from, to);
    }
}
