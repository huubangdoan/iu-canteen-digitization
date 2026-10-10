package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.WeatherData;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WeatherDao {

    public void upsertWeather(WeatherData data) {
        String sql = "INSERT INTO weather_data (date, temperature, rain_probability, weather_condition) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(date) DO UPDATE SET " +
                     "temperature = excluded.temperature, " +
                     "rain_probability = excluded.rain_probability, " +
                     "weather_condition = excluded.weather_condition";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, data.getDate().toString());
            ps.setDouble(2, data.getTemperature());
            ps.setDouble(3, data.getRainProbability());
            ps.setString(4, data.getWeatherCondition());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error upserting weather data for date: " + data.getDate(), e);
        }
    }

    public Optional<WeatherData> getWeatherByDate(LocalDate date) {
        String sql = "SELECT id, date, temperature, rain_probability, weather_condition FROM weather_data WHERE date = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new WeatherData(
                            rs.getInt("id"),
                            LocalDate.parse(rs.getString("date")),
                            rs.getDouble("temperature"),
                            rs.getDouble("rain_probability"),
                            rs.getString("weather_condition")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting weather for date: " + date, e);
        }
        return Optional.empty();
    }

    public List<WeatherData> getWeatherForecast(LocalDate from, int days) {
        List<WeatherData> list = new ArrayList<>();
        String sql = "SELECT id, date, temperature, rain_probability, weather_condition " +
                     "FROM weather_data " +
                     "WHERE date >= ? AND date <= ? " +
                     "ORDER BY date ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, from.plusDays(days).toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new WeatherData(
                            rs.getInt("id"),
                            LocalDate.parse(rs.getString("date")),
                            rs.getDouble("temperature"),
                            rs.getDouble("rain_probability"),
                            rs.getString("weather_condition")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting weather forecast range", e);
        }
        return list;
    }
}
