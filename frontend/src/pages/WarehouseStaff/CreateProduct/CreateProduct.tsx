import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ProductFormData } from '../../../types/product.types';
import { PRODUCT_CATEGORY_LABELS } from '../../../data/products.mock';
import { Input } from '../../../components/Input/Input';
import { Select } from '../../../components/Select/Select';
import { Button } from '../../../components/Button/Button';
import { Card, CardHeader, CardBody } from '../../../components/Card/Card';
import { validate, isRequired, isPositiveNumber } from '../../../utils/validators';
import { ROUTES } from '../../../constants/routes';
import styles from './CreateProduct.module.css';

const CATEGORY_OPTIONS = Object.entries(PRODUCT_CATEGORY_LABELS).map(([v, l]) => ({ value: v, label: l }));
const UNIT_OPTIONS = [
  { value: 'Cái', label: 'Cái' },
  { value: 'Đôi', label: 'Đôi' },
  { value: 'Bộ', label: 'Bộ' },
  { value: 'Chiếc', label: 'Chiếc' },
];

const INITIAL: ProductFormData = {
  sku: '', name: '', category: 'ao',
  importPrice: '', salePrice: '', unit: 'Cái',
  description: '', image: '',
};

export function CreateProduct() {
  const navigate = useNavigate();
  const [form, setForm] = useState<ProductFormData>(INITIAL);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleChange = (field: keyof ProductFormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
      setForm((prev) => ({ ...prev, [field]: e.target.value }));
      if (errors[field]) setErrors((prev) => ({ ...prev, [field]: '' }));
    };

  const handleSubmit = () => {
    const errs = validate(form as unknown as Record<string, string>, {
      sku: [isRequired],
      name: [isRequired],
      category: [isRequired],
      importPrice: [(v) => isPositiveNumber(v)],
      salePrice: [(v) => isPositiveNumber(v)],
    });

    if (Object.keys(errs).length) { setErrors(errs); return; }

    console.log('Tạo sản phẩm:', form);
    navigate(ROUTES.WAREHOUSE_PRODUCTS);
  };

  return (
    <section>
      <div className={styles.container}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>Tạo sản phẩm mới</h2>
            <p className={styles.subtitle}>Thêm sản phẩm vào hệ thống kho</p>
          </div>
        </div>

        <div className={styles.content}>
          <div className={styles.mainCol}>
            <Card>
              <CardHeader title="Thông tin cơ bản" />
              <CardBody>
                <div className={styles.formGrid}>
                  <Input id="sku" label="SKU" required value={form.sku} onChange={handleChange('sku')} error={errors.sku} placeholder="VD: AO-003" />
                  <Input id="name" label="Tên sản phẩm" required value={form.name} onChange={handleChange('name')} error={errors.name} placeholder="Nhập tên sản phẩm" />
                  <Select id="category" label="Danh mục" required options={CATEGORY_OPTIONS} value={form.category} onChange={handleChange('category')} error={errors.category} />
                  <Select id="unit" label="Đơn vị tính" options={UNIT_OPTIONS} value={form.unit} onChange={handleChange('unit')} />
                  <Input id="importPrice" label="Giá nhập" required type="number" min={0} value={form.importPrice} onChange={handleChange('importPrice')} error={errors.importPrice} placeholder="0" />
                  <Input id="salePrice" label="Giá bán" required type="number" min={0} value={form.salePrice} onChange={handleChange('salePrice')} error={errors.salePrice} placeholder="0" />
                </div>

                <div className={styles.formGroup}>
                  <label htmlFor="description" className={styles.label}>Mô tả sản phẩm</label>
                  <textarea id="description" className={styles.textarea} rows={4} value={form.description} onChange={handleChange('description')} placeholder="Nhập mô tả sản phẩm..." maxLength={1000} />
                </div>
              </CardBody>
            </Card>
          </div>

          <div className={styles.sideCol}>
            <Card>
              <CardHeader title="Hình ảnh" />
              <CardBody>
                <div className={styles.imageUpload}>
                  <i className="fi fi-rr-picture" aria-hidden />
                  <p>Kéo thả hoặc chọn hình ảnh</p>
                  <Button variant="secondary" size="sm" icon="fi fi-rr-upload">Tải lên</Button>
                </div>
              </CardBody>
            </Card>

            <Card>
              <CardBody>
                <div className={styles.actions}>
                  <Button variant="secondary" onClick={() => navigate(-1)} icon="fi fi-rr-arrow-left">Hủy</Button>
                  <Button onClick={handleSubmit} icon="fi fi-rr-check">Lưu sản phẩm</Button>
                </div>
              </CardBody>
            </Card>
          </div>
        </div>
      </div>
    </section>
  );
}
