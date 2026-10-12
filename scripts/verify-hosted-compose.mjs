/** Verifies deployment boundaries without starting services. @author Enzo Ribas (https://github.com/oEnzoRibas). */
import { execFileSync } from 'node:child_process';
import assert from 'node:assert/strict';

const env = {
  ...process.env,
  IRR_EDGE_IMAGE: 'caddy:2-alpine', IRR_BACKEND_IMAGE: 'irr-backend-storage:candidate',
  IRR_FRONTEND_IMAGE: 'irr-frontend-security:candidate', IRR_POSTGRES_IMAGE: 'postgres:16-alpine',
  IRR_REDIS_IMAGE: 'redis:8.2-alpine', FRONTEND_DOMAIN: 'irr.example.com',
  BACKEND_DOMAIN: 'api.irr.example.com', ACME_EMAIL: 'operator@example.com',
  IRR_KEYS_DIRECTORY: '/srv/irr/keys', DB_NAME: 'irr', DB_USER: 'irr_app',
  DB_PASSWORD: 'synthetic-config-verification-only', REDIS_PASSWORD: 'another-synthetic-config-only',
  BOOTSTRAP_ADMIN_ENABLED: 'false', BOOTSTRAP_ADMIN_NAME: '', BOOTSTRAP_ADMIN_EMAIL: '',
  BOOTSTRAP_ADMIN_PASSWORD: '', MAIL_PROVIDER: 'disabled', MAIL_API_KEY: '', MAIL_FROM: '',
};
const args = ['compose', '--env-file', 'deploy/hosted.env.example', '-f', 'deploy/compose.yaml', 'config', '--format', 'json'];
let config;
try { config = JSON.parse(execFileSync('docker', args, { env, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] })); }
catch { throw new Error('Compose verification failed; resolved configuration is deliberately not printed.'); }
const services = config.services;
assert.deepEqual(Object.keys(services).sort(), ['backend', 'database', 'edge', 'frontend', 'redis']);
for (const name of ['backend', 'frontend', 'database', 'redis']) assert.equal(services[name].ports?.length ?? 0, 0, name + ' exposes host ports');
assert.deepEqual(services.edge.ports.map(p => p.target).sort((a, b) => a - b), [80, 443]);
assert.equal(config.networks.data.internal, true);
for (const name of ['database', 'redis']) assert.deepEqual(Object.keys(services[name].networks), ['data']);
assert.equal(services.backend.environment.DEV_BOOTSTRAP_ENABLED, 'false');
assert.equal(services.backend.environment.SPRING_PROFILES_ACTIVE, undefined);
assert.equal(services.backend.environment.IRR_INIT_STORAGE_PERMISSIONS, undefined);
assert.equal(services.backend.environment.CORS_ALLOWED_ORIGINS, 'https://irr.example.com');
assert.equal(services.backend.environment.PASSWORD_RESET_BASE_URL, 'https://irr.example.com/reset-password');
for (const name of ['backend', 'frontend']) {
  assert.equal(services[name].read_only, true);
  assert.deepEqual(services[name].cap_drop, ['ALL']);
  assert.equal(services[name].build, undefined);
}
let refused = false;
try { execFileSync('docker', args, { env: { ...env, IRR_BACKEND_IMAGE: '' }, stdio: 'pipe' }); }
catch { refused = true; }
assert.equal(refused, true, 'A missing backend image must fail before deployment.');
console.log('PASS: private data network, ingress-only public ports, no builds/development bootstrap, protected application containers, consistent origins and missing-image refusal. Synthetic configuration only; no services started.');
