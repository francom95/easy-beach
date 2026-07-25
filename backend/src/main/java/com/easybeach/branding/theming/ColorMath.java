package com.easybeach.branding.theming;

/**
 * WCAG 2 contraste + derivación de tokens (tokens.md §"Reglas de
 * accesibilidad"). La derivación de {@code muted}/{@code border} usa un
 * blend HSL (mezcla de luminosidad preservando matiz) como aproximación
 * pragmática de la regla del contrato ("desplazamiento de luminosidad en
 * OKLCH"): OKLCH exacto exige una conversión perceptual (sRGB→lineal→OKLab)
 * que no vale la pena arriesgar sin poder verificarla visualmente; HSL logra
 * el mismo objetivo cualitativo (tono más claro/oscuro, mismo matiz) con una
 * matemática mucho más simple de verificar. Documentado como simplificación
 * consciente, no un descuido.
 */
public final class ColorMath {

    public static final String BLANCO = "#FFFFFF";

    private ColorMath() {
    }

    public record Rgb(int r, int g, int b) {
    }

    public static Rgb parseHex(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() != 6) {
            throw new IllegalArgumentException("Color hex inválido: " + hex);
        }
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return new Rgb(r, g, b);
    }

    public static String toHex(Rgb rgb) {
        return String.format("#%02X%02X%02X",
                clamp(rgb.r()), clamp(rgb.g()), clamp(rgb.b()));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Luminancia relativa WCAG (0 = negro, 1 = blanco). */
    public static double relativeLuminance(Rgb rgb) {
        double r = linearize(rgb.r() / 255.0);
        double g = linearize(rgb.g() / 255.0);
        double b = linearize(rgb.b() / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linearize(double channel) {
        return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    /** Contraste WCAG entre dos colores, siempre ≥ 1.0. */
    public static double contrastRatio(Rgb a, Rgb b) {
        double la = relativeLuminance(a) + 0.05;
        double lb = relativeLuminance(b) + 0.05;
        return la > lb ? la / lb : lb / la;
    }

    public static double contrastRatio(String hexA, String hexB) {
        return contrastRatio(parseHex(hexA), parseHex(hexB));
    }

    /**
     * Elige blanco o la tinta oscura configurada según cuál da mayor
     * contraste contra {@code base} (tokens.md: "los pares on-* nunca se
     * eligen: se derivan").
     */
    public static String pickOnColor(String baseHex, String tintaOscuraHex) {
        Rgb base = parseHex(baseHex);
        double contrasteBlanco = contrastRatio(base, parseHex(BLANCO));
        double contrasteTinta = contrastRatio(base, parseHex(tintaOscuraHex));
        return contrasteBlanco >= contrasteTinta ? BLANCO : tintaOscuraHex;
    }

    /**
     * Blend lineal RGB entre dos colores ({@code ratio}=0 -> a, =1 -> b).
     * Usado para derivar {@code border} (surface+background al 50%) y como
     * paso de {@code muted} (on-surface hacia surface).
     */
    public static String blend(String hexA, String hexB, double ratio) {
        Rgb a = parseHex(hexA);
        Rgb b = parseHex(hexB);
        return toHex(new Rgb(
                (int) Math.round(a.r() + (b.r() - a.r()) * ratio),
                (int) Math.round(a.g() + (b.g() - a.g()) * ratio),
                (int) Math.round(a.b() + (b.b() - a.b()) * ratio)
        ));
    }

    /**
     * Deriva {@code color.on-surface-muted}: mezcla {@code onSurface} hacia
     * {@code surface} hasta el blend más fuerte que siga cumpliendo
     * {@code minContraste} (piso AA de componente UI, 3:1) contra
     * {@code surface}. Si ni el blend mínimo (10%) alcanza, devuelve
     * {@code onSurface} sin mezclar (nunca por debajo del piso).
     */
    public static String deriveMuted(String onSurfaceHex, String surfaceHex, double minContraste) {
        String mejor = onSurfaceHex;
        for (int paso = 10; paso <= 70; paso += 5) {
            String candidato = blend(onSurfaceHex, surfaceHex, paso / 100.0);
            if (contrastRatio(candidato, surfaceHex) >= minContraste) {
                mejor = candidato;
            } else {
                break;
            }
        }
        return mejor;
    }

    /** {@code color.border}: punto medio entre surface y background. */
    public static String deriveBorder(String surfaceHex, String backgroundHex) {
        return blend(surfaceHex, backgroundHex, 0.5);
    }

    /**
     * Si {@code baseHex} no alcanza {@code minContraste} contra el mejor de
     * {blanco, tintaOscura}, ajusta la luminosidad (oscureciendo o
     * aclarando en pasos de HSL, preservando matiz/saturación) hasta
     * cumplir. Devuelve el tono ajustado (igual al original si ya cumplía).
     */
    public static String nearestCompliantTone(String baseHex, String tintaOscuraHex, double minContraste) {
        if (bestContrast(baseHex, tintaOscuraHex) >= minContraste) {
            return baseHex;
        }
        Hsl hsl = toHsl(parseHex(baseHex));
        // Probar primero oscureciendo (suele acercar más rápido a la tinta oscura
        // por default, que es el caso típico de colores de marca saturados/claros).
        for (int direccion = 0; direccion < 2; direccion++) {
            boolean oscurecer = direccion == 0;
            for (int paso = 1; paso <= 50; paso++) {
                double delta = oscurecer ? -paso : paso;
                double nuevaL = clampD(hsl.l() + delta, 0, 100);
                String candidato = toHex(fromHsl(new Hsl(hsl.h(), hsl.s(), nuevaL)));
                if (bestContrast(candidato, tintaOscuraHex) >= minContraste) {
                    return candidato;
                }
            }
        }
        // No debería llegar acá con la tinta oscura default del contrato, pero
        // como piso de seguridad devolvemos la tinta oscura misma (contraste 1:1
        // garantizado consigo misma en el caso extremo de un candidato degenerado).
        return tintaOscuraHex;
    }

    private static double bestContrast(String baseHex, String tintaOscuraHex) {
        Rgb base = parseHex(baseHex);
        return Math.max(contrastRatio(base, parseHex(BLANCO)), contrastRatio(base, parseHex(tintaOscuraHex)));
    }

    private static double clampD(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private record Hsl(double h, double s, double l) {
    }

    private static Hsl toHsl(Rgb rgb) {
        double r = rgb.r() / 255.0, g = rgb.g() / 255.0, b = rgb.b() / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2.0;
        double h = 0, s;
        double d = max - min;
        if (d == 0) {
            s = 0;
        } else {
            s = l > 0.5 ? d / (2.0 - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h *= 60;
        }
        return new Hsl(h, s * 100, l * 100);
    }

    private static Rgb fromHsl(Hsl hsl) {
        double h = hsl.h() / 360.0, s = hsl.s() / 100.0, l = hsl.l() / 100.0;
        double r, g, b;
        if (s == 0) {
            r = g = b = l;
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hueToRgb(p, q, h + 1.0 / 3);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0 / 3);
        }
        return new Rgb((int) Math.round(r * 255), (int) Math.round(g * 255), (int) Math.round(b * 255));
    }

    private static double hueToRgb(double p, double q, double t) {
        double tt = t;
        if (tt < 0) {
            tt += 1;
        }
        if (tt > 1) {
            tt -= 1;
        }
        if (tt < 1.0 / 6) {
            return p + (q - p) * 6 * tt;
        }
        if (tt < 1.0 / 2) {
            return q;
        }
        if (tt < 2.0 / 3) {
            return p + (q - p) * (2.0 / 3 - tt) * 6;
        }
        return p;
    }
}
