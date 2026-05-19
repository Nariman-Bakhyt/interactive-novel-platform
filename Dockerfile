# Этап 1: Сборка статики Vue
FROM node:24-alpine3.23 AS build-stage
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Этап 2: Раздача через Nginx
FROM nginx:1.30.1-alpine3.23 AS production-stage
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build-stage /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
