import { useState, useMemo } from 'react';
import type { ReceiptItem, WarehouseReceipt } from '../../../types/payment.types';
import { MOCK_SUPPLIERS } from '../../../data/suppliers.mock';
import { MOCK_PRODUCTS } from '../../../data/products.mock';
import { Select } from '../../../components/Select/Select';
import { Input } from '../../../components/Input/Input';
import { Button } from '../../../components/Button/Button';
import { Card, CardHeader, CardBody } from '../../../components/Card/Card';
import { formatCurrency } from '../../../utils/formatters';
import styles from './WarehouseReceipt.module.css';

const SUPPLIER_OPTIONS = MOCK_SUPPLIERS.map((s) => ({ value: s.id, label: s.companyName }));
const PRODUCT_OPTIONS = MOCK_PRODUCTS.map((p) => ({ value: p.id, label: `${p.sku} - ${p.name}` }));

interface LineItem extends ReceiptItem {
  productId: string;
}

const EMPTY_LINE: LineItem = { id: '', productId: '', sku: '', productName: '', quantity: 1, unitPrice: 0, totalPrice: 0 };

export function WarehouseReceiptPage() {
  const [supplierId, setSupplierId] = useState('');
  const [note, setNote] = useState('');
  const [lines, setLines] = useState<LineItem[]>([{ ...EMPTY_LINE, id: '1' }]);
  const [saved, setSaved] = useState(false);

  const totalQty = useMemo(() => lines.reduce((s, l) => s + (l.quantity || 0), 0), [lines]);
  const totalAmount = useMemo(() => lines.reduce((s, l) => s + (l.totalPrice || 0), 0), [lines]);

  const updateLine = (idx: number, field: keyof LineItem, value: string | number) => {
    setLines((prev) =>
      prev.map((line, i) => {
        if (i !== idx) return line;

        if (field === 'productId') {
          const product = MOCK_PRODUCTS.find((p) => p.id === String(value));
          if (!product) return { ...line, productId: String(value) };
          return {
            ...line,
            productId: product.id,
            sku: product.sku,
            productName: product.name,
            unitPrice: product.importPrice,
            totalPrice: product.importPrice * line.quantity,
          };
        }

        const updated = { ...line, [field]: value };
        if (field === 'quantity' || field === 'unitPrice') {
          updated.totalPrice = (updated.quantity || 0) * (updated.unitPrice || 0);
        }
        return updated;
      }),
    );
  };

  const addLine = () => {
    setLines((prev) => [...prev, { ...EMPTY_LINE, id: String(Date.now()) }]);
  };

  const removeLine = (idx: number) => {
    setLines((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleSave = (isDraft: boolean) => {
    const receipt: Partial<WarehouseReceipt> = {
      code: `PNK-2026-${String(Date.now()).slice(-4)}`,
      supplierId,
      supplierName: MOCK_SUPPLIERS.find((s) => s.id === supplierId)?.companyName ?? '',
      items: lines,
      totalQuantity: totalQty,
      totalAmount,
      isDraft,
      createdAt: new Date().toISOString().split('T')[0],
    };

    console.log('Phiếu nhập kho:', receipt);
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <section>
      <div className={styles.container}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>Lập phiếu nhập kho</h2>
            <p className={styles.subtitle}>Tạo phiếu nhập mới từ nhà cung cấp</p>
          </div>
          {saved && (
            <div className={styles.savedAlert}>
              <i className="fi fi-rr-check-circle" aria-hidden />
              <span>Đã lưu thành công</span>
            </div>
          )}
        </div>

        <div className={styles.content}>
          <div className={styles.mainCol}>
            <Card>
              <CardHeader title="Thông tin nhà cung cấp" />
              <CardBody>
                <div className={styles.formRow}>
                  <Select
                    id="supplier"
                    label="Nhà cung cấp"
                    required
                    options={SUPPLIER_OPTIONS}
                    placeholder="Chọn nhà cung cấp"
                    value={supplierId}
                    onChange={(e) => setSupplierId(e.target.value)}
                  />
                  <Input id="receiptNote" label="Ghi chú" value={note} onChange={(e) => setNote(e.target.value)} placeholder="Ghi chú thêm..." />
                </div>
              </CardBody>
            </Card>

            <Card>
              <CardHeader
                title="Danh sách hàng nhập"
                actions={
                  <Button variant="ghost" size="sm" icon="fi fi-rr-add" onClick={addLine}>
                    Thêm dòng
                  </Button>
                }
              />
              <CardBody className={styles.tableCardBody}>
                <div className={styles.tableWrapper}>
                  <table className={styles.lineTable}>
                    <thead>
                      <tr>
                        <th>STT</th>
                        <th>Sản phẩm</th>
                        <th>SKU</th>
                        <th>Số lượng</th>
                        <th>Đơn giá</th>
                        <th>Thành tiền</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {lines.map((line, idx) => (
                        <tr key={line.id}>
                          <td className={styles.indexCell}>{idx + 1}</td>
                          <td>
                            <select
                              className={styles.lineSelect}
                              value={line.productId}
                              onChange={(e) => updateLine(idx, 'productId', e.target.value)}
                            >
                              <option value="">-- Chọn sản phẩm --</option>
                              {PRODUCT_OPTIONS.map((o) => (
                                <option key={o.value} value={o.value}>{o.label}</option>
                              ))}
                            </select>
                          </td>
                          <td className={styles.skuCell}>{line.sku || '—'}</td>
                          <td>
                            <input
                              className={styles.lineInput}
                              type="number"
                              min={1}
                              value={line.quantity}
                              onChange={(e) => updateLine(idx, 'quantity', Number(e.target.value))}
                            />
                          </td>
                          <td>
                            <input
                              className={styles.lineInput}
                              type="number"
                              min={0}
                              value={line.unitPrice}
                              onChange={(e) => updateLine(idx, 'unitPrice', Number(e.target.value))}
                            />
                          </td>
                          <td className={styles.totalCell}>{formatCurrency(line.totalPrice)}</td>
                          <td>
                            <button className={styles.removeBtn} onClick={() => removeLine(idx)} aria-label="Xóa dòng">
                              <i className="fi fi-rr-trash" aria-hidden />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardBody>
            </Card>
          </div>

          <div className={styles.sideCol}>
            <Card>
              <CardHeader title="Tổng kết" />
              <CardBody>
                <div className={styles.summaryList}>
                  <div className={styles.summaryRow}>
                    <span>Tổng số lượng</span>
                    <strong>{totalQty}</strong>
                  </div>
                  <div className={styles.summaryRow}>
                    <span>Tổng số dòng</span>
                    <strong>{lines.length}</strong>
                  </div>
                  <div className={[styles.summaryRow, styles.totalRow].join(' ')}>
                    <span>Tổng tiền</span>
                    <strong className={styles.totalAmount}>{formatCurrency(totalAmount)}</strong>
                  </div>
                </div>

                <div className={styles.sideActions}>
                  <Button variant="secondary" onClick={() => handleSave(true)} icon="fi fi-rr-disk">
                    Lưu nháp
                  </Button>
                  <Button onClick={() => handleSave(false)} icon="fi fi-rr-check">
                    Xác nhận nhập kho
                  </Button>
                </div>
              </CardBody>
            </Card>
          </div>
        </div>
      </div>
    </section>
  );
}
