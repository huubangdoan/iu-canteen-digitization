package iu.canteen.ui;

import iu.canteen.logic.Store;
import iu.canteen.model.Leftover;
import iu.canteen.service.LeftoverService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalTime;
import java.util.List;

/**
 * Giao diện Flash Sale dành cho Sinh viên & Nhân viên (Phase 5):
 * - Banner giờ vàng Flash Sale với đồng hồ đếm ngược thời gian thực
 * - Danh sách thẻ món ăn giảm giá cuối ngày
 * - Thao tác chọn số lượng và đặt mua ngay lập tức
 */
public class StudentFlashSaleView {
    private final LeftoverService leftoverService = new LeftoverService();
    private final FlowPane cardsPane = new FlowPane(16, 16);
    private final Label timerLabel = new Label("00:00:00");
    private Timeline countdownTimeline;
    private Runnable onOrderPlaced;

    public Node build(Runnable onOrderPlaced) {
        this.onOrderPlaced = onOrderPlaced;

        // 1. Banner đầu trang
        VBox bannerText = new VBox(4,
                Ui.label("🔥 GIỜ VÀNG FLASH SALE – GIẢM ĐẾN 50%", "flash-banner-title"),
                Ui.label("Thưởng thức món ngon chất lượng, tiết kiệm chi phí & chung tay chống lãng phí thức ăn tại Canteen IU!", "flash-banner-sub")
        );

        Label timerTitle = Ui.label("KẾT THÚC SAU", "flash-timer-label");
        timerLabel.getStyleClass().add("flash-timer-val");
        VBox timerBox = new VBox(2, timerTitle, timerLabel);
        timerBox.getStyleClass().add("flash-timer-box");

        HBox banner = new HBox(16, bannerText, Ui.spacer(), timerBox);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("flash-banner");

        // Đồng hồ đếm ngược kết thúc Flash Sale lúc 17:30
        startCountdown();

        // 2. Thanh điều hướng phụ & nút làm mới
        Button refreshBtn = Ui.button("🔄 Làm mới danh sách", "btn-small");
        refreshBtn.setOnAction(e -> refresh());

        HBox subBar = new HBox(12,
                Ui.label("Các món đang mở bán giảm giá hôm nay:", "h2"),
                Ui.spacer(),
                refreshBtn
        );
        subBar.setAlignment(Pos.CENTER_LEFT);
        subBar.setPadding(new Insets(10, 0, 4, 0));

        // 3. Vùng chứa danh sách thẻ món Flash Sale
        cardsPane.setPadding(new Insets(6, 0, 16, 0));
        cardsPane.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(cardsPane);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");

        VBox layout = new VBox(14, banner, subBar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        layout.setPadding(new Insets(16));

        refresh();
        return layout;
    }

    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();

        updateTimerDisplay();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimerDisplay()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateTimerDisplay() {
        LocalTime now = LocalTime.now();
        LocalTime endTime = LocalTime.of(17, 30); // Giờ chốt Flash Sale 17:30

        if (now.isAfter(endTime)) {
            timerLabel.setText("ĐÃ HẾT GIỜ");
        } else {
            java.time.Duration diff = java.time.Duration.between(now, endTime);
            long hours = diff.toHours();
            long minutes = diff.toMinutesPart();
            long seconds = diff.toSecondsPart();
            timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        }
    }

    public void refresh() {
        cardsPane.getChildren().clear();
        List<Leftover> items = leftoverService.getAvailableLeftovers();

        if (items.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            emptyBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e5e7eb; -fx-border-radius: 12;");

            Label icon = Ui.label("🍱", "menu-emoji");
            Label title = Ui.label("Hiện chưa có món Flash Sale nào đang mở bán", "h2");
            Label hint = Ui.label("Khung giờ Flash Sale thường mở từ 15:00 - 17:30 mỗi ngày khi bếp canteen kiểm kê thức ăn thừa.", "muted");

            emptyBox.getChildren().addAll(icon, title, hint);
            cardsPane.getChildren().add(emptyBox);
            return;
        }

        for (Leftover item : items) {
            cardsPane.getChildren().add(buildCard(item));
        }
    }

    private Node buildCard(Leftover item) {
        // Thumbnail & Discount Badge
        String emoji = guessEmoji(item.getMenuItemName());
        Label emojiLbl = Ui.label(emoji, "menu-emoji");
        StackPane thumb = new StackPane(emojiLbl);
        thumb.getStyleClass().add("menu-thumb");

        Label discountBadge = Ui.label("-" + item.getDiscountPercent() + "%", "discount-chip");
        StackPane.setAlignment(discountBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(discountBadge, new Insets(8));
        thumb.getChildren().add(discountBadge);

        // Tên món và Size
        Label name = Ui.label(item.getMenuItemName(), "dish-name");
        Label sizeBadge = Ui.label("Size " + item.getSizeLabel(), "size-badge");
        if ("S".equalsIgnoreCase(item.getSizeLabel())) sizeBadge.getStyleClass().add("size-s");
        else if ("L".equalsIgnoreCase(item.getSizeLabel())) sizeBadge.getStyleClass().add("size-l");
        else sizeBadge.getStyleClass().add("size-m");

        HBox titleRow = new HBox(8, name, Ui.spacer(), sizeBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Giá gốc (gạch ngang) và Giá giảm Flash Sale
        Label origPrice = Ui.label(Ui.money(item.getOriginalPrice()), "price-strike");
        Label flashPrice = Ui.label(Ui.money(item.getDiscountedPrice()), "flash-price");
        HBox priceRow = new HBox(8, origPrice, flashPrice);
        priceRow.setAlignment(Pos.BASELINE_LEFT);

        // Số phần còn lại
        Label stockLbl = Ui.label("⚡ Chỉ còn " + item.getRemainingQuantity() + " suất", "stock-warning-chip");

        // Bộ chọn số lượng đặt mua
        Spinner<Integer> qtySpinner = new Spinner<>(1, Math.max(1, item.getRemainingQuantity()), 1);
        qtySpinner.setPrefWidth(70);

        Button buyBtn = Ui.button("⚡ Đặt ngay", "btn-flash");
        buyBtn.setOnAction(e -> {
            int qty = qtySpinner.getValue();
            try {
                String email = (Store.currentUser != null && !Store.currentUser.isBlank()) ? Store.currentUser : "student@iu.edu.vn";
                leftoverService.purchaseLeftover(item.getId(), qty, email, "Đặt từ tab Flash Sale");
                Ui.info("Đặt món thành công! 🎉",
                        String.format("Bạn đã mua %d suất %s (Flash Sale)!\nĐơn hàng đã được gửi đến bảng bếp.",
                                qty, item.getMenuItemName()));
                refresh();
                if (onOrderPlaced != null) onOrderPlaced.run();
            } catch (Exception ex) {
                Ui.warn("Không thể đặt món", ex.getMessage());
            }
        });

        HBox actionRow = new HBox(8, qtySpinner, buyBtn);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(10, thumb, titleRow, priceRow, stockLbl, actionRow);
        card.getStyleClass().add("flash-card");
        return card;
    }

    private String guessEmoji(String name) {
        if (name == null) return "🍱";
        String lower = name.toLowerCase();
        if (lower.contains("gà") || lower.contains("sườn") || lower.contains("cơm")) return "🍛";
        if (lower.contains("phở") || lower.contains("bún")) return "🍜";
        if (lower.contains("bánh mì")) return "🥪";
        if (lower.contains("trà") || lower.contains("cà phê") || lower.contains("nước")) return "🧋";
        if (lower.contains("trứng")) return "🍳";
        return "🍱";
    }

    public void stopTimer() {
        if (countdownTimeline != null) countdownTimeline.stop();
    }
}
