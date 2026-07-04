import { useState } from "react";
import { Button } from "../../../components/Button/Button";
import { Input } from "../../../components/Input/Input";
import { Select } from "../../../components/Select/Select";
import { Modal } from "../../../components/Modal/Modal";
import { Card, CardHeader, CardBody } from "../../../components/Card/Card";
import { useToast } from "../../../components/Toast/ToastContext";
import { registerUser } from "../../../services/auth";
import { validate, isRequired, isEmail } from "../../../utils/validators";
import styles from "./UserManagement.module.css";

const ROLE_OPTIONS = [
  { value: "coordinator", label: "Nhân viên điều phối" },
  { value: "warehouse-staff", label: "Nhân viên kho" },
  { value: "store-keeper", label: "Thủ kho" },
];

interface FormData {
  username: string;
  password: string;
  confirmPassword: string;
  fullName: string;
  phone: string;
  email: string;
  role: string;
}

const INITIAL_FORM: FormData = {
  username: "",
  password: "",
  confirmPassword: "",
  fullName: "",
  phone: "",
  email: "",
  role: "warehouse-staff",
};

export function UserManagement() {
  const { showToast } = useToast();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form, setForm] = useState<FormData>(INITIAL_FORM);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (field: keyof FormData, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: "" }));
  };

  const handleOpen = () => {
    setForm(INITIAL_FORM);
    setErrors({});
    setIsModalOpen(true);
  };

  const handleClose = () => {
    setIsModalOpen(false);
    setForm(INITIAL_FORM);
    setErrors({});
  };

  const validateForm = (): boolean => {
    const rules = {
      username: [isRequired],
      password: [isRequired],
      fullName: [isRequired],
      role: [isRequired],
    };

    const newErrors = validate(form as unknown as Record<string, string>, rules);

    if (form.password && form.password.length < 6) {
      newErrors.password = "Mật khẩu phải có ít nhất 6 ký tự";
    }

    if (form.confirmPassword !== form.password) {
      newErrors.confirmPassword = "Mật khẩu xác nhận không khớp";
    }

    if (form.email) {
      const emailErr = isEmail(form.email);
      if (emailErr) newErrors.email = emailErr;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) return;
    setSubmitting(true);
    try {
      await registerUser({
        username: form.username.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        phone: form.phone.trim(),
        email: form.email.trim(),
        roles: [form.role],
      });
      showToast("Tạo tài khoản thành công!", "success");
      handleClose();
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "Tạo tài khoản thất bại!";
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <div className={styles.container}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>Quản lý tài khoản</h2>
            <p className={styles.subtitle}>Tạo tài khoản nhân viên hệ thống</p>
          </div>
          <Button icon="fi fi-rr-add" onClick={handleOpen} id="add-user-btn">
            Tạo tài khoản
          </Button>
        </div>

        <Card>
          <CardHeader title="Tài khoản hệ thống" />
          <CardBody>
            <div className={styles.emptyState}>
              <i className="fi fi-rr-users-alt" />
              <p>Nhấn <strong>Tạo tài khoản</strong> để thêm nhân viên mới vào hệ thống.</p>
            </div>
          </CardBody>
        </Card>
      </div>

      <Modal
        isOpen={isModalOpen}
        onClose={handleClose}
        title="Tạo tài khoản mới"
        size="md"
      >
        <div className={styles.form}>
          <div className={styles.formRow}>
            <Input
              id="username"
              label="Tên đăng nhập *"
              value={form.username}
              onChange={(e) => handleChange("username", e.target.value)}
              error={errors.username}
              placeholder="Nhập tên đăng nhập"
            />
            <Select
              id="role"
              label="Vai trò *"
              options={ROLE_OPTIONS}
              value={form.role}
              onChange={(e) => handleChange("role", e.target.value)}
              error={errors.role}
            />
          </div>

          <div className={styles.formRow}>
            <Input
              id="password"
              label="Mật khẩu *"
              type="password"
              value={form.password}
              onChange={(e) => handleChange("password", e.target.value)}
              error={errors.password}
              placeholder="Nhập mật khẩu"
            />
            <Input
              id="confirmPassword"
              label="Xác nhận mật khẩu *"
              type="password"
              value={form.confirmPassword}
              onChange={(e) => handleChange("confirmPassword", e.target.value)}
              error={errors.confirmPassword}
              placeholder="Nhập lại mật khẩu"
            />
          </div>

          <div className={styles.formRow}>
            <Input
              id="fullName"
              label="Họ và tên *"
              value={form.fullName}
              onChange={(e) => handleChange("fullName", e.target.value)}
              error={errors.fullName}
              placeholder="Nhập họ và tên"
            />
            <Input
              id="phone"
              label="Số điện thoại"
              value={form.phone}
              onChange={(e) => handleChange("phone", e.target.value)}
              error={errors.phone}
              placeholder="Nhập số điện thoại"
            />
          </div>

          <div className={styles.formRowSingle}>
            <Input
              id="email"
              label="Email"
              type="email"
              value={form.email}
              onChange={(e) => handleChange("email", e.target.value)}
              error={errors.email}
              placeholder="Nhập địa chỉ email"
            />
          </div>

          <div className={styles.formActions}>
            <Button variant="secondary" onClick={handleClose}>
              Hủy
            </Button>
            <Button
              onClick={handleSubmit}
              icon="fi fi-rr-check"
              disabled={submitting}
            >
              {submitting ? "Đang tạo..." : "Tạo tài khoản"}
            </Button>
          </div>
        </div>
      </Modal>
    </section>
  );
}
