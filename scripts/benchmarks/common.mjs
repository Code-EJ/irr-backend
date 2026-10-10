/** Isolated benchmark operating guards. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
export const root = fileURLToPath(new URL('../../', import.meta.url));
export const base = path.join(root, '.local', 'benchmarks');

/** Limits all mutable targets to a generated benchmark manifest beneath the ignored root. */
export function readRun(input) {
    if (!input) throw new Error('Supply the run directory printed by prepare.mjs.');
    const directory = fs.realpathSync(path.resolve(input));
    const relative = path.relative(fs.realpathSync(base), directory);
    if (!/^irr-bench-[a-f0-9]{12}$/.test(relative)) throw new Error('Run directory is outside the benchmark root.');
    const state = JSON.parse(fs.readFileSync(path.join(directory, 'run.json'), 'utf8'));
    if (state.project !== relative || state.purpose !== 'synthetic-report-benchmark') throw new Error('Invalid benchmark ownership manifest.');
    for (const kind of ['database','backend','redis']) {
        if (!/^[a-f0-9]{64}$/.test(state[kind])) throw new Error('Missing isolated service identity.');
    }
    return { directory, state };
}
/** Never prints runtime configuration, tokens or command payloads on failure. */
export function docker(args, { input } = {}) {
    try {
        return execFileSync('docker', args, { cwd: root, input, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024, stdio: ['pipe','pipe','pipe'] }).trim();
    } catch (error) {
        const message = error.stderr?.toString().split('\n').find(line => /^ERROR:/.test(line));
        throw new Error('Benchmark Docker command failed (' + args[0] + ')' + (message ? ': ' + message : '. Inspect the isolated service privately.'));
    }
}
/** Requires exact project and purpose labels before seeding, measuring or deleting resources. */
export function guard(state) {
    for (const kind of ['database','backend','redis']) {
        const service = JSON.parse(docker(['inspect',state[kind]]))[0];
        if (service.Config.Labels['com.docker.compose.project'] !== state.project || service.Config.Labels['irr.purpose'] !== state.purpose) throw new Error('Refusing access to an unowned container.');
        if(kind==='backend') {
            const port=service.NetworkSettings.Ports['9191/tcp']?.[0];
            if(!port||port.HostIp!=='127.0.0.1') throw new Error('Owned benchmark API must publish only to loopback.');
            state.api='http://127.0.0.1:'+port.HostPort;
        }
    }
}
export function sql(state, query) {
    guardDatabase(state);
    return docker(['exec','-i',state.database,'psql','-v','ON_ERROR_STOP=1','-U','irr_benchmark','-d','irr_benchmark','-At'], { input: query });
}
function guardDatabase(state) {
    const db = JSON.parse(docker(['inspect',state.database]))[0];
    if (db.Config.Labels['irr.purpose'] !== state.purpose || db.Config.Labels['com.docker.compose.project'] !== state.project) throw new Error('Refusing database mutation outside the benchmark.');
}
/** Removes only project/purpose-labeled disposable resources; leaves evidence files intact. */
export function cleanup(state) {
    const ids = docker(['ps','-aq','--filter','label=com.docker.compose.project=' + state.project,'--filter','label=irr.purpose=' + state.purpose]).split('\n').filter(Boolean);
    for (const id of ids) {
        const labels = JSON.parse(docker(['inspect',id]))[0].Config.Labels;
        if (labels['irr.purpose'] !== state.purpose || labels['com.docker.compose.project'] !== state.project) throw new Error('Refusing unexpected container removal.');
        docker(['rm','-f','-v',id]);
    }
    for (const [kind, list, remove] of [['volume','volume','rm'],['network','network','rm']]) {
        const names = docker([list,'ls','-q','--filter','label=com.docker.compose.project=' + state.project,'--filter','label=irr.purpose=' + state.purpose]).split('\n').filter(Boolean);
        for (const name of names) {
            const labels = JSON.parse(docker([kind,'inspect',name]))[0].Labels;
            if (labels['irr.purpose'] !== state.purpose || labels['com.docker.compose.project'] !== state.project) throw new Error('Refusing unexpected resource removal.');
            docker([kind,remove,name]);
        }
    }
}
