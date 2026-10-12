/**
 * Projects private Grype JSON onto a shareable package/finding inventory.
 * Never copies image configuration, environment, labels, file contents or credentials.
 * @author Enzo Ribas (https://github.com/oEnzoRibas)
 */
import fs from 'node:fs';
import {createHash} from 'node:crypto';
const [input, output] = process.argv.slice(2);
if (!input || !output) throw new Error('Usage: node scripts/summarize-image-scan.mjs <private-grype.json> <sanitized.json>');
const bytes = fs.readFileSync(input);
const report = JSON.parse(bytes.toString('utf8').replace(/^\uFEFF/, ''));
if (!Array.isArray(report.matches)) throw new Error('Unsupported Grype report structure.');
const counts = {};
const findings = report.matches.map(m => {
  const severity = m.vulnerability.severity;
  counts[severity] = (counts[severity] ?? 0) + 1;
  return {
    id:m.vulnerability.id, namespace:m.vulnerability.namespace, severity,
    package:m.artifact.name, version:m.artifact.version, type:m.artifact.type,
    fixState:m.vulnerability.fix?.state ?? 'unknown', fixVersions:m.vulnerability.fix?.versions ?? [],
    url:m.vulnerability.dataSource, disposition:'pending',
  };
});
const sanitized = {
  maintainer:'Enzo Ribas (https://github.com/oEnzoRibas)',
  scanner:report.descriptor?.name, scannerVersion:report.descriptor?.version,
  databaseBuilt:report.descriptor?.db?.status?.built, rawReportSha256:createHash('sha256').update(bytes).digest('hex'),
  source:{imageId:report.source?.target?.imageID, manifestDigest:report.source?.target?.manifestDigest},
  distro:report.distro, counts, matchCount:findings.length,
  interpretation:'Matches are not unique exploitable defects. Pending findings require package/vendor/configuration review; no suppression or clean-release assertion.',
  matchedPackages:[...new Map(report.matches.map(m=>[JSON.stringify([m.artifact.name,m.artifact.version,m.artifact.type]),{name:m.artifact.name,version:m.artifact.version,type:m.artifact.type}])).values()], findings,
};
fs.writeFileSync(output, JSON.stringify(sanitized,null,2)+'\n');
console.log(JSON.stringify({output,matchedPackages:sanitized.matchedPackages.length,counts,matchCount:findings.length}));
