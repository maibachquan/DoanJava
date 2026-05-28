package dao;

import model.Task;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class TaskDAO {

    // 1. LẤY DANH SÁCH CÔNG VIỆC THEO ID NHÓM
    public List<Task> getTasksByGroup(int groupId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Task WHERE groupId = :gId", Task.class)
                    .setParameter("gId", groupId)
                    .list();
        }
    }

    // 2. LƯU CÔNG VIỆC MỚI VÀO CSDL
    public void saveTask(Task task) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.persist(task);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    // 3. CẬP NHẬT TOÀN BỘ THÔNG TIN CÔNG VIỆC (Dùng khi Nhận việc / Hoàn thành)
    public void updateTask(Task task) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            // Đồng bộ thực thể task đã chỉnh sửa vào CSDL
            session.merge(task);

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }
}