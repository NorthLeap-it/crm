# syntax=docker/dockerfile:1

# Build del bundle browser (l'app e' una SPA Angular, niente SSR) e poi serve gli statici con
# nginx. .npmrc nel context porta legacy-peer-deps=true (lucide-angular non ha ancora il peerDep
# per Angular 22), cosi' npm ci non fallisce con ERESOLVE.
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json .npmrc ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine AS runtime
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
