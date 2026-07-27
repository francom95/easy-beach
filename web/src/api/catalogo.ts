import { apiRequest } from './client';
import type { CategoriaMenuResponse, ProductoResponse, ProductoVarianteResponse } from './types';

export function listarCategorias(): Promise<CategoriaMenuResponse[]> {
  return apiRequest<CategoriaMenuResponse[]>('/admin/categorias');
}

export function crearCategoria(nombre: string, orden: number, activa: boolean): Promise<CategoriaMenuResponse> {
  return apiRequest<CategoriaMenuResponse>('/admin/categorias', { method: 'POST', body: { nombre, orden, activa } });
}

export function actualizarCategoria(id: number, nombre: string, orden: number, activa: boolean): Promise<CategoriaMenuResponse> {
  return apiRequest<CategoriaMenuResponse>(`/admin/categorias/${id}`, { method: 'PUT', body: { nombre, orden, activa } });
}

export function eliminarCategoria(id: number): Promise<void> {
  return apiRequest<void>(`/admin/categorias/${id}`, { method: 'DELETE' });
}

export function listarProductos(): Promise<ProductoResponse[]> {
  return apiRequest<ProductoResponse[]>('/admin/productos');
}

export type ProductoInput = {
  categoriaId: number;
  nombre: string;
  descripcion: string;
  precioBase: string;
  disponible: boolean;
  orden: number;
};

export function crearProducto(input: ProductoInput): Promise<ProductoResponse> {
  return apiRequest<ProductoResponse>('/admin/productos', { method: 'POST', body: input });
}

export function actualizarProducto(id: number, input: ProductoInput): Promise<ProductoResponse> {
  return apiRequest<ProductoResponse>(`/admin/productos/${id}`, { method: 'PUT', body: input });
}

export function cambiarDisponibilidadProducto(id: number, disponible: boolean): Promise<ProductoResponse> {
  return apiRequest<ProductoResponse>(`/admin/productos/${id}/disponibilidad`, { method: 'PUT', body: { disponible } });
}

export function eliminarProducto(id: number): Promise<void> {
  return apiRequest<void>(`/admin/productos/${id}`, { method: 'DELETE' });
}

export function subirFotoProducto(id: number, file: File): Promise<ProductoResponse> {
  const formData = new FormData();
  formData.append('file', file);
  return apiRequest<ProductoResponse>(`/admin/productos/${id}/foto`, { method: 'POST', formData });
}

export function listarVariantes(productoId: number): Promise<ProductoVarianteResponse[]> {
  return apiRequest<ProductoVarianteResponse[]>(`/admin/productos/${productoId}/variantes`);
}

export type VarianteInput = { nombre: string; precio: string; disponible: boolean; orden: number };

export function crearVariante(productoId: number, input: VarianteInput): Promise<ProductoVarianteResponse> {
  return apiRequest<ProductoVarianteResponse>(`/admin/productos/${productoId}/variantes`, { method: 'POST', body: input });
}

export function actualizarVariante(productoId: number, id: number, input: VarianteInput): Promise<ProductoVarianteResponse> {
  return apiRequest<ProductoVarianteResponse>(`/admin/productos/${productoId}/variantes/${id}`, { method: 'PUT', body: input });
}

export function eliminarVariante(productoId: number, id: number): Promise<void> {
  return apiRequest<void>(`/admin/productos/${productoId}/variantes/${id}`, { method: 'DELETE' });
}
