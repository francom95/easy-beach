'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function OperativoIndex() {
  const router = useRouter();
  useEffect(() => {
    router.replace('/operativo/pedidos');
  }, [router]);
  return null;
}
