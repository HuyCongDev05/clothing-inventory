import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar/Sidebar';
import { Header } from './Header/Header';
import { Drawer } from '../../components/Drawer/Drawer';
import styles from './DashboardLayout.module.css';

export function DashboardLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  return (
    <div className={styles.layout}>
      <div className={styles.desktopSidebar}>
        <Sidebar collapsed={sidebarCollapsed} />
      </div>

      {/* Drawer dành cho mobile */}
      <Drawer isOpen={drawerOpen} onClose={() => setDrawerOpen(false)}>
        <Sidebar />
      </Drawer>

      <div className={styles.main}>
        <Header
          onMenuClick={() => setDrawerOpen(true)}
          onToggleSidebar={() => setSidebarCollapsed((v) => !v)}
        />
        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
