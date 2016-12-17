var path = require('path');
var webpack = require('webpack');
var CopyWebpackPlugin = require('copy-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var HtmlWebpackPlugin = require('html-webpack-plugin');

var ENV = process.env.npm_lifecycle_event;
var isProd = ENV === 'build';

module.exports = function makeWebpackConfig() {
  var config = {};

  config.entry = {
    app: './web/src/app/app.js'
  };

  config.output = {
    path: __dirname + '/web/dist',
    filename: '[name].bundle.js'
  };

  if (isProd) {
    config.devtool = 'cheap-module-source-map';
  } else {
    config.devtool = 'source-map';
  }

  config.module = {
    preLoaders: [
        { test: /\.json$/, loader: 'json'},
    ],
    loaders: [
      { test: /src.*\.js$/, loaders: ['ng-annotate?add=true&map=false'] },
      { test: /\.css$/, loader: ExtractTextPlugin.extract('style', 'css?sourceMap') },
      { test: /\.html$/, loader: 'raw' },
      { test: /\.(png|jpg)$/, loader: 'url-loader'}
    ]
  };

  config.plugins = []
  config.plugins.push(
    new HtmlWebpackPlugin({
      template: './web/src/public/index.html'
    }),
    new ExtractTextPlugin('[name].css', { disable: !isProd })
  );

  if (isProd) {
    config.plugins.push(
      new webpack.optimize.UglifyJsPlugin(),
      new CopyWebpackPlugin([{
        from: __dirname + '/web/src/public'
      }])
    );
  }

  config.devServer = {
    outputPath: path.join(__dirname, 'app'),
    contentBase: './web/src/public',
    stats: 'minimal',
    // host: '10.0.1.2',
    port: 8081,
    proxy: {
      '/api/*': {
        target: 'https://localhost:8443/',
        secure: false
      }
    }
  };

  return config;
}();
