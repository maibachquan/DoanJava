import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.HibernateUtil;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Đường dẫn này khớp chính xác với vị trí file hello-view.fxml trong thư mục resources của bạn
            URL fxmlLocation = getClass().getResource("/com/example/doanjava/login-view.fxml");

            if (fxmlLocation == null) {
                System.err.println("Lỗi: Không tìm thấy file hello-view.fxml! Kiểm tra lại đường dẫn.");
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);

            // Thiết lập cửa sổ hiển thị
            primaryStage.setTitle("Hệ thống Quản lý Thời gian và Hợp tác Nhóm");
            primaryStage.setScene(new Scene(root, 700, 500));
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Lỗi khi khởi chạy giao diện: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Giải phóng tài nguyên và ngắt kết nối MySQL an toàn khi đóng ứng dụng
        try {
            HibernateUtil.shutdown();
            System.out.println("Đã đóng kết nối Hibernate và cơ sở dữ liệu an toàn.");
        } catch (Exception e) {
            System.err.println("Lỗi khi đóng kết nối Hibernate: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Kích hoạt ứng dụng JavaFX
        launch(args);
    }
}