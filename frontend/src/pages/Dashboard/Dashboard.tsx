import { useEffect, useState } from 'react';
import styles from './Dashboard.module.css';
import { Card, CardBody } from '../../components/Card/Card';
import { MOCK_PRODUCTS } from '../../data/products.mock';
import { MOCK_RECEIPTS } from '../../data/payments.mock';
import { formatCurrency } from '../../utils/formatters';
import { getSuppliersPage } from '../../services/supplier';

const totalRevenue = MOCK_RECEIPTS.reduce((s, r) => s + r.totalAmount, 0);
const totalStock = MOCK_PRODUCTS.reduce((s, p) => s + p.stock, 0);

export function Dashboard() {
  const [supplierCount, setSupplierCount] = useState<number | string>('...');

  useEffect(() => {
    getSuppliersPage(1)
      .then((res) => {
        setSupplierCount(res.totalElements);
      })
      .catch((err) => {
        console.error('Lỗi tải số lượng nhà cung cấp:', err);
        setSupplierCount(0);
      });
  }, []);

  const stats = [
    { label: 'Tổng doanh thu nhập', value: formatCurrency(totalRevenue), icon: 'fi fi-rr-sack-dollar', color: 'primary' },
    { label: 'Tổng sản phẩm', value: MOCK_PRODUCTS.length.toString(), icon: 'fi fi-rr-box-alt', color: 'success' },
    { label: 'Nhà cung cấp', value: supplierCount.toString(), icon: 'fi fi-rr-building', color: 'warning' },
    { label: 'Tổng tồn kho', value: `${totalStock} sản phẩm`, icon: 'fi fi-rr-warehouse-alt', color: 'info' },
  ];
  return (
    <section>
      <div className={styles.container}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>Tổng quan hệ thống</h2>
            <p className={styles.subtitle}>Dữ liệu cập nhật hôm nay, 13/06/2026</p>
          </div>
        </div>

        <div className={styles.statsGrid}>
          {stats.map((stat) => (
            <Card key={stat.label}>
              <CardBody className={styles.statCard}>
                <div className={[styles.statIcon, styles[stat.color]].join(' ')}>
                  <i className={stat.icon} aria-hidden />
                </div>
                <div className={styles.statInfo}>
                  <span className={styles.statValue}>{stat.value}</span>
                  <span className={styles.statLabel}>{stat.label}</span>
                </div>
              </CardBody>
            </Card>
          ))}
        </div>

        <div className={styles.content}>
          <Card>
            <div className={styles.recentHeader}>
              <span className={styles.recentTitle}>Phiếu nhập gần đây</span>
            </div>
            <div className={styles.recentList}>
              {MOCK_RECEIPTS.map((receipt) => (
                <div key={receipt.id} className={styles.recentItem}>
                  <div className={styles.recentLeft}>
                    <div className={styles.receiptIcon}>
                      <i className="fi fi-rr-file-invoice" aria-hidden />
                    </div>
                    <div>
                      <p className={styles.receiptCode}>{receipt.code}</p>
                      <p className={styles.receiptSupplier}>{receipt.supplierName}</p>
                    </div>
                  </div>
                  <div className={styles.recentRight}>
                    <span className={styles.receiptAmount}>{formatCurrency(receipt.totalAmount)}</span>
                    <span className={[styles.receiptStatus, styles[receipt.paymentStatus]].join(' ')}>
                      {receipt.paymentStatus === 'paid' ? 'Đã thanh toán' : receipt.paymentStatus === 'partial' ? 'Một phần' : 'Chưa thanh toán'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </Card>

          <Card>
            <div className={styles.recentHeader}>
              <span className={styles.recentTitle}>Sản phẩm sắp hết hàng</span>
            </div>
            <div className={styles.recentList}>
              {MOCK_PRODUCTS.filter((p) => p.stock < 25).map((product) => (
                <div key={product.id} className={styles.recentItem}>
                  <div className={styles.recentLeft}>
                    <div className={styles.productThumb}>
                      <i className="fi fi-rr-shirt" aria-hidden />
                    </div>
                    <div>
                      <p className={styles.receiptCode}>{product.name}</p>
                      <p className={styles.receiptSupplier}>{product.sku}</p>
                    </div>
                  </div>
                  <div className={styles.recentRight}>
                    <span className={[styles.receiptStatus, product.stock < 20 ? styles.unpaid : styles.partial].join(' ')}>
                      Còn {product.stock}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </section>
  );
}
