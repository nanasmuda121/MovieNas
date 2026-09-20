const https = require('https');
const http = require('http');

module.exports = (req, res) => {
  if (req.method === 'OPTIONS') {
    return res.status(403).end();
  }

  if (req.method !== 'GET' && req.method !== 'HEAD') {
    return res.status(405).send('Method Not Allowed');
  }

  const targetUrl = req.query.url;
  if (!targetUrl) {
    return res.status(400).send('Parameter "url" dibutuhkan');
  }

  const filename = req.query.filename || 'video.mp4';
  const isDownload = req.query.download === '1';

  try {
    const parsed = new URL(targetUrl);
    const isHttps = parsed.protocol === 'https:';
    const client = isHttps ? https : http;

    const upstreamHeaders = {
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
      Referer: 'https://themoviebox.xyz/',
      Origin: 'https://themoviebox.xyz',
      Accept: '*/*',
    };

    if (req.headers.range) {
      upstreamHeaders['Range'] = req.headers.range;
    }

    const upstreamReq = client.request(
      {
        protocol: parsed.protocol,
        hostname: parsed.hostname,
        port: parsed.port || (isHttps ? 443 : 80),
        path: parsed.pathname + parsed.search,
        method: req.method,
        headers: upstreamHeaders,
        servername: parsed.hostname,
        timeout: 30000,
      },
      (upstreamRes) => {
        const statusCode = upstreamRes.statusCode || 200;
        res.status(statusCode);

        res.setHeader('Accept-Ranges', 'bytes');
        const contentType = upstreamRes.headers['content-type'] || 'video/mp4';
        res.setHeader('Content-Type', contentType);

        if (upstreamRes.headers['content-length']) {
          res.setHeader('Content-Length', upstreamRes.headers['content-length']);
        }
        if (upstreamRes.headers['content-range']) {
          res.setHeader('Content-Range', upstreamRes.headers['content-range']);
        }

        const disposition = isDownload ? 'attachment' : 'inline';
        res.setHeader(
          'Content-Disposition',
          `${disposition}; filename="${encodeURIComponent(filename)}"`
        );

        if (req.method === 'HEAD') {
          return res.end();
        }

        upstreamRes.pipe(res);

        upstreamRes.on('error', (err) => {
          console.error('[Proxy Stream Upstream Stream Error]:', err.message);
          if (!res.headersSent) res.status(502).end();
        });
      }
    );

    // Abort upstream if client disconnects (seeking, closing tab)
    req.on('close', () => {
      upstreamReq.destroy();
    });

    upstreamReq.on('timeout', () => {
      upstreamReq.destroy();
      if (!res.headersSent) res.status(504).send('Gateway Timeout');
    });

    upstreamReq.on('error', (err) => {
      console.error('[Proxy Stream Request Error]:', err.message);
      if (!res.headersSent) res.status(502).send(`Proxy Error: ${err.message}`);
    });

    upstreamReq.end();
  } catch (err) {
    console.error('[Proxy Stream URL Error]:', err.message);
    if (!res.headersSent) res.status(400).send(`Invalid URL: ${err.message}`);
  }
};
