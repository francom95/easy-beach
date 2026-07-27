import React, {createContext, useCallback, useContext, useMemo, useState} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {obtenerBranding} from '../api/balnearios';
import {DEFAULT_TOKENS} from './defaultTokens';
import {FONT_PAIRS, FONT_PAIR_DEFAULT, RADII, SHADOW_LEVELS, SPACING, TYPE_SCALE} from './tokenContract';
import type {BrandingTokens} from '../api/types';

type Theme = {
  tokens: BrandingTokens;
  fuente: (typeof FONT_PAIRS)[string];
  spacing: typeof SPACING;
  radii: typeof RADII;
  typeScale: typeof TYPE_SCALE;
  shadow: typeof SHADOW_LEVELS;
  /** true = marca EasyBeach (zona 1, antes de elegir balneario o tras cerrar estadía). */
  esMarcaEasyBeach: boolean;
};

type ThemeContextValue = Theme & {
  cargando: boolean;
  /** true si se está mostrando el último theme cacheado porque la red falló (nunca cae a EasyBeach). */
  usandoCacheOffline: boolean;
  cargarThemeDeBalneario: (slug: string) => Promise<void>;
  volverAMarcaEasyBeach: () => void;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

function claveCache(slug: string): string {
  return `theme:${slug}`;
}

function fusionarConDefaults(tokens: Partial<BrandingTokens> | null): BrandingTokens {
  // Contrato v1.0: claves desconocidas se ignoran (con TS ya no llegan acá
  // porque BrandingTokens las tipa), faltantes toman el default EasyBeach.
  return {...DEFAULT_TOKENS, ...(tokens ?? {})};
}

export function ThemeProvider({children}: {children: React.ReactNode}) {
  const [tokens, setTokens] = useState<BrandingTokens>(DEFAULT_TOKENS);
  const [esMarcaEasyBeach, setEsMarcaEasyBeach] = useState(true);
  const [cargando, setCargando] = useState(false);
  const [usandoCacheOffline, setUsandoCacheOffline] = useState(false);

  const cargarThemeDeBalneario = useCallback(async (slug: string) => {
    setCargando(true);
    setUsandoCacheOffline(false);

    // 1. Cache primero: permite mostrar el splash del balneario al instante
    // en el re-ingreso diario (etapa 06: "subsecuentes aperturas van directo
    // al splash del balneario"), sin esperar la red.
    const cacheado = await AsyncStorage.getItem(claveCache(slug));
    if (cacheado) {
      setTokens(fusionarConDefaults(JSON.parse(cacheado)));
      setEsMarcaEasyBeach(false);
    }

    // 2. Revalidar contra el servidor. GET /balnearios/{slug}/branding no
    // trae ETag/Cache-Control (a diferencia del menú de la etapa 11) - no
    // hay forma de pedir condicionalmente, así que esto es "traer y pisar
    // cache", no una revalidación condicional real.
    try {
      const frescos = await obtenerBranding(slug);
      setTokens(fusionarConDefaults(frescos));
      setEsMarcaEasyBeach(false);
      await AsyncStorage.setItem(claveCache(slug), JSON.stringify(frescos));
    } catch (error) {
      if (cacheado) {
        // Regla dura (etapa 06): dentro de una estadía, sin red, el
        // fallback es el ÚLTIMO theme cacheado - jamás el neutro EasyBeach.
        setUsandoCacheOffline(true);
      } else {
        // Primera vez que se entra a este balneario y no hay red: no hay
        // nada que mostrar todavía. Se propaga para que la pantalla
        // llamante decida (típicamente S33 offline).
        setCargando(false);
        throw error;
      }
    } finally {
      setCargando(false);
    }
  }, []);

  const volverAMarcaEasyBeach = useCallback(() => {
    setTokens(DEFAULT_TOKENS);
    setEsMarcaEasyBeach(true);
    setUsandoCacheOffline(false);
  }, []);

  const value = useMemo<ThemeContextValue>(() => {
    const familia = tokens['typography.family'] ?? FONT_PAIR_DEFAULT;
    return {
      tokens,
      fuente: FONT_PAIRS[familia] ?? FONT_PAIRS[FONT_PAIR_DEFAULT],
      spacing: SPACING,
      radii: RADII,
      typeScale: TYPE_SCALE,
      shadow: SHADOW_LEVELS,
      esMarcaEasyBeach,
      cargando,
      usandoCacheOffline,
      cargarThemeDeBalneario,
      volverAMarcaEasyBeach,
    };
  }, [tokens, esMarcaEasyBeach, cargando, usandoCacheOffline, cargarThemeDeBalneario, volverAMarcaEasyBeach]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme() debe usarse dentro de <ThemeProvider>');
  }
  return ctx;
}
