/**
 * Rehearses candidate PostgreSQL/Redis startup and restart in disposable volumes.
 * No host ports, development volumes, or retained credentials are used.
 * @author Enzo Ribas (https://github.com/oEnzoRibas)
 */
import {execFileSync} from 'node:child_process';
import {randomBytes} from 'node:crypto';
import {mkdirSync, writeFileSync, rmSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const [postgresImage, redisImage] = process.argv.slice(2);
if (!postgresImage || !redisImage) {
  throw new Error('Usage: node scripts/verify-stateful-images.mjs <postgres-image> <redis-image>');
}
const root = fileURLToPath(new URL('../', import.meta.url));
const prefix = `irr-stateful-review-${randomBytes(6).toString('hex')}`;
const privateRoot = path.resolve(root, '.local', 'security');
const privateDirectory = path.resolve(privateRoot, prefix);
if (path.dirname(privateDirectory) !== privateRoot || !path.basename(privateDirectory).startsWith('irr-stateful-review-')) {
  throw new Error('Invalid temporary credential directory.');
}
const containers = [];
const volumes = [];
const password = randomBytes(32).toString('hex');
let stage = 'PostgreSQL startup';
const docker = (args, options = {}) => execFileSync('docker', args, {
  encoding: 'utf8', timeout: 30000, stdio: ['pipe', 'pipe', 'pipe'],
  ...options,
}).trim();
const pause = (ms) => new Promise(resolve => setTimeout(resolve, ms));
async function ready(probe) {
  for (let attempt = 0; attempt < 30; attempt++) {
    try { if (probe()) return; } catch { /* Startup is bounded; secrets never enter diagnostics. */ }
    await pause(1000);
  }
  throw new Error('Candidate did not become ready within the startup window.');
}
function volume(name) {
  docker(['volume', 'create', '--label', `irr.rehearsal=${prefix}`, name]);
  volumes.push(name);
}
function start(name, image, dataVolume, destination, extraArgs) {
  docker(['run', '--detach', '--name', name, '--label', `irr.rehearsal=${prefix}`,
    '--network', 'none', '--memory', '250m',
    '--mount', `type=volume,source=${dataVolume},target=${destination}`,
    ...extraArgs, image,
  ], {env: {...process.env, POSTGRES_PASSWORD: password}});
  containers.push(name);
}
try {
  mkdirSync(privateDirectory, {recursive: true, mode: 0o700});
  const pg = `${prefix}-postgres`, pgVolume = `${pg}-data`;
  // Keep the temporary Unix-socket init server alive long enough to exercise startup ordering.
  const initProbe = path.join(privateDirectory, 'startup-delay.sql');
  writeFileSync(initProbe, 'SELECT pg_sleep(3);\n', {mode: 0o644});
  volume(pgVolume);
  start(pg, postgresImage, pgVolume, '/var/lib/postgresql/data', [
    '--env', 'POSTGRES_USER=irr_review', '--env', 'POSTGRES_DB=irr_review', '--env', 'POSTGRES_PASSWORD',
    '--env', 'POSTGRES_INITDB_ARGS=--auth-host=scram-sha-256',
    '--mount', `type=bind,source=${initProbe},target=/docker-entrypoint-initdb.d/startup-delay.sql,readonly`,
  ]);
  const sql = (statement) => docker(['exec', '--env', 'PGPASSWORD', pg, 'psql', '-h', '127.0.0.1',
    '-U', 'irr_review', '-d', 'irr_review', '-v', 'ON_ERROR_STOP=1', '-At', '-c', statement],
    {env: {...process.env, PGPASSWORD: password}});
  // The upstream init server accepts socket probes but deliberately does not listen on TCP.
  await ready(() => sql('SELECT 1;') === '1');
  stage = 'PostgreSQL TCP authentication';
  if (!sql('SHOW server_version;').startsWith('16.')) throw new Error('PostgreSQL 16 is required.');
  let incorrectPasswordRejected = false;
  try {
    docker(['exec', '--env', 'PGPASSWORD', pg, 'psql', '-h', '127.0.0.1', '-U', 'irr_review',
      '-d', 'irr_review', '-At', '-c', 'SELECT 1;'], {env: {...process.env, PGPASSWORD: 'incorrect-review-password'}});
  } catch { incorrectPasswordRejected = true; }
  if (!incorrectPasswordRejected) throw new Error('PostgreSQL must reject incorrect passwords.');
  sql('CREATE TABLE persistence_probe(id integer PRIMARY KEY); INSERT INTO persistence_probe VALUES (1);');
  stage = 'PostgreSQL restart persistence';
  docker(['restart', pg]);
  await ready(() => sql('SELECT count(*) FROM persistence_probe;') === '1');
  console.log('PASS: PostgreSQL 16 authenticated initialization and retained row after restart.');

  const redis = `${prefix}-redis`, redisVolume = `${redis}-data`;
  stage = 'Redis startup';
  volume(redisVolume);
  const config = path.join(privateDirectory, 'redis.conf');
  writeFileSync(config, `bind 127.0.0.1\nappendonly yes\nappendfsync always\ndir /data\nrequirepass ${password}\n`, {mode: 0o644});
  docker(['run', '--detach', '--name', redis, '--label', `irr.rehearsal=${prefix}`,
    '--network', 'none', '--memory', '125m',
    '--mount', `type=volume,source=${redisVolume},target=/data`,
    '--mount', `type=bind,source=${config},target=/tmp/redis-review.conf,readonly`,
    redisImage, 'redis-server', '/tmp/redis-review.conf']);
  containers.push(redis);
  const redisCli = (...args) => docker(['exec', '--env', 'REDISCLI_AUTH', redis, 'redis-cli', '--raw', ...args],
    {env: {...process.env, REDISCLI_AUTH: password}});
  await ready(() => redisCli('PING') === 'PONG');
  stage = 'Redis authentication';
  if (!docker(['exec', redis, 'redis-cli', '--raw', 'PING']).includes('NOAUTH')) {
    throw new Error('Redis must reject unauthenticated requests.');
  }
  if (redisCli('SET', 'persistence-probe', 'retained') !== 'OK') throw new Error('Redis write failed.');
  docker(['restart', redis]);
  stage = 'Redis restart persistence';
  await ready(() => redisCli('GET', 'persistence-probe') === 'retained');
  const redisProcess = docker(['exec', redis, 'cat', '/proc/1/status']);
  const redisUid = redisProcess.match(/^Uid:\s+\d+\s+(\d+)/m);
  if (!redisUid || Number(redisUid[1]) === 0) throw new Error('Redis must drop root privileges.');
  console.log('PASS: Redis authentication, AOF retention after restart and non-root entrypoint.');
} catch (error) {
  // Docker exceptions can contain command arguments/output; do not publish them.
  console.error(`FAIL: ${stage}; no raw credential-bearing diagnostics emitted.`);
  writeFileSync(path.join(privateRoot, `${prefix}-failure-private.txt`), String(error.stderr ?? error.message), {mode: 0o600});
  process.exitCode = 1;
} finally {
  for (const name of containers.reverse()) {
    try { docker(['rm', '--force', name]); } catch { console.error(`Cleanup pending: ${name}`); process.exitCode = 1; }
  }
  for (const name of volumes.reverse()) {
    try { docker(['volume', 'rm', name]); } catch { console.error(`Cleanup pending: ${name}`); process.exitCode = 1; }
  }
  rmSync(privateDirectory, {recursive: true, force: true});
}
