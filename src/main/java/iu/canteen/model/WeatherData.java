package iu.canteen.model;

import java.time.LocalDate;

public class WeatherData {
    private int id;
    private LocalDate date;
    private double temperature;
    private double rainProbability;
    private String weatherCondition;

    public WeatherData() {}

    public WeatherData(int id, LocalDate date, double temperature, double rainProbability, String weatherCondition) {
        this.id = id;
        this.date = date;
        this.temperature = temperature;
        this.rainProbability = rainProbability;
        this.weatherCondition = weatherCondition;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getRainProbability() { return rainProbability; }
    public void setRainProbability(double rainProbability) { this.rainProbability = rainProbability; }

    public String getWeatherCondition() { return weatherCondition; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }
}
