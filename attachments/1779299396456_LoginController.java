package controller;

import ConnectDatabase.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    private void handleLogin(ActionEvent event) {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Kiểm tra Admin
            String sqlAdmin = "SELECT * FROM admins WHERE username = ? AND password = ?";
            PreparedStatement psA = conn.prepareStatement(sqlAdmin);
            psA.setString(1, user);
            psA.setString(2, pass);
            ResultSet rsA = psA.executeQuery();

            if (rsA.next()) {
                UserSession.loggedInUsername = user; // Lưu 'admin'
                switchScene(event, "/com/example/dacs1/AdminDashboard.fxml", "Hệ thống Quản trị");
                return;
            }

            // 2. Kiểm tra Khách hàng
            String sqlUser = "SELECT * FROM customers WHERE username = ? AND password = ?";
            PreparedStatement psU = conn.prepareStatement(sqlUser);
            psU.setString(1, user);
            psU.setString(2, pass);
            ResultSet rsU = psU.executeQuery();

            if (rsU.next()) {
                UserSession.loggedInUsername = user; // Lưu tên thật (u01, u02...)
                switchScene(event, "/com/example/dacs1/UserDashboard.fxml", "Thực đơn nhà hàng");
                return;
            }

            showAlert("Thất bại", "Sai tên đăng nhập hoặc mật khẩu!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegister(ActionEvent event) {
        try {
            switchScene(event, "/com/example/dacs1/Register.fxml", "Đăng ký tài khoản");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.centerOnScreen();
        stage.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}