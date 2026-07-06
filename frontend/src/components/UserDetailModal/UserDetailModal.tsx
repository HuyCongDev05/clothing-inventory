import { useEffect, useState } from "react";
import { Modal } from "../Modal/Modal";
import { Button } from "../Button/Button";
import { getUserById, type UserResponse } from "../../services/auth";

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

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return "—";
  try {
    return new Date(dateStr).toLocaleString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return dateStr;
  }
}

export function UserDetailModal({ userId, userName, onClose }: UserDetailModalProps) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!userId) {
      setUser(null);
      setError(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

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
          {/* Avatar + tên */}
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "16px",
              padding: "16px",
              marginBottom: "12px",
              backgroundColor: "var(--color-bg)",
              borderRadius: "var(--radius-md)",
              border: "1px solid var(--color-border)",
            }}
          >
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: "50%",
                backgroundColor: "var(--color-primary-light, var(--color-hover))",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: "1.4rem",
                color: "var(--color-primary)",
                fontWeight: 700,
                flexShrink: 0,
              }}
            >
              {user.fullName?.charAt(0)?.toUpperCase() ?? "?"}
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: "var(--font-md)", color: "var(--color-text)" }}>
                {user.fullName}
              </div>
              <div style={{ fontSize: "var(--font-sm)", color: "var(--color-subtext)" }}>
                @{user.username}
              </div>
            </div>
            <div style={{ marginLeft: "auto" }}>
              <span
                style={{
                  display: "inline-block",
                  padding: "3px 12px",
                  borderRadius: "var(--radius-full)",
                  fontSize: "var(--font-xs)",
                  fontWeight: 600,
                  backgroundColor: isActive ? "var(--color-success-light)" : "var(--color-hover)",
                  color: isActive ? "var(--color-success)" : "var(--color-subtext)",
                }}
              >
                {isActive ? "Hoạt động" : "Ngừng hoạt động"}
              </span>
            </div>
          </div>

          {/* Chi tiết */}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px 20px" }}>
            <div style={rowStyle}>
              <span style={labelStyle}>Email</span>
              <span style={valueStyle}>{user.email || "—"}</span>
            </div>
            <div style={rowStyle}>
              <span style={labelStyle}>Số điện thoại</span>
              <span style={valueStyle}>{user.phone || "—"}</span>
            </div>
            <div style={{ ...rowStyle, gridColumn: "1 / -1" }}>
              <span style={labelStyle}>Quyền hạn (Vai trò)</span>
              <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "4px" }}>
                {(user.roles ?? []).map((role) => (
                  <span
                    key={role}
                    style={{
                      padding: "2px 10px",
                      borderRadius: "var(--radius-full)",
                      fontSize: "var(--font-xs)",
                      fontWeight: 600,
                      backgroundColor: "var(--color-info-light, var(--color-hover))",
                      color: "var(--color-primary)",
                      border: "1px solid var(--color-border)",
                    }}
                  >
                    {ROLE_LABELS[role] ?? role}
                  </span>
                ))}
              </div>
            </div>
            <div style={rowStyle}>
              <span style={labelStyle}>Ngày tạo tài khoản</span>
              <span style={valueStyle}>{formatDateTime(String(user.createdAt))}</span>
            </div>
          </div>

          <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "16px" }}>
            <Button variant="secondary" onClick={onClose}>
              Đóng
            </Button>
          </div>
        </>
      )}
    </Modal>
  );
}

const rowStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "2px",
  padding: "8px 12px",
  backgroundColor: "var(--color-bg)",
  borderRadius: "var(--radius-sm)",
  border: "1px solid var(--color-border)",
};

const labelStyle: React.CSSProperties = {
  fontSize: "var(--font-xs)",
  color: "var(--color-subtext)",
  fontWeight: 500,
  textTransform: "uppercase",
  letterSpacing: "0.04em",
};

const valueStyle: React.CSSProperties = {
  fontSize: "var(--font-sm)",
  color: "var(--color-text)",
  fontWeight: 500,
};
