/** Isolated restore rehearsal. Maintainer: Enzo Ribas (https://github.com/oEnzoRibas). */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { docker, sha256, verifyManifest, validateArchiveListing, attachmentFilename, HISTORY_SQL, tableCounts } from './recovery-common.mjs';
const directory = path.resolve(process.argv[2] ?? '');
if (!process.argv[2]) throw new Error('Usage: node scripts/verify-recovery.mjs <backup-directory>');
const manifest = JSON.parse(fs.readFileSync(path.join(directory, 'manifest.json'), 'utf8'));
verifyManifest(directory, manifest);
const name = 'irr-restore-' + crypto.randomUUID();
const run = args => docker(args);
const sql = query => run(['exec', name, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', 'postgres', '-d', 'irr_restore', '-Atc', query]);
let created = false;
try {
    run(['run', '-d', '--pull=never', '--name', name, '--label', 'irr.purpose=recovery-rehearsal', '--network', 'none', '-e', 'POSTGRES_HOST_AUTH_METHOD=trust', manifest.databaseImageId ?? manifest.databaseImage]); created = true;
    const isolated = JSON.parse(run(['inspect', name]))[0];
    if (isolated.HostConfig.NetworkMode !== 'none' || Object.keys(isolated.HostConfig.PortBindings ?? {}).length) throw new Error('Restore container is not isolated.');
    let ready = false;
    for (let i = 0; i < 60; i++) { try { run(['exec', name, 'pg_isready', '-h', '127.0.0.1', '-U', 'postgres']); ready = true; break; } catch { await new Promise(resolve => setTimeout(resolve, 500)); } }
    if (!ready) throw new Error('Disposable PostgreSQL did not become ready');
    run(['exec', name, 'createdb', '-U', 'postgres', 'irr_restore']);
    run(['cp', path.join(directory, 'database.dump'), name + ':/tmp/database.dump']);
    run(['exec', name, 'pg_restore', '-U', 'postgres', '-d', 'irr_restore', '--no-owner', '--no-privileges', '--exit-on-error', '/tmp/database.dump']);
    const history = sql(HISTORY_SQL);
    if (history !== manifest.history) throw new Error('Restored migration history differs');
    const restoredCounts = tableCounts(sql);
    if (manifest.tableCounts && JSON.stringify(restoredCounts) !== JSON.stringify(manifest.tableCounts)) throw new Error('Restored table row counts differ.');
    run(['cp', path.join(directory, 'attachments.tar.gz'), name + ':/tmp/attachments.tar.gz']);
    validateArchiveListing(run(['exec', name, 'tar', '-tzf', '/tmp/attachments.tar.gz']), run(['exec', name, 'tar', '-tvzf', '/tmp/attachments.tar.gz']));
    run(['exec', name, 'sh', '-ec', 'mkdir /tmp/restored-attachments; tar -xzf /tmp/attachments.tar.gz -C /tmp/restored-attachments']);
    const attachmentFiles = run(['exec', name, 'sh', '-ec', 'find /tmp/restored-attachments -type f | wc -l']);
    const tableCount = sql("SELECT count(*) FROM information_schema.tables WHERE table_schema='public'");
    const references = JSON.parse(sql("SELECT COALESCE(json_agg(storage_url), '[]'::json) FROM attachment"));
    for (const reference of references) {
        const file = attachmentFilename(reference);
        run(['exec', name, 'test', '-f', '/tmp/restored-attachments/' + file]);
    }
    if (manifest.attachmentHashes) {
        if (Number(attachmentFiles) !== Object.keys(manifest.attachmentHashes).length) throw new Error('Restored attachment count differs.');
        for (const [file, hash] of Object.entries(manifest.attachmentHashes)) {
            const restored = run(['exec', name, 'sha256sum', '/tmp/restored-attachments/' + file]).split(/\s/)[0];
            if (restored !== hash) throw new Error('Restored attachment bytes differ.');
        }
    }
    let reconciliation = 'not present in this schema';
    if (sql("SELECT to_regclass('public.stock_movement') IS NOT NULL") === 't') {
        const differences = sql(`WITH movements AS (SELECT organization_id,material_subtype_id,sum(weight_delta_kg) weight,sum(volume_delta_m3) volume FROM stock_movement GROUP BY 1,2), lots AS (SELECT organization_id,material_subtype_id,sum(available_weight_kg) weight,sum(available_volume_m3) volume FROM stock_lot WHERE is_active GROUP BY 1,2), keys AS (SELECT organization_id,material_subtype_id FROM movements UNION SELECT organization_id,material_subtype_id FROM lots UNION SELECT organization_id,material_subtype_id FROM inventory_balance WHERE organization_id IS NOT NULL) SELECT count(*) FROM keys k LEFT JOIN movements m USING(organization_id,material_subtype_id) LEFT JOIN lots l USING(organization_id,material_subtype_id) LEFT JOIN inventory_balance b USING(organization_id,material_subtype_id) WHERE COALESCE(m.weight,0)<>COALESCE(b.current_weight_kg,0) OR COALESCE(m.volume,0)<>COALESCE(b.current_volume_m3,0) OR COALESCE(l.weight,0)<>COALESCE(b.current_weight_kg,0) OR COALESCE(l.volume,0)<>COALESCE(b.current_volume_m3,0)`);
        if (differences !== '0') throw new Error('Restored stock reconciliation differences: ' + differences);
        reconciliation = 'zero differences';
    }
    const result = { verifiedAt: new Date().toISOString(), history, tableCount: Number(tableCount), rowCountsVerified: manifest.tableCounts ? restoredCounts : 'legacy manifest: not recorded', attachmentFiles: Number(attachmentFiles), attachmentReferencesVerified: references.length, attachmentByteChecksums: manifest.attachmentHashes ? 'verified' : 'legacy manifest: archive checksum only', reconciliation, archiveChecksums: 'verified', isolatedNetwork: true, backendImageId: manifest.backendImageId, databaseImageId: manifest.databaseImageId };
    result.manifestSha256 = sha256(path.join(directory, 'manifest.json'));
    fs.writeFileSync(path.join(directory, 'restore-evidence.json'), JSON.stringify(result, null, 2)); console.log(JSON.stringify(result, null, 2));
} finally {
    if (created) { const purpose = run(['inspect', '--format', '{{index .Config.Labels "irr.purpose"}}', name]); if (purpose !== 'recovery-rehearsal') throw new Error('Refusing removal: unexpected container label'); run(['rm', '-f', '-v', name]); }
}
