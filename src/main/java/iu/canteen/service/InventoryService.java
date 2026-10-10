package iu.canteen.service;

import iu.canteen.dao.*;
import iu.canteen.logic.Store;
import iu.canteen.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Quản lý kho, nhập kho theo lô, xuất kho tự động theo FIFO và ghi nhận hao hụt.
 */
public class InventoryService {
    private static final Logger logger = Logger.getLogger(InventoryService.class.getName());

    private final BatchDao batchDao;
    private final IngredientDao ingredientDao;
    private final WasteDao wasteDao;
    private final RecipeDao recipeDao;

    public InventoryService() {
        this.batchDao = new BatchDao();
        this.ingredientDao = new IngredientDao();
        this.wasteDao = new WasteDao();
        this.recipeDao = new RecipeDao();
    }

    public InventoryService(BatchDao batchDao, IngredientDao ingredientDao, WasteDao wasteDao, RecipeDao recipeDao) {
        this.batchDao = batchDao;
        this.ingredientDao = ingredientDao;
        this.wasteDao = wasteDao;
        this.recipeDao = recipeDao;
    }

    /**
     * Nhập kho một lô hàng mới:
     * - Lưu vào bảng inventory_batches
     * - Tăng tồn kho current_stock trong bảng ingredients
     * - Cập nhật đồng bộ Store trong bộ nhớ
     */
    public InventoryBatch receiveBatch(int ingredientId, double qty, double costPerUnit,
                                       LocalDate receivedDate, LocalDate expiryDate, String batchCode) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Số lượng nhập kho phải lớn hơn 0.");
        }
        if (costPerUnit < 0) {
            throw new IllegalArgumentException("Chi phí đơn vị không được âm.");
        }
        if (expiryDate.isBefore(receivedDate)) {
            throw new IllegalArgumentException("Hạn sử dụng không được trước ngày nhập.");
        }

        String code = (batchCode != null && !batchCode.trim().isEmpty())
                ? batchCode.trim()
                : "BATCH-" + ingredientId + "-" + System.currentTimeMillis() % 10000;

        InventoryBatch created = batchDao.createBatch(ingredientId, qty, costPerUnit, receivedDate, expiryDate, code);
        ingredientDao.addStock(ingredientId, qty);

        // Đồng bộ in-memory Store
        for (Store.Ingredient ing : Store.ingredients) {
            if (ing.id == ingredientId) {
                ing.stock.set(ing.stock.get() + qty);
                Store.batches.add(0, new Store.Batch(ing, qty, costPerUnit * qty, receivedDate, expiryDate));
                break;
            }
        }

        logger.info("Nhập kho thành công: " + qty + " cho nguyên liệu #" + ingredientId + " (Mã lô: " + code + ")");
        return created;
    }

    /**
     * Xuất kho tự động theo FIFO (lô nào hết hạn trước thì xuất trước).
     * @param ingredientId ID nguyên liệu cần trừ
     * @param amountRequired Tổng khối lượng/số lượng cần xuất
     * @return Số lượng thực tế đã trừ được
     */
    public double deductFIFO(int ingredientId, double amountRequired) {
        if (amountRequired <= 0) return 0;

        List<InventoryBatch> fifoBatches = batchDao.getAvailableBatchesFIFO(ingredientId);
        double remainingNeed = amountRequired;

        for (InventoryBatch batch : fifoBatches) {
            if (remainingNeed <= 0) break;

            double availableInBatch = batch.getRemainingQuantity();
            double toDeduct = Math.min(availableInBatch, remainingNeed);

            batchDao.deductQuantity(batch.getId(), toDeduct);
            remainingNeed -= toDeduct;
        }

        // Cập nhật tổng tồn kho trong bảng ingredients
        ingredientDao.addStock(ingredientId, -amountRequired);

        // Cập nhật in-memory Store
        for (Store.Ingredient ing : Store.ingredients) {
            if (ing.id == ingredientId) {
                ing.stock.set(Math.max(0, ing.stock.get() - amountRequired));
                break;
            }
        }

        double deducted = amountRequired - Math.max(0, remainingNeed);
        logger.info("FIFO: Đã trừ " + deducted + "/" + amountRequired + " nguyên liệu #" + ingredientId);
        return deducted;
    }

    /**
     * Tự động trừ nguyên liệu theo định lượng công thức món và size khi đơn hàng được chuẩn bị/hoàn thành.
     */
    public void deductForOrder(Order order) {
        if (order == null || order.getItems() == null) return;

        for (OrderItem item : order.getItems()) {
            List<Recipe> recipes = recipeDao.getRecipesByMenuItemId(item.getMenuItemId());
            double multiplier = item.getPortionMultiplier() > 0 ? item.getPortionMultiplier() : 1.0;
            int qty = item.getQuantity();

            for (Recipe r : recipes) {
                double totalNeeded = r.getQuantity() * multiplier * qty;
                deductFIFO(r.getIngredientId(), totalNeeded);
            }
        }
    }

    /**
     * Ghi nhận hao hụt / hư hỏng nguyên liệu:
     * - Lưu waste_logs
     * - Trừ tồn kho nguyên liệu
     * - Trừ lô hàng FIFO
     */
    public WasteLog logWaste(int ingredientId, double quantity, String reason, LocalDate date, String recordedBy) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng hao hụt phải lớn hơn 0.");
        }

        WasteLog log = new WasteLog(0, ingredientId, quantity, reason, date, recordedBy);
        WasteLog saved = wasteDao.logWaste(log);

        // Trừ FIFO trong các lô và cập nhật tồn kho nguyên liệu
        deductFIFO(ingredientId, quantity);

        // Đồng bộ Store
        for (Store.Ingredient ing : Store.ingredients) {
            if (ing.id == ingredientId) {
                Store.wastes.add(0, new Store.WasteLog(ing, quantity, reason, date));
                break;
            }
        }

        logger.info("Đã ghi nhận hao hụt: " + quantity + " cho nguyên liệu #" + ingredientId + " (Lý do: " + reason + ")");
        return saved;
    }

    public List<InventoryBatch> getAllBatches() {
        return batchDao.getAllBatches();
    }

    public List<InventoryBatch> getExpiringBatches(int withinDays) {
        return batchDao.getExpiringBatches(withinDays);
    }

    public List<Ingredient> getAllIngredients() {
        return ingredientDao.getAll();
    }

    public List<Ingredient> getLowStockAlerts() {
        return ingredientDao.getAll().stream()
                .filter(i -> i.getCurrentStock() < i.getMinStock())
                .toList();
    }
}
