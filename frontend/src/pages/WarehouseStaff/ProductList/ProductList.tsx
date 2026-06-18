import { useState } from 'react';
import type { Product } from '../../../types/product.types';
import { MOCK_PRODUCTS, PRODUCT_CATEGORY_LABELS } from '../../../data/products.mock';
import { Table } from '../../../components/Table/Table';
import { SearchBox } from '../../../components/SearchBox/SearchBox';
import { Pagination } from '../../../components/Pagination/Pagination';
import { Select } from '../../../components/Select/Select';
import { Card, CardHeader, CardBody } from '../../../components/Card/Card';
import type { TableColumn, SelectOption } from '../../../types/common.types';
import { useSearch } from '../../../hooks/useSearch';
import { usePagination } from '../../../hooks/usePagination';
import { formatCurrency } from '../../../utils/formatters';
import styles from './ProductList.module.css';

const CATEGORY_OPTIONS: SelectOption[] = [
  { value: '', label: 'Tất cả danh mục' },
  ...Object.entries(PRODUCT_CATEGORY_LABELS).map(([v, l]) => ({ value: v, label: l })),
];

export function ProductList() {
  const [categoryFilter, setCategoryFilter] = useState('');
  const { query, setQuery, filteredItems } = useSearch(MOCK_PRODUCTS, ['sku', 'name', 'categoryLabel']);

  const filtered = categoryFilter
    ? filteredItems.filter((p) => p.category === categoryFilter)
    : filteredItems;

  const { currentItems, pagination, goToPage } = usePagination(filtered, 6);

  const columns: TableColumn<Product>[] = [
    { key: 'sku', label: 'SKU', width: '110px', render: (val) => <code className={styles.sku}>{val as string}</code> },
    {
      key: 'image', label: 'Hình ảnh', width: '70px', align: 'center',
      render: () => (
        <div className={styles.imgThumb}>
          <i className="fi fi-rr-shirt" aria-hidden />
        </div>
      ),
    },
    { key: 'name', label: 'Tên sản phẩm' },
    { key: 'categoryLabel', label: 'Danh mục', width: '110px' },
    {
      key: 'stock', label: 'Tồn kho', align: 'center', width: '90px',
      render: (val) => (
        <span className={[styles.stockBadge, (val as number) < 20 ? styles.lowStock : ''].join(' ')}>
          {val as number}
        </span>
      ),
    },
    {
      key: 'salePrice', label: 'Giá bán', align: 'right',
      render: (val) => <strong>{formatCurrency(val as number)}</strong>,
    },
  ];

  return (
    <section>
      <div className={styles.container}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>Danh sách sản phẩm</h2>
            <p className={styles.subtitle}>{filtered.length} sản phẩm</p>
          </div>
        </div>

        <Card>
          <CardHeader
            title="Tất cả sản phẩm"
            actions={
              <div className={styles.filters}>
                <Select
                  id="categoryFilter"
                  options={CATEGORY_OPTIONS}
                  value={categoryFilter}
                  onChange={(e) => setCategoryFilter(e.target.value)}
                />
                <SearchBox
                  placeholder="Tìm SKU, tên sản phẩm..."
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  onClear={() => setQuery('')}
                />
              </div>
            }
          />
          <CardBody className={styles.tableBody}>
            <Table columns={columns} data={currentItems} rowKey="id" emptyText="Không tìm thấy sản phẩm" />
            <div className={styles.paginationWrap}>
              <Pagination pagination={pagination} onPageChange={goToPage} />
            </div>
          </CardBody>
        </Card>
      </div>
    </section>
  );
}
