FROM node:latest

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
RUN mkdir -p backend
RUN mkdir -p web

COPY ./backend ./backend
RUN mkdir -p ./backend/uploads
COPY ./web ./web
RUN mkdir -p ./web/dist
COPY package.json .
COPY webpack.config.js .
RUN npm install -g webpack
RUN npm config set registry https://registry.npmjs.org/
RUN npm install
RUN npm run build
CMD ["node", "backend/server.js"]
