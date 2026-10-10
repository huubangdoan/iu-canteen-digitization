package iu.canteen.service;

import iu.canteen.dao.OrderDao;
import iu.canteen.logic.Store;
import iu.canteen.logic.Store.CartLine;
import iu.canteen.logic.Store.Dish;
import iu.canteen.logic.Store.Status;
import iu.canteen.model.Order;
import iu.canteen.model.OrderItem;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Quản lý vòng đời đơn hàng: tạo đơn, chuyển trạng thái bếp, hủy đơn và trừ tồn kho tự động.
 */
public class OrderService {
    private static final Logger logger = Logger.getLogger(OrderService.class.getName());

    private final OrderDao orderDao;
    private final InventoryService inventoryService;
    private final Set<Integer> deductedOrderIds = Collections.synchronizedSet(new HashSet<>());

    public OrderService() {
        this.orderDao = new OrderDao();
        this.inventoryService = new InventoryService();
    }

    public OrderService(OrderDao orderDao, InventoryService inventoryService) {
        this.orderDao = orderDao;
        this.inventoryService = inventoryService;
    }

    /**
     * Tạo đơn hàng mới từ giỏ hàng hiện tại:
     * - Lưu bảng orders và order_items trong DB
     * - Trừ số phần còn lại hôm nay (remaining) của từng món
     * - Thêm vào danh sách hiển thị Store.orders
     */
    public Store.Order createOrder(String customerEmail, String pickupTime, String note, List<CartLine> cartLines) {
        if (cartLines == null || cartLines.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống.");
        }
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Email khách hàng không được để trống.");
        }

        // 1. Kiểm tra số phần còn lại
        Map<Dish, Integer> totalPerDish = new HashMap<>();
        for (CartLine c : cartLines) {
            totalPerDish.merge(c.dish, c.qty.get(), Integer::sum);
        }
        for (Map.Entry<Dish, Integer> entry : totalPerDish.entrySet()) {
            Dish dish = entry.getKey();
            int requested = entry.getValue();
            if (!dish.available.get() || dish.remaining.get() <= 0) {
                throw new IllegalStateException("Món \"" + dish.name.get() + "\" đã hết hàng.");
            }
            if (requested > dish.remaining.get()) {
                throw new IllegalStateException("Món \"" + dish.name.get() + "\" chỉ còn " + dish.remaining.get() + " phần.");
            }
        }

        // 2. Tính tiền và tạo danh sách dòng đơn
        int totalAmount = 0;
        List<Store.Order.Line> storeLines = new ArrayList<>();
        List<OrderItem> dbItems = new ArrayList<>();

        for (CartLine c : cartLines) {
            int subtotal = c.subtotal();
            totalAmount += subtotal;

            storeLines.add(new Store.Order.Line(
                    c.dish.name.get(),
                    c.size.get(),
                    c.qty.get(),
                    c.unitPrice.get(),
                    c.portionMultiplier.get(),
                    c.note.get()
            ));

            OrderItem it = new OrderItem();
            it.setMenuItemId(c.dish.id);
            it.setMenuItemName(c.dish.name.get());
            it.setSizeLabel(c.size.get());
            it.setQuantity(c.qty.get());
            it.setUnitPrice(c.unitPrice.get());
            it.setPortionMultiplier(c.portionMultiplier.get());
            it.setNote(c.note.get());
            dbItems.add(it);

            // Trừ remaining
            c.dish.remaining.set(c.dish.remaining.get() - c.qty.get());
        }

        // 3. Lưu vào Database
        Order dbOrder = new Order();
        dbOrder.setCustomerEmail(customerEmail.trim());
        dbOrder.setPickupTime(pickupTime != null ? pickupTime.trim() : "11:30");
        dbOrder.setNote(note != null ? note.trim() : "");
        dbOrder.setTotalAmount(totalAmount);
        dbOrder.setStatus(Order.Status.PENDING);
        dbOrder.setCreatedAt(LocalDateTime.now());
        dbOrder.setItems(dbItems);

        Order saved = orderDao.createOrder(dbOrder);

        // 4. Tạo Store.Order cho giao diện
        Store.Order storeOrder = new Store.Order(
                saved.getId(),
                customerEmail.trim(),
                saved.getCreatedAt(),
                saved.getPickupTime(),
                saved.getNote(),
                totalAmount,
                storeLines,
                Status.PENDING
        );
        Store.orders.add(0, storeOrder);
        Store.cart.clear();

        logger.info("Đã tạo đơn #" + saved.getId() + " thành công cho " + customerEmail);
        return storeOrder;
    }

    /**
     * Cập nhật trạng thái đơn hàng (Bếp hoặc quản lý duyệt).
     * Khi đơn chuyển sang PREPARING hoặc READY/COMPLETED, tự động trừ kho FIFO theo công thức và size.
     */
    public void updateStatus(int orderId, Status newStatus) {
        if (newStatus == null) return;

        Order.Status dbStatus = Order.Status.fromString(newStatus.name());
        orderDao.updateOrderStatus(orderId, dbStatus);

        // Cập nhật Store in-memory
        for (Store.Order o : Store.orders) {
            if (o.id == orderId) {
                o.status.set(newStatus);
                break;
            }
        }

        // Tự động trừ kho FIFO khi quán bắt đầu chuẩn bị (PREPARING) hoặc hoàn thành (COMPLETED)
        if ((newStatus == Status.PREPARING || newStatus == Status.READY || newStatus == Status.COMPLETED)
                && !deductedOrderIds.contains(orderId)) {
            deductedOrderIds.add(orderId);
            orderDao.getOrderById(orderId).ifPresent(inventoryService::deductForOrder);
            logger.info("Đã tự động trừ nguyên liệu FIFO cho đơn hàng #" + orderId);
        }
    }

    /**
     * Hủy đơn hàng (chỉ cho phép khi đang PENDING).
     * Trả lại số phần còn lại hôm nay (remaining).
     */
    public void cancelOrder(int orderId) {
        Optional<Order> orderOpt = orderDao.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId);
        }

        Order o = orderOpt.get();
        if (o.getStatus() != Order.Status.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy đơn khi quán chưa nhận đơn.");
        }

        orderDao.updateOrderStatus(orderId, Order.Status.CANCELLED);

        // Cập nhật trạng thái in-memory
        for (Store.Order so : Store.orders) {
            if (so.id == orderId) {
                so.status.set(Status.CANCELLED);
                // Trả lại số phần
                for (Store.Order.Line l : so.lines) {
                    for (Dish d : Store.menu) {
                        if (d.name.get().equals(l.name())) {
                            d.remaining.set(d.remaining.get() + l.qty());
                        }
                    }
                }
                break;
            }
        }
        logger.info("Đã hủy đơn hàng #" + orderId);
    }

    /**
     * Làm mới danh sách đơn của sinh viên từ database (dùng cho polling thời gian thực)
     */
    public void refreshMyOrders(String email) {
        if (email == null || email.trim().isEmpty()) return;

        List<Order> dbOrders = orderDao.getOrdersByCustomer(email);
        for (Order dbo : dbOrders) {
            Status s = Status.valueOf(dbo.getStatus().name());
            for (Store.Order so : Store.orders) {
                if (so.id == dbo.getId()) {
                    if (so.status.get() != s) {
                        so.status.set(s);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Làm mới toàn bộ đơn hàng cho màn hình bảng bếp (Vendor)
     */
    public void refreshAllOrders() {
        List<Order> dbOrders = orderDao.getAllOrders();
        for (Order dbo : dbOrders) {
            Status s = Status.valueOf(dbo.getStatus().name());
            boolean exists = false;
            for (Store.Order so : Store.orders) {
                if (so.id == dbo.getId()) {
                    if (so.status.get() != s) {
                        so.status.set(s);
                    }
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                List<Store.Order.Line> lines = new ArrayList<>();
                for (OrderItem it : dbo.getItems()) {
                    lines.add(new Store.Order.Line(
                            it.getMenuItemName(),
                            it.getSizeLabel(),
                            it.getQuantity(),
                            it.getUnitPrice(),
                            it.getPortionMultiplier(),
                            it.getNote()
                    ));
                }
                Store.Order so = new Store.Order(
                        dbo.getId(),
                        dbo.getCustomerEmail(),
                        dbo.getCreatedAt(),
                        dbo.getPickupTime(),
                        dbo.getNote(),
                        dbo.getTotalAmount(),
                        lines,
                        s
                );
                Store.orders.add(so);
            }
        }
    }
}
