const proxyStreamHandler = require('../../lib/handlers/proxy-stream');

export const config = {
  api: {
    responseLimit: false,
    bodyParser: false,
    externalResolver: true,
  },
};

export default function handler(req, res) {
  return proxyStreamHandler(req, res);
}
