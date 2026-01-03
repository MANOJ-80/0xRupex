FROM node:18-alpine

WORKDIR /app

COPY package*.json ./

RUN npm install --production

COPY . .

EXPOSE 3000

# Run migrations and then start the application
CMD sh -c "npm run migrate && npm start"
