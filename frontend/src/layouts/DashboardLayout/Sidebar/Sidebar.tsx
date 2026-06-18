import { NavLink, useLocation } from 'react-router-dom';
import { NAV_GROUPS } from '../../../constants/navigation';
import { ROUTES } from '../../../constants/routes';
import styles from './Sidebar.module.css';

interface SidebarProps {
  collapsed?: boolean;
}

export function Sidebar({ collapsed = false }: SidebarProps) {
  const location = useLocation();

  return (
    <nav className={[styles.sidebar, collapsed ? styles.collapsed : ''].join(' ')}>
      <div className={styles.logo}>
        <div className={styles.logoIcon}>
          <i className="fi fi-rr-box-alt" aria-hidden />
        </div>
        {!collapsed && (
          <div className={styles.logoText}>
            <span className={styles.logoName}>WareFlow</span>
            <span className={styles.logoSub}>Quản lý nhập kho</span>
          </div>
        )}
      </div>

      <NavLink
        to={ROUTES.DASHBOARD}
        className={({ isActive }) =>
          [styles.navItem, isActive ? styles.active : ''].join(' ')
        }
        end
      >
        <i className="fi fi-rr-dashboard" aria-hidden />
        {!collapsed && <span>Dashboard</span>}
      </NavLink>

      <div className={styles.navGroups}>
        {NAV_GROUPS.map((group) => (
          <div key={group.groupLabel} className={styles.navGroup}>
            {!collapsed && (
              <span className={styles.groupLabel}>{group.groupLabel}</span>
            )}
            {group.items.map((item) => {
              const isActive = item.path
                ? location.pathname === item.path || location.pathname.startsWith(item.path + '/')
                : false;

              return (
                <NavLink
                  key={item.label}
                  to={item.path ?? '#'}
                  className={[styles.navItem, isActive ? styles.active : ''].join(' ')}
                  title={collapsed ? item.label : undefined}
                >
                  <i className={item.icon} aria-hidden />
                  {!collapsed && <span>{item.label}</span>}
                </NavLink>
              );
            })}
          </div>
        ))}
      </div>

      <div className={styles.userCard}>
        <div className={styles.avatar} aria-hidden>
          <span>N</span>
        </div>
        {!collapsed && (
          <div className={styles.userInfo}>
            <span className={styles.userName}>Nguyễn Văn A</span>
            <span className={styles.userRole}>Điều phối viên</span>
          </div>
        )}
      </div>
    </nav>
  );
}
