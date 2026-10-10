package iu.canteen.service;

import iu.canteen.dao.UserDao;
import iu.canteen.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class AuthService {
    private final UserDao userDao;
    private static User currentUser;

    public AuthService() {
        this.userDao = new UserDao();
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Xác thực đăng nhập bằng email và mật khẩu
     */
    public User login(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập email trường.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        }

        String trimmedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userDao.findByEmail(trimmedEmail);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Tài khoản không tồn tại trên hệ thống.");
        }

        String passwordHash = userDao.getPasswordHash(trimmedEmail);
        if (passwordHash == null || !BCrypt.checkpw(password, passwordHash)) {
            throw new IllegalArgumentException("Mật khẩu không chính xác.");
        }

        User user = userOpt.get();
        currentUser = user;
        return user;
    }

    /**
     * Đăng ký tài khoản sinh viên / cán bộ mới
     */
    public User register(String fullName, String email, String studentId, String password, String role) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }

        String trimmedEmail = email.trim().toLowerCase();
        if (!trimmedEmail.endsWith("@iu.edu.vn")) {
            throw new IllegalArgumentException("Email phải có đuôi @iu.edu.vn của trường Quốc Tế.");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự.");
        }

        if (userDao.findByEmail(trimmedEmail).isPresent()) {
            throw new IllegalArgumentException("Email này đã được đăng ký tài khoản.");
        }

        String assignedRole = (role != null && !role.trim().isEmpty()) ? role.toUpperCase() : "STUDENT";
        if (trimmedEmail.startsWith("admin")) {
            assignedRole = "ADMIN";
        } else if (trimmedEmail.startsWith("vendor")) {
            assignedRole = "VENDOR";
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(10));
        User newUser = userDao.create(fullName.trim(), trimmedEmail, studentId != null ? studentId.trim() : "", hashedPassword, assignedRole);
        currentUser = newUser;
        return newUser;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
