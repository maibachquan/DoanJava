package dao;

import model.ChatGroup;
import model.GroupMessage;
import model.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.ArrayList;
import java.util.List;

public class ChatDAO {

    // 1. TẠO NHÓM MỚI VÀ TỰ ĐỘNG THÊM NGƯỜI TẠO VÀO NHÓM
    public ChatGroup createGroup(String name, int creatorId) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            ChatGroup group = new ChatGroup();
            group.setName(name);
            session.persist(group);

            // Thêm người tạo vào bảng trung gian group_members
            session.createNativeQuery("INSERT INTO group_members (group_id, user_id) VALUES (:gId, :uId)")
                    .setParameter("gId", group.getId())
                    .setParameter("uId", creatorId)
                    .executeUpdate();

            transaction.commit();
            return group;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    // 2. CHỈ LẤY CÁC NHÓM MÀ USER LÀ THÀNH VIÊN
    public List<ChatGroup> getUserGroups(int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createNativeQuery(
                            "SELECT g.* FROM chat_groups g " +
                                    "JOIN group_members gm ON g.id = gm.group_id " +
                                    "WHERE gm.user_id = :uId", ChatGroup.class)
                    .setParameter("uId", userId)
                    .list();
        }
    }

    // 3. TẢI LỊCH SỬ CHAT BẢO MẬT (KIỂM TRA QUYỀN TRUY CẬP)
    public List<String> getGroupChatHistorySecure(int groupId, int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Kiểm tra xem thành viên có trong nhóm không
            Long count = session.createNativeQuery(
                            "SELECT COUNT(*) FROM group_members WHERE group_id = :gId AND user_id = :uId", Long.class)
                    .setParameter("gId", groupId)
                    .setParameter("uId", userId)
                    .uniqueResult();

            if (count == 0) {
                List<String> errorList = new ArrayList<>();
                errorList.add("Hệ thống: Bạn không có quyền xem tin nhắn nhóm này!");
                return errorList;
            }

            // Lấy tin nhắn sắp xếp từ cũ đến mới
            List<Object[]> results = session.createNativeQuery(
                            "SELECT u.username, m.content FROM group_messages m " +
                                    "JOIN users u ON m.sender_id = u.id " +
                                    "WHERE m.group_id = :gId ORDER BY m.sent_at ASC")
                    .setParameter("gId", groupId)
                    .list();

            List<String> history = new ArrayList<>();
            for (Object[] row : results) {
                history.add(row[0] + ": " + row[1]);
            }
            return history;
        }
    }

    // 4. LƯU TIN NHẮN MỚI XUỐNG DATABASE
    public void saveMessage(int groupId, int senderId, String content) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            GroupMessage msg = new GroupMessage();
            msg.setGroupId(groupId);
            msg.setSenderId(senderId);
            msg.setContent(content);

            session.persist(msg);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    // 5. THÊM THÀNH VIÊN VÀO NHÓM BẰNG USERNAME (SỬA LỖI ROLLBACK)
    public String addMemberByUsername(int groupId, String targetUsername) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            User targetUser = session.createQuery("FROM User WHERE username = :uname", User.class)
                    .setParameter("uname", targetUsername)
                    .uniqueResult();

            if (targetUser == null) {
                if (transaction != null) transaction.rollback();
                return "Lỗi: Người dùng không tồn tại!";
            }

            session.createNativeQuery("INSERT INTO group_members (group_id, user_id) VALUES (:gId, :uId)")
                    .setParameter("gId", groupId)
                    .setParameter("uId", targetUser.getId())
                    .executeUpdate();

            transaction.commit();
            return "Đã thêm " + targetUsername + " vào nhóm!";
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            return "Lỗi: Người này đã có trong nhóm hoặc lỗi hệ thống!";
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }
    // 6. THOÁT NHÓM (Xóa bản ghi trong bảng trung gian group_members)
    public boolean leaveGroup(int groupId, int userId) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            // Xóa user khỏi nhóm
            int rowsAffected = session.createNativeQuery("DELETE FROM group_members WHERE group_id = :gId AND user_id = :uId")
                    .setParameter("gId", groupId)
                    .setParameter("uId", userId)
                    .executeUpdate();

            transaction.commit();
            return rowsAffected > 0; // Trả về true nếu xóa thành công
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }
}