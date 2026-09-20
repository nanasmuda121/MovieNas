const proxyStreamHandler = require('../../api/proxy-stream');

export const config = {
  api: {
    responseLimit: false,
    bodyParser: false,
  },
};

export default function handler(req, res) {
  return proxyStreamHandler(req, res);
}
