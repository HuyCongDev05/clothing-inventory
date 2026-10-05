import { useState, useEffect, useMemo } from "react";
import styles from "./WarehouseCapacityWidget.module.css";
import {
  getWarehouseCapacity,
  updateWarehouseCapacity,
  type WarehouseCapacityResponse,
  type WarehouseCapacityStatus,
} from "../../services/warehouseCapacity";
import { getUserAuthorities } from "../../services/auth";
import { Modal } from "../Modal/Modal";
import { Button } from "../Button/Button";
import { useToast } from "../Toast/ToastContext";

interface WarehouseCapacityWidgetProps {
  onUpdated?: () => void;
}

const PRESET_THRESHOLDS = [75, 80, 85, 90];

export function WarehouseCapacityWidget({ onUpdated }: WarehouseCapacityWidgetProps) {
  const { showToast } = useToast();
  const [capacity, setCapacity] = useState<WarehouseCapacityResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [isConfigOpen, setIsConfigOpen] = useState(false);
  const [formMax, setFormMax] = useState("");
  const [formWarning, setFormWarning] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const authorities = getUserAuthorities();
  const isAdmin = authorities.includes("admin") || authorities.includes("ROLE_ADMIN");

  const loadData = () => {
    setLoading(true);
    getWarehouseCapacity()
      .then((data) => {
        setCapacity(data);
        setFormMax(String(data.maxCapacity));
        setFormWarning(String(data.warningThreshold));
      })
      .catch((err) => {
        console.error("Failed to load warehouse capacity:", err);
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleOpenConfig = () => {
    if (capacity) {
      setFormMax(String(capacity.maxCapacity));
      setFormWarning(String(capacity.warningThreshold));
    }
    setErrorMessage(null);
    setIsConfigOpen(true);
  };

  // Tính toán mô phỏng trực tiếp trong modal cấu hình
  const liveSimulation = useMemo(() => {
    if (!capacity) return null;
    const maxVal = Number(formMax);
    const warnVal = Number(formWarning);

    if (isNaN(maxVal) || maxVal <= 0) return null;

    const currentInv = capacity.currentInventory;
    const projectedRate = Math.round((currentInv / maxVal) * 1000) / 10;
    const projectedRemaining = maxVal - currentInv;

    let projectedStatus: WarehouseCapacityStatus = "SAFE";
    if (currentInv >= maxVal || projectedRate >= 100) {
      projectedStatus = "FULL";
    } else if (!isNaN(warnVal) && projectedRate >= warnVal) {
      projectedStatus = "WARNING";
    } else if (projectedRate >= 75) {
      projectedStatus = "MODERATE";
    }

    return {
      rate: projectedRate,
      remaining: projectedRemaining,
      status: projectedStatus,
      isExceeded: currentInv > maxVal,
    };
  }, [capacity, formMax, formWarning]);

  const handleSaveConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const maxVal = Number(formMax);
    const warnVal = Number(formWarning);

    if (isNaN(maxVal) || maxVal <= 0) {
      const msg = "Sức chứa tối đa phải lớn hơn 0";
      setErrorMessage(msg);
      showToast(msg, "error");
      return;
    }
    if (isNaN(warnVal) || warnVal < 1 || warnVal > 100) {
      const msg = "Ngưỡng cảnh báo phải nằm trong khoảng từ 1% đến 100%";
      setErrorMessage(msg);
      showToast(msg, "error");
      return;
    }

    try {
      setIsSaving(true);
      const updated = await updateWarehouseCapacity({
        maxCapacity: maxVal,
        warningThreshold: warnVal,
      });
      setCapacity(updated);
      showToast("Cập nhật sức chứa kho hàng thành công!", "success");
      setIsConfigOpen(false);
      onUpdated?.();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Không thể cập nhật cấu hình sức chứa kho!";
      setErrorMessage(msg);
      showToast(msg, "error");
    } finally {
      setIsSaving(false);
    }
  };

  if (loading && !capacity) {
    return (
      <div className={styles.capacityCard}>
        <div style={{ display: "flex", alignItems: "center", gap: 10, padding: 12, color: "var(--color-subtext)" }}>
          <i className="fi fi-rr-spinner" style={{ animation: "spin 1s linear infinite" }} />
          <span>Đang tải thông tin sức chứa kho hàng...</span>
        </div>
      </div>
    );
  }

  if (!capacity) return null;

  const occupancy = capacity.occupancyRate ?? 0;
  const rawStatus = (capacity.status || "").toUpperCase();

  // Xác định cấu hình hiển thị theo 4 trạng thái chuẩn BE: FULL, WARNING, MODERATE, SAFE
  let statusKey: WarehouseCapacityStatus = "SAFE";
  if (rawStatus === "FULL" || capacity.isFull || occupancy >= 100) {
    statusKey = "FULL";
  } else if (rawStatus === "WARNING" || capacity.isWarning || occupancy >= capacity.warningThreshold) {
    statusKey = "WARNING";
  } else if (rawStatus === "MODERATE" || occupancy >= 75) {
    statusKey = "MODERATE";
  } else {
    statusKey = "SAFE";
  }

  const statusConfig = {
    SAFE: {
      label: "Ngưỡng an toàn",
      badgeClass: styles.statusSafe,
      percentClass: styles.percentSafe,
      barClass: styles.barSafe,
      icon: "fi fi-rr-check-circle",
      isPulse: false,
    },
    MODERATE: {
      label: "Mức khá cao",
      badgeClass: styles.statusModerate,
      percentClass: styles.percentModerate,
      barClass: styles.barModerate,
      icon: "fi fi-rr-info",
      isPulse: false,
    },
    WARNING: {
      label: "Cảnh báo quá tải",
      badgeClass: styles.statusWarning,
      percentClass: styles.percentWarning,
      barClass: styles.barWarning,
      icon: "fi fi-rr-triangle-warning",
      isPulse: true,
    },
    FULL: {
      label: "Kho đầy tải 100%",
      badgeClass: styles.statusFull,
      percentClass: styles.percentFull,
      barClass: styles.barFull,
      icon: "fi fi-rr-ban",
      isPulse: true,
    },
  }[statusKey];

  const progressPercent = Math.min(100, Math.max(0, occupancy));

  return (
    <>
      <div className={styles.capacityCard}>
        <div className={styles.widgetHeader}>
          <div className={styles.titleArea}>
            <div className={styles.iconBox}>
              <i className="fi fi-rr-warehouse-alt" />
            </div>
            <div className={styles.titleText}>
              <div className={styles.mainTitle}>Sức chứa & Dung tích Kho hàng</div>
              <div className={styles.subTitle}>
                {capacity.statusMessage || "Kiểm soát dung lượng và giới hạn lưu trữ tồn kho"}
              </div>
            </div>
          </div>

          <div className={styles.headerActions}>
            <div className={`${styles.statusBadge} ${statusConfig.badgeClass}`}>
              <span
                className={`${styles.statusDot} ${statusConfig.isPulse ? styles.pulseDot : ""}`}
              />
              <i className={statusConfig.icon} style={{ fontSize: "12px" }} />
              <span>{statusConfig.label}</span>
            </div>

            {isAdmin && (
              <button
                type="button"
                className={styles.configBtn}
                onClick={handleOpenConfig}
                title="Cấu hình sức chứa tối đa và ngưỡng cảnh báo"
              >
                <i className="fi fi-rr-settings-sliders" />
                <span>Cấu hình</span>
              </button>
            )}
          </div>
        </div>

        <div className={styles.progressSection}>
          <div className={styles.progressHeader}>
            <div className={styles.progressLabelGroup}>
              <span className={styles.progressLabel}>Tỷ lệ lấp đầy kho</span>
              <span className={styles.statusMessageText}>
                {capacity.statusMessage || `Kho đang đạt ${occupancy.toFixed(1)}% dung tích`}
              </span>
            </div>
            <span className={`${styles.progressPercent} ${statusConfig.percentClass}`}>
              {occupancy.toFixed(1)}%
            </span>
          </div>

          <div className={styles.progressBarTrack}>
            <div
              className={`${styles.progressBarFill} ${statusConfig.barClass}`}
              style={{ width: `${progressPercent}%` }}
            />
            {capacity.warningThreshold > 0 && capacity.warningThreshold <= 100 && (
              <div
                className={styles.thresholdMarker}
                style={{ left: `${capacity.warningThreshold}%` }}
              >
                <span className={styles.thresholdTooltip}>
                  Ngưỡng: {capacity.warningThreshold}%
                </span>
              </div>
            )}
          </div>
        </div>

        <div className={styles.statsGrid}>
          <div className={styles.statItem}>
            <div className={styles.statIconWrap}>
              <i className="fi fi-rr-boxes" />
            </div>
            <div className={styles.statDetails}>
              <span className={styles.statTitle}>Đang lưu kho</span>
              <span className={styles.statValue}>
                {capacity.currentInventory.toLocaleString()} sp
              </span>
            </div>
          </div>

          <div className={styles.statItem}>
            <div className={styles.statIconWrap}>
              <i className="fi fi-rr-box" />
            </div>
            <div className={styles.statDetails}>
              <span className={styles.statTitle}>Sức chứa tối đa</span>
              <span className={styles.statValue}>
                {capacity.maxCapacity.toLocaleString()} sp
              </span>
            </div>
          </div>

          <div className={styles.statItem}>
            <div className={styles.statIconWrap}>
              <i className="fi fi-rr-cube" />
            </div>
            <div className={styles.statDetails}>
              <span className={styles.statTitle}>Chỗ trống còn lại</span>
              <span
                className={styles.statValue}
                style={{
                  color: capacity.remainingCapacity <= 0 ? "#DC2626" : undefined,
                }}
              >
                {capacity.remainingCapacity.toLocaleString()} chỗ
              </span>
            </div>
          </div>

          <div className={styles.statItem}>
            <div className={styles.statIconWrap}>
              <i className="fi fi-rr-bell" />
            </div>
            <div className={styles.statDetails}>
              <span className={styles.statTitle}>Ngưỡng cảnh báo</span>
              <span className={styles.statValue}>
                {capacity.warningThreshold}%
              </span>
            </div>
          </div>
        </div>
      </div>

      <Modal
        isOpen={isConfigOpen}
        onClose={() => !isSaving && setIsConfigOpen(false)}
        title="Cấu hình Sức chứa & Ngưỡng cảnh báo Kho"
        size="md"
      >
        <form className={styles.modalForm} onSubmit={handleSaveConfig}>
          <div className={styles.currentInfoBanner}>
            <i className="fi fi-rr-info" />
            <div>
              Tổng tồn kho thực tế hiện tại:{" "}
              <strong>{capacity.currentInventory.toLocaleString()} sản phẩm</strong>.
            </div>
          </div>

          {errorMessage && (
            <div className={styles.errorAlert}>
              <i className="fi fi-rr-exclamation" />
              <span>{errorMessage}</span>
            </div>
          )}

          <div className={styles.formGroup}>
            <label className={styles.formLabel}>
              <span>Sức chứa tối đa (sản phẩm)</span>
              <span style={{ color: "#EF4444" }}>*</span>
            </label>
            <input
              type="number"
              className={styles.formInput}
              min="1"
              step="1"
              value={formMax}
              onChange={(e) => {
                setFormMax(e.target.value);
                setErrorMessage(null);
              }}
              placeholder="Ví dụ: 25000"
              required
            />
            <span className={styles.formHelper}>
              Tổng số lượng sản phẩm tối đa mà toàn bộ kho có thể lưu trữ.
            </span>
          </div>

          <div className={styles.formGroup}>
            <label className={styles.formLabel}>
              <span>Ngưỡng kích hoạt cảnh báo (%)</span>
              <span style={{ color: "#EF4444" }}>*</span>
            </label>
            <input
              type="number"
              className={styles.formInput}
              min="1"
              max="100"
              step="1"
              value={formWarning}
              onChange={(e) => {
                setFormWarning(e.target.value);
                setErrorMessage(null);
              }}
              placeholder="Ví dụ: 85"
              required
            />
            <div className={styles.quickChips}>
              <span className={styles.quickChipsLabel}>Chọn nhanh:</span>
              {PRESET_THRESHOLDS.map((pct) => (
                <button
                  key={pct}
                  type="button"
                  className={`${styles.chipBtn} ${
                    Number(formWarning) === pct ? styles.chipBtnActive : ""
                  }`}
                  onClick={() => {
                    setFormWarning(String(pct));
                    setErrorMessage(null);
                  }}
                >
                  {pct}%
                </button>
              ))}
            </div>
            <span className={styles.formHelper}>
              Hệ thống sẽ chuyển trạng thái sang WARNING khi tỷ lệ lấp đầy đạt hoặc vượt mức này (Mặc định: 85%).
            </span>
          </div>

          {liveSimulation && (
            <div className={styles.livePreviewCard}>
              <div className={styles.previewHeader}>
                <span>Mô phỏng tác động thay đổi</span>
                <span
                  className={`${styles.previewStatusBadge} ${
                    liveSimulation.status === "SAFE"
                      ? styles.statusSafe
                      : liveSimulation.status === "MODERATE"
                      ? styles.statusModerate
                      : liveSimulation.status === "WARNING"
                      ? styles.statusWarning
                      : styles.statusFull
                  }`}
                >
                  Trạng thái: {liveSimulation.status}
                </span>
              </div>
              <div className={styles.previewStats}>
                <span>
                  Tỷ lệ lấp đầy dự kiến: <strong>{liveSimulation.rate}%</strong>
                </span>
                <span>
                  Chỗ trống còn lại:{" "}
                  <strong>{liveSimulation.remaining.toLocaleString()} chỗ</strong>
                </span>
              </div>
              {liveSimulation.isExceeded && (
                <div className={styles.previewWarning}>
                  <i className="fi fi-rr-triangle-warning" />
                  <span>
                    Chú ý: Sức chứa mới nhỏ hơn số lượng tồn kho thực tế hiện tại!
                  </span>
                </div>
              )}
            </div>
          )}

          <div className={styles.modalActions}>
            <Button
              variant="secondary"
              type="button"
              disabled={isSaving}
              onClick={() => setIsConfigOpen(false)}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={isSaving}
              icon={isSaving ? "fi fi-rr-spinner" : "fi fi-rr-check"}
            >
              {isSaving ? "Đang lưu..." : "Lưu thay đổi"}
            </Button>
          </div>
        </form>
      </Modal>
    </>
  );
}
