import { useEffect, useState } from "react";
import { Modal } from "../Modal/Modal";
import { Button } from "../Button/Button";
import { getSupplierById } from "../../services/supplier";
import type { Supplier } from "../../types/supplier.types";
import styles from "./SupplierDetailModal.module.css";

interface SupplierDetailModalProps {
  supplierId: string | null;
  onClose: () => void;
}

const STATUS_LABEL: Record<string, string> = {
  active: "Hoạt động",
  inactive: "Ngừng hoạt động",
};

// Định dạng ngày giờ
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

// Thành phần hiển thị chi tiết nhà cung cấp
export function SupplierDetailModal({ supplierId, onClose }: SupplierDetailModalProps) {
  const [prevSupplierId, setPrevSupplierId] = useState<string | null>(null);
  const [supplier, setSupplier] = useState<Supplier | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (supplierId !== prevSupplierId) {
    setPrevSupplierId(supplierId);
    setSupplier(null);
    setError(null);
    setLoading(!!supplierId);
  }

  useEffect(() => {
    if (!supplierId) {
      return;
    }

    let cancelled = false;

    getSupplierById(supplierId)
      .then((data) => {
        if (!cancelled) setSupplier(data);
      })
      .catch(() => {
        if (!cancelled) setError("Không thể tải thông tin nhà cung cấp.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [supplierId]);

  const rows: { key: string; value: string; fullWidth: boolean; isStatus?: boolean }[] = supplier
    ? [
        { key: "Mã NCC", value: supplier.code, fullWidth: false },
        { key: "Tên công ty", value: supplier.companyName, fullWidth: false },
        { key: "Mã số thuế", value: supplier.taxCode || "—", fullWidth: false },
        { key: "Người đại diện", value: supplier.representative || "—", fullWidth: false },
        { key: "Người liên hệ", value: supplier.contactPerson || "—", fullWidth: false },
        { key: "Số điện thoại", value: supplier.phone || "—", fullWidth: false },
        { key: "Email", value: supplier.email || "—", fullWidth: false },
        { key: "Trạng thái", value: STATUS_LABEL[supplier.status] ?? supplier.status, fullWidth: false, isStatus: true },
        { key: "Địa chỉ", value: supplier.address || "—", fullWidth: true },
        { key: "Ghi chú", value: supplier.note || "—", fullWidth: true },
        { key: "Ngày thêm", value: formatDateTime(supplier.createdAt), fullWidth: false },
        { key: "Ngày cập nhật", value: formatDateTime(supplier.updatedAt || supplier.createdAt), fullWidth: false },
      ]
    : [];

  return (
    <Modal isOpen={!!supplierId} onClose={onClose} title="Chi tiết nhà cung cấp" size="lg">
      {loading && (
        <div style={{ padding: "32px", textAlign: "center", color: "var(--color-subtext)" }}>
          <i className="fi fi-rr-spinner" style={{ marginRight: 8 }} />
          Đang tải thông tin nhà cung cấp...
        </div>
      )}

      {error && !loading && (
        <div style={{ padding: "32px", textAlign: "center", color: "var(--color-danger)" }}>
          <i className="fi fi-rr-exclamation" style={{ marginRight: 8 }} />
          {error}
        </div>
      )}

      {supplier && !loading && (
        <>
          <div className={styles.detail}>
            {rows.map(({ key, value, fullWidth, isStatus }) => (
              <div
                key={key}
                className={[
                  styles.detailRow,
                  fullWidth ? styles.detailRowFullWidth : "",
                ].join(" ")}
              >
                <span className={styles.detailKey}>{key}</span>
                <span className={styles.detailVal}>
                  {isStatus ? (
                    <span
                      className={[
                        styles.badge,
                        supplier.status === "active" ? styles.active : styles.inactive,
                      ].join(" ")}
                    >
                      {value}
                    </span>
                  ) : (
                    value
                  )}
                </span>
              </div>
            ))}
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
