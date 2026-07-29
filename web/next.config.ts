import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Etapa 20: imagen Docker minima para el deploy - copia solo lo necesario
  // a .next/standalone en vez de requerir todo node_modules en runtime.
  output: "standalone",
};

export default nextConfig;
