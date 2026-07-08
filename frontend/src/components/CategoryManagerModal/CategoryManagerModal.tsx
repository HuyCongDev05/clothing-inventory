import { useState, useEffect, useRef } from "react";
import { Modal } from "../Modal/Modal";
import { Button } from "../Button/Button";
import { useToast } from "../Toast/ToastContext";
import { ConfirmDialog } from "../ConfirmDialog/ConfirmDialog";
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  restoreCategory,
  type CategoryResponseDto,
} from "../../services/product";
import styles from "./CategoryManagerModal.module.css";

interface CategoryManagerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCategoriesChanged: (categories: CategoryResponseDto[]) => void;
}

// Thành phần CategoryManagerModal
export function CategoryManagerModal({
  isOpen,
  onClose,
  onCategoriesChanged,
}: CategoryManagerModalProps) {
  const { showToast } = useToast();

  const [categories, setCategories] = useState<CategoryResponseDto[]>([]);
  const [loading, setLoading] = useState(false);

  // Trạng thái thêm mới
  const [newName, setNewName] = useState("");
  const [addLoading, setAddLoading] = useState(false);
  const newInputRef = useRef<HTMLInputElement>(null);

  // Trạng thái đang sửa
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingName, setEditingName] = useState("");
  const [editLoading, setEditLoading] = useState(false);



  // Trạng thái khôi phục danh mục đã xóa
  const [showRestoreConfirm, setShowRestoreConfirm] = useState(false);
  const [restoreName, setRestoreName] = useState("");

  // Khôi phục danh mục đã bị xóa trước đó
  const handleRestore = async () => {
    if (!restoreName) return;
    try {
      const restored = await restoreCategory(restoreName);
      const updated = [...categories, restored];
      setCategories(updated);
      onCategoriesChanged(updated);
      setNewName("");
      showToast(`Đã khôi phục danh mục "${restored.name}" thành công!`, "success");
    } catch {
      showToast("Khôi phục danh mục thất bại. Vui lòng thử lại!", "error");
    } finally {
      setShowRestoreConfirm(false);
      setRestoreName("");
    }
  };

  // Load danh mục khi mở modal
  useEffect(() => {
    if (!isOpen) return;

    let active = true;
    Promise.resolve().then(() => {
      if (active) setLoading(true);
    });

    getCategories()
      .then((data) => {
        if (active) {
          setCategories(data);
          setLoading(false);
        }
      })
      .catch(() => {
        if (active) {
          showToast("Không thể tải danh mục", "error");
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [isOpen, showToast]);

  // Reset khi đóng
  const handleClose = () => {
    setNewName("");
    setEditingId(null);
    setEditingName("");
    onClose();
  };

  // Thêm danh mục
  const handleAdd = async () => {
    const trimmed = newName.trim();
    if (!trimmed) {
      showToast("Tên danh mục không được để trống", "error");
      newInputRef.current?.focus();
      return;
    }
    setAddLoading(true);
    try {
      const created = await createCategory(trimmed);
      const updated = [...categories, created];
      setCategories(updated);
      onCategoriesChanged(updated);
      setNewName("");
      showToast(`Đã thêm danh mục "${created.name}"`, "success");
      newInputRef.current?.focus();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "";
      if (msg === "Category name already exists") {
        setRestoreName(trimmed);
        setShowRestoreConfirm(true);
      } else {
        showToast("Thêm danh mục thất bại. Vui lòng thử lại!", "error");
      }
    } finally {
      setAddLoading(false);
    }
  };

  // Xử lý phím nhấn (Enter) khi thêm danh mục mới
  const handleNewKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") handleAdd();
  };

  // Bắt đầu sửa
  const startEdit = (cat: CategoryResponseDto) => {
    setEditingId(cat.id);
    setEditingName(cat.name);
  };

  // Hủy bỏ chế độ chỉnh sửa danh mục
  const cancelEdit = () => {
    setEditingId(null);
    setEditingName("");
  };

  // Xác nhận sửa
  const confirmEdit = async (id: number) => {
    const trimmed = editingName.trim();
    if (!trimmed) {
      showToast("Tên danh mục không được để trống", "error");
      return;
    }

    const original = categories.find((c) => c.id === id);
    if (original && original.name === trimmed) {
      setEditingId(null);
      setEditingName("");
      return;
    }

    setEditLoading(true);
    try {
      const updated = await updateCategory(id, trimmed);
      const newList = categories.map((c) => (c.id === id ? updated : c));
      setCategories(newList);
      onCategoriesChanged(newList);
      setEditingId(null);
      showToast(`Đã cập nhật danh mục "${updated.name}"`, "success");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "";
      if (msg === "Category name already exists") {
        showToast("Danh mục đã tồn tại trong hệ thống!", "error");
      } else {
        showToast("Cập nhật danh mục thất bại. Vui lòng thử lại!", "error");
      }
    } finally {
      setEditLoading(false);
    }
  };

  // Xử lý phím nhấn (Enter/Escape) khi đang sửa danh mục
  const handleEditKeyDown = (
    e: React.KeyboardEvent<HTMLInputElement>,
    id: number
  ) => {
    if (e.key === "Enter") confirmEdit(id);
    if (e.key === "Escape") cancelEdit();
  };

  // Xử lý xóa danh mục
  const handleDelete = async (id: number, name: string) => {
    try {
      await deleteCategory(id);
      const newList = categories.filter((c) => c.id !== id);
      setCategories(newList);
      onCategoriesChanged(newList);
      showToast(`Đã xóa danh mục "${name}"`, "success");
    } catch (err: unknown) {
      // Backend trả 409 nếu còn sản phẩm đang dùng danh mục này
      const msg =
        err instanceof Error && err.message.includes("409")
          ? `Không thể xóa danh mục "${name}" vì vẫn còn sản phẩm đang thuộc danh mục này.`
          : "Xóa danh mục thất bại. Vui lòng thử lại!";
      showToast(msg, "error");
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Quản lý danh mục" size="md">
      <div className={styles.wrapper}>
        
        <div className={styles.addRow}>
          <input
            ref={newInputRef}
            className={styles.addInput}
            type="text"
            placeholder="Nhập tên danh mục mới..."
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            onKeyDown={handleNewKeyDown}
            disabled={addLoading}
            maxLength={100}
          />
          <Button
            size="sm"
            icon="fi fi-rr-add"
            onClick={handleAdd}
            type="button"
            disabled={addLoading || !newName.trim()}
          >
            {addLoading ? "Đang thêm..." : "Thêm"}
          </Button>
        </div>

        
        <div className={styles.listWrapper}>
          {loading ? (
            <div className={styles.emptyState}>
              <i className="fi fi-rr-spinner" />
              <span>Đang tải danh mục...</span>
            </div>
          ) : categories.length === 0 ? (
            <div className={styles.emptyState}>
              <i className="fi fi-rr-box-open" />
              <span>Chưa có danh mục nào. Hãy thêm danh mục đầu tiên!</span>
            </div>
          ) : (
            <ul className={styles.list}>
              {categories.map((cat) => (
                <li key={cat.id} className={styles.item}>
                  {editingId === cat.id ? (
                    
                    <div className={styles.editRow}>
                      <input
                        className={styles.editInput}
                        type="text"
                        value={editingName}
                        onChange={(e) => setEditingName(e.target.value)}
                        onKeyDown={(e) => handleEditKeyDown(e, cat.id)}
                        autoFocus
                        maxLength={100}
                        disabled={editLoading}
                      />
                      <div className={styles.editActions}>
                        <button
                          className={`${styles.iconBtn} ${styles.iconBtnSuccess}`}
                          onClick={() => confirmEdit(cat.id)}
                          disabled={editLoading}
                          title="Lưu"
                          type="button"
                        >
                          <i className="fi fi-rr-check" />
                        </button>
                        <button
                          className={styles.iconBtn}
                          onClick={cancelEdit}
                          disabled={editLoading}
                          title="Hủy"
                          type="button"
                        >
                          <i className="fi fi-rr-cross-small" />
                        </button>
                      </div>
                    </div>
                  ) : (
                    
                    <div className={styles.viewRow}>
                      <div className={styles.catInfo}>
                        <i className="fi fi-rr-tag" />
                        <span className={styles.catName}>{cat.name}</span>
                      </div>
                      <div className={styles.viewActions}>
                        <button
                          className={`${styles.iconBtn} ${styles.iconBtnEdit}`}
                          onClick={() => startEdit(cat)}
                          title="Sửa tên"
                          type="button"
                        >
                          <i className="fi fi-rr-edit" />
                        </button>
                        <button
                          className={`${styles.iconBtn} ${styles.iconBtnDanger}`}
                          onClick={() => handleDelete(cat.id, cat.name)}
                          title="Xóa danh mục"
                          type="button"
                        >
                          <i className="fi fi-rr-trash" />
                        </button>
                      </div>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>

        
        <div className={styles.footer}>
          <span className={styles.count}>
            {categories.length} danh mục
          </span>
          <Button variant="secondary" size="sm" onClick={handleClose} type="button">
            Đóng
          </Button>
        </div>
      </div>
      <ConfirmDialog
        isOpen={showRestoreConfirm}
        title="Khôi phục danh mục?"
        message={`Danh mục "${restoreName}" đã từng bị xóa trước đó. Bạn có muốn khôi phục lại danh mục này không?`}
        confirmLabel="Khôi phục"
        cancelLabel="Hủy"
        onConfirm={handleRestore}
        onCancel={() => {
          setShowRestoreConfirm(false);
          setRestoreName("");
        }}
      />
    </Modal>
  );
}
