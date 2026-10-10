package iu.canteen;

import iu.canteen.dao.*;
import iu.canteen.db.Database;
import iu.canteen.model.User;
import iu.canteen.model.MenuItem;
import iu.canteen.model.MenuItemSize;
import iu.canteen.model.Ingredient;
import iu.canteen.model.InventoryBatch;
import iu.canteen.model.OrderItem;
import iu.canteen.service.AuthService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseAndAuthTest {
    private static final String TEST_DB = "test_canteen.db";

    @BeforeAll
    static void setup() {
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
        Database.setDbUrl("jdbc:sqlite:" + TEST_DB);
        Database.init();
    }

    @AfterAll
    static void cleanup() {
        Database.setDbUrl("jdbc:sqlite:iu_canteen.db");
        File f = new File(TEST_DB);
        if (f.exists()) f.delete();
    }

    @Test
    @Order(1)
    void testDatabaseSeedUsers() {
        UserDao userDao = new UserDao();
        List<User> users = userDao.getAllUsers();
        assertTrue(users.size() >= 3, "Seed data should contain at least 3 users");

        Optional<User> admin = userDao.findByEmail("admin@iu.edu.vn");
        assertTrue(admin.isPresent(), "admin@iu.edu.vn should exist");
        assertEquals("ADMIN", admin.get().getRole());

        Optional<User> vendor = userDao.findByEmail("vendor@iu.edu.vn");
        assertTrue(vendor.isPresent(), "vendor@iu.edu.vn should exist");
        assertEquals("VENDOR", vendor.get().getRole());
    }

    @Test
    @Order(2)
    void testAuthServiceLoginSuccess() {
        AuthService authService = new AuthService();
        User user = authService.login("student@iu.edu.vn", "student123");
        assertNotNull(user);
        assertEquals("student@iu.edu.vn", user.getEmail());
        assertEquals("STUDENT", user.getRole());
    }

    @Test
    @Order(3)
    void testAuthServiceLoginWrongPassword() {
        AuthService authService = new AuthService();
        assertThrows(IllegalArgumentException.class, () -> {
            authService.login("student@iu.edu.vn", "wrongpass");
        });
    }

    @Test
    @Order(4)
    void testAuthServiceRegisterSuccess() {
        AuthService authService = new AuthService();
        User newUser = authService.register("Lê Thị C", "c.le@iu.edu.vn", "ITCSIU21099", "password123", "STUDENT");
        assertNotNull(newUser);
        assertTrue(newUser.getId() > 0);

        // Try login with newly registered user
        User loggedIn = authService.login("c.le@iu.edu.vn", "password123");
        assertNotNull(loggedIn);
        assertEquals("Lê Thị C", loggedIn.getFullName());
    }

    @Test
    @Order(5)
    void testAuthServiceRegisterInvalidEmail() {
        AuthService authService = new AuthService();
        assertThrows(IllegalArgumentException.class, () -> {
            authService.register("Hacker", "hacker@gmail.com", "999", "123456", "STUDENT");
        });
    }

    @Test
    @Order(6)
    void testMenuDaoAndSizes() {
        MenuDao menuDao = new MenuDao();
        List<MenuItem> items = menuDao.getAllMenuItems();
        assertFalse(items.isEmpty(), "Menu items should be seeded");

        MenuItem comGa = items.stream().filter(m -> m.getName().contains("Cơm gà")).findFirst().orElse(null);
        assertNotNull(comGa);
        assertFalse(comGa.getSizes().isEmpty(), "Cơm gà should have sizes S, M, L");

        MenuItemSize sizeM = comGa.getSize("M");
        assertNotNull(sizeM);
        assertEquals(35000, sizeM.getPrice());
    }

    @Test
    @Order(7)
    void testIngredientAndBatches() {
        IngredientDao ingredientDao = new IngredientDao();
        List<Ingredient> ingredients = ingredientDao.getAll();
        assertFalse(ingredients.isEmpty(), "Ingredients should be seeded");

        BatchDao batchDao = new BatchDao();
        List<InventoryBatch> batches = batchDao.getAllBatches();
        assertFalse(batches.isEmpty(), "Batches should be seeded");
    }

    @Test
    @Order(8)
    void testOrdersAndOrderItems() {
        OrderDao orderDao = new OrderDao();
        List<iu.canteen.model.Order> orders = orderDao.getAllOrders();
        assertFalse(orders.isEmpty(), "Sample orders should be seeded");

        iu.canteen.model.Order first = orders.get(0);
        assertNotNull(first.getItems());
        assertFalse(first.getItems().isEmpty());
    }
}
