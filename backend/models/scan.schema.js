var mongoose = require('mongoose');

var scanSchema = new mongoose.Schema({
  text: String,
  originals: Array,
  thumbnails: Array,
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }
}, {
  timestamps: true
});

module.exports = scanSchema;