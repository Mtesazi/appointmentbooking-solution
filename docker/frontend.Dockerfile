FROM node:24.15.0-alpine AS build
WORKDIR /app
COPY Front-End/angular/package.json Front-End/angular/package-lock.json ./
RUN npm ci
COPY Front-End/angular/ ./
RUN npm run build

FROM nginx:alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/appointmentbooking/browser/ /usr/share/nginx/html/
EXPOSE 80
