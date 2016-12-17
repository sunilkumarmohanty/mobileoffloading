FROM node:latest

RUN mkdir -p /usr/src/app/backend
WORKDIR /usr/src/app
COPY ./backend ./backend
COPY package.json .
RUN npm install

CMD ["node", "backend/server.js"]
