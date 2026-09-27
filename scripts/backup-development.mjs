/** Consistent development backup. Maintainer: Enzo Ribas (https://github.com/oEnzoRibas). */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
const root = fileURLToPath(new URL('../', import.meta.url));
const run = (args) => execFileSync('docker', args, { cwd: root, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
const compose = (...args) => run(['compose', ...args]);
const directory = path.join(root, '.local', 'recovery', new Date().toISOString().replaceAll(':', '-'));
fs.mkdirSync(directory, { recursive: true, mode: 0o700 });
const database = compose('ps', '-q', 'database'), backend = compose('ps', '-q', 'backend');
if (!database || !backend) throw new Error('Start the database and backend before taking a backup.');
const image = run(['inspect', '--format', '{{.Config.Image}}', database]);
let paused = false;
try {
    compose('stop', 'backend'); paused = true;
    run(['exec', database, 'sh', '-ec', 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --file=/tmp/irr-backup.dump']);
    run(['cp', database + ':/tmp/irr-backup.dump', path.join(directory, 'database.dump')]);
    run(['exec', database, 'rm', '/tmp/irr-backup.dump']);
    run(['run', '--rm', '--volumes-from', backend + ':ro', '--mount', 'type=bind,source=' + directory + ',target=/backup', image, 'sh', '-ec', 'tar -czf /backup/attachments.tar.gz -C /data/uploads .']);
    const history = run(['exec', database, 'sh', '-ec', "psql -U \"$POSTGRES_USER\" -d \"$POSTGRES_DB\" -Atc 'SELECT version || chr(58) || checksum FROM flyway_schema_history WHERE success ORDER BY installed_rank'".replaceAll('\\"', '"')]);
    fs.copyFileSync(path.join(root, '.env'), path.join(directory, '.env'));
    fs.cpSync(path.join(root, '.local', 'keys'), path.join(directory, 'keys'), { recursive: true });
    const hashes = {}; for (const name of ['database.dump', 'attachments.tar.gz', '.env', 'keys/private.pem', 'keys/public.pem']) hashes[name] = crypto.createHash('sha256').update(fs.readFileSync(path.join(directory, name))).digest('hex');
    fs.writeFileSync(path.join(directory, 'manifest.json'), JSON.stringify({ createdAt: new Date().toISOString(), backendCommit: execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim(), databaseImage: image, history, hashes }, null, 2));
    console.log('Backup created: ' + directory);
} finally { if (paused) compose('start', 'backend'); }
