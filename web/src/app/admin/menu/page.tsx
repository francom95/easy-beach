'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  actualizarCategoria,
  actualizarProducto,
  actualizarVariante,
  cambiarDisponibilidadProducto,
  crearCategoria,
  crearProducto,
  crearVariante,
  eliminarCategoria,
  eliminarProducto,
  eliminarVariante,
  listarCategorias,
  listarProductos,
  listarVariantes,
  subirFotoProducto,
  type ProductoInput,
} from '../../../api/catalogo';
import { assetUrl } from '../../../api/config';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { EstadoCargando } from '../../../components/EstadoCarga';
import { EmptyState } from '../../../components/EmptyState';
import { useToast } from '../../../components/Toast';
import { formatearMonto } from '../../../utils/money';
import type { CategoriaMenuResponse, ProductoResponse } from '../../../api/types';
import styles from './page.module.css';

const PRODUCTO_VACIO: ProductoInput = { categoriaId: 0, nombre: '', descripcion: '', precioBase: '0', disponible: true, orden: 0 };

export default function MenuPage() {
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const [categoriaSeleccionada, setCategoriaSeleccionada] = useState<number | null>(null);
  const [productoEditando, setProductoEditando] = useState<ProductoResponse | 'nuevo' | null>(null);
  const [nuevaCategoria, setNuevaCategoria] = useState('');

  const categoriasQuery = useQuery({ queryKey: ['categorias'], queryFn: listarCategorias });
  const productosQuery = useQuery({ queryKey: ['productos'], queryFn: listarProductos });

  const invalidarTodo = () => {
    queryClient.invalidateQueries({ queryKey: ['categorias'] });
    queryClient.invalidateQueries({ queryKey: ['productos'] });
  };

  const crearCategoriaMut = useMutation({
    mutationFn: () => crearCategoria(nuevaCategoria, (categoriasQuery.data?.length ?? 0) + 1, true),
    onSuccess: () => {
      setNuevaCategoria('');
      invalidarTodo();
    },
    onError: () => mostrar('No pudimos crear la categoría', 'error'),
  });

  const eliminarCategoriaMut = useMutation({
    mutationFn: (id: number) => eliminarCategoria(id),
    onSuccess: () => {
      setCategoriaSeleccionada(null);
      invalidarTodo();
    },
    onError: () => mostrar('No pudimos eliminar la categoría', 'error'),
  });

  const toggleDisponibilidad = useMutation({
    mutationFn: ({ id, disponible }: { id: number; disponible: boolean }) => cambiarDisponibilidadProducto(id, disponible),
    onSuccess: invalidarTodo,
    onError: () => mostrar('No pudimos cambiar la disponibilidad', 'error'),
  });

  const renombrarCategoriaMut = useMutation({
    mutationFn: (cat: CategoriaMenuResponse) => {
      const nuevoNombre = window.prompt('Nuevo nombre de la categoría', cat.nombre);
      if (!nuevoNombre || !nuevoNombre.trim()) return Promise.reject(new Error('cancelado'));
      return actualizarCategoria(cat.id, nuevoNombre.trim(), cat.orden, cat.activa);
    },
    onSuccess: invalidarTodo,
    onError: (e: Error) => {
      if (e.message !== 'cancelado') mostrar('No pudimos renombrar la categoría', 'error');
    },
  });

  if (categoriasQuery.isLoading || productosQuery.isLoading) return <EstadoCargando texto="Cargando menú…" />;

  const categorias = categoriasQuery.data ?? [];
  const productos = productosQuery.data ?? [];
  const productosVisibles = categoriaSeleccionada ? productos.filter(p => p.categoriaId === categoriaSeleccionada) : productos;

  return (
    <div className={styles.wrap}>
      <h1 className={styles.titulo}>Menú</h1>
      <div className={styles.layout}>
        <Card className={styles.panelCategorias}>
          <div className={styles.panelTitulo}>Categorías</div>
          <button
            className={[styles.categoriaItem, categoriaSeleccionada === null ? styles.categoriaActiva : ''].join(' ')}
            onClick={() => setCategoriaSeleccionada(null)}
          >
            Todos los productos
          </button>
          {categorias.map((cat: CategoriaMenuResponse) => (
            <div key={cat.id} className={styles.categoriaRow}>
              <button
                className={[styles.categoriaItem, categoriaSeleccionada === cat.id ? styles.categoriaActiva : ''].join(' ')}
                onClick={() => setCategoriaSeleccionada(cat.id)}
                onDoubleClick={() => renombrarCategoriaMut.mutate(cat)}
                title="Doble click para renombrar"
              >
                {cat.nombre}
              </button>
              <button
                className={styles.borrarChico}
                onClick={() => {
                  if (confirm(`¿Eliminar la categoría "${cat.nombre}"?`)) eliminarCategoriaMut.mutate(cat.id);
                }}
              >
                ✕
              </button>
            </div>
          ))}
          <div className={styles.nuevaCategoria}>
            <input
              className={styles.input}
              placeholder="Nueva categoría"
              value={nuevaCategoria}
              onChange={e => setNuevaCategoria(e.target.value)}
            />
            <Button size="md" disabled={!nuevaCategoria.trim()} onClick={() => crearCategoriaMut.mutate()}>
              +
            </Button>
          </div>
        </Card>

        <Card className={styles.panelProductos}>
          <div className={styles.panelHeader}>
            <div className={styles.panelTitulo}>Productos</div>
            <Button size="md" onClick={() => setProductoEditando('nuevo')} disabled={categorias.length === 0}>
              + Producto
            </Button>
          </div>
          {productosVisibles.length === 0 ? (
            <EmptyState titulo="Sin productos" descripcion="Agregá el primer producto de esta categoría." />
          ) : (
            <table className={styles.tabla}>
              <thead>
                <tr>
                  <th></th>
                  <th>Nombre</th>
                  <th>Precio</th>
                  <th>Disponible</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {productosVisibles.map(p => (
                  <tr key={p.id}>
                    <td>
                      {p.fotoUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img src={assetUrl(p.fotoUrl)} alt="" className={styles.foto} />
                      ) : (
                        <div className={styles.fotoVacia} />
                      )}
                    </td>
                    <td onClick={() => setProductoEditando(p)} className={styles.nombreClickable}>
                      {p.nombre}
                    </td>
                    <td>{formatearMonto(p.precioBase)}</td>
                    <td>
                      <label className={styles.switch}>
                        <input
                          type="checkbox"
                          checked={p.disponible}
                          onChange={e => toggleDisponibilidad.mutate({ id: p.id, disponible: e.target.checked })}
                        />
                        <span className={styles.slider} />
                      </label>
                    </td>
                    <td>
                      <button className={styles.editarBtn} onClick={() => setProductoEditando(p)}>
                        Editar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      {productoEditando ? (
        <ProductoDrawer
          producto={productoEditando === 'nuevo' ? null : productoEditando}
          categorias={categorias}
          categoriaPreseleccionada={categoriaSeleccionada}
          onClose={() => setProductoEditando(null)}
          onGuardado={invalidarTodo}
        />
      ) : null}
    </div>
  );
}

function ProductoDrawer({
  producto,
  categorias,
  categoriaPreseleccionada,
  onClose,
  onGuardado,
}: {
  producto: ProductoResponse | null;
  categorias: CategoriaMenuResponse[];
  categoriaPreseleccionada: number | null;
  onClose: () => void;
  onGuardado: () => void;
}) {
  const { mostrar } = useToast();
  const [form, setForm] = useState<ProductoInput>(
    producto
      ? {
          categoriaId: producto.categoriaId,
          nombre: producto.nombre,
          descripcion: producto.descripcion ?? '',
          precioBase: producto.precioBase,
          disponible: producto.disponible,
          orden: producto.orden,
        }
      : { ...PRODUCTO_VACIO, categoriaId: categoriaPreseleccionada ?? categorias[0]?.id ?? 0 },
  );

  const variantesQuery = useQuery({
    queryKey: ['variantes', producto?.id],
    queryFn: () => listarVariantes(producto!.id),
    enabled: !!producto,
  });

  const guardarMut = useMutation({
    mutationFn: () => (producto ? actualizarProducto(producto.id, form) : crearProducto(form)),
    onSuccess: () => {
      mostrar('Producto guardado', 'exito');
      onGuardado();
      onClose();
    },
    onError: () => mostrar('No pudimos guardar el producto', 'error'),
  });

  const eliminarMut = useMutation({
    mutationFn: () => eliminarProducto(producto!.id),
    onSuccess: () => {
      onGuardado();
      onClose();
    },
    onError: () => mostrar('No pudimos eliminar el producto', 'error'),
  });

  const subirFotoMut = useMutation({
    mutationFn: (file: File) => subirFotoProducto(producto!.id, file),
    onSuccess: () => {
      mostrar('Foto actualizada', 'exito');
      onGuardado();
    },
    onError: () => mostrar('No pudimos subir la foto (¿es un PNG/JPEG válido?)', 'error'),
  });

  return (
    <div className={styles.drawerOverlay} onClick={onClose}>
      <div className={styles.drawer} onClick={e => e.stopPropagation()}>
        <h2>{producto ? 'Editar producto' : 'Nuevo producto'}</h2>

        <label className={styles.campo}>
          Categoría
          <select
            className={styles.input}
            value={form.categoriaId}
            onChange={e => setForm({ ...form, categoriaId: Number(e.target.value) })}
          >
            {categorias.map(c => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </select>
        </label>

        <label className={styles.campo}>
          Nombre
          <input className={styles.input} value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} />
        </label>

        <label className={styles.campo}>
          Descripción
          <textarea
            className={styles.input}
            rows={2}
            value={form.descripcion}
            onChange={e => setForm({ ...form, descripcion: e.target.value })}
          />
        </label>

        <label className={styles.campo}>
          {variantesQuery.data && variantesQuery.data.length > 0 ? 'Precio base (no aplica, usa el de cada variante)' : 'Precio'}
          <input
            className={styles.input}
            type="number"
            step="0.01"
            value={form.precioBase}
            onChange={e => setForm({ ...form, precioBase: e.target.value })}
          />
        </label>

        <label className={styles.checkboxRow}>
          <input
            type="checkbox"
            checked={form.disponible}
            onChange={e => setForm({ ...form, disponible: e.target.checked })}
          />
          Disponible
        </label>

        {producto ? (
          <label className={styles.campo}>
            Foto
            <input
              type="file"
              accept="image/png,image/jpeg"
              onChange={e => {
                const file = e.target.files?.[0];
                if (file) subirFotoMut.mutate(file);
              }}
            />
          </label>
        ) : (
          <p className={styles.hint}>Guardá el producto primero para poder subirle una foto.</p>
        )}

        {producto ? <VariantesSection productoId={producto.id} /> : null}

        <div className={styles.drawerAcciones}>
          {producto ? (
            <Button
              variant="danger"
              onClick={() => {
                if (confirm('¿Eliminar este producto?')) eliminarMut.mutate();
              }}
              cargando={eliminarMut.isPending}
            >
              Eliminar
            </Button>
          ) : (
            <span />
          )}
          <div style={{ display: 'flex', gap: 8 }}>
            <Button variant="ghost" onClick={onClose}>
              Cancelar
            </Button>
            <Button onClick={() => guardarMut.mutate()} cargando={guardarMut.isPending} disabled={!form.nombre.trim()}>
              Guardar
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

function VariantesSection({ productoId }: { productoId: number }) {
  const { mostrar } = useToast();
  const queryClient = useQueryClient();
  const [nombre, setNombre] = useState('');
  const [precio, setPrecio] = useState('');

  const variantesQuery = useQuery({ queryKey: ['variantes', productoId], queryFn: () => listarVariantes(productoId) });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['variantes', productoId] });

  const crearMut = useMutation({
    mutationFn: () => crearVariante(productoId, { nombre, precio, disponible: true, orden: (variantesQuery.data?.length ?? 0) + 1 }),
    onSuccess: () => {
      setNombre('');
      setPrecio('');
      invalidar();
    },
    onError: () => mostrar('No pudimos crear la variante', 'error'),
  });

  const toggleMut = useMutation({
    mutationFn: ({ id, disponible, nombreVar, precioVar, orden }: { id: number; disponible: boolean; nombreVar: string; precioVar: string; orden: number }) =>
      actualizarVariante(productoId, id, { nombre: nombreVar, precio: precioVar, disponible, orden }),
    onSuccess: invalidar,
  });

  const eliminarMut = useMutation({
    mutationFn: (id: number) => eliminarVariante(productoId, id),
    onSuccess: invalidar,
  });

  return (
    <div className={styles.variantes}>
      <div className={styles.panelTitulo}>Variantes</div>
      {(variantesQuery.data ?? []).map(v => (
        <div key={v.id} className={styles.varianteRow}>
          <span>{v.nombre}</span>
          <span>{formatearMonto(v.precio)}</span>
          <label className={styles.switch}>
            <input
              type="checkbox"
              checked={v.disponible}
              onChange={e =>
                toggleMut.mutate({ id: v.id, disponible: e.target.checked, nombreVar: v.nombre, precioVar: String(v.precio), orden: v.orden })
              }
            />
            <span className={styles.slider} />
          </label>
          <button className={styles.borrarChico} onClick={() => eliminarMut.mutate(v.id)}>
            ✕
          </button>
        </div>
      ))}
      <div className={styles.nuevaCategoria}>
        <input className={styles.input} placeholder="Nombre" value={nombre} onChange={e => setNombre(e.target.value)} />
        <input
          className={styles.input}
          placeholder="Precio"
          type="number"
          step="0.01"
          value={precio}
          onChange={e => setPrecio(e.target.value)}
        />
        <Button size="md" disabled={!nombre.trim() || !precio} onClick={() => crearMut.mutate()}>
          +
        </Button>
      </div>
    </div>
  );
}
