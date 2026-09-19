const express = require('express');
const cors = require('cors');
const path = require('path');

const trendingHandler = require('./api/trending');
const searchHandler = require('./api/search');
const detailHandler = require('./api/detail');
const streamHandler = require('./api/stream');
const captionsHandler = require('./api/captions');
const proxyStreamHandler = require('./api/proxy-stream');
const subtitleHandler = require('./api/subtitle');

const app = express();
const PORT = process.env.PORT || 4000;

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// API Endpoints
app.all('/api/trending', trendingHandler);
app.all('/api/search', searchHandler);
app.all('/api/detail/:detailPath', (req, res) => {
  req.query.detailPath = req.params.detailPath;
  return detailHandler(req, res);
});
app.all('/api/detail', detailHandler);
app.all('/api/stream', streamHandler);
app.all('/api/captions', captionsHandler);
app.all('/api/proxy-stream', proxyStreamHandler);
app.all('/api/subtitle', subtitleHandler);

// Clean frontend routes
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.get('/search', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'search.html'));
});

app.get(['/detail', '/detail/:path'], (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'detail.html'));
});

app.get('/player', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'player.html'));
});

// Fallback for 404
app.use((req, res) => {
  if (req.accepts('html')) {
    return res.status(404).sendFile(path.join(__dirname, 'public', 'index.html'));
  }
  res.status(404).json({ status: false, error: 'Not Found' });
});

if (process.env.NODE_ENV !== 'test') {
  app.listen(PORT, () => {
    console.log(`====================================================`);
    console.log(`🎬 MovieNas — Streaming Movie & Series Platform`);
    console.log(`📡 Local Server: http://localhost:${PORT}`);
    console.log(`⚡ API Base:     http://localhost:${PORT}/api`);
    console.log(`====================================================`);
  });
}

module.exports = app;
