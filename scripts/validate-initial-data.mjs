/**
 * Validates a client-supplied foundation manifest without network or database writes.
 * @author Enzo Ribas (https://github.com/oEnzoRibas)
 */
import fs from 'node:fs';
import {createHash} from 'node:crypto';
import {pathToFileURL} from 'node:url';

const keyPattern = /^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$/;
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
/** Produces deterministic content fingerprints, independent of JSON object property order. */
function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map(k => [k, canonical(value[k])]));
  return value;
}
/** Errors identify fields/row numbers only; source values can contain private client information. */
export function validateInitialData(manifest) {
  const errors = [], warnings = [], maps = {};
  const issue = (location, message) => errors.push({location, message});
  const fields = (row, allowed, location) => {
    if (!row || typeof row !== 'object' || Array.isArray(row)) { issue(location, 'Expected an object.'); return false; }
    for (const name of Object.keys(row)) if (!allowed.includes(name)) issue(`${location}.${name}`, 'Unsupported field; credentials and unreviewed operational data are prohibited.');
    return true;
  };
  if (!fields(manifest, ['version', 'sourceBatchId', 'organizations', 'users', 'memberships', 'openingStock'], 'manifest')) return {valid:false, errors, warnings};
  if (manifest.version !== 1) issue('version', 'Expected manifest version 1.');
  if (typeof manifest.sourceBatchId !== 'string' || !keyPattern.test(manifest.sourceBatchId)) issue('sourceBatchId', 'Expected a stable batch identifier.');
  for (const kind of ['organizations', 'users']) {
    maps[kind] = new Map();
    if (!Array.isArray(manifest[kind])) { issue(kind, 'Expected an array.'); continue; }
    const emails = new Set(), destinationIds = new Set();
    manifest[kind].forEach((row, index) => {
      const location = `${kind}[${index}]`;
      if (!fields(row, kind === 'users' ? ['key', 'existingId', 'fullName', 'email', 'userRole'] : ['key', 'existingId', 'name', 'organizationType'], location)) return;
      if (typeof row.key !== 'string' || !keyPattern.test(row.key)) issue(`${location}.key`, 'Expected a stable external key.');
      else if (maps[kind].has(row.key)) issue(`${location}.key`, 'Duplicate external key.');
      else maps[kind].set(row.key, row);
      if (row.existingId !== undefined) {
        if (typeof row.existingId !== 'string' || !uuidPattern.test(row.existingId)) issue(`${location}.existingId`, 'Expected a destination UUID; verify existence and ownership separately.');
        else {
          const normalizedId = row.existingId.toLowerCase();
          if (destinationIds.has(normalizedId)) issue(`${location}.existingId`, 'Duplicate destination identity.');
          destinationIds.add(normalizedId);
        }
      }
      const name = kind === 'users' ? row.fullName : row.name;
      if (typeof name !== 'string' || !name.trim() || name.length > 255) issue(`${location}.${kind === 'users' ? 'fullName' : 'name'}`, 'Expected a nonempty name of at most 255 characters.');
      if (kind === 'organizations' && (typeof row.organizationType !== 'string' || !row.organizationType.trim() || row.organizationType.length > 50)) issue(`${location}.organizationType`, 'Expected the reviewed organization type, at most 50 characters, required by the API contract.');
      if (kind === 'users') {
        if (typeof row.email !== 'string' || row.email.length > 255 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(row.email)) issue(`${location}.email`, 'Invalid email shape; delivery requires separate confirmation.');
        else {
          const email = row.email.trim().toLowerCase();
          if (emails.has(email)) issue(`${location}.email`, 'Duplicate normalized email.');
          emails.add(email);
        }
        if (!['ADMINISTRATOR', 'REPRESENTATIVE', 'ORGANIZATION', 'CITY_HALL'].includes(row.userRole)) issue(`${location}.userRole`, 'Unsupported account role.');
        if (row.userRole === 'ADMINISTRATOR') warnings.push({location:`${location}.userRole`, message:'Global administration requires explicit owner review; no account is created by validation.'});
      }
    });
  }
  const links = new Set();
  if (!Array.isArray(manifest.memberships)) issue('memberships', 'Expected an array.');
  else manifest.memberships.forEach((row, index) => {
    const location = `memberships[${index}]`;
    if (!fields(row, ['organizationKey', 'userKey', 'role'], location)) return;
    if (!maps.organizations.has(row.organizationKey)) issue(`${location}.organizationKey`, 'Unresolved organization reference.');
    if (!maps.users.has(row.userKey)) issue(`${location}.userKey`, 'Unresolved account reference.');
    if (!['MEMBER', 'MANAGER'].includes(row.role)) issue(`${location}.role`, 'Unsupported membership role.');
    const link = JSON.stringify([row.organizationKey, row.userKey]);
    if (links.has(link)) issue(location, 'Duplicate organization/account association.');
    links.add(link);
  });
  if (!fields(manifest.openingStock, ['decision'], 'openingStock') || !['not-required', 'pending'].includes(manifest.openingStock?.decision)) issue('openingStock.decision', 'Explicitly select not-required or pending; opening quantities require a separate approved provenance workflow.');
  if (manifest.openingStock?.decision === 'pending') warnings.push({location:'openingStock', message:'Opening stock remains blocked until quantities, evidence and provenance are approved.'});
  return {
    valid:errors.length === 0, errors, warnings,
    contentSha256:createHash('sha256').update(JSON.stringify(canonical(manifest))).digest('hex'),
    counts:Object.fromEntries(['organizations', 'users', 'memberships'].map(k => [k, Array.isArray(manifest[k]) ? manifest[k].length : 0])),
    writesPerformed:0,
    remainingGates:['Client source approval', 'Destination UUID/ownership reconciliation', 'Private external-key to UUID checkpoint', 'Credential provisioning through the supported lifecycle', 'Approved operational/stock provenance if required'],
  };
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const file = process.argv[2];
  if (!file) throw new Error('Provide a private initial-data JSON manifest path.');
  let manifest;
  try {
    manifest = JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
  } catch {
    console.error('Initial-data validation failed: unreadable or malformed JSON. No writes performed.');
    process.exitCode = 1;
  }
  if (manifest !== undefined) {
    const report = validateInitialData(manifest);
    console.log(JSON.stringify(report, null, 2));
    if (!report.valid) process.exitCode = 1;
  }
}
