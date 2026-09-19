const express = require('express');
const cors = require('cors');
const path = require('path');

const movie = require('./lib/movie');
const { trendingCache, detailCache, homeCache } = require('./lib/cache');

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
app.use(express.static(path.join(__dirname, 'public'), { index: false }));

// Configure EJS Template Engine for Server-Side Rendering (SSR)
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// ==========================================
// 1. SSR FRONTEND ROUTES (Full HTML Source)
// ==========================================

// Homepage SSR with rich multi-category recommendations from themoviebox.xyz/id
app.get('/', async (req, res) => {
  try {
    const cacheKey = 'home_sections_id';
    let homeData = homeCache.get(cacheKey);

    if (!homeData) {
      try {
        homeData = await movie.home('id');
        if (homeData && homeData.categories && homeData.categories.length > 0) {
          homeCache.set(cacheKey, homeData);
        }
      } catch (homeErr) {
        console.warn('[SSR Homepage] movie.home failed, falling back to trending:', homeErr.message);
        const trending = await movie.trending(1, 30, 'id');
        homeData = {
          heroItem: trending[0] || null,
          categories: [
            {
              title: 'Trending Sekarang',
              items: trending,
            },
          ],
        };
      }
    }

    let heroItem = homeData.heroItem;
    if (!heroItem && homeData.categories && homeData.categories.length > 0) {
      heroItem = homeData.categories[0].items[0] || null;
    }

    res.render('index', {
      heroItem,
      categories: homeData.categories || [],
      trending: (homeData.categories && homeData.categories[0]?.items) || [],
    });
  } catch (err) {
    console.error('[SSR Homepage Error]:', err.message);
    res.render('index', { heroItem: null, categories: [], trending: [] });
  }
});

// Search Page SSR
app.get('/search', async (req, res) => {
  try {
    const query = (req.query.q || '').trim();
    const type = parseInt(req.query.type, 10) || 0; // 0=All, 1=Movie, 2=Series

    let results = [];
    if (query) {
      results = await movie.search(query, 1, 30, type, 'id');
    } else {
      // Default trending recommendation
      const trending = await movie.trending(1, 24, 'id');
      results = trending;
      if (type === 1) results = results.filter((i) => i.typeLabel === 'Movie' || i.subjectType === 1);
      if (type === 2) results = results.filter((i) => i.typeLabel === 'Series' || i.subjectType === 2);
    }

    res.render('search', {
      query,
      currentType: type,
      results,
    });
  } catch (err) {
    console.error('[SSR Search Error]:', err.message);
    res.render('search', { query: req.query.q || '', currentType: 0, results: [] });
  }
});

// Detail Page SSR
app.get(['/detail/:path', '/detail'], async (req, res) => {
  try {
    const detailPath = req.params.path || req.query.path || req.query.slug || req.query.detailPath;
    if (!detailPath) return res.redirect('/');

    const cacheKey = `detail_${detailPath}_id`;
    let detailData = detailCache.get(cacheKey);

    if (!detailData) {
      detailData = await movie.detail(detailPath, 'id');
      detailCache.set(cacheKey, detailData);
    }

    res.render('detail', {
      detail: detailData,
    });
  } catch (err) {
    console.error('[SSR Detail Error]:', err.message);
    res.status(404).send(`Detail tidak ditemukan: ${err.message}`);
  }
});

// Player Page SSR (/play/:slug for movies, /play/:slug/:ep/:se for series)
async function renderPlayerPage(req, res) {
  try {
    const slug = req.params.slug || req.query.path || req.query.slug;
    if (!slug) return res.redirect('/');

    const subjectId = req.query.id || req.query.subjectId || '';
    const episode = parseInt(req.params.ep || req.query.ep || req.query.episode || '0', 10);
    const season = parseInt(req.params.se || req.query.se || req.query.season || '0', 10);

    const [streamResult, detailResult] = await Promise.allSettled([
      movie.stream(slug, subjectId, season, episode, 'id'),
      movie.detail(slug, 'id'),
    ]);

    const stream = streamResult.status === 'fulfilled' ? streamResult.value : null;
    const detail = detailResult.status === 'fulfilled' ? detailResult.value : null;

    if (!stream || !stream.streams || stream.streams.length === 0) {
      return res.status(404).send('Stream video tidak ditemukan untuk konten ini.');
    }

    res.render('player', {
      stream,
      detail,
    });
  } catch (err) {
    console.error('[SSR Player Error]:', err.message);
    res.status(500).send(`Gagal memuat player: ${err.message}`);
  }
}

// Support /play/slug/ep/se (series) and /play/slug (movie)
app.get(['/play/:slug/:ep/:se', '/play/:slug/:ep', '/play/:slug'], renderPlayerPage);

// Backward-compatible redirect from old /player query style to clean /play URLs
app.get('/player', (req, res) => {
  const slug = req.query.path || req.query.slug;
  if (!slug) return res.redirect('/');
  const se = req.query.se || req.query.season;
  const ep = req.query.ep || req.query.episode;
  if (ep && se) {
    return res.redirect(301, `/play/${encodeURIComponent(slug)}/${encodeURIComponent(ep)}/${encodeURIComponent(se)}`);
  }
  return res.redirect(301, `/play/${encodeURIComponent(slug)}`);
});

// ==========================================
// 2. REST API ENDPOINTS (For Client-side & Proxy)
// ==========================================
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

// Fallback for 404
app.use((req, res) => {
  res.status(404).redirect('/');
});

if (process.env.NODE_ENV !== 'test') {
  app.listen(PORT, () => {
    console.log(`====================================================`);
    console.log(`🎬 MovieNas — Streaming Movie & Series Platform (SSR)`);
    console.log(`📡 Local Server: http://localhost:${PORT}`);
    console.log(`⚡ View Source:  100% Real Semantic HTML (No Empty Chunks)`);
    console.log(`====================================================`);
  });
}

module.exports = app;
