package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.CalendarEvent;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CalendarEventDao {

    public void upsertEvent(CalendarEvent event) {
        String sql = "INSERT INTO calendar_events (date, event_type, description, demand_factor) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(date) DO UPDATE SET " +
                     "event_type = excluded.event_type, " +
                     "description = excluded.description, " +
                     "demand_factor = excluded.demand_factor";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, event.getDate().toString());
            ps.setString(2, event.getEventType());
            ps.setString(3, event.getDescription());
            ps.setDouble(4, event.getDemandFactor());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error upserting calendar event for date: " + event.getDate(), e);
        }
    }

    public Optional<CalendarEvent> getEventByDate(LocalDate date) {
        String sql = "SELECT id, date, event_type, description, demand_factor FROM calendar_events WHERE date = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new CalendarEvent(
                            rs.getInt("id"),
                            LocalDate.parse(rs.getString("date")),
                            rs.getString("event_type"),
                            rs.getString("description"),
                            rs.getDouble("demand_factor")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting calendar event for date: " + date, e);
        }
        return Optional.empty();
    }

    public List<CalendarEvent> getEventsBetween(LocalDate from, LocalDate to) {
        List<CalendarEvent> list = new ArrayList<>();
        String sql = "SELECT id, date, event_type, description, demand_factor " +
                     "FROM calendar_events " +
                     "WHERE date >= ? AND date <= ? " +
                     "ORDER BY date ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CalendarEvent(
                            rs.getInt("id"),
                            LocalDate.parse(rs.getString("date")),
                            rs.getString("event_type"),
                            rs.getString("description"),
                            rs.getDouble("demand_factor")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting events between dates", e);
        }
        return list;
    }
}
