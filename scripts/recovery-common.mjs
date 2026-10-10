/** Recovery validation primitives. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';

export const UUID_FILE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
export const HISTORY_SQL = "SELECT version || chr(58) || checksum FROM flyway_schema_history WHERE success ORDER BY installed_rank";

/** Avoids echoing private Compose values or process output on command failure. */
export function docker(args, cwd) {
    try {
        return execFileSync('docker', args, { cwd, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] }).trim();
    } catch {
        throw new Error('Docker recovery command failed (' + args[0] + '); inspect the selected local service privately.');
    }
}

/** Hashes an archived file without logging its content. */
export function sha256(file) {
    const hash = crypto.createHash('sha256');
    const handle = fs.openSync(file, 'r');
    const buffer = Buffer.alloc(1024 * 1024);
    try {
        let bytes;
        while ((bytes = fs.readSync(handle, buffer, 0, buffer.length, null)) > 0) hash.update(buffer.subarray(0, bytes));
        return hash.digest('hex');
    } finally { fs.closeSync(handle); }
}

/** Checks mandatory entries before starting a restore; legacy manifests remain readable. */
export function verifyManifest(directory, manifest) {
    const legacy = manifest.formatVersion === undefined;
    if (!legacy && manifest.formatVersion !== 2) throw new Error('Unsupported backup format.');
    const required = legacy
        ? ['database.dump', 'attachments.tar.gz', '.env', 'keys/private.pem', 'keys/public.pem']
        : ['database.dump', 'attachments.tar.gz', 'compose.resolved.json', 'keys/private.pem', 'keys/public.pem'];
    const allowed = new Set([...required, ...(legacy ? [] : ['environment.env'])]);
    if (!manifest.hashes || required.some(name => !Object.hasOwn(manifest.hashes, name))) throw new Error('Incomplete backup manifest.');
    for (const [name, hash] of Object.entries(manifest.hashes)) {
        if (!allowed.has(name) || !/^[a-f0-9]{64}$/.test(hash)) throw new Error('Unexpected backup entry or checksum.');
        const file = path.join(directory, name);
        if (!fs.lstatSync(file).isFile() || sha256(file) !== hash) throw new Error('Backup checksum mismatch: ' + name);
    }
    if (!legacy) {
        if (!/^sha256:[a-f0-9]{64}$/.test(manifest.databaseImageId ?? '')) throw new Error('Missing immutable database image ID.');
        if (!manifest.attachmentHashes || typeof manifest.attachmentHashes !== 'object') throw new Error('Missing attachment checksums.');
        for (const [file, hash] of Object.entries(manifest.attachmentHashes)) {
            if (!UUID_FILE.test(file) || !/^[a-f0-9]{64}$/.test(hash)) throw new Error('Invalid attachment checksum entry.');
        }
        if (!manifest.tableCounts || !Object.keys(manifest.tableCounts).length) throw new Error('Missing database row counts.');
    }
}

/** Allows only flat UUID attachment files and the root directory, never links or traversal. */
export function validateArchiveListing(names, details) {
    for (const name of names.split('\n').filter(Boolean)) {
        if (name === './' || name === '.') continue;
        if (!UUID_FILE.test(name.replace(/^\.\//, ''))) throw new Error('Attachment archive contains a noncanonical entry.');
    }
    for (const line of details.split('\n').filter(Boolean)) {
        if (line[0] !== '-' && !(line[0] === 'd' && /\s\.\/?$/.test(line))) throw new Error('Attachment archive contains a link or unsupported entry.');
    }
}

/** Keeps database attachment references within the canonical restored directory. */
export function attachmentFilename(reference) {
    if (typeof reference !== 'string') throw new Error('Invalid attachment reference.');
    const filename = path.posix.basename(reference.replaceAll('\\', '/'));
    if (!UUID_FILE.test(filename)) throw new Error('Attachment requires an explicit legacy import mapping.');
    return filename;
}

/** Reads public-table cardinalities using catalog identifiers, never user-supplied SQL names. */
export function tableCounts(sql) {
    const tables = JSON.parse(sql("SELECT coalesce(json_agg(tablename ORDER BY tablename),'[]') FROM pg_tables WHERE schemaname='public'"));
    return Object.fromEntries(tables.map(table => {
        if (!/^[a-z_][a-z0-9_]*$/.test(table)) throw new Error('Unexpected table identifier.');
        return [table, sql('SELECT count(*) FROM public."' + table + '"')];
    }));
}
