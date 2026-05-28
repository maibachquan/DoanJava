package controller;

import dao.ChatDAO;
import dao.TaskDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import model.ChatGroup;
import model.Task;
import javafx.stage.FileChooser;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.awt.Desktop;
import java.io.File;

import java.util.List;
import java.util.Optional;

public class KanbanController {

    // ==========================================
    // KHAI BÁO CÁC THÀNH PHẦN GIAO DIỆN (UI)
    // ==========================================
    @FXML private ListView<Task> lvTodo;
    @FXML private ListView<Task> lvInProgress;
    @FXML private ListView<Task> lvDone;
    @FXML private ListView<ChatGroup> lvGroups;

    // ==========================================
    // CÁC BIẾN LOGIC KẾT NỐI DATABASE
    // ==========================================
    private final ChatDAO chatDAO = new ChatDAO();
    private final TaskDAO taskDAO = new TaskDAO();

    private int myUserId; // Nhận ID từ LoginController truyền sang
    private int currentGroupId = -1; // ID nhóm đang được bôi xanh

    // ==========================================
    // HÀM KHỞI TẠO VÀ NHẬN DỮ LIỆU
    // ==========================================
    @FXML
    public void initialize() {
        // Lắng nghe sự kiện click chuột trên danh sách nhóm
        lvGroups.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                currentGroupId = newValue.getId();
                // Mỗi khi click nhóm, load lại bảng công việc của nhóm đó
                loadGroupTasks(currentGroupId);
            }
        });
    }

    public void initData(int loggedInUserId) {
        this.myUserId = loggedInUserId;
        loadUserGroups(); // Nạp danh sách phòng làm việc sau khi đăng nhập thành công
    }

    private void loadUserGroups() {
        lvGroups.getItems().clear();
        List<ChatGroup> groups = chatDAO.getUserGroups(myUserId);
        lvGroups.getItems().addAll(groups);
    }

    // Tải công việc và rải vào 3 hàng ngang (Cần làm - Đang làm - Đã xong)
    private void loadGroupTasks(int groupId) {
        lvTodo.getItems().clear();
        lvInProgress.getItems().clear();
        lvDone.getItems().clear();

        List<Task> groupTasks = taskDAO.getTasksByGroup(groupId);
        for (Task task : groupTasks) {
            if ("TODO".equalsIgnoreCase(task.getStatus())) {
                lvTodo.getItems().add(task);
            } else if ("IN_PROGRESS".equalsIgnoreCase(task.getStatus())) {
                lvInProgress.getItems().add(task);
            } else if ("DONE".equalsIgnoreCase(task.getStatus())) {
                lvDone.getItems().add(task);
            }
        }
    }

    // ==========================================
    // CHỨC NĂNG 1: QUẢN LÝ THẺ CÔNG VIỆC KANBAN
    // ==========================================
    @FXML
    public void handleAddTask() {
        if (currentGroupId == -1) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một nhóm bên trái trước khi thêm công việc!");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm công việc");
        dialog.setHeaderText("Tạo một công việc mới cho nhóm");
        dialog.setContentText("Tên công việc:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(taskName -> {
            if (!taskName.trim().isEmpty()) {
                Task newTask = new Task();
                newTask.setTitle(taskName);
                newTask.setStatus("TODO");
                newTask.setGroupId(currentGroupId);

                taskDAO.saveTask(newTask);
                lvTodo.getItems().add(newTask);
            }
        });
    }

    @FXML
    public void handleAcceptTask() {
        Task selectedTask = lvTodo.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng click chọn 1 công việc ở mục CẦN LÀM trước!");
            return;
        }

        // Đổi trạng thái và ghi đè danh tính người nhận việc là chính mình
        selectedTask.setStatus("IN_PROGRESS");
        selectedTask.setAssigneeId(myUserId);

        taskDAO.updateTask(selectedTask); // Lưu xuống DB

        // Di chuyển phần tử trên giao diện người dùng
        lvTodo.getItems().remove(selectedTask);
        lvInProgress.getItems().add(selectedTask);
    }

    @FXML
    public void handleCompleteTask() {
        Task selectedTask = lvInProgress.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng click chọn 1 công việc ở mục ĐANG LÀM trước!");
            return;
        }

        // BẢO MẬT: Chỉ chủ nhân công việc mới được nhấn hoàn thành
        if (selectedTask.getAssigneeId() == null || selectedTask.getAssigneeId() != myUserId) {
            showAlert(Alert.AlertType.ERROR, "Lỗi phân quyền", "Đại ca không phụ trách việc này, không thể nhấn Hoàn thành!");
            return;
        }

        // ==================================================
        // HỘP THOẠI HỎI ĐÍNH KÈM FILE
        // ==================================================
        Alert attachAlert = new Alert(Alert.AlertType.CONFIRMATION);
        attachAlert.setTitle("Hoàn thành công việc");
        attachAlert.setHeaderText("Nộp kết quả: " + selectedTask.getTitle());
        attachAlert.setContentText("Đại ca có muốn đính kèm file báo cáo/sản phẩm không?");

        ButtonType btnYes = new ButtonType("Có đính kèm");
        ButtonType btnNo = new ButtonType("Không cần");
        ButtonType btnCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        attachAlert.getButtonTypes().setAll(btnYes, btnNo, btnCancel);

        Optional<ButtonType> result = attachAlert.showAndWait();

        if (result.isPresent() && result.get() == btnCancel) {
            return; // Nếu chọn Hủy thì dừng lại luôn, chưa hoàn thành việc
        }

        // NẾU ĐẠI CA CHỌN "CÓ ĐÍNH KÈM"
        if (result.isPresent() && result.get() == btnYes) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn file báo cáo (PDF, Word, Ảnh...)");
            File file = fileChooser.showOpenDialog(lvInProgress.getScene().getWindow());

            if (file != null) {
                try {
                    // Tạo thư mục "attachments" bên trong project nếu chưa có
                    Path uploadDir = Paths.get("attachments");
                    if (!Files.exists(uploadDir)) {
                        Files.createDirectory(uploadDir);
                    }

                    // Copy file vào thư mục (thêm timestamp để không bị trùng tên file)
                    String fileName = System.currentTimeMillis() + "_" + file.getName();
                    Path targetPath = uploadDir.resolve(fileName);
                    Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                    // Lưu đường dẫn vào Task
                    selectedTask.setFilePath(targetPath.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải file lên hệ thống!");
                    return;
                }
            } else {
                // Tắt cửa sổ FileChooser mà không chọn gì -> Quay lại
                return;
            }
        }

        // ==================================================
        // CẬP NHẬT TRẠNG THÁI VÀ CHUYỂN CỘT
        // ==================================================
        selectedTask.setStatus("DONE");
        taskDAO.updateTask(selectedTask); // Lưu đè Task xuống CSDL (đã có thêm đường dẫn file)

        lvInProgress.getItems().remove(selectedTask);
        lvDone.getItems().add(selectedTask);
        lvDone.refresh(); // Làm mới UI để hiện biểu tượng 📎
    }

    // ==========================================
    // CHỨC NĂNG 2: TẠO NHÓM VÀ MỜI THÀNH VIÊN
    // ==========================================
    @FXML
    public void handleAddGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tạo Nhóm Mới");
        dialog.setHeaderText("Tạo không gian làm việc nhóm");
        dialog.setContentText("Nhập tên nhóm:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(groupName -> {
            if (!groupName.trim().isEmpty()) {
                ChatGroup newGroup = chatDAO.createGroup(groupName, myUserId);
                if (newGroup != null) {
                    loadUserGroups();
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tạo nhóm: " + groupName);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu nhóm vào CSDL!");
                }
            }
        });
    }

    @FXML
    public void handleAddMember() {
        if (currentGroupId == -1) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một nhóm ở danh sách trước!");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm thành viên");
        dialog.setHeaderText("Mời thành viên tham gia nhóm");
        dialog.setContentText("Nhập Username:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            if (!username.trim().isEmpty()) {
                String msg = chatDAO.addMemberByUsername(currentGroupId, username);
                Alert.AlertType type = msg.contains("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
                showAlert(type, "Thông báo", msg);
            }
        });
    }

    // ==========================================
    // CHỨC NĂNG 3: NÚT VÀO XEM TIN NHẮN NHÓM
    // ==========================================
    @FXML
    public void handleOpenChat() {
        ChatGroup selectedGroup = lvGroups.getSelectionModel().getSelectedItem();

        if (selectedGroup == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một nhóm trước khi bấm vào xem tin nhắn!");
            return;
        }

        openSeparateChatWindow(selectedGroup.getId(), selectedGroup.getName());
    }

    private void openSeparateChatWindow(int groupId, String groupName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/doanjava/chat-view.fxml"));
            Parent root = loader.load();

            ChatController chatController = loader.getController();
            chatController.initData(groupId, groupName, myUserId);

            Stage chatStage = new Stage();
            chatStage.setTitle("Trò chuyện nhóm - " + groupName);
            chatStage.setScene(new Scene(root));

            chatStage.setOnCloseRequest(windowEvent -> chatController.closeChatWindow());
            chatStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể khởi tạo cửa sổ chat FXML!");
        }
    }

    // ==========================================
    // CHỨC NĂNG 4: ĐĂNG XUẤT HỆ THỐNG
    // ==========================================
    @FXML
    public void handleLogout() {
        try {
            Stage stage = (Stage) lvTodo.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/doanjava/login-view.fxml"));
            Parent root = loader.load();

            stage.setTitle("Đăng nhập hệ thống");
            stage.setScene(new Scene(root, 700, 500));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    // ==========================================
    // CHỨC NĂNG: THOÁT KHỎI NHÓM
    // ==========================================
    @FXML
    public void handleLeaveGroup() {
        if (currentGroupId == -1) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Đại ca vui lòng chọn một nhóm bên trái trước khi thoát!");
            return;
        }

        ChatGroup selectedGroup = lvGroups.getSelectionModel().getSelectedItem();

        // 1. Hiển thị hộp thoại hỏi cho chắc chắn
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận rời nhóm");
        confirmDialog.setHeaderText("Thoát nhóm: " + selectedGroup.getName());
        confirmDialog.setContentText("Bạn có chắc chắn muốn rời khỏi nhóm này không? Toàn bộ công việc và tin nhắn sẽ không thể xem được nữa.");

        Optional<javafx.scene.control.ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {

            // 2. Gọi DAO để xóa dữ liệu
            boolean success = chatDAO.leaveGroup(currentGroupId, myUserId);

            if (success) {
                // 3. Reset lại giao diện
                currentGroupId = -1; // Hủy chọn nhóm
                loadUserGroups();    // Tải lại danh sách nhóm (nhóm vừa thoát sẽ biến mất)

                // Xóa trắng bảng Kanban vì không còn ở trong nhóm nữa
                lvTodo.getItems().clear();
                lvInProgress.getItems().clear();
                lvDone.getItems().clear();

                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã rời khỏi nhóm!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Hệ thống gặp lỗi, chưa thể rời nhóm lúc này.");
            }
        }
    }
    // ==========================================
    // CHỨC NĂNG: XEM FILE ĐÍNH KÈM CỦA CÔNG VIỆC
    // ==========================================
    @FXML
    public void handleViewAttachment() {
        // Lấy công việc đang được chọn ở mục ĐÃ XONG
        Task selectedTask = lvDone.getSelectionModel().getSelectedItem();

        if (selectedTask == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Đại ca vui lòng click chọn 1 công việc ở mục ĐÃ XONG trước!");
            return;
        }

        // Kiểm tra xem công việc này có file đính kèm không
        if (selectedTask.getFilePath() == null || selectedTask.getFilePath().trim().isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Công việc này được hoàn thành trực tiếp, không có file đính kèm!");
            return;
        }

        try {
            File file = new File(selectedTask.getFilePath());
            if (!file.exists()) {
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "File không tồn tại trên máy!");
                return;
            }

            // DÙNG MULTITHREAD Ở ĐÂY: Mở file trên một luồng khác để không làm đơ giao diện
            new Thread(() -> {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}