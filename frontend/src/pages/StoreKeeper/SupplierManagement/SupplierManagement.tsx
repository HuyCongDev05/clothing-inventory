import { useState } from 'react';
import type { Supplier } from '../../../types/supplier.types';
import { MOCK_SUPPLIERS } from '../../../data/suppliers.mock';
import { Table } from '../../../components/Table/Table';
import { Button } from '../../../components/Button/Button';
import { SearchBox } from '../../../components/SearchBox/SearchBox';
import { ConfirmDialog } from '../../../components/ConfirmDialog/ConfirmDialog';
import { Card, CardHeader, CardBody } from '../../../components/Card/Card';
import type { TableColumn } from '../../../types/common.types';
import { useSearch } from '../../../hooks/useSearch';
import styles from './SupplierManagement.module.css';

export function SupplierManagement() {
  const [suppliers, setSuppliers] = useState<Supplier[]>(MOCK_SUPPLIERS);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const { query, setQuery, filteredItems } = useSearch(suppliers, ['code', 'companyName', 'contactPerson', 'email']);

  const handleDelete = () => {
    if (!deleteId) return;
    setSuppliers((prev) => prev.filter((s) => s.id !== deleteId));
    setDeleteId(null);
  };

  const columns: TableColumn<Supplier>[] = [
    { key: 'code', label: 'Mã NCC', width: '100px' },
    { key: 'companyName', label: 'Tên công ty' },
    { key: 'contactPerson', label: 'Người liên hệ' },
    { key: 'phone', label: 'Điện thoại', width: '130px' },
    { key: 'email', label: 'Email' },
    {
      key: 'id', label: 'Hành động', width: '160px', align: 'center',
      render: (_, row) => (
        <div className={styles.actions}>
          <Button variant="ghost" size="sm" icon="fi fi-rr-eye">Xem</Button>
          <Button variant="danger" size="sm" icon="fi fi-rr-trash" onClick={() => setDeleteId(row.id)}>Xóa</Button>
        </div>
      ),
    },
  ];

  const deleteTarget = suppliers.find((s) => s.id === deleteId);

  return (
    <section>
      <div className={styles.container}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>Quản lý nhà cung cấp</h2>
            <p className={styles.subtitle}>{filteredItems.length} nhà cung cấp</p>
          </div>
        </div>

        <Card>
          <CardHeader
            title="Danh sách nhà cung cấp"
            actions={
              <SearchBox
                placeholder="Tìm kiếm..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onClear={() => setQuery('')}
              />
            }
          />
          <CardBody className={styles.tableBody}>
            <Table columns={columns} data={filteredItems} rowKey="id" />
          </CardBody>
        </Card>
      </div>

      <ConfirmDialog
        isOpen={!!deleteId}
        title="Xóa nhà cung cấp"
        message={`Bạn có chắc muốn xóa "${deleteTarget?.companyName}"? Hành động này không thể hoàn tác.`}
        confirmLabel="Xóa"
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />
    </section>
  );
}
