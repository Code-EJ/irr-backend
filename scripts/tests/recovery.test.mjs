/** Recovery corruption and containment regression tests. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { sha256, verifyManifest, validateArchiveListing, attachmentFilename } from '../recovery-common.mjs';

const file = '11111111-1111-4111-8111-111111111111';
/** Creates only disposable files beneath the OS test temporary directory. */
function fixture(t, legacy = false) {
    const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'irr-recovery-test-'));
    t.after(() => {
        const relative = path.relative(path.resolve(os.tmpdir()), path.resolve(directory));
        assert.match(relative, /^irr-recovery-test-[^/\\]+$/);
        fs.rmSync(directory, { recursive: true });
    });
    fs.mkdirSync(path.join(directory, 'keys'));
    const hashes = {};
    for (const name of ['database.dump', 'attachments.tar.gz', legacy ? '.env' : 'compose.resolved.json', 'keys/private.pem', 'keys/public.pem']) {
        fs.writeFileSync(path.join(directory, name), 'synthetic fixture, not a credential');
        hashes[name] = sha256(path.join(directory, name));
    }
    return { directory, manifest: { ...(legacy ? {} : { formatVersion: 2, databaseImageId: 'sha256:' + 'a'.repeat(64), attachmentHashes: { [file]: 'b'.repeat(64) }, tableCounts: { attachment: '1' } }), hashes } };
}
test('accepts complete current and historical manifests', t => {
    for (const legacy of [false, true]) { const { directory, manifest } = fixture(t, legacy); verifyManifest(directory, manifest); }
});
test('rejects a missing mandatory manifest entry even if every remaining hash matches', t => {
    const { directory, manifest } = fixture(t); delete manifest.hashes['database.dump'];
    assert.throws(() => verifyManifest(directory, manifest), /Incomplete/);
});
test('rejects tampered database bytes before Docker is invoked', t => {
    const { directory, manifest } = fixture(t); fs.appendFileSync(path.join(directory, 'database.dump'), 'corruption');
    assert.throws(() => verifyManifest(directory, manifest), /checksum mismatch/);
});
test('rejects traversal and missing immutable image identity in the manifest', t => {
    const { directory, manifest } = fixture(t); manifest.hashes['../outside'] = 'a'.repeat(64);
    assert.throws(() => verifyManifest(directory, manifest), /Unexpected/);
    delete manifest.hashes['../outside']; manifest.databaseImageId = 'postgres:latest';
    assert.throws(() => verifyManifest(directory, manifest), /immutable/);
});
test('rejects traversal, directories, symbolic links and hard links in attachment archives', () => {
    validateArchiveListing('./\n./' + file, 'drwxr-xr-x root/root 0 2026-10-10 ./\n-rw-r--r-- root/root 12 2026-10-10 ./' + file);
    for (const name of ['../' + file, '/tmp/' + file, './nested/' + file]) assert.throws(() => validateArchiveListing(name, ''), /noncanonical/);
    for (const type of ['l', 'h']) assert.throws(() => validateArchiveListing('./' + file, type + 'rwxrwxrwx link'), /link/);
});
test('rejects unmapped legacy attachment references and retains canonical identifiers', () => {
    assert.equal(attachmentFilename('/data/uploads/' + file), file);
    assert.throws(() => attachmentFilename('/data/uploads/invoice.pdf'), /mapping/);
    assert.throws(() => attachmentFilename(null), /Invalid/);
});
