/** Container storage regression checks. @author Enzo Ribas (https://github.com/oEnzoRibas). */
import { spawnSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import assert from 'node:assert/strict';

const image = process.argv[2];
if (!image || image.startsWith('-')) throw new Error('Usage: node scripts/verify-container-storage.mjs IMAGE');
const volume = `irr-storage-verification-${randomUUID()}`;
const label = 'irr.purpose=storage-bootstrap-verification';
function docker(args, expected = 0) {
  const result = spawnSync('docker', args, { encoding: 'utf8', timeout: 60000 });
  if (result.error) throw result.error;
  assert.equal(result.status, expected, result.stderr);
  return result.stdout.trim();
}
const run = (args, expected = 0) => docker(['run', '--rm', ...args, image, 'sh', '-c', 'id -u; grep -E "^(Uid|CapBnd|NoNewPrivs):" /proc/1/status; test "$(id -u)" = 10001; printf verified > /data/uploads/probe'], expected);
docker(['volume', 'create', '--label', label, volume]);
try {
  // Deliberately reproduce root-owned runtime mounts, without application data.
  const mount = ['--mount', `type=volume,src=${volume},dst=/data/uploads,volume-nocopy`];
  docker(['run', '--rm', '--user', '0:0', '--entrypoint', 'sh', ...mount, image, '-c', 'chown 0:0 /data/uploads; chmod 0700 /data/uploads']);
  run(['--user', '0:0', ...mount], 1);
  run(mount, 1);
  const output = run(['--user', '0:0', '-e', 'IRR_INIT_STORAGE_PERMISSIONS=true', ...mount]);
  assert.match(output, /Uid:\s+10001\s+10001\s+10001\s+10001/);
  assert.match(output, /CapBnd:\s+0000000000000000/);
  assert.match(output, /NoNewPrivs:\s+1/);
  docker(['run', '--rm', ...mount, image, 'sh', '-c', 'test "$(cat /data/uploads/probe)" = verified; test "$(id -u)" = 10001; printf retained > /data/uploads/probe']);
  run(['--user', '0:0', '-e', 'IRR_INIT_STORAGE_PERMISSIONS=true', '-e', 'STORAGE_DIRECTORY=/tmp'], 1);
  docker(['run', '--rm', '--user', '0:0', '--entrypoint', 'sh', '-e', 'IRR_INIT_STORAGE_PERMISSIONS=true', image, '-c', 'rmdir /data/uploads; ln -s /tmp /data/uploads; exec /app/backend-entrypoint.sh true'], 1);
  run(['--user', '10002:10002'], 1);
  console.log('PASS: implicit root, unwritable volume, opt-in privilege drop, retained files, invalid path, symlink and unexpected identity.');
} finally {
  const actual = docker(['volume', 'inspect', '--format', '{{ index .Labels "irr.purpose" }}', volume]);
  assert.equal(actual, 'storage-bootstrap-verification', 'Refusing to remove a volume without the verification label.');
  docker(['volume', 'rm', volume]);
}
