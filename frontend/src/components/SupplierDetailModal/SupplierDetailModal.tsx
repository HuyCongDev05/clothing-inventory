import { useEffect, useState } from "react";
import { Modal } from "../Modal/Modal";
import { Button } from "../Button/Button";
import { getSupplierById } from "../../services/supplier";
import type { Supplier } from "../../types/supplier.types";

interface SupplierDetailModalProps {
  supplierId: string | null;
  onClose: () => void;
}

const STATUS_LABEL: Record<string, string> = {
  active: "Hoạt động",
  inactive: "Ngừng hoạt động",
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

export function SupplierDetailModal({ supplierId, onClose }: SupplierDetailModalProps) {
  const [supplier, setSupplier] = useState<Supplier | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!supplierId) {
      setSupplier(null);
      setError(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

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
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: "10px 20px",
            }}
          >
            {rows.map(({ key, value, fullWidth, isStatus }) => (
              <div
                key={key}
                style={{
                  gridColumn: fullWidth ? "1 / -1" : undefined,
                  display: "flex",
                  flexDirection: "column",
                  gap: "2px",
                  padding: "8px 12px",
                  backgroundColor: "var(--color-bg)",
                  borderRadius: "var(--radius-sm)",
                  border: "1px solid var(--color-border)",
                }}
              >
                <span
                  style={{
                    fontSize: "var(--font-xs)",
                    color: "var(--color-subtext)",
                    fontWeight: 500,
                    textTransform: "uppercase",
                    letterSpacing: "0.04em",
                  }}
                >
                  {key}
                </span>
                <span style={{ fontSize: "var(--font-sm)", color: "var(--color-text)", fontWeight: 500 }}>
                  {isStatus ? (
                    <span
                      style={{
                        display: "inline-block",
                        padding: "2px 10px",
                        borderRadius: "var(--radius-full)",
                        fontSize: "var(--font-xs)",
                        fontWeight: 600,
                        backgroundColor:
                          supplier.status === "active"
                            ? "var(--color-success-light)"
                            : "var(--color-hover)",
                        color:
                          supplier.status === "active"
                            ? "var(--color-success)"
                            : "var(--color-subtext)",
                      }}
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
