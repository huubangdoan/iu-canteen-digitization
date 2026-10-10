package iu.canteen.service;

import iu.canteen.dao.*;
import iu.canteen.logic.Store;
import iu.canteen.logic.Store.Status;
import iu.canteen.model.Leftover;
import iu.canteen.model.MenuItem;
import iu.canteen.model.MenuItemSize;
import iu.canteen.model.Order;
import iu.canteen.model.OrderItem;
import iu.canteen.model.WasteSaving;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Dịch vụ quản lý thức ăn thừa & Giảm lãng phí (Flash Sale, kiểm kê cuối ngày, quyên góp, phản hồi dự báo).
 */
public class LeftoverService {
    private static final Logger logger = Logger.getLogger(LeftoverService.class.getName());

    private final LeftoverDao leftoverDao;
    private final WasteSavingDao wasteSavingDao;
    private final MenuDao menuDao;
    private final OrderDao orderDao;
    private final DailySaleDao dailySaleDao;
    private final WasteDao wasteDao;

    public LeftoverService() {
        this.leftoverDao = new LeftoverDao();
        this.wasteSavingDao = new WasteSavingDao();
        this.menuDao = new MenuDao();
        this.orderDao = new OrderDao();
        this.dailySaleDao = new DailySaleDao();
        this.wasteDao = new WasteDao();
    }

    public LeftoverService(LeftoverDao leftoverDao, WasteSavingDao wasteSavingDao, MenuDao menuDao,
                           OrderDao orderDao, DailySaleDao dailySaleDao, WasteDao wasteDao) {
        this.leftoverDao = leftoverDao;
        this.wasteSavingDao = wasteSavingDao;
        this.menuDao = menuDao;
        this.orderDao = orderDao;
        this.dailySaleDao = dailySaleDao;
        this.wasteDao = wasteDao;
    }

    /**
     * Tạo đợt Flash Sale giảm giá cho món ăn còn thừa cuối ngày.
     */
    public Leftover createFlashSale(int menuItemId, String sizeLabel, int quantity, int discountPercent, LocalDate date) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phần thừa phải lớn hơn 0.");
        }
        if (discountPercent < 5 || discountPercent > 90) {
            throw new IllegalArgumentException("Mức giảm giá Flash Sale phải từ 5% đến 90%.");
        }
        if (date == null) date = LocalDate.now();
        if (sizeLabel == null || sizeLabel.trim().isEmpty()) sizeLabel = "M";
        sizeLabel = sizeLabel.trim().toUpperCase();

        Optional<MenuItem> itemOpt = menuDao.getMenuItemById(menuItemId);
        if (itemOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy món ăn với ID: " + menuItemId);
        }
        MenuItem item = itemOpt.get();

        int originalPrice = 35000;
        for (MenuItemSize s : item.getSizes()) {
            if (s.getSizeLabel().equalsIgnoreCase(sizeLabel)) {
                originalPrice = s.getPrice();
                break;
            }
        }

        Leftover leftover = new Leftover(
                0,
                menuItemId,
                item.getName(),
                sizeLabel,
                originalPrice,
                quantity,
                quantity,
                discountPercent,
                "ACTIVE",
                date
        );

        Leftover saved = leftoverDao.insert(leftover);
        logger.info(String.format("Tạo Flash Sale thành công: %s (Size %s), %d phần, giảm %d%%",
                item.getName(), sizeLabel, quantity, discountPercent));
        return saved;
    }

    /**
     * Lấy danh sách các món Flash Sale hiện đang khả dụng.
     */
    public List<Leftover> getAvailableLeftovers() {
        return leftoverDao.getAllActiveLeftovers();
    }

    /**
     * Lấy danh sách thức ăn thừa / Flash Sale theo ngày.
     */
    public List<Leftover> getLeftoversByDate(LocalDate date) {
        if (date == null) date = LocalDate.now();
        return leftoverDao.getLeftoversByDate(date);
    }

    /**
     * Đặt mua món Flash Sale (dành cho sinh viên/nhân viên):
     * - Giảm số lượng phần còn lại của Leftover
     * - Ghi nhận số suất cứu được và kg thức ăn giảm lãng phí vào waste_savings
     * - Tạo đơn hàng lưu vào DB và Store.orders với giá đã giảm
     */
    public Store.Order purchaseLeftover(int leftoverId, int quantity, String customerEmail, String note) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng đặt mua phải lớn hơn 0.");
        }
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Email khách hàng không được để trống.");
        }

        Optional<Leftover> opt = leftoverDao.findById(leftoverId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy món Flash Sale với ID: " + leftoverId);
        }
        Leftover leftover = opt.get();

        if (!"ACTIVE".equalsIgnoreCase(leftover.getStatus()) || leftover.getRemainingQuantity() <= 0) {
            throw new IllegalStateException("Món Flash Sale \"" + leftover.getMenuItemName() + "\" đã hết hàng hoặc kết thúc.");
        }
        if (leftover.getRemainingQuantity() < quantity) {
            throw new IllegalStateException("Chỉ còn " + leftover.getRemainingQuantity() + " phần Flash Sale.");
        }

        // 1. Cập nhật số lượng còn lại của Leftover
        int newRemaining = leftover.getRemainingQuantity() - quantity;
        String newStatus = newRemaining == 0 ? "SOLD_OUT" : "ACTIVE";
        leftoverDao.updateRemainingAndStatus(leftoverId, newRemaining, newStatus);
        leftover.setRemainingQuantity(newRemaining);
        leftover.setStatus(newStatus);

        // 2. Ghi nhận tiết kiệm lãng phí vào waste_savings
        double foodSavedKg = Math.round(quantity * 0.35 * 100.0) / 100.0; // Trung bình ~0.35 kg/suất
        int discountedPrice = leftover.getDiscountedPrice();
        int totalSavedMoney = discountedPrice * quantity;
        wasteSavingDao.recordSale(leftover.getDate(), quantity, foodSavedKg, totalSavedMoney);

        // 3. Tạo đơn hàng vào Database
        String flashNote = "[⚡ Flash Sale -" + leftover.getDiscountPercent() + "%]";
        if (note != null && !note.trim().isEmpty()) {
            flashNote = note.trim() + " " + flashNote;
        }

        Order dbOrder = new Order();
        dbOrder.setCustomerEmail(customerEmail.trim());
        dbOrder.setPickupTime("17:00 (Flash Sale)");
        dbOrder.setNote(flashNote);
        dbOrder.setTotalAmount(totalSavedMoney);
        dbOrder.setStatus(Order.Status.PENDING);
        dbOrder.setCreatedAt(LocalDateTime.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setMenuItemId(leftover.getMenuItemId());
        orderItem.setMenuItemName(leftover.getMenuItemName());
        orderItem.setSizeLabel(leftover.getSizeLabel());
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(discountedPrice);
        orderItem.setPortionMultiplier(1.0);
        orderItem.setNote(flashNote);
        dbOrder.setItems(List.of(orderItem));

        Order savedOrder = orderDao.createOrder(dbOrder);

        // 4. Tạo đối tượng Store.Order hiển thị realtime
        Store.Order.Line storeLine = new Store.Order.Line(
                leftover.getMenuItemName(),
                leftover.getSizeLabel(),
                quantity,
                discountedPrice,
                1.0,
                flashNote
        );
        Store.Order storeOrder = new Store.Order(
                savedOrder.getId(),
                customerEmail.trim(),
                savedOrder.getCreatedAt(),
                savedOrder.getPickupTime(),
                savedOrder.getNote(),
                totalSavedMoney,
                List.of(storeLine),
                Status.PENDING
        );
        Store.orders.add(0, storeOrder);

        logger.info(String.format("Mua Flash Sale thành công: Đơn #%d, %s x%d, tổng tiền: %dđ, giảm lãng phí: %.2f kg",
                savedOrder.getId(), leftover.getMenuItemName(), quantity, totalSavedMoney, foodSavedKg));

        return storeOrder;
    }

    /**
     * Quy trình kết thúc ngày (End-of-Day Process):
     * - Xử lý tất cả món Flash Sale còn tồn
     * - donateRemaining = true: Chuyển trạng thái sang DONATED (quyên góp)
     * - donateRemaining = false: Chuyển trạng thái sang EXPIRED / WASTED (hủy bỏ)
     */
    public int endOfDayProcess(LocalDate date, boolean donateRemaining) {
        if (date == null) date = LocalDate.now();
        List<Leftover> leftovers = leftoverDao.getLeftoversByDate(date);
        int processedCount = 0;

        for (Leftover l : leftovers) {
            if ("ACTIVE".equalsIgnoreCase(l.getStatus()) && l.getRemainingQuantity() > 0) {
                String targetStatus = donateRemaining ? "DONATED" : "EXPIRED";
                leftoverDao.updateRemainingAndStatus(l.getId(), l.getRemainingQuantity(), targetStatus);

                if (!donateRemaining) {
                    // Ghi nhận vào waste_logs nếu bỏ đi
                    try {
                        wasteDao.logWaste(new iu.canteen.model.WasteLog(
                                0, 1, (double) l.getRemainingQuantity(),
                                "Hao hụt cuối ngày: " + l.getMenuItemName() + " (" + l.getSizeLabel() + ")",
                                date, "system"));
                    } catch (Exception ignored) {}
                }
                processedCount++;
            }
        }

        logger.info(String.format("Chốt sổ cuối ngày %s: Đã xử lý %d món thừa (donate=%b)",
                date, processedCount, donateRemaining));
        return processedCount;
    }

    /**
     * Lấy dữ liệu tiết kiệm lãng phí theo ngày.
     */
    public WasteSaving getSavingsForDate(LocalDate date) {
        if (date == null) date = LocalDate.now();
        return wasteSavingDao.findByDate(date)
                .orElse(new WasteSaving(0, date, 0, 0.0, 0));
    }

    /**
     * Lấy tổng kết tiết kiệm lãng phí trong khoảng thời gian.
     */
    public WasteSaving getTotalSavings(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return wasteSavingDao.getSummary(from, to);
    }

    /**
     * Tính tỷ lệ thức ăn thừa trung bình của một món ăn trong N ngày qua:
     * leftover_rate = tổng phần thừa / (tổng phần bán + tổng phần thừa)
     * Kết quả từ 0.0 đến 1.0 (ví dụ 0.15 = 15%).
     */
    public double getAverageLeftoverRate(int menuItemId, int lookbackDays) {
        if (lookbackDays <= 0) lookbackDays = 7;
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(lookbackDays);

        int totalLeftover = leftoverDao.getTotalLeftoversForMenuItem(menuItemId, from, to);
        var sales = dailySaleDao.getSalesByMenuItem(menuItemId, lookbackDays);
        int totalSold = sales.stream().mapToInt(iu.canteen.model.DailySale::getQuantitySold).sum();

        int grandTotal = totalSold + totalLeftover;
        if (grandTotal == 0) return 0.0;

        double rate = (double) totalLeftover / grandTotal;
        return Math.round(rate * 100.0) / 100.0;
    }

    /**
     * Tính hệ số điều chỉnh lãng phí (Waste Adjustment Factor) cho AI Forecast.
     * Càng thừa nhiều thì hệ số điều chỉnh càng cao (giảm bớt mua hàng), tối đa 30% (0.30).
     */
    public double getWasteAdjustmentFactor(int menuItemId, int lookbackDays) {
        double rate = getAverageLeftoverRate(menuItemId, lookbackDays);
        // Giới hạn trong khoảng 0.0 đến 0.30 theo Section 8.4 REQUIREMENTS.md
        return Math.min(0.30, Math.max(0.0, rate));
    }
}
