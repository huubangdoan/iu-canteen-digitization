package iu.canteen.dao;

import iu.canteen.db.Database;
import iu.canteen.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, full_name, email, student_id, role, created_at FROM users WHERE lower(email) = lower(?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by email: " + email, e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT id, full_name, email, student_id, role, created_at FROM users WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by id: " + id, e);
        }
        return Optional.empty();
    }

    public String getPasswordHash(String email) {
        String sql = "SELECT password FROM users WHERE lower(email) = lower(?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting password for: " + email, e);
        }
        return null;
    }

    public User create(String fullName, String email, String studentId, String hashedPassword, String role) {
        String sql = "INSERT INTO users (full_name, email, student_id, password, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, email.trim().toLowerCase());
            ps.setString(3, studentId);
            ps.setString(4, hashedPassword);
            ps.setString(5, role);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new User(id, fullName, email.trim().toLowerCase(), studentId, role, LocalDateTime.now());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creating user: " + email, e);
        }
        return null;
    }

    public void updatePassword(int userId, String newHashedPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating password for user: " + userId, e);
        }
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, full_name, email, student_id, role, created_at FROM users ORDER BY id ASC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting all users", e);
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String studentId = rs.getString("student_id");
        String role = rs.getString("role");
        String createdStr = rs.getString("created_at");
        LocalDateTime created = LocalDateTime.now();
        if (createdStr != null) {
            try {
                created = LocalDateTime.parse(createdStr.replace(" ", "T"));
            } catch (Exception ignored) {}
        }
        return new User(id, fullName, email, studentId, role, created);
    }
}
