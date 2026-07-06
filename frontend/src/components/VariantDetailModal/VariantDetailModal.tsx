import { useEffect, useState } from "react";
import { Modal } from "../Modal/Modal";
import { Button } from "../Button/Button";
import { getVariantById, type ProductVariantDetailResponseDto } from "../../services/product";

interface VariantDetailModalProps {
  variantId: string | null;
  onClose: () => void;
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("vi-VN").format(amount) + " VND";
}

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

export function VariantDetailModal({ variantId, onClose }: VariantDetailModalProps) {
  const [variant, setVariant] = useState<ProductVariantDetailResponseDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!variantId) {
      setVariant(null);
      setError(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    getVariantById(variantId)
      .then((data) => {
        if (!cancelled) setVariant(data);
      })
      .catch(() => {
        if (!cancelled) setError("Không thể tải thông tin phiên bản sản phẩm.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [variantId]);

  const isActive = variant?.status?.toUpperCase() === "ACTIVE";

  return (
    <Modal isOpen={!!variantId} onClose={onClose} title="Chi tiết phiên bản sản phẩm" size="lg">
      {loading && (
        <div style={{ padding: "32px", textAlign: "center", color: "var(--color-subtext)" }}>
          <i className="fi fi-rr-spinner" style={{ marginRight: 8 }} />
          Đang tải thông tin phiên bản...
        </div>
      )}

      {error && !loading && (
        <div style={{ padding: "32px", textAlign: "center", color: "var(--color-danger)" }}>
          <i className="fi fi-rr-exclamation" style={{ marginRight: 8 }} />
          {error}
        </div>
      )}

      {variant && !loading && (
        <>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: "10px 20px",
            }}
          >
            {/* Thông tin sản phẩm */}
            <div style={rowStyle(true)}>
              <span style={labelStyle}>Tên sản phẩm</span>
              <span style={valueStyle}>{variant.productName}</span>
            </div>

            <div style={rowStyle(false)}>
              <span style={labelStyle}>Mã SKU</span>
              <span style={valueStyle}>
                <code style={{ fontFamily: "monospace", fontSize: "var(--font-sm)" }}>{variant.sku}</code>
              </span>
            </div>

            <div style={rowStyle(false)}>
              <span style={labelStyle}>Mã sản phẩm</span>
              <span style={valueStyle}>{variant.productCode}</span>
            </div>

            <div style={rowStyle(false)}>
              <span style={labelStyle}>Danh mục</span>
              <span style={valueStyle}>{variant.categoryName || "—"}</span>
            </div>

            {variant.brand && (
              <div style={rowStyle(false)}>
                <span style={labelStyle}>Thương hiệu</span>
                <span style={valueStyle}>{variant.brand}</span>
              </div>
            )}

            {/* Thuộc tính biến thể */}
            {variant.attributes && Object.keys(variant.attributes).length > 0 &&
              Object.entries(variant.attributes).map(([attrName, attrValue]) => (
                <div key={attrName} style={rowStyle(false)}>
                  <span style={labelStyle}>{attrName}</span>
                  <span style={valueStyle}>{attrValue || "—"}</span>
                </div>
              ))
            }

            {/* Giá & tồn kho */}
            <div style={rowStyle(false)}>
              <span style={labelStyle}>Giá nhập</span>
              <span style={valueStyle}>{formatCurrency(variant.purchasePrice)}</span>
            </div>

            <div style={rowStyle(false)}>
              <span style={labelStyle}>Giá bán</span>
              <span style={valueStyle}>{formatCurrency(variant.salePrice)}</span>
            </div>

            <div style={rowStyle(false)}>
              <span style={labelStyle}>Tồn kho</span>
              <span style={{ ...valueStyle, color: variant.quantityOnHand < 20 ? "var(--color-warning)" : "var(--color-text)" }}>
                {variant.quantityOnHand} sản phẩm
              </span>
            </div>

            <div style={rowStyle(false)}>
              <span style={labelStyle}>Trạng thái</span>
              <span style={valueStyle}>
                <span
                  style={{
                    display: "inline-block",
                    padding: "2px 10px",
                    borderRadius: "var(--radius-full)",
                    fontSize: "var(--font-xs)",
                    fontWeight: 600,
                    backgroundColor: isActive ? "var(--color-success-light)" : "var(--color-hover)",
                    color: isActive ? "var(--color-success)" : "var(--color-subtext)",
                  }}
                >
                  {isActive ? "Đang bán" : "Ngừng bán"}
                </span>
              </span>
            </div>

            <div style={rowStyle(false)}>
              <span style={labelStyle}>Ngày tạo</span>
              <span style={valueStyle}>{formatDateTime(variant.createdAt)}</span>
            </div>

            <div style={rowStyle(false)}>
              <span style={labelStyle}>Ngày cập nhật</span>
              <span style={valueStyle}>{formatDateTime(variant.updatedAt)}</span>
            </div>

            {variant.description && (
              <div style={rowStyle(true)}>
                <span style={labelStyle}>Mô tả</span>
                <span style={valueStyle}>{variant.description}</span>
              </div>
            )}
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

function rowStyle(fullWidth: boolean): React.CSSProperties {
  return {
    gridColumn: fullWidth ? "1 / -1" : undefined,
    display: "flex",
    flexDirection: "column",
    gap: "2px",
    padding: "8px 12px",
    backgroundColor: "var(--color-bg)",
    borderRadius: "var(--radius-sm)",
    border: "1px solid var(--color-border)",
  };
}

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
