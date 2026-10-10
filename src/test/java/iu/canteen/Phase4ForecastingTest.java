package iu.canteen;

import iu.canteen.dao.DailySaleDao;
import iu.canteen.dao.ForecastDao;
import iu.canteen.dao.OrderDao;
import iu.canteen.db.Database;
import iu.canteen.logic.ForecastMethod;
import iu.canteen.logic.ForecastService;
import iu.canteen.logic.ForecastService.ForecastResult;
import iu.canteen.logic.Store;
import iu.canteen.logic.Store.CartLine;
import iu.canteen.logic.Store.Dish;
import iu.canteen.model.*;
import iu.canteen.service.HolidayService;
import iu.canteen.service.InventoryService;
import iu.canteen.service.OrderService;
import iu.canteen.service.WeatherService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase4ForecastingTest {
    private static final String TEST_DB = "test_phase4.db";
    private static WeatherService weatherService;
    private static HolidayService holidayService;
    private static DailySaleDao dailySaleDao;
    private static ForecastDao forecastDao;
    private static OrderService orderService;

    @BeforeAll
    static void setup() {
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
        Database.setDbUrl("jdbc:sqlite:" + TEST_DB);
        Database.init();

        weatherService = new WeatherService();
        holidayService = new HolidayService();
        dailySaleDao = new DailySaleDao();
        forecastDao = new ForecastDao();
        orderService = new OrderService(new OrderDao(), new InventoryService());

        Store.currentUser = "student@iu.edu.vn";
        Store.cart.clear();
        Store.orders.clear();
    }

    @AfterAll
    static void cleanup() {
        Database.setDbUrl("jdbc:sqlite:iu_canteen.db");
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
        Store.cart.clear();
        Store.orders.clear();
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void testWeatherServiceAndDemandFactor() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WeatherData w = weatherService.getWeatherByDate(tomorrow);
        assertNotNull(w);
        assertEquals(tomorrow, w.getDate());
        assertTrue(w.getTemperature() >= 20.0 && w.getTemperature() <= 45.0, "Temperature should be realistic");

        // Kiểm tra logic tính hệ số nhu cầu thời tiết
        WeatherData storm = new WeatherData(0, tomorrow, 26.0, 0.85, "Thunderstorm");
        assertEquals(0.75, weatherService.getDemandFactor(storm), 0.001);

        WeatherData heatwave = new WeatherData(0, tomorrow, 35.0, 0.05, "Nắng gắt");
        assertEquals(1.10, weatherService.getDemandFactor(heatwave), 0.001);

        WeatherData normal = new WeatherData(0, tomorrow, 30.0, 0.20, "Nắng nhẹ");
        assertEquals(1.05, weatherService.getDemandFactor(normal), 0.001);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void testHolidayServiceAndEvents() {
        int year = LocalDate.now().getYear();

        // Tết Dương lịch -> nghỉ lễ hệ số 0.2
        LocalDate newYear = LocalDate.of(year, 1, 1);
        assertEquals(0.2, holidayService.getDemandFactor(newYear), 0.001);

        // Thêm sự kiện thi cử tại IU
        LocalDate examDate = LocalDate.of(year, 6, 15);
        holidayService.addEvent(new CalendarEvent(0, examDate, "Thi cử", "Tuần thi cuối kỳ", 1.25));

        assertEquals(1.25, holidayService.getDemandFactor(examDate), 0.001);

        // Ngày thường không sự kiện và không phải cuối tuần
        LocalDate normalDay = LocalDate.of(year, 10, 14); // Thứ Tư nếu năm 2026
        double factor = holidayService.getDemandFactor(normalDay);
        assertTrue(factor >= 0.55 && factor <= 1.0, "Weekday or weekend factor should be within normal bounds");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void testDailySaleDaoAndAggregation() {
        LocalDate today = LocalDate.now();

        // Tạo đơn hàng thật và lưu vào DB
        Dish dish = Store.menu.get(0);
        MenuItemSize sizeM = dish.getSize("M");
        Store.cart.clear();
        Store.cart.add(new CartLine(dish, sizeM, 5));
        orderService.createOrder("student@iu.edu.vn", "11:30", "Test Sale", Store.cart);

        // Chạy aggregation từ orders sang daily_sales
        int rowsAggregated = dailySaleDao.aggregateSalesFromOrders(today);
        assertTrue(rowsAggregated > 0, "Aggregation should insert or update daily_sales from orders");

        // Truy vấn daily_sales theo món
        List<DailySale> sales = dailySaleDao.getSalesByMenuItem(dish.id, 7);
        assertFalse(sales.isEmpty(), "Should find daily sales for menu item");
        assertTrue(sales.stream().anyMatch(s -> s.getQuantitySold() >= 5));
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void testMathematicalForecastingAlgorithms() {
        List<Double> historicalSales = List.of(20.0, 22.0, 25.0, 28.0, 30.0, 33.0, 35.0);

        // 1. Simple Moving Average
        double sma = ForecastService.computeSMA(historicalSales, 5);
        // Avg of last 5: (25 + 28 + 30 + 33 + 35) / 5 = 151 / 5 = 30.2
        assertEquals(30.2, sma, 0.1);

        // 2. Weighted Moving Average (ngày gần có trọng số lớn hơn)
        double wma = ForecastService.computeWMA(historicalSales, 5);
        assertTrue(wma > sma, "WMA should be higher than SMA when sales trend upward");

        // 3. Exponential Smoothing
        double es = ForecastService.computeExponentialSmoothing(historicalSales, 0.4);
        assertTrue(es >= 20.0 && es <= 36.0);

        // 4. Linear Regression
        double reg = ForecastService.computeLinearRegression(historicalSales);
        assertTrue(reg > 35.0, "Linear regression on upward trend should predict value higher than the latest sale");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void testSmartForecastDetailedIntegration() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        ForecastResult result = ForecastService.forecastDetailed(5, ForecastMethod.SMART_AI_ENSEMBLE, tomorrow);

        assertNotNull(result);
        assertFalse(result.rows().isEmpty(), "Forecast rows should be populated");
        assertFalse(result.dbEntries().isEmpty(), "DB forecast entries should be populated");
        assertTrue(result.weatherFactor() > 0, "Weather factor should be positive");
        assertTrue(result.holidayFactor() > 0, "Holiday factor should be positive");
        assertTrue(result.overallMultiplier() > 0, "Overall multiplier should be positive");

        // Đảm bảo các nguyên liệu chính đều có dự báo
        for (Store.ForecastRow row : result.rows()) {
            assertNotNull(row.name());
            assertNotNull(row.unit());
            assertTrue(row.need() >= 0, "Need should be >= 0");
            assertTrue(row.buy() >= 0, "Buy should be >= 0");
            assertNotNull(row.level());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void testForecastDaoPersistenceAndAccuracy() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        ForecastResult result = ForecastService.forecastDetailed(3, ForecastMethod.SMART_AI_ENSEMBLE, tomorrow);

        // Lưu vào DB
        ForecastService.saveForecastToDatabase(result.dbEntries());

        // Đọc lại từ DB
        List<ForecastEntry> saved = forecastDao.getForecastsByDate(tomorrow);
        assertFalse(saved.isEmpty(), "Forecasts should be persisted in database");
        assertEquals(result.dbEntries().size(), saved.size());

        // Cập nhật actual_used cho bản ghi đầu tiên
        ForecastEntry first = saved.get(0);
        double actualUsed = first.getPredictedNeed() * 0.96; // sai lệch 4%
        forecastDao.updateActualUsed(first.getId(), actualUsed);

        // Kiểm tra tính toán % độ chính xác
        double accuracy = ForecastService.getAccuracy(tomorrow.minusDays(5), tomorrow.plusDays(5));
        assertTrue(accuracy >= 80.0 && accuracy <= 100.0, "Accuracy percentage should be high and realistic");
    }
}
