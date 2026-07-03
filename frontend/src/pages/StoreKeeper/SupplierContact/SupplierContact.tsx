import { useState, useEffect, useMemo } from "react";
import { Card, CardHeader, CardBody } from "../../../components/Card/Card";
import { Button } from "../../../components/Button/Button";
import { SearchBox } from "../../../components/SearchBox/SearchBox";
import { useToast } from "../../../components/Toast/ToastContext";
import { getSuppliersPage } from "../../../services/supplier";
import type { Supplier } from "../../../types/supplier.types";
import { Pagination } from "../../../components/Pagination/Pagination";
import styles from "./SupplierContact.module.css";

export function SupplierContact() {
  const [allActiveSuppliers, setAllActiveSuppliers] = useState<Supplier[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const pageSize = 9; // 9 fits beautifully in a 3-column grid!
  const { showToast } = useToast();

  useEffect(() => {
    const fetchAllSuppliers = async () => {
      try {
        const firstPage = await getSuppliersPage(1);
        let all = [...firstPage.items];
        const totalPages = firstPage.totalPages;
        if (totalPages > 1) {
          const promises = [];
          for (let p = 2; p <= totalPages; p++) {
            promises.push(getSuppliersPage(p));
          }
          const results = await Promise.all(promises);
          results.forEach((res) => {
            all = all.concat(res.items);
          });
        }
        const activeOnly = all.filter((s) => s.status === "active");
        setAllActiveSuppliers(activeOnly);
      } catch (err) {
        console.error("Failed to fetch contact suppliers:", err);
      }
    };
    fetchAllSuppliers();
  }, []);

  // Debounce search query
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(searchQuery);
      setCurrentPage(1);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Filter suppliers locally based on debounced search query
  const filteredSuppliers = useMemo(() => {
    if (!debouncedQuery.trim()) {
      return allActiveSuppliers;
    }
    const lowerQuery = debouncedQuery.toLowerCase().trim();
    return allActiveSuppliers.filter(
      (s) =>
        s.code.toLowerCase().includes(lowerQuery) ||
        s.companyName.toLowerCase().includes(lowerQuery) ||
        s.representative.toLowerCase().includes(lowerQuery) ||
        s.contactPerson.toLowerCase().includes(lowerQuery) ||
        s.email.toLowerCase().includes(lowerQuery) ||
        (s.phone && s.phone.includes(lowerQuery))
    );
  }, [allActiveSuppliers, debouncedQuery]);

  const totalElements = filteredSuppliers.length;
  const activeSuppliers = useMemo(() => {
    return filteredSuppliers.slice(
      (currentPage - 1) * pageSize,
      currentPage * pageSize
    );
  }, [filteredSuppliers, currentPage, pageSize]);

  return (
    <section>
      <div className={styles.container}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>Liên hệ đặt hàng</h2>
            <p className={styles.subtitle}>
              {totalElements} nhà cung cấp có sẵn
            </p>
          </div>
        </div>

        <Card>
          <CardHeader
            title="Danh bạ nhà cung cấp"
            actions={
              <SearchBox
                placeholder="Tìm tên, mã, SĐT..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onClear={() => setSearchQuery("")}
              />
            }
          />
          <CardBody>
            {activeSuppliers.length === 0 ? (
              <div
                style={{
                  padding: "40px",
                  textAlign: "center",
                  color: "var(--color-subtext)",
                }}
              >
                Không tìm thấy nhà cung cấp nào phù hợp
              </div>
            ) : (
              <div className={styles.grid}>
                {activeSuppliers.map((supplier) => (
                  <Card key={supplier.id} className={styles.contactCard}>
                    <CardBody>
                      <div className={styles.cardContent}>
                        <div>
                          <h3 className={styles.companyName}>
                            {supplier.companyName}
                          </h3>
                          <span className={styles.code}>{supplier.code}</span>
                        </div>

                        <div className={styles.contactInfo}>
                          <div className={styles.infoRow}>
                            <i className="fi fi-rr-user" aria-hidden />
                            <span>{supplier.contactPerson}</span>
                          </div>
                          <div className={styles.infoRow}>
                            <i className="fi fi-rr-phone-call" aria-hidden />
                            <span>{supplier.phone}</span>
                          </div>
                          <div className={styles.infoRow}>
                            <i className="fi fi-rr-envelope" aria-hidden />
                            <span>{supplier.email}</span>
                          </div>
                        </div>

                        <div className={styles.cardActions}>
                          <Button
                            variant="secondary"
                            size="sm"
                            icon="fi fi-rr-phone-call"
                            onClick={() => {
                              const isMobile =
                                /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
                                  navigator.userAgent,
                                );
                              if (isMobile) {
                                window.location.href = `tel:${supplier.phone}`;
                              } else {
                                navigator.clipboard.writeText(supplier.phone);
                                showToast(
                                  `Đã sao chép số điện thoại "${supplier.phone}" vào bộ nhớ tạm.`,
                                  "success",
                                );
                              }
                            }}
                          >
                            Gọi điện
                          </Button>
                          <Button
                            variant="primary"
                            size="sm"
                            icon="fi fi-rr-envelope"
                            onClick={() => window.open(`mailto:${supplier.email}`)}
                          >
                            Gửi Email
                          </Button>
                        </div>
                      </div>
                    </CardBody>
                  </Card>
                ))}
              </div>
            )}
            {totalElements > 0 && (
              <div className={styles.paginationWrap}>
                <Pagination
                  pagination={{
                    page: currentPage,
                    pageSize: pageSize,
                    total: totalElements,
                  }}
                  onPageChange={setCurrentPage}
                />
              </div>
            )}
          </CardBody>
        </Card>
      </div>
    </section>
  );
}
