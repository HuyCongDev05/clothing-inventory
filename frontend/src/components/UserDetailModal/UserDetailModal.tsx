import { useEffect, useState } from "react";
import { Modal } from "../Modal/Modal";
import { Button } from "../Button/Button";
import { getUserById, type UserResponse } from "../../services/auth";
import { formatDateTime } from "../../utils/formatters";
import styles from "./UserDetailModal.module.css";

interface UserDetailModalProps {
  userId: string | null;
  userName: string;
  onClose: () => void;
}

const ROLE_LABELS: Record<string, string> = {
  admin: "Quản trị viên",
  coordinator: "Nhân viên điều phối",
  "warehouse-staff": "Nhân viên kho",
  "store-keeper": "Thủ kho",
};

// Thành phần hiển thị chi tiết người dùng
export function UserDetailModal({ userId, userName, onClose }: UserDetailModalProps) {
  const [prevUserId, setPrevUserId] = useState<string | null>(null);
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (userId !== prevUserId) {
    setPrevUserId(userId);
    setUser(null);
    setError(null);
    setLoading(!!userId);
  }

  useEffect(() => {
    if (!userId) {
      return;
    }

    let cancelled = false;

    getUserById(userId)
      .then((data) => {
        if (!cancelled) setUser(data);
      })
      .catch(() => {
        if (!cancelled) setError("Không thể tải thông tin người dùng.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [userId]);

  const isActive = user?.status === "ACTIVE";

  return (
    <Modal isOpen={!!userId} onClose={onClose} title={`Thông tin người dùng: ${userName}`} size="md">
      {loading && (
        <div style={{ padding: "32px", textAlign: "center", color: "var(--color-subtext)" }}>
          <i className="fi fi-rr-spinner" style={{ marginRight: 8 }} />
          Đang tải thông tin...
        </div>
      )}

      {error && !loading && (
        <div style={{ padding: "32px", textAlign: "center", color: "var(--color-danger)" }}>
          <i className="fi fi-rr-exclamation" style={{ marginRight: 8 }} />
          {error}
        </div>
      )}

      {user && !loading && (
        <>
          
          <div className={styles.avatarSection}>
            <div className={styles.avatar}>
              {user.fullName?.charAt(0)?.toUpperCase() ?? "?"}
            </div>
            <div className={styles.userInfo}>
              <div className={styles.userName}>
                {user.fullName}
              </div>
              <div className={styles.userUsername}>
                @{user.username}
              </div>
            </div>
            <div className={styles.statusContainer}>
              <span className={`${styles.badge} ${isActive ? styles.active : styles.inactive}`}>
                {isActive ? "Hoạt động" : "Ngừng hoạt động"}
              </span>
            </div>
          </div>

          
          <div className={styles.detail}>
            <div className={styles.detailRow}>
              <span className={styles.detailKey}>Email</span>
              <span className={styles.detailVal}>{user.email || "—"}</span>
            </div>
            <div className={styles.detailRow}>
              <span className={styles.detailKey}>Số điện thoại</span>
              <span className={styles.detailVal}>{user.phone || "—"}</span>
            </div>
            <div className={`${styles.detailRow} ${styles.detailRowFullWidth}`}>
              <span className={styles.detailKey}>Quyền hạn (Vai trò)</span>
              <div className={styles.rolesContainer}>
                {(user.roles ?? []).map((role) => (
                  <span key={role} className={styles.roleBadge}>
                    {ROLE_LABELS[role] ?? role}
                  </span>
                ))}
              </div>
            </div>
            <div className={styles.detailRow}>
              <span className={styles.detailKey}>Ngày tạo tài khoản</span>
              <span className={styles.detailVal}>{formatDateTime(String(user.createdAt))}</span>
            </div>
          </div>

          <div className={styles.modalActions}>
            <Button variant="secondary" onClick={onClose}>
              Đóng
            </Button>
          </div>
        </>
      )}
    </Modal>
  );
}

