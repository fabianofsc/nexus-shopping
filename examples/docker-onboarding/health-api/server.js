const http = require('node:http');

function sendJson(response, statusCode, payload) {
  response.writeHead(statusCode, { 'content-type': 'application/json; charset=utf-8' });
  response.end(JSON.stringify(payload));
}

function createServer() {
  return http.createServer((request, response) => {
    if (request.method === 'GET' && request.url === '/health') {
      sendJson(response, 200, { status: 'ok' });
      return;
    }

    sendJson(response, 404, { error: 'Not Found' });
  });
}

function startServer(port = Number(process.env.PORT || 3000)) {
  const server = createServer();
  server.listen(port, '0.0.0.0');
  return server;
}

if (require.main === module) {
  startServer();
}

module.exports = { createServer, startServer };
