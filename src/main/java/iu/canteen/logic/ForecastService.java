package iu.canteen.logic;

import iu.canteen.dao.DailySaleDao;
import iu.canteen.dao.ForecastDao;
import iu.canteen.logic.Store.Dish;
import iu.canteen.logic.Store.ForecastRow;
import iu.canteen.logic.Store.Ingredient;
import iu.canteen.logic.Store.RecipeLine;
import iu.canteen.model.DailySale;
import iu.canteen.model.ForecastEntry;
import iu.canteen.model.WeatherData;
import iu.canteen.service.HolidayService;
import iu.canteen.service.WeatherService;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Dịch vụ dự báo nhu cầu nguyên liệu thông minh (AI Smart Forecast):
 * - Phân tích dữ liệu lịch sử bán hàng (Daily Sales)
 * - Tích hợp hệ số thời tiết (Weather API)
 * - Tích hợp hệ số ngày lễ Việt Nam & lịch học trường (Holiday Service)
 * - Đa dạng thuật toán: Moving Average, WMA, Exponential Smoothing, Linear Regression, Smart AI Ensemble
 */
public final class ForecastService {
    private static final Logger logger = Logger.getLogger(ForecastService.class.getName());

    private static final DailySaleDao dailySaleDao = new DailySaleDao();
    private static final ForecastDao forecastDao = new ForecastDao();
    private static final WeatherService weatherService = new WeatherService();
    private static final HolidayService holidayService = new HolidayService();
    private static final iu.canteen.service.LeftoverService leftoverService = new iu.canteen.service.LeftoverService();

    private ForecastService() {}

    /**
     * Kết quả dự báo chi tiết bao gồm hàng bảng hiển thị và bản ghi lưu database.
     */
    public record ForecastResult(
            List<ForecastRow> rows,
            List<ForecastEntry> dbEntries,
            double weatherFactor,
            double holidayFactor,
            double overallMultiplier,
            double wasteAdjustment
    ) {
        public ForecastResult(List<ForecastRow> rows, List<ForecastEntry> dbEntries, double weatherFactor, double holidayFactor, double overallMultiplier) {
            this(rows, dbEntries, weatherFactor, holidayFactor, overallMultiplier, 0.0);
        }
    }

    /**
     * Phương thức dự báo tương thích ngược.
     */
    public static List<ForecastRow> forecast(int days, ForecastStrategy strategy) {
        ForecastMethod method = (strategy instanceof ForecastMethod fm) ? fm : ForecastMethod.MOVING_AVERAGE;
        return forecastDetailed(days, method, LocalDate.now().plusDays(1)).rows();
    }

    /**
     * Dự báo chi tiết toàn diện với các thuật toán hiện đại.
     * @param days Số ngày cần dự báo (3 - 7 ngày)
     * @param method Thuật toán dự báo được chọn
     * @param startDate Ngày bắt đầu dự báo (mặc định ngày mai)
     */
    public static ForecastResult forecastDetailed(int days, ForecastMethod method, LocalDate startDate) {
        if (days <= 0) days = 5;
        if (startDate == null) startDate = LocalDate.now().plusDays(1);
        if (method == null) method = ForecastMethod.SMART_AI_ENSEMBLE;

        // 1. Tính toán hệ số thời tiết và ngày lễ trung bình trong khoảng thời gian dự báo
        double totalWeatherFactor = 0.0;
        double totalHolidayFactor = 0.0;
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            WeatherData w = weatherService.getWeatherByDate(d);
            totalWeatherFactor += weatherService.getDemandFactor(w);
            totalHolidayFactor += holidayService.getDemandFactor(d);
        }
        double avgWeatherFactor = round2(totalWeatherFactor / days);
        double avgHolidayFactor = round2(totalHolidayFactor / days);

        // Hệ số tổng hợp tùy thuộc vào phương pháp
        double baseMultiplier = method.demandMultiplier(days);
        double overallMultiplier = baseMultiplier;
        if (method == ForecastMethod.SMART_AI_ENSEMBLE) {
            overallMultiplier = round2(avgWeatherFactor * avgHolidayFactor);
        }

        // 2. Dự báo số phần bán mỗi ngày cho từng món ăn
        Map<Dish, Double> dishPredictedDaily = new HashMap<>();
        for (Dish dish : Store.menu) {
            double predictedDaily = predictDishSales(dish, method, days, startDate);
            dishPredictedDaily.put(dish, predictedDaily);
        }

        // 3. Tính toán nhu cầu nguyên liệu dựa trên công thức món ăn (Recipe)
        Map<Ingredient, Double> need = new LinkedHashMap<>();
        for (Ingredient ingredient : Store.ingredients) {
            need.put(ingredient, 0.0);
        }

        double totalWasteAdjustment = 0.0;
        for (Map.Entry<Dish, Double> entry : dishPredictedDaily.entrySet()) {
            Dish dish = entry.getKey();
            double predictedDailyPortions = entry.getValue();

            // Tính hệ số điều chỉnh lãng phí từ lịch sử thức ăn thừa (waste_adjustment: 0.0 ~ 0.30)
            double wasteAdjustment = leftoverService.getWasteAdjustmentFactor(dish.id, 7);
            totalWasteAdjustment += wasteAdjustment;
            double wasteMultiplier = Math.max(0.70, 1.0 - wasteAdjustment);

            // Tính hệ số phần trung bình dựa trên các size có sẵn của món
            double avgPortionMultiplier = dish.sizes.isEmpty() ? 1.0 :
                    dish.sizes.stream().mapToDouble(iu.canteen.model.MenuItemSize::getPortionMultiplier).average().orElse(1.0);

            for (RecipeLine recipeLine : Store.recipeFor(dish)) {
                double totalDishNeed = predictedDailyPortions * recipeLine.adjustedQty(avgPortionMultiplier) * days * overallMultiplier * wasteMultiplier;
                need.merge(recipeLine.ingredient, totalDishNeed, Double::sum);
            }
        }
        double avgWasteAdjustment = dishPredictedDaily.isEmpty() ? 0.0 : round2(totalWasteAdjustment / dishPredictedDaily.size());

        // 4. Tạo các hàng hiển thị (ForecastRow) và đối tượng lưu DB (ForecastEntry)
        List<ForecastRow> rows = new ArrayList<>();
        List<ForecastEntry> dbEntries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<Ingredient, Double> entry : need.entrySet()) {
            Ingredient ingredient = entry.getKey();
            double required = round1(entry.getValue());
            double stock = round1(ingredient.stock.get());
            double buy = Math.max(0, round1(required + ingredient.minStock - stock));
            String level = buy <= 0 ? "Đủ" : (stock < required ? "Khẩn cấp" : "Nên mua");

            rows.add(new ForecastRow(ingredient.name, ingredient.unit, stock, required, buy, level));

            ForecastEntry fe = new ForecastEntry(
                    0,
                    startDate,
                    ingredient.id,
                    ingredient.name,
                    ingredient.unit,
                    stock,
                    required,
                    null,
                    method.name(),
                    now
            );
            dbEntries.add(fe);
        }

        logger.info(String.format("Dự báo hoàn tất: method=%s, days=%d, weatherFactor=%.2f, holidayFactor=%.2f, wasteAdjustment=%.2f, overall=%.2f",
                method.name(), days, avgWeatherFactor, avgHolidayFactor, avgWasteAdjustment, overallMultiplier));

        return new ForecastResult(rows, dbEntries, avgWeatherFactor, avgHolidayFactor, overallMultiplier, avgWasteAdjustment);
    }

    /**
     * Dự báo số suất bán trung bình/ngày cho một món ăn theo thuật toán cụ thể.
     */
    public static double predictDishSales(Dish dish, ForecastMethod method, int days, LocalDate targetDate) {
        List<DailySale> sales = dailySaleDao.getSalesByMenuItem(dish.id, 14);

        // Nếu chưa có đủ lịch sử bán trong DB, sử dụng avgDaily làm giá trị ban đầu
        if (sales.isEmpty()) {
            return dish.avgDaily > 0 ? dish.avgDaily : 30.0;
        }

        // Lấy chuỗi số suất bán theo thứ tự thời gian tăng dần
        List<Double> series = new ArrayList<>();
        for (int i = sales.size() - 1; i >= 0; i--) {
            series.add((double) sales.get(i).getQuantitySold());
        }

        return switch (method) {
            case MOVING_AVERAGE -> computeSMA(series, 7);
            case WEIGHTED_MOVING_AVG -> computeWMA(series, 7);
            case EXPONENTIAL_SMOOTHING -> computeExponentialSmoothing(series, 0.35);
            case LINEAR_REGRESSION -> computeLinearRegression(series);
            case WEEKDAY_AVERAGE -> computeWeekdayAverage(sales, targetDate, dish.avgDaily);
            case SMART_AI_ENSEMBLE -> {
                // Kết hợp có trọng số giữa WMA (50%), Exponential Smoothing (30%) và Regression (20%)
                double wma = computeWMA(series, 7);
                double es = computeExponentialSmoothing(series, 0.35);
                double reg = computeLinearRegression(series);
                yield round1(0.50 * wma + 0.30 * es + 0.20 * reg);
            }
        };
    }

    /**
     * Simple Moving Average (SMA)
     */
    public static double computeSMA(List<Double> series, int k) {
        if (series.isEmpty()) return 30.0;
        int count = Math.min(series.size(), k);
        double sum = 0.0;
        for (int i = series.size() - count; i < series.size(); i++) {
            sum += series.get(i);
        }
        return round1(sum / count);
    }

    /**
     * Weighted Moving Average (WMA) – ngày gần đây hơn có trọng số lớn hơn
     */
    public static double computeWMA(List<Double> series, int k) {
        if (series.isEmpty()) return 30.0;
        int count = Math.min(series.size(), k);
        double weightedSum = 0.0;
        double weightTotal = 0.0;
        int weight = 1;
        for (int i = series.size() - count; i < series.size(); i++) {
            weightedSum += series.get(i) * weight;
            weightTotal += weight;
            weight++;
        }
        return round1(weightedSum / weightTotal);
    }

    /**
     * Exponential Smoothing: S_t = alpha * Y_t + (1 - alpha) * S_{t-1}
     */
    public static double computeExponentialSmoothing(List<Double> series, double alpha) {
        if (series.isEmpty()) return 30.0;
        double s = series.get(0);
        for (int i = 1; i < series.size(); i++) {
            s = alpha * series.get(i) + (1.0 - alpha) * s;
        }
        return round1(s);
    }

    /**
     * Linear Regression Trendline (sử dụng Apache Commons Math SimpleRegression)
     */
    public static double computeLinearRegression(List<Double> series) {
        if (series.size() < 2) return series.isEmpty() ? 30.0 : series.get(0);
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < series.size(); i++) {
            regression.addData(i + 1, series.get(i));
        }
        double nextVal = regression.predict(series.size() + 1);
        return round1(Math.max(5.0, nextVal)); // Không âm, tối thiểu 5 suất
    }

    /**
     * Lọc và tính trung bình theo thứ trong tuần (Thứ Hai, Thứ Ba...)
     */
    private static double computeWeekdayAverage(List<DailySale> sales, LocalDate targetDate, double fallback) {
        if (sales.isEmpty()) return fallback;
        int targetDow = targetDate.getDayOfWeek().getValue();
        double sum = 0.0;
        int count = 0;
        for (DailySale s : sales) {
            if (s.getDate().getDayOfWeek().getValue() == targetDow) {
                sum += s.getQuantitySold();
                count++;
            }
        }
        return count > 0 ? round1(sum / count) : (fallback > 0 ? fallback : 30.0);
    }

    /**
     * Lưu danh sách dự báo vào Database
     */
    public static void saveForecastToDatabase(List<ForecastEntry> entries) {
        forecastDao.insertAll(entries);
        logger.info("Đã lưu " + entries.size() + " mục dự báo vào database.");
    }

    /**
     * Tính toán % độ chính xác dự báo (Accuracy rate)
     */
    public static double getAccuracy(LocalDate from, LocalDate to) {
        return forecastDao.getAverageAccuracy(from, to);
    }

    /**
     * Tính toán lượng nguyên liệu cần thiết cho một món ăn cụ thể theo size và số lượng phần.
     */
    public static double calculateRecipeNeed(RecipeLine recipeLine, double portionMultiplier, int quantity) {
        return round1(recipeLine.adjustedQty(portionMultiplier) * quantity);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
