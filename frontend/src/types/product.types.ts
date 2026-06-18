export type ProductCategory =
  | 'ao'
  | 'quan'
  | 'dam'
  | 'vay'
  | 'phu_kien'
  | 'giay';

export interface Product {
  id: string;
  sku: string;
  name: string;
  category: ProductCategory;
  categoryLabel: string;
  importPrice: number;
  salePrice: number;
  unit: string;
  stock: number;
  description: string;
  image: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductFormData {
  sku: string;
  name: string;
  category: ProductCategory;
  importPrice: number | string;
  salePrice: number | string;
  unit: string;
  description: string;
  image: string;
}
