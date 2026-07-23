const assert = require('node:assert/strict');
const http = require('node:http');
const test = require('node:test');

let createServer;
let startServer;

try {
  ({ createServer, startServer } = require('../server'));
} catch {
  // A primeira execucao deve falhar de forma legivel antes de server.js existir.
}

function request(server, path) {
  return new Promise((resolve, reject) => {
    const address = server.address();
    const client = http.get({ host: '127.0.0.1', port: address.port, path }, (response) => {
      let body = '';
      response.setEncoding('utf8');
      response.on('data', (chunk) => {
        body += chunk;
      });
      response.on('end', () => {
        resolve({ statusCode: response.statusCode, headers: response.headers, body });
      });
    });
    client.on('error', reject);
  });
}

async function withServer(callback) {
  assert.equal(typeof createServer, 'function', 'server.js deve exportar createServer');
  const server = createServer();
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));

  try {
    await callback(server);
  } finally {
    await new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
  }
}

test('GET /health responde 200 com JSON de status', async () => {
  await withServer(async (server) => {
    const response = await request(server, '/health');

    assert.equal(response.statusCode, 200);
    assert.equal(response.headers['content-type'], 'application/json; charset=utf-8');
    assert.deepEqual(JSON.parse(response.body), { status: 'ok' });
  });
});

test('rotas desconhecidas respondem 404 com JSON', async () => {
  await withServer(async (server) => {
    const response = await request(server, '/nao-existe');

    assert.equal(response.statusCode, 404);
    assert.equal(response.headers['content-type'], 'application/json; charset=utf-8');
    assert.deepEqual(JSON.parse(response.body), { error: 'Not Found' });
  });
});

test('startServer usa PORT e faz bind em 0.0.0.0', async () => {
  assert.equal(typeof startServer, 'function', 'server.js deve exportar startServer');
  const originalPort = process.env.PORT;
  process.env.PORT = '0';

  try {
    const server = startServer();
    await new Promise((resolve) => server.once('listening', resolve));

    assert.equal(server.address().address, '0.0.0.0');
    await new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
  } finally {
    if (originalPort === undefined) {
      delete process.env.PORT;
    } else {
      process.env.PORT = originalPort;
    }
  }
});
