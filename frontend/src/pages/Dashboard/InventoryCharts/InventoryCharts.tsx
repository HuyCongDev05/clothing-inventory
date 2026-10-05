import { useState, useEffect, useMemo } from "react";
import styles from "./InventoryCharts.module.css";
import { formatCurrency } from "../../../utils/formatters";
import {
  getInventoryAnalytics,
  type InventoryAnalyticsResponse,
  type MonthlyStockMovement,
} from "../../../services/inventoryAnalytics";

export function InventoryCharts() {
  const [data, setData] = useState<InventoryAnalyticsResponse | null>(null);
  const [timeframe, setTimeframe] = useState<"6months" | "3months" | "year">("6months");

  const [hoveredData, setHoveredData] = useState<{
    item: MonthlyStockMovement;
    x: number;
    y: number;
  } | null>(null);
  const [hoveredCategory, setHoveredCategory] = useState<string | null>(null);
  const [card3Tab, setCard3Tab] = useState<"stock" | "orders">("stock");
  const [card3Hovered, setCard3Hovered] = useState<{
    label: string;
    count: number;
    unit: string;
    percentage: number;
    extra?: string;
  } | null>(null);

  useEffect(() => {
    getInventoryAnalytics(timeframe).then(setData).catch(console.error);
  }, [timeframe]);

  // Tính toán thông số cho biểu đồ SVG Cột kép & Đường xu hướng
  const chartMath = useMemo(() => {
    if (!data || !data.monthlyMovements.length) return null;
    const movements = data.monthlyMovements;

    const maxVal = Math.max(
      ...movements.map((m) => Math.max(m.inbound, m.outbound, m.balance))
    );
    // Làm tròn trần thang đo lên bội số của 1000
    const yMax = Math.ceil((maxVal * 1.15) / 1000) * 1000;

    const svgWidth = 540;
    const svgHeight = 220;
    const padLeft = 45;
    const padRight = 20;
    const padTop = 20;
    const padBottom = 30;

    const plotWidth = svgWidth - padLeft - padRight;
    const plotHeight = svgHeight - padTop - padBottom;

    const slotWidth = plotWidth / movements.length;
    const barWidth = 14;

    const getY = (val: number) => padTop + plotHeight - (val / yMax) * plotHeight;

    const points = movements.map((m, i) => {
      const centerX = padLeft + i * slotWidth + slotWidth / 2;
      return {
        centerX,
        inboundX: centerX - barWidth - 2,
        outboundX: centerX + 2,
        inboundY: getY(m.inbound),
        outboundY: getY(m.outbound),
        inboundHeight: Math.max(2, padTop + plotHeight - getY(m.inbound)),
        outboundHeight: Math.max(2, padTop + plotHeight - getY(m.outbound)),
        balanceY: getY(m.balance),
        data: m,
      };
    });

    // Đường xu hướng nối các điểm Balance
    const linePath = points
      .map((p, idx) => `${idx === 0 ? "M" : "L"} ${p.centerX} ${p.balanceY}`)
      .join(" ");

    // 4 đường kẻ ngang trục Y
    const yTicks = [0, 0.33, 0.66, 1].map((ratio) => {
      const val = Math.round(yMax * ratio);
      const y = padTop + plotHeight - ratio * plotHeight;
      return { val, y };
    });

    return { svgWidth, svgHeight, points, linePath, yTicks, padLeft, padRight, padTop, plotHeight, plotWidth };
  }, [data]);

  // Tính toán phân bổ hình tròn Donut Chart
  const donutMath = useMemo(() => {
    if (!data || !data.categoryDistribution.length) return null;
    const radius = 55;
    const circumference = 2 * Math.PI * radius;
    let accumulatedPercent = 0;

    const slices = data.categoryDistribution.map((cat) => {
      const strokeLength = (cat.percentage / 100) * circumference;
      const strokeOffset = -(accumulatedPercent / 100) * circumference;
      accumulatedPercent += cat.percentage;
      return {
        ...cat,
        strokeDasharray: `${strokeLength} ${circumference - strokeLength}`,
        strokeDashoffset: strokeOffset,
      };
    });

    const totalQty = data.categoryDistribution.reduce((acc, cur) => acc + cur.quantity, 0);

    return { radius, circumference, slices, totalQty };
  }, [data]);

  // Tính toán thông số cho biểu đồ Cột đứng Card 3
  const card3Math = useMemo(() => {
    if (!data) return null;
    const isStock = card3Tab === "stock";
    const items = isStock
      ? data.stockHealthSegments.map((s) => ({
          id: s.id,
          label: s.label,
          shortLabel: s.label,
          val: s.skuCount,
          unit: "SKU",
          percentage: s.percentage,
          color: s.color,
          desc: s.description,
        }))
      : data.orderStatusDistribution.map((o) => ({
          id: o.status,
          label: o.label,
          shortLabel:
            o.status === "RECEIVED"
              ? "Đã nhập"
              : o.status === "PENDING"
              ? "Chờ duyệt"
              : o.status === "DRAFT"
              ? "Bản nháp"
              : "Đã hủy",
          val: o.orderCount,
          unit: "đơn",
          percentage: o.percentage,
          color: o.color,
          desc: formatCurrency(o.totalAmount),
        }));

    const maxVal = Math.max(...items.map((i) => i.val));
    const yMax = isStock ? 160 : Math.ceil((maxVal * 1.25) / 10) * 10;

    const svgWidth = 380;
    const svgHeight = 200;
    const padLeft = 32;
    const padRight = 16;
    const padTop = 26;
    const padBottom = 32;

    const plotWidth = svgWidth - padLeft - padRight;
    const plotHeight = svgHeight - padTop - padBottom;

    const slotWidth = plotWidth / items.length;
    const barWidth = isStock ? 46 : 38;

    const getY = (val: number) => padTop + plotHeight - (val / yMax) * plotHeight;

    const bars = items.map((item, idx) => {
      const centerX = padLeft + idx * slotWidth + slotWidth / 2;
      const barHeight = Math.max(4, padTop + plotHeight - getY(item.val));
      const barY = getY(item.val);
      return {
        ...item,
        centerX,
        barX: centerX - barWidth / 2,
        barY,
        barWidth,
        barHeight,
      };
    });

    const yTicks = [0, 0.5, 1].map((ratio) => {
      const val = Math.round(yMax * ratio);
      const y = padTop + plotHeight - ratio * plotHeight;
      return { val, y };
    });

    return { svgWidth, svgHeight, padLeft, padRight, bars, yTicks };
  }, [data, card3Tab]);

  if (!data) return null;

  return (
    <div className={styles.sectionContainer}>
      <div className={styles.sectionHeader}>
        <div className={styles.sectionTitleGroup}>
          <div className={styles.titleIcon}>
            <i className="fi fi-rr-chart-histogram" />
          </div>
          <div>
            <h3 className={styles.sectionTitle}>Biểu đồ & Phân tích Dữ liệu Kho</h3>
            <p className={styles.sectionSubtitle}>
              Trực quan hóa diễn biến luân chuyển hàng hóa, cơ cấu danh mục và trạng thái đơn hàng
            </p>
          </div>
        </div>

        <div className={styles.controls}>
          <button
            type="button"
            className={`${styles.timeframeBtn} ${
              timeframe === "3months" ? styles.activeTimeframe : ""
            }`}
            onClick={() => setTimeframe("3months")}
          >
            3 tháng qua
          </button>
          <button
            type="button"
            className={`${styles.timeframeBtn} ${
              timeframe === "6months" ? styles.activeTimeframe : ""
            }`}
            onClick={() => setTimeframe("6months")}
          >
            6 tháng qua
          </button>
        </div>
      </div>

      <div className={styles.chartsRowOne}>
        <div className={styles.chartCard}>
          <div className={styles.cardHeader}>
            <div>
              <div className={styles.cardTitle}>
                <i className="fi fi-rr-arrows-repeat" style={{ color: "var(--color-primary)" }} />
                Biến động Nhập - Xuất & Tồn lũy kế
              </div>
              <div className={styles.cardSubtitle}>
                So sánh lượng hàng nhập vào so với xuất ra và biến động tồn kho
              </div>
            </div>

            <div className={styles.chartLegend}>
              <div className={styles.legendItem}>
                <span className={styles.legendDot} style={{ backgroundColor: "#2563EB" }} />
                <span>Nhập kho</span>
              </div>
              <div className={styles.legendItem}>
                <span className={styles.legendDot} style={{ backgroundColor: "#F97316" }} />
                <span>Xuất kho</span>
              </div>
              <div className={styles.legendItem}>
                <span className={styles.legendLine} style={{ backgroundColor: "#8B5CF6" }} />
                <span>Tồn kho</span>
              </div>
            </div>
          </div>

          <div className={styles.barChartContainer}>
            {chartMath && (
              <svg
                className={styles.svgChart}
                viewBox={`0 0 ${chartMath.svgWidth} ${chartMath.svgHeight}`}
                preserveAspectRatio="none"
              >
                {chartMath.yTicks.map((t, idx) => (
                  <g key={idx}>
                    <line
                      x1={chartMath.padLeft}
                      y1={t.y}
                      x2={chartMath.svgWidth - chartMath.padRight}
                      y2={t.y}
                      className={styles.gridLine}
                    />
                    <text
                      x={chartMath.padLeft - 8}
                      y={t.y + 4}
                      textAnchor="end"
                      className={styles.axisText}
                    >
                      {t.val.toLocaleString()}
                    </text>
                  </g>
                ))}

                {chartMath.points.map((p, idx) => (
                  <g key={idx}>
                    <rect
                      x={p.inboundX}
                      y={p.inboundY}
                      width={14}
                      height={p.inboundHeight}
                      rx={3}
                      fill="#2563EB"
                      className={styles.chartBar}
                      onMouseEnter={(e) => {
                        const rect = e.currentTarget.getBoundingClientRect();
                        setHoveredData({
                          item: p.data,
                          x: rect.left + rect.width / 2,
                          y: rect.top,
                        });
                      }}
                      onMouseLeave={() => setHoveredData(null)}
                    />

                    <rect
                      x={p.outboundX}
                      y={p.outboundY}
                      width={14}
                      height={p.outboundHeight}
                      rx={3}
                      fill="#F97316"
                      className={styles.chartBar}
                      onMouseEnter={(e) => {
                        const rect = e.currentTarget.getBoundingClientRect();
                        setHoveredData({
                          item: p.data,
                          x: rect.left + rect.width / 2,
                          y: rect.top,
                        });
                      }}
                      onMouseLeave={() => setHoveredData(null)}
                    />

                    <text
                      x={p.centerX}
                      y={chartMath.svgHeight - 8}
                      textAnchor="middle"
                      className={styles.axisText}
                    >
                      {p.data.month}
                    </text>
                  </g>
                ))}

                <path d={chartMath.linePath} className={styles.trendLine} />

                {chartMath.points.map((p, idx) => (
                  <circle
                    key={idx}
                    cx={p.centerX}
                    cy={p.balanceY}
                    r={4}
                    className={styles.trendPoint}
                    onMouseEnter={(e) => {
                      const rect = e.currentTarget.getBoundingClientRect();
                      setHoveredData({
                        item: p.data,
                        x: rect.left + rect.width / 2,
                        y: rect.top,
                      });
                    }}
                    onMouseLeave={() => setHoveredData(null)}
                  />
                ))}
              </svg>
            )}

            {hoveredData && (
              <div
                className={styles.chartTooltip}
                style={{
                  left: "50%",
                  top: "30%",
                }}
              >
                <div className={styles.tooltipTitle}>{hoveredData.item.month}</div>
                <div className={styles.tooltipRow}>
                  <span style={{ color: "#93C5FD" }}>Nhập kho:</span>
                  <strong>{hoveredData.item.inbound.toLocaleString()} sp</strong>
                </div>
                <div className={styles.tooltipRow}>
                  <span style={{ color: "#FDBA74" }}>Xuất kho:</span>
                  <strong>{hoveredData.item.outbound.toLocaleString()} sp</strong>
                </div>
                <div className={styles.tooltipRow}>
                  <span style={{ color: "#C4B5FD" }}>Tồn cuối kỳ:</span>
                  <strong>{hoveredData.item.balance.toLocaleString()} sp</strong>
                </div>
                <div className={styles.tooltipRow} style={{ marginTop: "4px", paddingTop: "4px", borderTop: "1px dashed rgba(255,255,255,0.2)" }}>
                  <span style={{ color: "#E2E8F0" }}>Giá trị nhập:</span>
                  <span>{formatCurrency(hoveredData.item.inboundValue)}</span>
                </div>
              </div>
            )}
          </div>
        </div>

        <div className={styles.chartCard}>
          <div className={styles.cardHeader}>
            <div>
              <div className={styles.cardTitle}>
                <i className="fi fi-rr-chart-pie" style={{ color: "var(--color-primary)" }} />
                Cơ cấu Tồn kho theo Danh mục
              </div>
              <div className={styles.cardSubtitle}>
                Tỷ trọng số lượng & giá trị vốn lưu kho
              </div>
            </div>
          </div>

          <div className={styles.donutWrapper}>
            {donutMath && (
              <div className={styles.donutSvgBox}>
                <svg className={styles.donutSvg} viewBox="0 0 170 170">
                  {donutMath.slices.map((slice) => {
                    const isHovered = hoveredCategory === slice.id;
                    return (
                      <circle
                        key={slice.id}
                        cx={85}
                        cy={85}
                        r={donutMath.radius}
                        stroke={slice.color}
                        strokeDasharray={slice.strokeDasharray}
                        strokeDashoffset={slice.strokeDashoffset}
                        className={styles.donutSlice}
                        style={{
                          opacity: hoveredCategory && !isHovered ? 0.45 : 1,
                          strokeWidth: isHovered ? 34 : 26,
                        }}
                        onMouseEnter={() => setHoveredCategory(slice.id)}
                        onMouseLeave={() => setHoveredCategory(null)}
                      />
                    );
                  })}
                </svg>

                <div className={styles.donutCenterInfo}>
                  <span className={styles.donutCenterValue}>
                    {hoveredCategory
                      ? `${donutMath.slices.find((s) => s.id === hoveredCategory)?.percentage}%`
                      : donutMath.totalQty.toLocaleString()}
                  </span>
                  <span className={styles.donutCenterLabel}>
                    {hoveredCategory
                      ? donutMath.slices.find((s) => s.id === hoveredCategory)?.name
                      : "Tổng sản phẩm"}
                  </span>
                </div>
              </div>
            )}

            <div className={styles.donutLegendList}>
              {data.categoryDistribution.map((cat) => (
                <div
                  key={cat.id}
                  className={styles.categoryItem}
                  onMouseEnter={() => setHoveredCategory(cat.id)}
                  onMouseLeave={() => setHoveredCategory(null)}
                  style={{
                    backgroundColor: hoveredCategory === cat.id ? "var(--color-hover)" : "transparent",
                  }}
                >
                  <div className={styles.categoryLeft}>
                    <span className={styles.categoryDot} style={{ backgroundColor: cat.color }} />
                    <span className={styles.categoryName}>{cat.name}</span>
                  </div>
                  <div className={styles.categoryRight}>
                    <span className={styles.categoryQty}>{cat.quantity.toLocaleString()} sp</span>
                    <span className={styles.categoryPercent}>{cat.percentage}%</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className={styles.chartsRowTwo}>
        <div className={styles.chartCard}>
          <div className={styles.cardHeader}>
            <div>
              <div className={styles.cardTitle}>
                <i className="fi fi-rr-chart-histogram" style={{ color: "var(--color-primary)" }} />
                {card3Tab === "stock" ? "Sơ đồ Mức Tồn kho (SKU)" : "Sơ đồ Đơn đặt hàng (PO)"}
              </div>
              <div className={styles.cardSubtitle}>
                {card3Tab === "stock"
                  ? "Phân bổ số lượng SKU theo định mức an toàn"
                  : "Thống kê số lượng đơn PO theo trạng thái"}
              </div>
            </div>

            <div className={styles.cardTabGroup}>
              <button
                type="button"
                className={`${styles.cardTabBtn} ${card3Tab === "stock" ? styles.activeCardTab : ""}`}
                onClick={() => setCard3Tab("stock")}
              >
                Mức tồn kho
              </button>
              <button
                type="button"
                className={`${styles.cardTabBtn} ${card3Tab === "orders" ? styles.activeCardTab : ""}`}
                onClick={() => setCard3Tab("orders")}
              >
                Đơn hàng PO
              </button>
            </div>
          </div>

          <div className={styles.columnChartWrapper}>
            <div className={styles.columnChartBox}>
              {card3Math && (
                <svg
                  className={styles.columnSvg}
                  viewBox={`0 0 ${card3Math.svgWidth} ${card3Math.svgHeight}`}
                  preserveAspectRatio="none"
                >
                  {card3Math.yTicks.map((t, idx) => (
                    <g key={idx}>
                      <line
                        x1={card3Math.padLeft}
                        y1={t.y}
                        x2={card3Math.svgWidth - card3Math.padRight}
                        y2={t.y}
                        className={styles.gridLine}
                      />
                      <text
                        x={card3Math.padLeft - 6}
                        y={t.y + 4}
                        textAnchor="end"
                        className={styles.axisText}
                      >
                        {t.val}
                      </text>
                    </g>
                  ))}
                  {card3Math.bars.map((bar) => (
                    <g key={bar.id}>
                      <rect
                        x={bar.barX}
                        y={bar.barY}
                        width={bar.barWidth}
                        height={bar.barHeight}
                        rx={5}
                        fill={bar.color}
                        className={styles.columnBar}
                        onMouseEnter={() =>
                          setCard3Hovered({
                            label: bar.label,
                            count: bar.val,
                            unit: bar.unit,
                            percentage: bar.percentage,
                            extra: bar.desc,
                          })
                        }
                        onMouseLeave={() => setCard3Hovered(null)}
                      />

                      <text
                        x={bar.centerX}
                        y={bar.barY - 7}
                        textAnchor="middle"
                        className={styles.columnTopVal}
                      >
                        {bar.percentage}%
                      </text>

                      <text
                        x={bar.centerX}
                        y={card3Math.svgHeight - 10}
                        textAnchor="middle"
                        className={styles.columnAxisText}
                      >
                        {bar.shortLabel}
                      </text>
                    </g>
                  ))}
                </svg>
              )}

              {card3Hovered && (
                <div
                  className={styles.chartTooltip}
                  style={{
                    left: "50%",
                    top: "35%",
                  }}
                >
                  <div className={styles.tooltipTitle}>{card3Hovered.label}</div>
                  <div className={styles.tooltipRow}>
                    <span>Số lượng:</span>
                    <strong>{card3Hovered.count} {card3Hovered.unit} ({card3Hovered.percentage}%)</strong>
                  </div>
                  {card3Hovered.extra && (
                    <div className={styles.tooltipRow} style={{ marginTop: "3px", color: "#E2E8F0" }}>
                      <span>Chi tiết:</span>
                      <span>{card3Hovered.extra}</span>
                    </div>
                  )}
                </div>
              )}
            </div>

            <div className={styles.card3FooterBadges}>
              {card3Tab === "stock"
                ? data.stockHealthSegments.map((s) => (
                    <div key={s.id} className={styles.card3BadgeItem}>
                      <span className={styles.card3BadgeDot} style={{ backgroundColor: s.color }} />
                      <span className={styles.card3BadgeLabel}>{s.label}:</span>
                      <span className={styles.card3BadgeValue}>{s.skuCount}</span>
                    </div>
                  ))
                : data.orderStatusDistribution.map((o) => (
                    <div key={o.status} className={styles.card3BadgeItem}>
                      <span className={styles.card3BadgeDot} style={{ backgroundColor: o.color }} />
                      <span className={styles.card3BadgeLabel}>{o.label}:</span>
                      <span className={styles.card3BadgeValue}>{o.orderCount}</span>
                    </div>
                  ))}
            </div>
          </div>
        </div>

        <div className={styles.chartCard}>
          <div className={styles.cardHeader}>
            <div>
              <div className={styles.cardTitle}>
                <i className="fi fi-rr-ranking-star" style={{ color: "var(--color-primary)" }} />
                Top 5 Mã hàng Chiếm dụng Vốn lưu kho lớn nhất
              </div>
              <div className={styles.cardSubtitle}>
                Thống kê các sản phẩm có tổng giá trị lưu kho cao nhất
              </div>
            </div>
          </div>

          <div className={styles.skuList}>
            {data.topValueProducts.map((prod) => {
              const maxVal = data.topValueProducts[0]?.totalValue || 1;
              const barPercent = Math.round((prod.totalValue / maxVal) * 100);

              return (
                <div key={prod.id} className={styles.skuItem}>
                  <div className={styles.skuItemHeader}>
                    <div style={{ display: "flex", alignItems: "center" }}>
                      <span className={styles.skuName} title={prod.productName}>
                        {prod.productName}
                      </span>
                      <span className={styles.skuCode}>({prod.sku})</span>
                    </div>
                    <span className={styles.skuValue}>
                      {formatCurrency(prod.totalValue)}
                    </span>
                  </div>

                  <div className={styles.skuBarTrack}>
                    <div
                      className={styles.skuBarFill}
                      style={{ width: `${barPercent}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
