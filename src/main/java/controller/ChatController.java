package controller;

import dao.ChatDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.util.List;

public class ChatController {

    @FXML private Label lblGroupName;
    @FXML private ListView<String> lvChatMessages;
    @FXML private TextField txtChatInput;

    private final ChatDAO chatDAO = new ChatDAO();

    private int groupId;
    private int userId;
    private Timeline timeline; // Vòng lặp thời gian điều khiển polling

    // Hàm nhận dữ liệu từ màn hình chính truyền sang
    public void initData(int groupId, String groupName, int userId) {
        this.groupId = groupId;
        this.userId = userId;
        this.lblGroupName.setText("Nhóm: " + groupName);

        // 1. Tải dữ liệu lần đầu tiên ngay khi mở cửa sổ
        refreshChat();

        // 2. KÍCH HOẠT TIMELINE: CỨ 5 GIÂY TỰ ĐỘNG GỌI HÀM REFRESHCHAT 1 LẦN
        timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            refreshChat();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE); // Chạy vô hạn
        timeline.play(); // Bắt đầu đếm thời gian
    }

    // Hàm quét CSDL lấy tin nhắn mới
    private void refreshChat() {
        System.out.println("Đang quét CSDL để cập nhật tin nhắn nhóm " + groupId);
        List<String> history = chatDAO.getGroupChatHistorySecure(groupId, userId);

        // Cập nhật lại danh sách hiển thị
        lvChatMessages.getItems().clear();
        lvChatMessages.getItems().addAll(history);

        if (!history.isEmpty()) {
            lvChatMessages.scrollTo(history.size() - 1); // Tự cuộn xuống tin cuối cùng
        }
    }

    @FXML
    public void handleSendMessage() {
        String content = txtChatInput.getText().trim();
        if (!content.isEmpty()) {
            // Lưu trực tiếp xuống CSDL, 5 giây sau lần quét tiếp theo sẽ tự nảy chữ lên UI
            chatDAO.saveMessage(groupId, userId, content);
            txtChatInput.clear();

            // Cập nhật ngay lập tức cho người gửi đỡ phải đợi 5 giây
            refreshChat();
        }
    }

    // Hàm dọn dẹp tắt bộ đếm thời gian khi người dùng tắt cửa sổ chat này đi
    public void closeChatWindow() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}