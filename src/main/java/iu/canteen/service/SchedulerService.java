package iu.canteen.service;

import iu.canteen.dao.DailySaleDao;
import iu.canteen.model.WeatherData;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Quản lý tác vụ chạy ngầm định kỳ (Scheduler):
 * - Tự động tổng hợp số liệu bán hàng (daily_sales) từ đơn hàng thực tế
 * - Tự động đồng bộ và nạp dự báo thời tiết mới nhất từ OpenWeatherMap
 */
public class SchedulerService {
    private static final Logger logger = Logger.getLogger(SchedulerService.class.getName());
    private static ScheduledExecutorService scheduler;
    private static boolean autoSyncEnabled = true;

    private final DailySaleDao dailySaleDao;
    private final WeatherService weatherService;

    public SchedulerService() {
        this.dailySaleDao = new DailySaleDao();
        this.weatherService = new WeatherService();
    }

    public SchedulerService(DailySaleDao dailySaleDao, WeatherService weatherService) {
        this.dailySaleDao = dailySaleDao;
        this.weatherService = weatherService;
    }

    /**
     * Bắt đầu tiến trình định kỳ chạy ngầm (chạy mỗi 30 phút).
     */
    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "IU-Canteen-Scheduler");
            t.setDaemon(true); // Daemon thread không cản trở JVM tắt
            return t;
        });

        // Chạy ngay sau 10 giây và lặp lại mỗi 30 phút
        scheduler.scheduleAtFixedRate(this::runPeriodicJobs, 10, 1800, TimeUnit.SECONDS);
        logger.info("SchedulerService đã được khởi chạy thành công.");
    }

    /**
     * Dừng tiến trình định kỳ.
     */
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            logger.info("SchedulerService đã dừng.");
        }
    }

    /**
     * Thực thi các tác vụ định kỳ tự động.
     */
    public void runPeriodicJobs() {
        if (!autoSyncEnabled) {
            logger.info("Tự động đồng bộ đang tắt.");
            return;
        }
        try {
            logger.info("Bắt đầu tác vụ định kỳ tự động...");
            int aggregated = aggregateTodaySales();
            int weatherCount = syncWeather();
            logger.info(String.format("Tác vụ định kỳ hoàn tất: %d mục bán hàng tổng hợp, %d mục thời tiết cập nhật.",
                    aggregated, weatherCount));
        } catch (Exception e) {
            logger.warning("Lỗi khi chạy tác vụ định kỳ: " + e.getMessage());
        }
    }

    /**
     * Tổng hợp doanh số bán hàng của ngày hôm nay từ orders sang daily_sales.
     */
    public int aggregateTodaySales() {
        return aggregateSalesForDate(LocalDate.now());
    }

    /**
     * Tổng hợp doanh số cho ngày cụ thể.
     */
    public int aggregateSalesForDate(LocalDate date) {
        if (date == null) date = LocalDate.now();
        int affected = dailySaleDao.aggregateSalesFromOrders(date);
        logger.info("Đã tổng hợp " + affected + " dòng doanh số bán hàng cho ngày " + date);
        return affected;
    }

    /**
     * Tự động lấy dự báo thời tiết 5 ngày tới từ OpenWeatherMap API hoặc cache.
     */
    public int syncWeather() {
        try {
            List<WeatherData> forecast = weatherService.fetchForecast(5);
            logger.info("Đã đồng bộ dự báo thời tiết cho " + forecast.size() + " ngày.");
            return forecast.size();
        } catch (Exception e) {
            logger.warning("Không thể đồng bộ thời tiết từ API: " + e.getMessage());
            return 0;
        }
    }

    public static boolean isAutoSyncEnabled() {
        return autoSyncEnabled;
    }

    public static void setAutoSyncEnabled(boolean enabled) {
        autoSyncEnabled = enabled;
    }
}
