const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const onboardingRoot = path.resolve(__dirname, '..', '..');

function readArtifact(...segments) {
  return fs.readFileSync(path.join(onboardingRoot, ...segments), 'utf8');
}

test('landing page Dockerfile mantém o contrato de publicação estática', () => {
  const dockerfile = readArtifact('landing-page', 'Dockerfile');

  assert.match(dockerfile, /^FROM nginx:alpine$/m);
  assert.match(dockerfile, /^COPY index\.html \/usr\/share\/nginx\/html\/index\.html$/m);
  assert.match(dockerfile, /^EXPOSE 80$/m);
});

test('landing page .dockerignore ignora arquivos do macOS', () => {
  const dockerignore = readArtifact('landing-page', '.dockerignore');

  assert.match(dockerignore, /^\.DS_Store$/m);
});

test('health API Dockerfile mantém a ordem de build e o contrato de execução', () => {
  const dockerfile = readArtifact('health-api', 'Dockerfile');
  const copyDependencies = dockerfile.search(/^COPY package\.json package-lock\.json \.\/$/m);
  const installDependencies = dockerfile.search(/^RUN npm ci$/m);

  assert.match(dockerfile, /^FROM node:22-alpine$/m);
  assert.match(dockerfile, /^WORKDIR \/app$/m);
  assert.ok(copyDependencies >= 0, 'deve copiar package.json e package-lock.json');
  assert.ok(installDependencies > copyDependencies, 'deve executar npm ci após copiar os manifests');
  assert.match(dockerfile, /^COPY server\.js \.\/$/m);
  assert.match(dockerfile, /^EXPOSE 3000$/m);
  assert.match(dockerfile, /^CMD \["npm", "start"\]$/m);
});

test('health API .dockerignore ignora node_modules', () => {
  const dockerignore = readArtifact('health-api', '.dockerignore');

  assert.match(dockerignore, /^node_modules\/$/m);
});
