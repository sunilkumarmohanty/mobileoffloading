var express = require('express');
var fs = require('fs');
var bodyParser = require('body-parser');
var busboyBodyParser = require('busboy-body-parser');
var mongoose = require('mongoose');
var passport = require('passport');
var path = require('path');
var http = require('http')
var https = require('https')
var forceSSL = require('express-force-ssl');

var sslOpts = {
  key: fs.readFileSync('./backend/certs/server.key'),
  cert: fs.readFileSync('./backend/certs/server.crt')
};

require('./models/db');
require('./conf/passport.conf');

var app = express();
var httpServer = http.createServer(app);
var httpsServer = https.createServer(sslOpts, app);
app.use(forceSSL);

app.use(bodyParser.json());
app.use(passport.initialize());

require('./routes/auth.routes')(app);
require('./routes/ocr.routes')(app);

app.use('/', express.static(path.join(__dirname, '../web/dist')))

// Catch unauthorised
app.use(function (err, req, res, next) {
  if (err.name === 'UnauthorizedError') {
    res.status(401).json({"message" : err.name + ": " + err.message});
  }
});

// start cleanup job
require('./conf/cron.conf');

httpsServer.listen(8443);
httpServer.listen(8080);
console.log('Listening on 8080 and 8443.');
