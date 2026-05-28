package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.User;
import org.hibernate.Session;
import util.HibernateUtil;
import util.PasswordUtil;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMessage;

    @FXML
    public void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setStyle("-fx-text-fill: red;");
            lblMessage.setText("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        // Mã hóa mật khẩu người dùng vừa nhập
        String hashedPassword = PasswordUtil.hashPassword(password);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // So sánh mật khẩu đã mã hóa với CSDL
            User user = session.createQuery("FROM User WHERE username = :user AND password = :pass", User.class)
                    .setParameter("user", username)
                    .setParameter("pass", hashedPassword)
                    .uniqueResult();

            if (user != null) {
                // Đăng nhập thành công, tải màn hình chính (đã đổi tên thành main-view.fxml)
                Stage stage = (Stage) txtUsername.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/doanjava/main-view.fxml"));
                Parent root = loader.load();

                // Bơm ID người dùng thật sang KanbanController
                KanbanController kanbanController = loader.getController();
                kanbanController.initData(user.getId());

                stage.setTitle("Workspace - Xin chào " + user.getUsername());
                stage.setScene(new Scene(root, 900, 600));
            } else {
                lblMessage.setStyle("-fx-text-fill: red;");
                lblMessage.setText("Sai tài khoản hoặc mật khẩu!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblMessage.setText("Lỗi: Không thể tải giao diện hoặc mất kết nối CSDL!");
        }
    }

    @FXML
    public void handleRegister() {
        try {
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/doanjava/register-view.fxml"));
            Parent root = loader.load();

            stage.setTitle("Đăng ký tài khoản mới");
            stage.setScene(new Scene(root, 700, 500));
        } catch (Exception e) {
            e.printStackTrace();
            lblMessage.setText("Lỗi: Không tìm thấy file register-view.fxml");
        }
    }
}