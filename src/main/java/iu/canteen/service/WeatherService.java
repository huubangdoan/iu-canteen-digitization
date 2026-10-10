package iu.canteen.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import iu.canteen.dao.WeatherDao;
import iu.canteen.model.WeatherData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Dịch vụ thời tiết: Lấy dự báo từ OpenWeatherMap API hoặc tạo mô phỏng thực tế
 * cho khu vực Đại học Quốc tế IU (Thủ Đức, TP.HCM), lưu DB và tính hệ số nhu cầu canteen.
 */
public class WeatherService {
    private static final Logger logger = Logger.getLogger(WeatherService.class.getName());

    // Tọa độ ĐH Quốc tế IU - ĐHQG TP.HCM
    private static final double IU_LAT = 10.8779;
    private static final double IU_LON = 106.8018;

    private final WeatherDao weatherDao;
    private final HttpClient httpClient;
    private String apiKey = ""; // Người dùng có thể truyền API key nếu có

    public WeatherService() {
        this.weatherDao = new WeatherDao();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public WeatherService(WeatherDao weatherDao) {
        this.weatherDao = weatherDao;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    /**
     * Lấy dự báo thời tiết cho N ngày tới (kết hợp cache DB và fetch mới).
     */
    public List<WeatherData> getForecast(int days) {
        LocalDate today = LocalDate.now();
        List<WeatherData> cached = weatherDao.getWeatherForecast(today, days);

        if (cached.size() >= days) {
            return cached;
        }

        // Nếu thiếu ngày trong DB, thử fetch từ API hoặc tạo dữ liệu thực tế
        List<WeatherData> fresh = fetchOrSimulateForecast(days);
        for (WeatherData w : fresh) {
            weatherDao.upsertWeather(w);
        }
        return weatherDao.getWeatherForecast(today, days);
    }

    public List<WeatherData> fetchForecast(int days) {
        return getForecast(days);
    }

    /**
     * Lấy thời tiết của một ngày cụ thể (mặc định hôm nay nếu null).
     */
    public WeatherData getWeatherByDate(LocalDate date) {
        if (date == null) date = LocalDate.now();
        Optional<WeatherData> opt = weatherDao.getWeatherByDate(date);
        if (opt.isPresent()) return opt.get();

        // Tạo dữ liệu thời tiết mặc định cho ngày đó
        WeatherData simulated = generateSimulatedWeather(date);
        weatherDao.upsertWeather(simulated);
        return simulated;
    }

    /**
     * Tính toán hệ số nhu cầu ăn uống tại canteen dựa trên thời tiết:
     * - Mưa to/bão: Sinh viên ngại di chuyển sang canteen -> 0.75
     * - Mưa nhẹ: 0.90
     * - Nắng nóng gay gắt (> 33°C): Nhu cầu nước giải khát tăng -> 1.10
     * - Mát mẻ dễ chịu (27 - 31°C): 1.05
     * - Bình thường: 1.0
     */
    public double getDemandFactor(WeatherData w) {
        if (w == null) return 1.0;

        String cond = w.getWeatherCondition() != null ? w.getWeatherCondition().toLowerCase() : "";
        double rainProb = w.getRainProbability();
        double temp = w.getTemperature();

        if (cond.contains("thunderstorm") || cond.contains("bão") || rainProb >= 0.70) {
            return 0.75;
        } else if (cond.contains("rain") || cond.contains("mưa") || rainProb >= 0.40) {
            return 0.90;
        } else if (temp >= 33.5) {
            return 1.10;
        } else if (temp >= 27.0 && temp <= 31.0) {
            return 1.05;
        }

        return 1.0;
    }

    private List<WeatherData> fetchOrSimulateForecast(int days) {
        if (!apiKey.isEmpty()) {
            try {
                String url = String.format(
                        "https://api.openweathermap.org/data/2.5/forecast?lat=%.4f&lon=%.4f&appid=%s&units=metric&lang=vi",
                        IU_LAT, IU_LON, apiKey
                );
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    return parseOpenWeatherJson(resp.body(), days);
                }
            } catch (Exception e) {
                logger.warning("Không thể gọi OpenWeather API: " + e.getMessage() + ". Dùng mô phỏng thời tiết IU.");
            }
        }

        // Mô phỏng thời tiết Thủ Đức / IU TP.HCM
        List<WeatherData> list = new ArrayList<>();
        LocalDate cur = LocalDate.now();
        for (int i = 0; i <= days; i++) {
            list.add(generateSimulatedWeather(cur.plusDays(i)));
        }
        return list;
    }

    private List<WeatherData> parseOpenWeatherJson(String json, int maxDays) {
        List<WeatherData> results = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray list = root.getAsJsonArray("list");
            LocalDate lastDate = null;

            for (JsonElement el : list) {
                JsonObject item = el.getAsJsonObject();
                String dtTxt = item.get("dt_txt").getAsString(); // "2026-10-10 12:00:00"
                LocalDate date = LocalDate.parse(dtTxt.substring(0, 10));

                if (!date.equals(lastDate)) {
                    lastDate = date;
                    JsonObject main = item.getAsJsonObject("main");
                    double temp = main.get("temp").getAsDouble();
                    double pop = item.has("pop") ? item.get("pop").getAsDouble() : 0.0;
                    String cond = item.getAsJsonArray("weather").get(0).getAsJsonObject().get("main").getAsString();

                    results.add(new WeatherData(0, date, Math.round(temp * 10) / 10.0, pop, cond));
                    if (results.size() >= maxDays) break;
                }
            }
        } catch (Exception e) {
            logger.warning("Lỗi phân tích JSON OpenWeather: " + e.getMessage());
        }
        return results;
    }

    private WeatherData generateSimulatedWeather(LocalDate date) {
        int daySeed = date.getDayOfYear() + date.getYear();
        // Nhiệt độ TP.HCM dao động 28 - 34°C
        double temp = 29.0 + (daySeed % 6);
        // Xác suất mưa 10% - 60%
        double rainProb = ((daySeed * 17) % 55) / 100.0;
        String cond;
        if (rainProb > 0.45) {
            cond = "Mưa rào nhẹ";
        } else if (temp >= 33.0) {
            cond = "Nắng gắt";
        } else {
            cond = "Nắng nhẹ, có mây";
        }
        return new WeatherData(0, date, temp, rainProb, cond);
    }
}
