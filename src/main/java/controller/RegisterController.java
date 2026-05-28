package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.User;
import org.hibernate.Session;
import util.HibernateUtil;
import util.PasswordUtil;

public class RegisterController {

    @FXML private TextField txtRegUsername;
    @FXML private TextField txtRegEmail;
    @FXML private PasswordField txtRegPassword;
    @FXML private PasswordField txtRegConfirm;
    @FXML private Label lblRegMessage;

    @FXML
    public void handleRegisterSubmit() {
        String username = txtRegUsername.getText().trim();
        String email = txtRegEmail.getText().trim();
        String password = txtRegPassword.getText().trim();
        String confirm = txtRegConfirm.getText().trim();

        // 1. Kiểm tra rỗng
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            lblRegMessage.setStyle("-fx-text-fill: red;");
            lblRegMessage.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        // 2. Kiểm tra mật khẩu nhập lại
        if (!password.equals(confirm)) {
            lblRegMessage.setStyle("-fx-text-fill: red;");
            lblRegMessage.setText("Mật khẩu xác nhận không khớp!");
            return;
        }

        // 3. Băm mật khẩu bằng SHA-256
        String hashedPassword = PasswordUtil.hashPassword(password);

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setPassword(hashedPassword); // Lưu mật khẩu đã mã hóa

            session.persist(newUser);
            session.getTransaction().commit();

            // Xóa sạch các ô nhập liệu sau khi đăng ký thành công
            txtRegUsername.clear();
            txtRegEmail.clear();
            txtRegPassword.clear();
            txtRegConfirm.clear();

            lblRegMessage.setStyle("-fx-text-fill: green;");
            lblRegMessage.setText("Đăng ký thành công! Hãy bấm 'Quay lại Đăng nhập'.");

        } catch (Exception e) {
            // Nếu lưu bị lỗi (thường là do trùng Username bị chặn bởi Ràng buộc Unique)
            if (session != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            lblRegMessage.setStyle("-fx-text-fill: red;");
            lblRegMessage.setText("Lỗi: Tên đăng nhập hoặc Email đã tồn tại!");
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @FXML
    public void handleBackToLogin() {
        try {
            // Quay lại màn hình Đăng nhập
            Stage stage = (Stage) txtRegUsername.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/doanjava/login-view.fxml"));
            Parent root = loader.load();

            stage.setTitle("Đăng nhập hệ thống");
            stage.setScene(new Scene(root, 700, 500));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}