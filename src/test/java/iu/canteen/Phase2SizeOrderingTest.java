package iu.canteen;

import iu.canteen.dao.OrderDao;
import iu.canteen.db.Database;
import iu.canteen.logic.ForecastService;
import iu.canteen.logic.Store;
import iu.canteen.logic.Store.CartLine;
import iu.canteen.logic.Store.Dish;
import iu.canteen.logic.Store.RecipeLine;
import iu.canteen.model.MenuItemSize;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase2SizeOrderingTest {
    private static final String TEST_DB = "test_phase2.db";

    @BeforeAll
    static void setup() {
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
        Database.setDbUrl("jdbc:sqlite:" + TEST_DB);
        Database.init();
        Store.currentUser = "student@iu.edu.vn";
        Store.cart.clear();
    }

    @AfterAll
    static void cleanup() {
        Database.setDbUrl("jdbc:sqlite:iu_canteen.db");
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
        Store.cart.clear();
    }

    @Test
    @Order(1)
    void testDishSizesAvailable() {
        assertFalse(Store.menu.isEmpty(), "Store menu should not be empty");
        Dish comGa = Store.menu.stream().filter(d -> d.name.get().contains("Cơm gà")).findFirst().orElse(null);
        assertNotNull(comGa, "Cơm gà dish should exist");

        assertEquals(3, comGa.sizes.size(), "Cơm gà should have S, M, L sizes");
        MenuItemSize sizeS = comGa.getSize("S");
        MenuItemSize sizeM = comGa.getSize("M");
        MenuItemSize sizeL = comGa.getSize("L");

        assertNotNull(sizeS);
        assertNotNull(sizeM);
        assertNotNull(sizeL);

        assertEquals(30000, sizeS.getPrice());
        assertEquals(35000, sizeM.getPrice());
        assertEquals(42000, sizeL.getPrice());

        assertEquals(0.8, sizeS.getPortionMultiplier());
        assertEquals(1.0, sizeM.getPortionMultiplier());
        assertEquals(1.3, sizeL.getPortionMultiplier());
    }

    @Test
    @Order(2)
    void testCartLineWithSizes() {
        Dish comGa = Store.menu.get(0);
        MenuItemSize sizeS = comGa.getSize("S");
        MenuItemSize sizeL = comGa.getSize("L");

        CartLine itemS = new CartLine(comGa, sizeS, 2);
        assertEquals("S", itemS.size.get());
        assertEquals(30000, itemS.unitPrice.get());
        assertEquals(2, itemS.qty.get());
        assertEquals(60000, itemS.subtotal());

        CartLine itemL = new CartLine(comGa, sizeL, 1);
        assertEquals("L", itemL.size.get());
        assertEquals(42000, itemL.unitPrice.get());
        assertEquals(42000, itemL.subtotal());
    }

    @Test
    @Order(3)
    void testAddingMultipleSizesToCart() {
        Store.cart.clear();
        Dish comGa = Store.menu.get(0);

        MenuItemSize sizeS = comGa.getSize("S");
        MenuItemSize sizeM = comGa.getSize("M");

        Store.cart.add(new CartLine(comGa, sizeS, 1));
        Store.cart.add(new CartLine(comGa, sizeM, 2));

        assertEquals(2, Store.cart.size(), "Cart should have 2 distinct lines for size S and size M");
        assertEquals(3, Store.cartCount(), "Cart count should be 3 items total");

        // 1 * 30000 + 2 * 35000 = 100000
        assertEquals(100000, Store.cartTotal(), "Cart total should equal sum of items across sizes");
    }

    @Test
    @Order(4)
    void testPlaceOrderWithSizes() {
        Store.cart.clear();
        Dish comGa = Store.menu.get(0);
        Store.cart.add(new CartLine(comGa, comGa.getSize("S"), 2)); // 60,000đ

        Store.Order order = Store.placeOrder("12:15", "Không hành");
        assertNotNull(order);
        assertTrue(Store.cart.isEmpty(), "Cart should be cleared after order");

        assertFalse(order.lines.isEmpty());
        Store.Order.Line line = order.lines.get(0);
        assertEquals("S", line.size());
        assertEquals(30000, line.unitPrice());
        assertEquals(2, line.qty());
        assertEquals(0.8, line.portionMultiplier());
        assertTrue(order.summary().contains("(S)"), "Order summary should include size S");

        // Verify SQLite database has order items with size
        OrderDao orderDao = new OrderDao();
        List<iu.canteen.model.Order> dbOrders = orderDao.getOrdersByCustomer("student@iu.edu.vn");
        assertFalse(dbOrders.isEmpty());
        iu.canteen.model.Order latest = dbOrders.get(0);
        assertNotNull(latest.getItems());
        assertFalse(latest.getItems().isEmpty());
        assertEquals("S", latest.getItems().get(0).getSizeLabel());
        assertEquals(0.8, latest.getItems().get(0).getPortionMultiplier());
    }

    @Test
    @Order(5)
    void testRecipePortionMultiplierCalculation() {
        // Gạo trong cơm gà: định lượng chuẩn size M là 0.15kg
        RecipeLine riceLine = Store.recipeFor(Store.menu.get(0)).get(0);
        assertEquals(0.15, riceLine.qty.get());

        // Size S (0.8 multiplier): 0.15 * 0.8 = 0.12 kg
        double needS = ForecastService.calculateRecipeNeed(riceLine, 0.8, 1);
        assertEquals(0.1, needS, 0.05); // round1 = 0.1kg

        // Size L (1.3 multiplier): 0.15 * 1.3 = 0.195 kg
        double needL = ForecastService.calculateRecipeNeed(riceLine, 1.3, 10);
        // 0.15 * 1.3 * 10 = 1.95 -> round1 = 2.0 kg
        assertEquals(2.0, needL, 0.1);
    }
}
