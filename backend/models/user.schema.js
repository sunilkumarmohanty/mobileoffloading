var mongoose = require('mongoose');
var crypto = require('crypto');
var jwt = require('jsonwebtoken');

var userSchema = new mongoose.Schema({
  username: {
    type: String,
    required: true,
    unique: true
  },
  hash: String,
  salt: String,
  facebook: {
    type: Boolean,
    default: false
  },
  scans: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Scan'
  }]
});

userSchema.methods.setPassword = function(password) {
  this.salt = crypto.randomBytes(16).toString('hex');
  this.hash = crypto.pbkdf2Sync(password, this.salt, 1000, 64).toString('hex');
};

userSchema.methods.checkPassword = function(password) {
  return this.hash === crypto.pbkdf2Sync(password, this.salt, 1000, 64).toString('hex');
};

userSchema.methods.genToken = function() {
  var exp = new Date();
  exp.setDate(exp.getDate() + 7);
  return jwt.sign({
    _id: this._id,
    username: this.username,
    exp: parseInt(exp.getTime() / 1000),
  }, "dogsarebetterthancats");
};

module.exports = userSchema;