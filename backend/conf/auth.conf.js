var jwt = require('express-jwt');
var auth = jwt({
  secret: 'dogsarebetterthancats',
  userProperty: 'tokenPayload'
});

module.exports = auth;