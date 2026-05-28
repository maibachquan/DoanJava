package model;

import jakarta.persistence.*;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String status; // TODO, IN_PROGRESS, DONE

    @Column(name = "group_id")
    private int groupId;

    // LƯU ID NGƯỜI NHẬN VIỆC (Dùng kiểu Integer để có thể nhận giá trị null)
    @Column(name = "assignee_id")
    private Integer assigneeId;


    // ==========================================
    // GETTER VÀ SETTER
    // ==========================================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public Integer getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Integer assigneeId) { this.assigneeId = assigneeId; }

    // THÊM TRƯỜNG NÀY ĐỂ LƯU ĐƯỜNG DẪN FILE ĐÍNH KÈM
    @Column(name = "file_path")
    private String filePath;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    // Sửa lại hàm toString để hiển thị biểu tượng nếu có file
    @Override
    public String toString() {
        return this.title + (this.filePath != null ? " 📎" : "");
    }
}