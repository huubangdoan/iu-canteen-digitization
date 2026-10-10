package iu.canteen;

import iu.canteen.dao.LeftoverDao;
import iu.canteen.dao.WasteSavingDao;
import iu.canteen.db.Database;
import iu.canteen.logic.ForecastMethod;
import iu.canteen.logic.ForecastService;
import iu.canteen.logic.ForecastService.ForecastResult;
import iu.canteen.logic.Store;
import iu.canteen.model.Leftover;
import iu.canteen.model.WasteSaving;
import iu.canteen.service.LeftoverService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase5WasteReductionTest {
    private static final String TEST_DB = "test_phase5.db";
    private static LeftoverDao leftoverDao;
    private static WasteSavingDao wasteSavingDao;
    private static LeftoverService leftoverService;

    @BeforeAll
    static void setup() {
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
        Database.setDbUrl("jdbc:sqlite:" + TEST_DB);
        Database.init();

        leftoverDao = new LeftoverDao();
        wasteSavingDao = new WasteSavingDao();
        leftoverService = new LeftoverService();

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
    @Order(1)
    void testLeftoverDaoCrudAndQuery() {
        LocalDate today = LocalDate.now();
        Leftover item = new Leftover(0, 1, "Cơm gà xối mỡ", "M", 35000, 10, 10, 30, "ACTIVE", today);
        Leftover saved = leftoverDao.insert(item);

        assertTrue(saved.getId() > 0, "Leftover ID should be auto-generated");

        Optional<Leftover> foundOpt = leftoverDao.findById(saved.getId());
        assertTrue(foundOpt.isPresent(), "Leftover should be found by ID");
        Leftover found = foundOpt.get();
        assertEquals("Cơm gà xối mỡ", found.getMenuItemName());
        assertEquals(35000, found.getOriginalPrice());
        assertEquals(24500, found.getDiscountedPrice()); // 35000 * 0.7 = 24500

        // Test getActiveLeftovers
        List<Leftover> activeList = leftoverDao.getActiveLeftovers(today);
        assertFalse(activeList.isEmpty(), "Should return active leftovers for today");

        // Test updateRemainingAndStatus
        leftoverDao.updateRemainingAndStatus(saved.getId(), 5, "ACTIVE");
        found = leftoverDao.findById(saved.getId()).orElseThrow();
        assertEquals(5, found.getRemainingQuantity());

        int activeCount = leftoverDao.countByStatus(today, "ACTIVE");
        assertTrue(activeCount >= 1, "Active count should be at least 1");
    }

    @Test
    @Order(2)
    void testWasteSavingDaoAndAggregation() {
        LocalDate today = LocalDate.now();

        // Ghi nhận một đợt bán giảm giá: 4 suất, 1.4 kg thức ăn cứu được, 98.000đ thu hồi
        wasteSavingDao.recordSale(today, 4, 1.4, 98000);

        Optional<WasteSaving> savingOpt = wasteSavingDao.findByDate(today);
        assertTrue(savingOpt.isPresent(), "Waste saving record should exist");
        WasteSaving ws = savingOpt.get();
        assertEquals(4, ws.getTotalLeftoversSold());
        assertEquals(1.4, ws.getFoodWasteReducedKg(), 0.001);
        assertEquals(98000, ws.getCostSavedVnd());

        // Ghi nhận thêm 2 suất nữa vào cùng ngày -> cộng dồn
        wasteSavingDao.recordSale(today, 2, 0.7, 49000);
        ws = wasteSavingDao.findByDate(today).orElseThrow();
        assertEquals(6, ws.getTotalLeftoversSold());
        assertEquals(2.1, ws.getFoodWasteReducedKg(), 0.001);
        assertEquals(147000, ws.getCostSavedVnd());

        // Test summary range
        WasteSaving summary = wasteSavingDao.getSummary(today.minusDays(7), today.plusDays(1));
        assertTrue(summary.getTotalLeftoversSold() >= 6);
        assertTrue(summary.getFoodWasteReducedKg() >= 2.1);
        assertTrue(summary.getCostSavedVnd() >= 147000);
    }

    @Test
    @Order(3)
    void testLeftoverServiceCreateFlashSale() {
        LocalDate today = LocalDate.now();

        // Tạo Flash Sale hợp lệ cho Phở bò (id=3, size L)
        Leftover flashPho = leftoverService.createFlashSale(3, "L", 8, 40, today);
        assertNotNull(flashPho);
        assertEquals(3, flashPho.getMenuItemId());
        assertEquals("L", flashPho.getSizeLabel());
        assertEquals(8, flashPho.getInitialQuantity());
        assertEquals(8, flashPho.getRemainingQuantity());
        assertEquals(40, flashPho.getDiscountPercent());
        assertEquals("ACTIVE", flashPho.getStatus());

        // Kiểm tra validation
        assertThrows(IllegalArgumentException.class, () ->
                leftoverService.createFlashSale(3, "L", 0, 40, today),
                "Số lượng <= 0 phải ném ngoại lệ");

        assertThrows(IllegalArgumentException.class, () ->
                leftoverService.createFlashSale(3, "L", 5, 95, today),
                "Giảm giá > 90% phải ném ngoại lệ");

        assertThrows(IllegalArgumentException.class, () ->
                leftoverService.createFlashSale(9999, "M", 5, 30, today),
                "Món không tồn tại phải ném ngoại lệ");
    }

    @Test
    @Order(4)
    void testLeftoverServicePurchaseFlow() {
        LocalDate today = LocalDate.now();
        Leftover sale = leftoverService.createFlashSale(2, "M", 6, 50, today); // Cơm sườn 38k giảm 50% = 19k

        int initialRemaining = sale.getRemainingQuantity();
        int buyQty = 2;

        Store.Order order = leftoverService.purchaseLeftover(sale.getId(), buyQty, "an.nguyen@iu.edu.vn", "Mang về");
        assertNotNull(order);
        assertTrue(order.id > 0);
        assertEquals(38000, order.total); // 19k * 2 = 38k
        assertTrue(order.lines.get(0).note().contains("Flash Sale"));

        // Kiểm tra số lượng tồn đã giảm
        Leftover updated = leftoverDao.findById(sale.getId()).orElseThrow();
        assertEquals(initialRemaining - buyQty, updated.getRemainingQuantity());
        assertEquals("ACTIVE", updated.getStatus());

        // Mua nốt 4 phần còn lại -> chuyển thành SOLD_OUT
        leftoverService.purchaseLeftover(sale.getId(), 4, "student@iu.edu.vn", "");
        updated = leftoverDao.findById(sale.getId()).orElseThrow();
        assertEquals(0, updated.getRemainingQuantity());
        assertEquals("SOLD_OUT", updated.getStatus());

        // Cố mua tiếp khi đã hết hàng -> ném ngoại lệ
        assertThrows(IllegalStateException.class, () ->
                leftoverService.purchaseLeftover(sale.getId(), 1, "student@iu.edu.vn", ""));
    }

    @Test
    @Order(5)
    void testLeftoverServiceEndOfDayProcess() {
        LocalDate today = LocalDate.now();

        // 1. Kiểm tra quyên góp (DONATED)
        Leftover donateItem = leftoverService.createFlashSale(5, "M", 5, 30, today);
        int donatedProcessed = leftoverService.endOfDayProcess(today, true);
        assertTrue(donatedProcessed >= 1, "Should process at least 1 leftover item");

        Leftover checkDonate = leftoverDao.findById(donateItem.getId()).orElseThrow();
        assertEquals("DONATED", checkDonate.getStatus());

        // 2. Kiểm tra hủy bỏ / tiêu hủy (EXPIRED)
        Leftover expireItem = leftoverService.createFlashSale(4, "M", 3, 30, today);
        int expireProcessed = leftoverService.endOfDayProcess(today, false);
        assertTrue(expireProcessed >= 1);

        Leftover checkExpire = leftoverDao.findById(expireItem.getId()).orElseThrow();
        assertEquals("EXPIRED", checkExpire.getStatus());
    }

    @Test
    @Order(6)
    void testWasteAdjustmentFeedingIntoSmartForecast() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Tính tỷ lệ thừa và hệ số điều chỉnh
        double wasteFactorDish1 = leftoverService.getWasteAdjustmentFactor(1, 7);
        assertTrue(wasteFactorDish1 >= 0.0 && wasteFactorDish1 <= 0.30,
                "Waste adjustment factor should be bounded between 0.0 and 0.30");

        // Chạy dự báo AI Smart Ensemble
        ForecastResult result = ForecastService.forecastDetailed(5, ForecastMethod.SMART_AI_ENSEMBLE, tomorrow);
        assertNotNull(result);
        assertFalse(result.rows().isEmpty());
        assertTrue(result.wasteAdjustment() >= 0.0, "ForecastResult should record wasteAdjustment");

        // Kiểm tra các hàng nguyên liệu có giá trị hợp lệ
        for (Store.ForecastRow row : result.rows()) {
            assertTrue(row.need() >= 0);
            assertTrue(row.buy() >= 0);
        }
    }
}
