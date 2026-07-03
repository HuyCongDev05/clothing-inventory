import { useEffect, useState, useMemo } from "react";
import { Navigate } from "react-router-dom";
import styles from "./Dashboard.module.css";
import { Card, CardHeader, CardBody } from "../../components/Card/Card";
import { MOCK_RECEIPTS } from "../../data/payments.mock";
import { formatCurrency } from "../../utils/formatters";
import { getSuppliersPage } from "../../services/supplier";
import { getProductsPage } from "../../services/product";
import { getUserAuthorities } from "../../services/auth";
import { useToast } from "../../components/Toast/ToastContext";
import { Button } from "../../components/Button/Button";
import { Table } from "../../components/Table/Table";
import { SearchBox } from "../../components/SearchBox/SearchBox";
import { Input } from "../../components/Input/Input";
import { Select } from "../../components/Select/Select";
import { Modal } from "../../components/Modal/Modal";
import { ConfirmDialog } from "../../components/ConfirmDialog/ConfirmDialog";
import { ROUTES } from "../../constants/routes";
import type { Product } from "../../types/product.types";
import type { TableColumn } from "../../types/common.types";

const totalRevenue = MOCK_RECEIPTS.reduce((s, r) => s + r.totalAmount, 0);

interface MockUser {
  uuid: string;
  username: string;
  fullName: string;
  email: string;
  phone: string;
  status: "active" | "inactive";
  authorities: string[];
  createdAt: string;
}

const DEFAULT_USERS: MockUser[] = [
  {
    uuid: "admin-uuid",
    username: "admin",
    fullName: "Nguyễn Văn Admin",
    email: "admin@sapoware.com",
    phone: "0912345678",
    status: "active",
    authorities: ["admin"],
    createdAt: "2026-06-01T08:00:00Z",
  },
  {
    uuid: "coord-uuid",
    username: "coordinator",
    fullName: "Nguyễn Văn Điều Phối",
    email: "coord@sapoware.com",
    phone: "0987654321",
    status: "active",
    authorities: ["coordinator"],
    createdAt: "2026-06-02T09:30:00Z",
  },
  {
    uuid: "warehouse-uuid",
    username: "warehouse",
    fullName: "Trần Văn Kho",
    email: "warehouse@sapoware.com",
    phone: "0977665544",
    status: "active",
    authorities: ["warehouse-staff"],
    createdAt: "2026-06-03T10:15:00Z",
  },
  {
    uuid: "keeper-uuid",
    username: "keeper",
    fullName: "Lê Thị Thủ Kho",
    email: "keeper@sapoware.com",
    phone: "0966554433",
    status: "active",
    authorities: ["store-keeper"],
    createdAt: "2026-06-04T14:20:00Z",
  },
];

const ROLE_LABELS: Record<string, string> = {
  admin: "Quản trị viên",
  coordinator: "Nhân viên điều phối",
  "warehouse-staff": "Nhân viên kho",
  "store-keeper": "Thủ kho",
};

const ASSIGNABLE_ROLES: Record<string, string> = {
  coordinator: "Nhân viên điều phối",
  "warehouse-staff": "Nhân viên kho",
  "store-keeper": "Thủ kho",
};

export function Dashboard() {
  const authorities = getUserAuthorities();
  const isAdmin = authorities.includes("admin");

  const { showToast } = useToast();
  const [activeTab, setActiveTab] = useState<"system" | "accounts">("system");

  // State quản lý sản phẩm / NCC
  const [supplierCount, setSupplierCount] = useState<number | string>("...");
  const [productCount, setProductCount] = useState<number | string>("...");
  const [totalStock, setTotalStock] = useState<number | string>("...");
  const [lowStockProducts, setLowStockProducts] = useState<Product[]>([]);

  // State quản lý tài khoản mock
  const [users, setUsers] = useState<MockUser[]>(() => {
    const stored = localStorage.getItem("mockUsers");
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch {
        return DEFAULT_USERS;
      }
    }
    localStorage.setItem("mockUsers", JSON.stringify(DEFAULT_USERS));
    return DEFAULT_USERS;
  });

  const [searchAccount, setSearchAccount] = useState("");
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<MockUser | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const [createForm, setCreateForm] = useState({
    username: "",
    fullName: "",
    email: "",
    phone: "",
    authorities: [] as string[],
  });

  const [editForm, setEditForm] = useState({
    fullName: "",
    email: "",
    phone: "",
    status: "active" as "active" | "inactive",
    authorities: [] as string[],
  });

  useEffect(() => {
    getSuppliersPage(1)
      .then((res) => {
        setSupplierCount(res.totalElements);
      })
      .catch((err) => {
        console.error("Lỗi tải số lượng nhà cung cấp:", err);
        setSupplierCount(0);
      });
  }, []);

  useEffect(() => {
    getProductsPage(1)
      .then(async (firstPage) => {
        setProductCount(firstPage.totalElements);

        let allItems = [...firstPage.items];
        const totalPages = firstPage.totalPages;

        if (totalPages > 1) {
          const promises = [];
          for (let p = 2; p <= totalPages; p++) {
            promises.push(getProductsPage(p));
          }
          const results = await Promise.all(promises);
          results.forEach((res) => {
            allItems = allItems.concat(res.items);
          });
        }

        const stockSum = allItems.reduce((sum, item) => sum + item.stock, 0);
        setTotalStock(stockSum);
        setLowStockProducts(allItems.filter((p) => p.stock < 25));
      })
      .catch((err) => {
        console.error("Lỗi tải dữ liệu sản phẩm cho dashboard:", err);
        setProductCount(0);
        setTotalStock(0);
        setLowStockProducts([]);
      });
  }, []);

  const stats = [
    {
      label: "Tổng doanh thu nhập",
      value: formatCurrency(totalRevenue),
      icon: "fi fi-rr-sack-dollar",
      color: "primary",
    },
    {
      label: "Tổng sản phẩm",
      value: productCount.toString(),
      icon: "fi fi-rr-box-alt",
      color: "success",
    },
    {
      label: "Nhà cung cấp",
      value: supplierCount.toString(),
      icon: "fi fi-rr-building",
      color: "warning",
    },
    {
      label: "Tổng tồn kho",
      value: totalStock === "..." ? "..." : `${totalStock} sản phẩm`,
      icon: "fi fi-rr-warehouse-alt",
      color: "info",
    },
  ];

  const saveUsers = (updatedUsers: MockUser[]) => {
    setUsers(updatedUsers);
    localStorage.setItem("mockUsers", JSON.stringify(updatedUsers));
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.username || !createForm.fullName || !createForm.email) {
      showToast("Vui lòng điền đầy đủ các trường bắt buộc!", "warning");
      return;
    }
    if (users.some((u) => u.username.toLowerCase() === createForm.username.toLowerCase())) {
      showToast("Tên đăng nhập đã tồn tại!", "error");
      return;
    }
    const newUser: MockUser = {
      uuid: "user-" + Math.random().toString(36).substring(2, 11),
      username: createForm.username,
      fullName: createForm.fullName,
      email: createForm.email,
      phone: createForm.phone,
      status: "active",
      authorities: createForm.authorities.length > 0 ? createForm.authorities : ["warehouse-staff"],
      createdAt: new Date().toISOString(),
    };
    saveUsers([...users, newUser]);
    setIsCreateOpen(false);
    setCreateForm({ username: "", fullName: "", email: "", phone: "", authorities: [] });
    showToast("Tạo tài khoản thành công!", "success");
  };

  const handleEditClick = (u: MockUser) => {
    setSelectedUser(u);
    setEditForm({
      fullName: u.fullName,
      email: u.email,
      phone: u.phone || "",
      status: u.status,
      authorities: u.authorities,
    });
    setIsEditOpen(true);
  };

  const handleEditSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUser) return;
    if (!editForm.fullName || !editForm.email) {
      showToast("Vui lòng điền đầy đủ các trường bắt buộc!", "warning");
      return;
    }
    if (selectedUser.uuid === "admin-uuid" && editForm.status === "inactive") {
      showToast("Không thể khóa tài khoản Admin mặc định!", "error");
      return;
    }
    const updated = users.map((u) => {
      if (u.uuid === selectedUser.uuid) {
        return {
          ...u,
          fullName: editForm.fullName,
          email: editForm.email,
          phone: editForm.phone,
          status: editForm.status,
          authorities: editForm.authorities.length > 0 ? editForm.authorities : u.authorities,
        };
      }
      return u;
    });
    saveUsers(updated);
    setIsEditOpen(false);
    setSelectedUser(null);
    showToast("Cập nhật tài khoản thành công!", "success");
  };

  const handleDelete = () => {
    if (!deleteId) return;
    if (deleteId === "admin-uuid") {
      showToast("Không thể xóa tài khoản Admin mặc định!", "error");
      setDeleteId(null);
      return;
    }
    const filtered = users.filter((u) => u.uuid !== deleteId);
    saveUsers(filtered);
    showToast("Đã xóa tài khoản!", "success");
    setDeleteId(null);
  };

  const filteredUsers = useMemo(() => {
    return users.filter(
      (u) =>
        u.username.toLowerCase().includes(searchAccount.toLowerCase()) ||
        u.fullName.toLowerCase().includes(searchAccount.toLowerCase()) ||
        u.email.toLowerCase().includes(searchAccount.toLowerCase())
    );
  }, [users, searchAccount]);

  const toggleCreateRole = (role: string) => {
    setCreateForm((prev) => {
      const has = prev.authorities.includes(role);
      const next = has
        ? prev.authorities.filter((r) => r !== role)
        : [...prev.authorities, role];
      return { ...prev, authorities: next };
    });
  };

  const toggleEditRole = (role: string) => {
    setEditForm((prev) => {
      const has = prev.authorities.includes(role);
      const next = has
        ? prev.authorities.filter((r) => r !== role)
        : [...prev.authorities, role];
      return { ...prev, authorities: next };
    });
  };

  const deleteTarget = users.find((u) => u.uuid === deleteId);

  const columns: TableColumn<MockUser>[] = [
    { key: "username", label: "Tên đăng nhập", width: "150px" },
    { key: "fullName", label: "Họ và tên" },
    { key: "email", label: "Email" },
    { key: "phone", label: "Số điện thoại", width: "135px", render: (val) => (val as string) || "—" },
    {
      key: "authorities",
      label: "Quyền hạn",
      render: (val) => (
        <div className={styles.roleBadges}>
          {(val as string[]).map((auth) => (
            <span key={auth} className={[styles.roleBadge, styles[auth] || ""].join(" ")}>
              {ROLE_LABELS[auth] || auth}
            </span>
          ))}
        </div>
      ),
    },
    {
      key: "status",
      label: "Trạng thái",
      width: "140px",
      align: "center",
      render: (val) => (
        <span className={[styles.badge, val === "active" ? styles.active : styles.inactive].join(" ")}>
          {val === "active" ? "Hoạt động" : "Ngừng hoạt động"}
        </span>
      ),
    },
    {
      key: "uuid",
      label: "Hành động",
      width: "160px",
      align: "center",
      render: (_, row) => (
        <div className={styles.actions}>
          <Button
            variant="ghost"
            size="sm"
            icon="fi fi-rr-edit"
            onClick={() => handleEditClick(row)}
          >
            Sửa
          </Button>
          <Button
            variant="danger"
            size="sm"
            icon="fi fi-rr-trash"
            onClick={() => setDeleteId(row.uuid)}
            disabled={row.uuid === "admin-uuid"}
          >
            Xóa
          </Button>
        </div>
      ),
    },
  ];

  if (!isAdmin) {
    return <Navigate to={ROUTES.PROFILE} replace />;
  }

  return (
    <section>
      <div className={styles.container}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>Quản trị hệ thống</h2>
            <p className={styles.subtitle}>Bảng quản lý dành riêng cho Admin</p>
          </div>
        </div>

        <div className={styles.tabs}>
          <button
            onClick={() => setActiveTab("system")}
            className={[styles.tab, activeTab === "system" ? styles.active : ""].join(" ")}
          >
            Tổng quan hệ thống
          </button>
          <button
            onClick={() => setActiveTab("accounts")}
            className={[styles.tab, activeTab === "accounts" ? styles.active : ""].join(" ")}
          >
            Quản lý tài khoản
          </button>
        </div>

        {activeTab === "system" ? (
          <>
            <div className={styles.statsGrid}>
              {stats.map((stat) => (
                <Card key={stat.label}>
                  <CardBody className={styles.statCard}>
                    <div className={[styles.statIcon, styles[stat.color]].join(" ")}>
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
                        <span className={styles.receiptAmount}>
                          {formatCurrency(receipt.totalAmount)}
                        </span>
                        <span
                          className={[styles.receiptStatus, styles[receipt.paymentStatus]].join(
                            " "
                          )}
                        >
                          {receipt.paymentStatus === "paid"
                            ? "Đã thanh toán"
                            : receipt.paymentStatus === "partial"
                              ? "Một phần"
                              : "Chưa thanh toán"}
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
                  {lowStockProducts.length === 0 ? (
                    <div
                      style={{
                        padding: "20px",
                        textAlign: "center",
                        color: "var(--color-subtext)",
                      }}
                    >
                      Không có sản phẩm nào sắp hết hàng
                    </div>
                  ) : (
                    lowStockProducts.map((product) => (
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
                          <span
                            className={[
                              styles.receiptStatus,
                              product.stock < 20 ? styles.unpaid : styles.partial,
                            ].join(" ")}
                          >
                            Còn {product.stock}
                          </span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </Card>
            </div>
          </>
        ) : (
          <div className={styles.container}>
            <div className={styles.header} style={{ justifyContent: "flex-end" }}>
              <Button
                icon="fi fi-rr-add"
                onClick={() => setIsCreateOpen(true)}
              >
                Tạo tài khoản
              </Button>
            </div>

            <Card>
              <CardHeader
                title="Danh sách tài khoản"
                actions={
                  <SearchBox
                    placeholder="Tìm tên, tài khoản..."
                    value={searchAccount}
                    onChange={(e) => setSearchAccount(e.target.value)}
                    onClear={() => setSearchAccount("")}
                  />
                }
              />
              <CardBody className={styles.tableBody}>
                <Table
                  columns={columns}
                  data={filteredUsers}
                  rowKey="uuid"
                  emptyText="Không tìm thấy tài khoản nào"
                />
              </CardBody>
            </Card>
          </div>
        )}
      </div>

      <Modal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        title="Tạo tài khoản mới"
        size="lg"
      >
        <div className={styles.form}>
          <div className={styles.formRow}>
            <Input
              id="create-username"
              label="Tên đăng nhập"
              required
              value={createForm.username}
              onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
              placeholder="Nhập tên đăng nhập"
            />
            <Input
              id="create-fullName"
              label="Họ và tên"
              required
              value={createForm.fullName}
              onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })}
              placeholder="Nhập họ và tên"
            />
          </div>
          <div className={styles.formRow}>
            <Input
              id="create-email"
              label="Email"
              required
              type="email"
              value={createForm.email}
              onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
              placeholder="example@sapoware.com"
            />
            <Input
              id="create-phone"
              label="Số điện thoại"
              value={createForm.phone}
              onChange={(e) => setCreateForm({ ...createForm, phone: e.target.value })}
              placeholder="Nhập số điện thoại"
            />
          </div>
          <div className={styles.formGroup}>
            <label className={styles.label}>Quyền hạn (Vai trò)</label>
            <div className={styles.checkboxGrid}>
              {Object.keys(ASSIGNABLE_ROLES).map((role) => (
                <label key={role} className={styles.checkboxLabel}>
                  <input
                    type="checkbox"
                    checked={createForm.authorities.includes(role)}
                    onChange={() => toggleCreateRole(role)}
                    className={styles.checkboxInput}
                  />
                  {ASSIGNABLE_ROLES[role]}
                </label>
              ))}
            </div>
          </div>
        </div>
        <div className={styles.modalActions}>
          <Button variant="secondary" onClick={() => setIsCreateOpen(false)}>
            Hủy
          </Button>
          <Button onClick={handleCreate} icon="fi fi-rr-check">
            Tạo tài khoản
          </Button>
        </div>
      </Modal>

      <Modal
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        title={`Chỉnh sửa tài khoản: ${selectedUser?.username ?? ""}`}
        size="lg"
      >
        <div className={styles.form}>
          <div className={styles.formRow}>
            <Input
              id="edit-fullName"
              label="Họ và tên"
              required
              value={editForm.fullName}
              onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
              placeholder="Nhập họ và tên"
            />
            <Input
              id="edit-email"
              label="Email"
              required
              type="email"
              value={editForm.email}
              onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
              placeholder="example@sapoware.com"
            />
          </div>
          <div className={styles.formRow}>
            <Input
              id="edit-phone"
              label="Số điện thoại"
              value={editForm.phone}
              onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
              placeholder="Nhập số điện thoại"
            />
            <Select
              id="edit-status"
              label="Trạng thái hoạt động"
              required
              options={[
                { value: "active", label: "Hoạt động" },
                { value: "inactive", label: "Ngừng hoạt động" },
              ]}
              value={editForm.status}
              onChange={(e) => setEditForm({ ...editForm, status: e.target.value as "active" | "inactive" })}
            />
          </div>
          <div className={styles.formGroup}>
            <label className={styles.label}>Quyền hạn (Vai trò)</label>
            <div className={styles.checkboxGrid}>
              {Object.keys(ASSIGNABLE_ROLES).map((role) => (
                <label key={role} className={styles.checkboxLabel}>
                  <input
                    type="checkbox"
                    checked={editForm.authorities.includes(role)}
                    onChange={() => toggleEditRole(role)}
                    className={styles.checkboxInput}
                  />
                  {ASSIGNABLE_ROLES[role]}
                </label>
              ))}
            </div>
          </div>
        </div>
        <div className={styles.modalActions}>
          <Button variant="secondary" onClick={() => setIsEditOpen(false)}>
            Hủy
          </Button>
          <Button onClick={handleEditSubmit} icon="fi fi-rr-check">
            Lưu thay đổi
          </Button>
        </div>
      </Modal>

      <ConfirmDialog
        isOpen={!!deleteId}
        title="Xóa tài khoản"
        message={`Bạn có chắc chắn muốn xóa tài khoản "${deleteTarget?.username}"? Hành động này không thể hoàn tác.`}
        confirmLabel="Xóa"
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />
    </section>
  );
}
