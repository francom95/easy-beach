# Etapa 20 - imagen de produccion del panel web (Next.js 16, output standalone).
# NEXT_PUBLIC_API_BASE_URL queda HORNEADO en el bundle del cliente en build
# time (asi funciona cualquier env var NEXT_PUBLIC_* en Next.js) - por eso es
# un build ARG, no una env var de runtime del contenedor final.

FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

COPY . .

ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
RUN npm run build

# ---------------------------------------------------------------- runtime
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production

RUN addgroup --system easybeach && adduser --system --ingroup easybeach easybeach

COPY --from=build /app/public ./public
COPY --from=build --chown=easybeach:easybeach /app/.next/standalone ./
COPY --from=build --chown=easybeach:easybeach /app/.next/static ./.next/static

USER easybeach
EXPOSE 3000
ENV PORT=3000 HOSTNAME=0.0.0.0

# 127.0.0.1 explicito, no "localhost": en esta imagen /etc/hosts resuelve
# localhost a ::1 antes que a 127.0.0.1, y el server standalone de Next.js
# solo escucha en IPv4 (0.0.0.0) - con "localhost" el healthcheck fallaba
# siempre (connection refused) aunque el server respondiera bien.
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget -qO- http://127.0.0.1:3000/ >/dev/null 2>&1 || exit 1

CMD ["node", "server.js"]
