package iu.canteen;

import iu.canteen.dao.DailySaleDao;
import iu.canteen.dao.UserDao;
import iu.canteen.db.Database;
import iu.canteen.logic.Store;
import iu.canteen.model.DailySale;
import iu.canteen.model.User;
import iu.canteen.service.AuthService;
import iu.canteen.service.ReportService;
import iu.canteen.service.ReportService.SalesSummary;
import iu.canteen.service.ReportService.TopSellingItem;
import iu.canteen.service.SchedulerService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase6ReportingAndAdminTest {
    private static final String TEST_DB = "test_phase6.db";
    private static ReportService reportService;
    private static SchedulerService schedulerService;
    private static UserDao userDao;
    private static AuthService authService;
    private static DailySaleDao dailySaleDao;

    @BeforeAll
    static void setup() {
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
        Database.setDbUrl("jdbc:sqlite:" + TEST_DB);
        Database.init();

        reportService = new ReportService();
        schedulerService = new SchedulerService();
        userDao = new UserDao();
        authService = new AuthService();
        dailySaleDao = new DailySaleDao();

        Store.currentUser = "admin@iu.edu.vn";
        Store.currentRole = Store.Role.ADMIN;
    }

    @AfterAll
    static void cleanup() {
        schedulerService.stop();
        Database.setDbUrl("jdbc:sqlite:iu_canteen.db");
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
    }

    @Test
    @Order(1)
    void testReportServiceSalesSummary() {
        LocalDate today = LocalDate.now();
        SalesSummary summary = reportService.getSalesSummary(today.minusDays(5), today);

        assertNotNull(summary);
        assertTrue(summary.totalOrders() >= 0, "Total orders should be non-negative");
        assertTrue(summary.totalRevenue() >= 0, "Total revenue should be non-negative");
        assertNotNull(summary.bestSellingDish(), "Best selling dish should not be null");
        assertTrue(summary.peakHour() >= 7 && summary.peakHour() <= 17, "Peak hour should be within canteen operating hours");
    }

    @Test
    @Order(2)
    void testReportServiceTopSelling() {
        LocalDate today = LocalDate.now();
        List<TopSellingItem> topList = reportService.getTopSelling(today.minusDays(5), today, 5);

        assertNotNull(topList);
        for (TopSellingItem item : topList) {
            assertTrue(item.menuItemId() > 0);
            assertNotNull(item.menuItemName());
            assertNotNull(item.sizeLabel());
            assertTrue(item.quantitySold() >= 1);
            assertTrue(item.revenue() >= 0);
        }
    }

    @Test
    @Order(3)
    void testReportServiceHourlyAndTrend() {
        LocalDate today = LocalDate.now();

        // 1. Phân bổ theo giờ
        Map<Integer, Integer> hourly = reportService.getHourlyDistribution(today.minusDays(1), today);
        assertNotNull(hourly);
        assertEquals(11, hourly.size(), "Should cover 11 operating hours (7h to 17h)");
        assertTrue(hourly.containsKey(7));
        assertTrue(hourly.containsKey(12));
        assertTrue(hourly.containsKey(17));

        // 2. Xu hướng doanh thu theo ngày
        Map<LocalDate, Integer> trend = reportService.getDailyRevenueTrend(today.minusDays(6), today);
        assertNotNull(trend);
        assertEquals(7, trend.size(), "Should have 7 days in the trend map");
        assertTrue(trend.containsKey(today));
    }

    @Test
    @Order(4)
    void testSchedulerServiceDailyAggregation() {
        LocalDate today = LocalDate.now();

        // Chạy tác vụ tổng hợp đơn hàng vào bảng daily_sales
        int affectedRows = schedulerService.aggregateTodaySales();
        assertTrue(affectedRows >= 0, "Aggregated rows should be >= 0");

        // Đọc lại từ daily_sales
        List<DailySale> sales = dailySaleDao.getSalesBetween(today, today);
        assertNotNull(sales);
    }

    @Test
    @Order(5)
    void testSchedulerServiceWeatherSync() {
        // Đồng bộ thời tiết
        int syncCount = schedulerService.syncWeather();
        assertTrue(syncCount >= 1, "Should sync at least 1 day of weather data");
    }

    @Test
    @Order(6)
    void testSchedulerLifecycleAndToggle() {
        assertTrue(SchedulerService.isAutoSyncEnabled());
        SchedulerService.setAutoSyncEnabled(false);
        assertFalse(SchedulerService.isAutoSyncEnabled());
        SchedulerService.setAutoSyncEnabled(true);

        // Khởi động và dừng scheduler
        schedulerService.start();
        schedulerService.runPeriodicJobs();
        schedulerService.stop();
    }

    @Test
    @Order(7)
    void testAdminUserManagementAndSecurity() {
        String testEmail = "test.admin" + System.currentTimeMillis() + "@iu.edu.vn";
        User created = authService.register("Quản Trị Viên Test", testEmail, "ADM999", "adminpass123", "ADMIN");

        assertNotNull(created);
        assertEquals("ADMIN", created.getRole());
        assertTrue(created.isAdmin());

        // Kiểm tra mật khẩu mã hóa BCrypt
        String hash = userDao.getPasswordHash(testEmail);
        assertNotNull(hash);
        assertNotEquals("adminpass123", hash, "Password must be salted and hashed with BCrypt");
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw("adminpass123", hash));

        // Kiểm tra danh sách người dùng
        List<User> users = userDao.getAllUsers();
        boolean found = users.stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(testEmail));
        assertTrue(found, "Newly created user should appear in userDao.getAllUsers()");
    }
}
