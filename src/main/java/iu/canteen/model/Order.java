package iu.canteen.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Order {
    public enum Status {
        PENDING("Chờ nhận"),
        ACCEPTED("Đã nhận"),
        PREPARING("Đang chuẩn bị"),
        READY("Sẵn sàng"),
        COMPLETED("Hoàn thành"),
        CANCELLED("Đã hủy");

        public final String label;
        Status(String label) { this.label = label; }

        public Status next() {
            return switch (this) {
                case PENDING -> ACCEPTED;
                case ACCEPTED -> PREPARING;
                case PREPARING -> READY;
                case READY -> COMPLETED;
                default -> null;
            };
        }

        public static Status fromString(String val) {
            if (val == null) return PENDING;
            for (Status s : values()) {
                if (s.name().equalsIgnoreCase(val) || s.label.equalsIgnoreCase(val)) return s;
            }
            return PENDING;
        }

        @Override
        public String toString() { return label; }
    }

    private int id;
    private Integer userId;
    private String customerEmail;
    private String pickupTime;
    private String note;
    private int totalAmount;
    private Status status = Status.PENDING;
    private LocalDateTime createdAt;
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public Order(int id, Integer userId, String customerEmail, String pickupTime, String note,
                 int totalAmount, Status status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.customerEmail = customerEmail;
        this.pickupTime = pickupTime;
        this.note = note != null ? note : "";
        this.totalAmount = totalAmount;
        this.status = status != null ? status : Status.PENDING;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getTotalAmount() { return totalAmount; }
    public void setTotalAmount(int totalAmount) { this.totalAmount = totalAmount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String summary() {
        if (items == null || items.isEmpty()) return "Không có món";
        StringJoiner j = new StringJoiner(", ");
        for (OrderItem it : items) {
            j.add(it.getMenuItemName() + " (" + it.getSizeLabel() + ") x" + it.getQuantity());
        }
        return j.toString();
    }

    @Override
    public String toString() {
        return "#" + id + " - " + customerEmail + " [" + status.label + "]";
    }
}
