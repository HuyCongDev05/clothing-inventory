import { useState } from 'react';
import type { Supplier, SupplierFormData } from '../../../types/supplier.types';
import { MOCK_SUPPLIERS } from '../../../data/suppliers.mock';
import { useSearch } from '../../../hooks/useSearch';
import { Table } from '../../../components/Table/Table';
import { Button } from '../../../components/Button/Button';
import { Modal } from '../../../components/Modal/Modal';
import { SearchBox } from '../../../components/SearchBox/SearchBox';
import { Input } from '../../../components/Input/Input';
import { Card, CardHeader, CardBody } from '../../../components/Card/Card';
import type { TableColumn } from '../../../types/common.types';
import { validate, isRequired, isEmail, isPhone } from '../../../utils/validators';
import styles from './SupplierList.module.css';

const INITIAL_FORM: SupplierFormData = {
  companyName: '', taxCode: '', representative: '',
  address: '', email: '', phone: '', note: '',
};

const STATUS_MAP = { active: 'Hoạt động', inactive: 'Ngừng hoạt động' } as const;

type ModalMode = 'add' | 'edit' | 'detail' | null;

export function SupplierList() {
  const [suppliers, setSuppliers] = useState<Supplier[]>(MOCK_SUPPLIERS);
  const [modalMode, setModalMode] = useState<ModalMode>(null);
  const [selectedSupplier, setSelectedSupplier] = useState<Supplier | null>(null);
  const [form, setForm] = useState<SupplierFormData>(INITIAL_FORM);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const { query, setQuery, filteredItems } = useSearch(suppliers, ['code', 'companyName', 'contactPerson', 'email']);

  const openAdd = () => {
    setForm(INITIAL_FORM);
    setErrors({});
    setSelectedSupplier(null);
    setModalMode('add');
  };

  const openEdit = (supplier: Supplier) => {
    setForm({
      companyName: supplier.companyName,
      taxCode: supplier.taxCode,
      representative: supplier.representative,
      address: supplier.address,
      email: supplier.email,
      phone: supplier.phone,
      note: supplier.note,
    });
    setErrors({});
    setSelectedSupplier(supplier);
    setModalMode('edit');
  };

  const openDetail = (supplier: Supplier) => {
    setSelectedSupplier(supplier);
    setModalMode('detail');
  };

  const closeModal = () => {
    setModalMode(null);
    setSelectedSupplier(null);
    setErrors({});
  };

  const handleChange = (field: keyof SupplierFormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setForm((prev) => ({ ...prev, [field]: e.target.value }));
      if (errors[field]) setErrors((prev) => ({ ...prev, [field]: '' }));
    };

  const validateForm = () =>
    validate(form as unknown as Record<string, string>, {
      companyName: [isRequired],
      email: [isRequired, isEmail],
      phone: [isRequired, isPhone],
    });

  const handleAdd = () => {
    const errs = validateForm();
    if (Object.keys(errs).length) { setErrors(errs); return; }

    const newSupplier: Supplier = {
      id: String(Date.now()),
      code: `NCC00${suppliers.length + 1}`,
      contactPerson: form.representative,
      status: 'active',
      createdAt: new Date().toISOString().split('T')[0],
      ...form,
    };

    setSuppliers((prev) => [newSupplier, ...prev]);
    closeModal();
  };

  const handleEdit = () => {
    const errs = validateForm();
    if (Object.keys(errs).length) { setErrors(errs); return; }

    setSuppliers((prev) =>
      prev.map((s) =>
        s.id === selectedSupplier?.id
          ? { ...s, ...form, contactPerson: form.representative }
          : s,
      ),
    );
    closeModal();
  };

  const columns: TableColumn<Supplier>[] = [
    { key: 'code', label: 'Mã NCC', width: '110px' },
    { key: 'companyName', label: 'Tên NCC' },
    { key: 'contactPerson', label: 'Người liên hệ' },
    { key: 'phone', label: 'Số điện thoại', width: '130px' },
    { key: 'email', label: 'Email' },
    {
      key: 'status', label: 'Trạng thái', width: '130px', align: 'center',
      render: (val) => (
        <span className={[styles.badge, val === 'active' ? styles.active : styles.inactive].join(' ')}>
          {STATUS_MAP[val as keyof typeof STATUS_MAP]}
        </span>
      ),
    },
    {
      key: 'id', label: 'Hành động', width: '90px', align: 'center',
      render: (_, row) => (
        <Button variant="ghost" size="sm" icon="fi fi-rr-eye" onClick={() => openDetail(row)}>Xem</Button>
      ),
    },
  ];

  const renderForm = () => (
    <div className={styles.form}>
      <div className={styles.formRow}>
        <Input id="companyName" label="Tên công ty" required value={form.companyName} onChange={handleChange('companyName')} error={errors.companyName} placeholder="Nhập tên công ty" />
        <Input id="taxCode" label="Mã số thuế" value={form.taxCode} onChange={handleChange('taxCode')} placeholder="Nhập mã số thuế" />
      </div>
      <div className={styles.formRow}>
        <Input id="representative" label="Người đại diện" value={form.representative} onChange={handleChange('representative')} placeholder="Nhập tên người đại diện" />
        <Input id="phone" label="Số điện thoại" required value={form.phone} onChange={handleChange('phone')} error={errors.phone} placeholder="0901234567" />
      </div>
      <div className={styles.formRow}>
        <Input id="email" label="Email" required type="email" value={form.email} onChange={handleChange('email')} error={errors.email} placeholder="email@company.vn" />
        <Input id="address" label="Địa chỉ" value={form.address} onChange={handleChange('address')} placeholder="Nhập địa chỉ" />
      </div>
      <div className={styles.formGroup}>
        <label htmlFor="note" className={styles.label}>Ghi chú</label>
        <textarea id="note" className={styles.textarea} value={form.note} onChange={handleChange('note')} rows={3} placeholder="Ghi chú thêm..." maxLength={1000} />
      </div>
    </div>
  );

  return (
    <section>
      <div className={styles.container}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>Nhà cung cấp</h2>
            <p className={styles.subtitle}>{filteredItems.length} nhà cung cấp</p>
          </div>
          <Button icon="fi fi-rr-add" onClick={openAdd} id="add-supplier-btn">Thêm mới</Button>
        </div>

        <Card>
          <CardHeader
            title="Danh sách nhà cung cấp"
            actions={
              <SearchBox
                placeholder="Tìm theo tên, mã, email..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onClear={() => setQuery('')}
              />
            }
          />
          <CardBody className={styles.tableBody}>
            <Table columns={columns} data={filteredItems} rowKey="id" emptyText="Không tìm thấy nhà cung cấp" />
          </CardBody>
        </Card>
      </div>

      <Modal isOpen={modalMode === 'add'} onClose={closeModal} title="Thêm nhà cung cấp mới" size="lg">
        {renderForm()}
        <div className={styles.modalActions}>
          <Button variant="secondary" onClick={closeModal}>Hủy</Button>
          <Button onClick={handleAdd} icon="fi fi-rr-check">Lưu nhà cung cấp</Button>
        </div>
      </Modal>

      <Modal isOpen={modalMode === 'edit'} onClose={closeModal} title={`Chỉnh sửa: ${selectedSupplier?.companyName ?? ''}`} size="lg">
        {renderForm()}
        <div className={styles.modalActions}>
          <Button variant="secondary" onClick={closeModal}>Hủy</Button>
          <Button onClick={handleEdit} icon="fi fi-rr-check">Lưu thay đổi</Button>
        </div>
      </Modal>

      <Modal isOpen={modalMode === 'detail'} onClose={closeModal} title="Chi tiết nhà cung cấp">
        {selectedSupplier && (
          <div className={styles.detail}>
            {(Object.entries({
              'Mã NCC': selectedSupplier.code,
              'Tên công ty': selectedSupplier.companyName,
              'Mã số thuế': selectedSupplier.taxCode,
              'Người đại diện': selectedSupplier.representative,
              'Người liên hệ': selectedSupplier.contactPerson,
              'Số điện thoại': selectedSupplier.phone,
              'Email': selectedSupplier.email,
              'Địa chỉ': selectedSupplier.address,
              'Ghi chú': selectedSupplier.note || '—',
              'Trạng thái': STATUS_MAP[selectedSupplier.status],
            })).map(([k, v]) => (
              <div key={k} className={styles.detailRow}>
                <span className={styles.detailKey}>{k}</span>
                <span className={styles.detailVal}>{v}</span>
              </div>
            ))}
            <div className={styles.modalActions}>
              <Button variant="secondary" onClick={closeModal}>Đóng</Button>
              <Button variant="secondary" icon="fi fi-rr-edit" onClick={() => openEdit(selectedSupplier)}>Chỉnh sửa</Button>
            </div>
          </div>
        )}
      </Modal>
    </section>
  );
}
