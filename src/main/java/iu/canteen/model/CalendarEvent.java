package iu.canteen.model;

import java.time.LocalDate;

public class CalendarEvent {
    private int id;
    private LocalDate date;
    private String eventType;
    private String description;
    private double demandFactor;

    public CalendarEvent() {
        this.demandFactor = 1.0;
    }

    public CalendarEvent(int id, LocalDate date, String eventType, String description, double demandFactor) {
        this.id = id;
        this.date = date;
        this.eventType = eventType;
        this.description = description;
        this.demandFactor = demandFactor;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getDemandFactor() { return demandFactor; }
    public void setDemandFactor(double demandFactor) { this.demandFactor = demandFactor; }
}
