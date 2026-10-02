/** Checks repository links and source coverage. Maintainer: Enzo Ribas (https://github.com/oEnzoRibas). */
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const root = fileURLToPath(new URL('../', import.meta.url));
const files = [...new Set(execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard'], { cwd: root, encoding: 'utf8' }).split(/\r?\n/))].filter(p => p && fs.existsSync(path.join(root, p)));
const sources = files.filter(p => /^src\/.*\.java$/.test(p)).sort();
if (process.argv.includes('--update-inventory')) {
    const header = '# Java source inventory\n\n- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).\n\nComplete maintained main/test Java index. Regenerate with node scripts/verify-documentation.mjs --update-inventory.\n\n';
    fs.writeFileSync(path.join(root, 'docs/source-inventory.md'), header + sources.map(p => '- [' + p + '](../' + p + ')').join('\n') + '\n');
}
const errors = [];
for (const file of files.filter(p => p.endsWith('.md'))) {
    const content = fs.readFileSync(path.join(root, file), 'utf8');
    for (const match of content.matchAll(/\]\(([^)]+)\)/g)) {
        let destination = match[1];
        if (/^(https?:|mailto:|#)/.test(destination)) continue;
        destination = destination.replace(/^<|>$/g, '').split('#')[0];
        if (destination && !fs.existsSync(path.resolve(root, path.dirname(file), destination))) errors.push(file + ' -> ' + destination);
    }
}
const inventory = fs.readFileSync(path.join(root, 'docs/source-inventory.md'), 'utf8');
for (const file of sources) if (!inventory.includes(file)) errors.push('Missing source inventory: ' + file);
if (errors.length) { console.error(errors.join('\n')); process.exitCode = 1; }
else console.log('Documentation links and ' + sources.length + ' Java source entries verified.');
