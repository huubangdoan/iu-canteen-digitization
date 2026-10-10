package iu.canteen;

import iu.canteen.dao.*;
import iu.canteen.db.Database;
import iu.canteen.logic.Store;
import iu.canteen.logic.Store.CartLine;
import iu.canteen.logic.Store.Dish;
import iu.canteen.logic.Store.Status;
import iu.canteen.model.InventoryBatch;
import iu.canteen.model.Ingredient;
import iu.canteen.model.OrderItem;
import iu.canteen.model.Recipe;
import iu.canteen.model.WasteLog;
import iu.canteen.model.MenuItemSize;
import iu.canteen.service.InventoryService;
import iu.canteen.service.OrderService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase3OrderAndInventoryTest {
    private static final String TEST_DB = "test_phase3.db";
    private static InventoryService inventoryService;
    private static OrderService orderService;
    private static IngredientDao ingredientDao;
    private static BatchDao batchDao;
    private static WasteDao wasteDao;
    private static OrderDao orderDao;
    private static RecipeDao recipeDao;

    @BeforeAll
    static void setup() {
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
        Database.setDbUrl("jdbc:sqlite:" + TEST_DB);
        Database.init();

        ingredientDao = new IngredientDao();
        batchDao = new BatchDao();
        wasteDao = new WasteDao();
        recipeDao = new RecipeDao();
        orderDao = new OrderDao();

        inventoryService = new InventoryService(batchDao, ingredientDao, wasteDao, recipeDao);
        orderService = new OrderService(orderDao, inventoryService);

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
    void testReceiveBatchAndStockIncrement() {
        List<Ingredient> ings = ingredientDao.getAll();
        assertFalse(ings.isEmpty(), "Ingredients should exist in DB");
        Ingredient rice = ings.get(0);
        int ingId = rice.getId();
        double initialStock = rice.getCurrentStock();

        // Nhập lô 1: 20kg gạo, hết hạn sau 10 ngày
        LocalDate today = LocalDate.now();
        InventoryBatch b1 = inventoryService.receiveBatch(
                ingId, 20.0, 18000.0, today, today.plusDays(10), "BATCH-TEST-01"
        );
        assertNotNull(b1);
        assertEquals("BATCH-TEST-01", b1.getBatchCode());
        assertEquals(20.0, b1.getQuantity());
        assertEquals(20.0, b1.getRemainingQuantity());

        // Kiểm tra tồn kho đã tăng
        Optional<Ingredient> updatedRice = ingredientDao.getById(ingId);
        assertTrue(updatedRice.isPresent());
        assertEquals(initialStock + 20.0, updatedRice.get().getCurrentStock(), 0.001);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void testFIFODeductionAcrossBatches() {
        List<Ingredient> ings = ingredientDao.getAll();
        Ingredient rice = ings.get(0);
        int ingId = rice.getId();
        double stockBefore = ingredientDao.getById(ingId).get().getCurrentStock();

        LocalDate today = LocalDate.now();
        // Nhập lô 2: 15kg gạo, hết hạn sau 20 ngày (sau lô 1)
        InventoryBatch b2 = inventoryService.receiveBatch(
                ingId, 15.0, 19000.0, today, today.plusDays(20), "BATCH-TEST-02"
        );
        assertNotNull(b2);

        // Xuất kho 25kg gạo bằng FIFO (lô 1 có 20kg sẽ hết sạch, lô 2 còn 10kg)
        double deducted = inventoryService.deductFIFO(ingId, 25.0);
        assertEquals(25.0, deducted, 0.001);

        // Lô 1 (hạn gần hơn) phải còn 0kg
        Optional<InventoryBatch> b1Check = batchDao.getBatchById(b2.getId() - 1);
        if (b1Check.isPresent()) {
            assertEquals(0.0, b1Check.get().getRemainingQuantity(), 0.001);
        }

        // Lô 2 (hạn xa hơn) phải còn 10kg (15 - 5)
        Optional<InventoryBatch> b2Check = batchDao.getBatchById(b2.getId());
        assertTrue(b2Check.isPresent());
        assertEquals(10.0, b2Check.get().getRemainingQuantity(), 0.001);

        // Tồn kho tổng giảm đúng 25 - 15 = 10 chênh lệch so với trước khi nhập lô 2
        double stockAfter = ingredientDao.getById(ingId).get().getCurrentStock();
        assertEquals(stockBefore + 15.0 - 25.0, stockAfter, 0.001);
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void testCreateOrderAndPortionDecrement() {
        Dish dish = Store.menu.get(0);
        int initialRemaining = dish.remaining.get();
        MenuItemSize sizeM = dish.getSize("M");

        Store.cart.clear();
        Store.cart.add(new CartLine(dish, sizeM, 2));

        Store.Order createdOrder = orderService.createOrder(
                "student@iu.edu.vn", "11:30", "Không cay", Store.cart
        );

        assertNotNull(createdOrder);
        assertTrue(createdOrder.id > 0);
        assertEquals(Status.PENDING, createdOrder.status.get());
        assertEquals("student@iu.edu.vn", createdOrder.customer);
        assertEquals(2 * sizeM.getPrice(), createdOrder.total);

        // Kiểm tra số phần của món đã bị trừ 2
        assertEquals(initialRemaining - 2, dish.remaining.get());

        // Kiểm tra giỏ hàng đã được làm rỗng
        assertTrue(Store.cart.isEmpty());

        // Kiểm tra database lưu đơn hàng chính xác
        Optional<iu.canteen.model.Order> dbOrder = orderDao.getOrderById(createdOrder.id);
        assertTrue(dbOrder.isPresent());
        assertEquals(1, dbOrder.get().getItems().size());
        OrderItem it = dbOrder.get().getItems().get(0);
        assertEquals("M", it.getSizeLabel());
        assertEquals(2, it.getQuantity());
        assertEquals(sizeM.getPortionMultiplier(), it.getPortionMultiplier(), 0.001);
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void testCancelOrderRestoresPortions() {
        Dish dish = Store.menu.get(0);
        int initialRemaining = dish.remaining.get();
        MenuItemSize sizeS = dish.getSize("S");

        Store.cart.clear();
        Store.cart.add(new CartLine(dish, sizeS, 3));

        Store.Order created = orderService.createOrder(
                "student@iu.edu.vn", "12:00", "", Store.cart
        );
        assertEquals(initialRemaining - 3, dish.remaining.get());

        // Hủy đơn hàng
        orderService.cancelOrder(created.id);

        // Trạng thái đơn chuyển sang CANCELLED
        assertEquals(Status.CANCELLED, created.status.get());
        Optional<iu.canteen.model.Order> dbOrder = orderDao.getOrderById(created.id);
        assertTrue(dbOrder.isPresent());
        assertEquals(iu.canteen.model.Order.Status.CANCELLED, dbOrder.get().getStatus());

        // Số phần phải được hoàn trả lại
        assertEquals(initialRemaining, dish.remaining.get());
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void testStatusUpdateTriggersRecipeFIFODeduction() {
        Dish dish = Store.menu.get(0);
        MenuItemSize sizeL = dish.getSize("L"); // portionMultiplier = 1.3
        assertNotNull(sizeL);

        // Xem công thức của món này
        List<Recipe> recipes = recipeDao.getRecipesByMenuItemId(dish.id);
        assertFalse(recipes.isEmpty(), "Dish should have recipe lines");
        Recipe firstRecipe = recipes.get(0);
        int ingId = firstRecipe.getIngredientId();

        // Nhập một lượng nguyên liệu đủ dùng
        LocalDate today = LocalDate.now();
        inventoryService.receiveBatch(ingId, 50.0, 20000.0, today, today.plusDays(30), "BATCH-TEST-ORDER");

        double stockBeforeOrder = ingredientDao.getById(ingId).get().getCurrentStock();

        // Đặt 2 phần size L
        Store.cart.clear();
        Store.cart.add(new CartLine(dish, sizeL, 2));
        Store.Order order = orderService.createOrder(
                "student@iu.edu.vn", "12:15", "Giao nhanh", Store.cart
        );

        // Bếp chuyển sang PREPARING
        orderService.updateStatus(order.id, Status.PREPARING);

        // Lượng nguyên liệu cần trừ = recipe.quantity * portionMultiplier (1.3) * qty (2)
        double expectedDeduction = firstRecipe.getQuantity() * sizeL.getPortionMultiplier() * 2;
        double stockAfterPreparing = ingredientDao.getById(ingId).get().getCurrentStock();

        assertEquals(stockBeforeOrder - expectedDeduction, stockAfterPreparing, 0.001,
                "Stock should be deducted according to recipe quantity and size multiplier");

        // Chuyển tiếp sang READY và COMPLETED không được trừ lần 2 (chống duplicate deduction)
        orderService.updateStatus(order.id, Status.READY);
        orderService.updateStatus(order.id, Status.COMPLETED);

        double stockAfterCompleted = ingredientDao.getById(ingId).get().getCurrentStock();
        assertEquals(stockAfterPreparing, stockAfterCompleted, 0.001,
                "Stock should not be deducted multiple times for the same order");
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void testLogWasteReducesStockAndBatches() {
        List<Ingredient> ings = ingredientDao.getAll();
        Ingredient ing = ings.get(0);
        int ingId = ing.getId();

        LocalDate today = LocalDate.now();
        inventoryService.receiveBatch(ingId, 10.0, 15000.0, today, today.plusDays(5), "BATCH-WASTE-TEST");
        double stockBefore = ingredientDao.getById(ingId).get().getCurrentStock();

        // Ghi nhận hao hụt 2.5 kg do hỏng mốc
        WasteLog log = inventoryService.logWaste(
                ingId, 2.5, "Mốc do ẩm ướt", today, "vendor@iu.edu.vn"
        );

        assertNotNull(log);
        assertTrue(log.getId() > 0);
        assertEquals(2.5, log.getQuantity());

        // Kiểm tra tồn kho giảm đúng 2.5 kg
        double stockAfter = ingredientDao.getById(ingId).get().getCurrentStock();
        assertEquals(stockBefore - 2.5, stockAfter, 0.001);

        // Kiểm tra bảng waste_logs trong DB
        List<WasteLog> wastes = wasteDao.getWasteLogsByIngredient(ingId);
        assertFalse(wastes.isEmpty());
        assertTrue(wastes.stream().anyMatch(w -> "Mốc do ẩm ướt".equals(w.getReason())));
    }
}
