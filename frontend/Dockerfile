FROM node:24-alpine3.23 AS build-stage
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM openresty/openresty:1.29.2.1-0-alpine AS production-stage
COPY nginx.conf /usr/local/openresty/nginx/conf/nginx.conf
COPY .htpasswd /usr/local/openresty/nginx/conf/.htpasswd
COPY certs/fullchain.pem /etc/letsencrypt/live/wenlib.com/fullchain.pem
COPY certs/privkey.pem /etc/letsencrypt/live/wenlib.com/privkey.pem
COPY --from=build-stage /app/dist /usr/share/nginx/html
EXPOSE 80 443
CMD ["openresty", "-g", "daemon off;"]