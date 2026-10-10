/** Consistent selected-stack backup. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import { parseArgs } from 'node:util';
import { docker, sha256, HISTORY_SQL, UUID_FILE, tableCounts } from './recovery-common.mjs';

const root = fileURLToPath(new URL('../', import.meta.url));
const { values } = parseArgs({ options: {
    'compose-file': { type: 'string', multiple: true }, project: { type: 'string' }, 'env-file': { type: 'string' },
} });
const selected = ['compose'];
for (const file of values['compose-file'] ?? []) selected.push('-f', path.resolve(root, file));
if (values.project) selected.push('-p', values.project);
if (values['env-file']) selected.push('--env-file', path.resolve(root, values['env-file']));
const run = args => docker(args, root);
const compose = (...args) => run([...selected, ...args]);
const config = JSON.parse(compose('config', '--format', 'json'));
const database = compose('ps', '-q', 'database'), backend = compose('ps', '-q', 'backend');
if (!/^[a-f0-9]{64}$/.test(database) || !/^[a-f0-9]{64}$/.test(backend)) throw new Error('Select exactly one running database and backend.');
const inspect = id => JSON.parse(run(['inspect', id]))[0];
const db = inspect(database), api = inspect(backend);
for (const [service, container] of [['database', db], ['backend', api]]) {
    if (container.State.Health?.Status !== 'healthy') throw new Error('Selected ' + service + ' must be healthy before backup.');
    const labels = container.Config.Labels;
    if (labels['com.docker.compose.project'] !== config.name || labels['com.docker.compose.service'] !== service) throw new Error('Compose selection differs from the running stack.');
    const environment = Object.fromEntries(container.Config.Env.map(entry => [entry.slice(0, entry.indexOf('=')), entry.slice(entry.indexOf('=') + 1)]));
    for (const [key, value] of Object.entries(config.services[service].environment ?? {})) {
        if (environment[key] !== String(value ?? '')) throw new Error('Running ' + service + ' environment differs from selected Compose configuration.');
    }
}
const storage = config.services.backend.environment.STORAGE_DIRECTORY;
if (storage !== '/data/uploads' || !api.Mounts.some(mount => mount.Destination === storage)) throw new Error('A persistent /data/uploads mount is required.');
const keys = Object.entries({ public: config.services.backend.environment.RSA_PUBLIC_KEY, private: config.services.backend.environment.RSA_PRIVATE_KEY });
for (const [, key] of keys) {
    if (typeof key !== 'string' || !(key.startsWith('-----BEGIN ') || key.startsWith('file:/'))) throw new Error('RSA keys must be explicit PEM values or absolute container file locations.');
}
const directory = path.join(root, '.local', 'recovery', new Date().toISOString().replaceAll(':', '-') + '-' + crypto.randomUUID().slice(0, 8));
fs.mkdirSync(path.join(directory, 'keys'), { recursive: true, mode: 0o700 });
fs.writeFileSync(path.join(directory, 'compose.resolved.json'), JSON.stringify(config, null, 2), { mode: 0o600 });
for (const [name, key] of keys) {
    const destination = path.join(directory, 'keys', name + '.pem');
    if (key.startsWith('file:/')) run(['cp', backend + ':' + key.slice(5), destination]);
    else fs.writeFileSync(destination, key, { mode: 0o600 });
}
const environmentFile = values['env-file'] ? path.resolve(root, values['env-file']) : undefined;
if (environmentFile) fs.copyFileSync(environmentFile, path.join(directory, 'environment.env'));
const sql = query => run(['exec', database, 'sh', '-ec', 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "$1"', 'recovery', query]);
const temporaryDump = '/tmp/irr-backup-' + crypto.randomUUID() + '.dump';
let stopped = false;
console.log('Backing up Compose project ' + config.name + '; API writes will pause briefly.');
try {
    stopped = true;
    compose('stop', 'backend');
    if (inspect(backend).State.Running) throw new Error('Backend did not stop; refusing inconsistent snapshot.');
    run(['exec', database, 'sh', '-ec', 'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --file="$1"', 'recovery', temporaryDump]);
    run(['cp', database + ':' + temporaryDump, path.join(directory, 'database.dump')]);
    const fileLines = run(['run', '--rm', '--pull=never', '--network', 'none', '--volumes-from', backend + ':ro', db.Image, 'sh', '-ec', 'cd /data/uploads; test -z "$(find . -mindepth 1 ! -type f -print)"; find . -type f -exec sha256sum {} +']);
    const attachmentHashes = {};
    for (const line of fileLines.split('\n').filter(Boolean)) {
        const match = /^([a-f0-9]{64})  \.\/(.+)$/.exec(line);
        if (!match || !UUID_FILE.test(match[2])) throw new Error('Upload storage contains a noncanonical file requiring review.');
        attachmentHashes[match[2]] = match[1];
    }
    run(['run', '--rm', '--pull=never', '--network', 'none', '--volumes-from', backend + ':ro', '--mount', 'type=bind,source=' + directory + ',target=/backup', db.Image, 'sh', '-ec', 'tar -czf /backup/attachments.tar.gz -C /data/uploads .']);
    const history = sql(HISTORY_SQL);
    const counts = tableCounts(sql);
    const hashes = {};
    for (const name of ['database.dump', 'attachments.tar.gz', 'compose.resolved.json', 'keys/private.pem', 'keys/public.pem', ...(environmentFile ? ['environment.env'] : [])]) hashes[name] = sha256(path.join(directory, name));
    fs.writeFileSync(path.join(directory, 'manifest.json'), JSON.stringify({
        formatVersion: 2, createdAt: new Date().toISOString(), composeProject: config.name,
        sourceCommit: execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim(),
        backendImageId: api.Image, databaseImage: db.Config.Image, databaseImageId: db.Image,
        history, tableCounts: counts, attachmentHashes, hashes,
    }, null, 2), { mode: 0o600 });
    console.log('Backup created: ' + directory);
} finally {
    try { run(['exec', database, 'rm', '-f', temporaryDump]); }
    finally {
        if (stopped) {
            compose('start', '--wait', '--wait-timeout', '120', 'backend');
            if (inspect(backend).State.Health?.Status !== 'healthy') throw new Error('Backup ended but backend health was not restored.');
            console.log('Selected backend restarted and healthy.');
        }
    }
}
