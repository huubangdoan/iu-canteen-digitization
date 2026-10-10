package iu.canteen.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String fullName;
    private String email;
    private String studentId;
    private String role; // "STUDENT", "VENDOR", "ADMIN"
    private LocalDateTime createdAt;

    public User() {}

    public User(int id, String fullName, String email, String studentId, String role, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.studentId = studentId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isStudent() { return "STUDENT".equalsIgnoreCase(role); }
    public boolean isVendor() { return "VENDOR".equalsIgnoreCase(role); }
    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }

    @Override
    public String toString() {
        return fullName + " (" + email + " - " + role + ")";
    }
}
