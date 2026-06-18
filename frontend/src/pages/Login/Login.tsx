import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Input } from '../../components/Input/Input';
import { Button } from '../../components/Button/Button';
import { ROUTES } from '../../constants/routes';
import styles from './Login.module.css';

export function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    await new Promise((r) => setTimeout(r, 800));
    navigate(ROUTES.DASHBOARD);
  };

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.header}>
          <div className={styles.brandRow}>
            <div className={styles.brandIcon}>
              <i className="fi fi-rr-shirt" />
            </div>
            <span className={styles.brandName}>Clothing Inventory</span>
          </div>
          <h1 className={styles.title}>Đăng nhập</h1>
          <p className={styles.subtitle}>Nhập thông tin tài khoản để tiếp tục</p>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <Input
            id="login-username"
            label="Tên đăng nhập"
            icon="fi fi-rr-user"
            placeholder="Nhập tên đăng nhập..."
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            autoFocus
          />

          <Input
            id="login-password"
            label="Mật khẩu"
            icon="fi fi-rr-lock"
            type="password"
            placeholder="Nhập mật khẩu..."
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />

          <Button
            id="login-submit"
            type="submit"
            size="md"
            loading={loading}
            style={{ width: '100%', height: 42 }}
          >
            Đăng nhập
          </Button>
        </form>
      </div>
    </div>
  );
}
