/** HTTP and PostgreSQL baseline against independent deterministic totals. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import os from 'node:os';
import assert from 'node:assert/strict';
import { parseArgs } from 'node:util';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { readRun,root,guard,sql,docker } from './common.mjs';
import { seedRun } from './seed.mjs';
import { assertReport } from './oracle.mjs';

export function statistics(times,elapsedMs,errors) {
    const sorted = [...times].sort((a,b) => a-b);
    const percentile = p => sorted.length ? sorted[Math.ceil(sorted.length*p)-1] : null;
    return {requests:times.length+errors,successes:times.length,errors,p50Ms:percentile(.50),p95Ms:percentile(.95),p99Ms:percentile(.99),elapsedMs,throughputRequestsPerSecond:(times.length+errors)*1000/elapsedMs};
}
/** Signs a synthetic reporter session with this isolated stack's ephemeral key; no live credential is read. */
function token(directory,actor) {
    const now = Math.floor(Date.now()/1000), email = 'benchmark@example.test';
    const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
    const content = encode({alg:'RS256',typ:'JWT'})+'.'+encode({iss:'self',iat:now,exp:now+14400,sub:email,email,id:actor,userRole:'REPRESENTATIVE',credentialVersion:0});
    return content+'.'+crypto.sign('RSA-SHA256',Buffer.from(content),fs.readFileSync(path.join(directory,'private.pem'))).toString('base64url');
}
/** Reads the exact maintained production SQL; fails if the source block shape changes. */
export function productionSql(endpoint,query,parameters) {
    const filename = endpoint==='analytics' ? 'AnalyticsReportService.java' : 'OperationalReportService.java';
    const content = fs.readFileSync(path.join(root,'src/main/java/org/code/api/reporting/application',filename),'utf8');
    const blocks = [...content.matchAll(/"""([\s\S]*?)"""/g)];
    if (blocks.length!==1) throw new Error('Review changed report SQL source before benchmarking.');
    const quoted = text => "'" + text.replaceAll("'","''") + "'";
    const args = {org:quoted(query.org),route:query.route?quoted(query.route):'NULL',municipality:'NULL',vehicle:'NULL',start:quoted(query.from+'T03:00:00Z')+'::timestamptz',end:quoted(new Date(Date.parse(query.to+'T03:00:00Z')+86400000).toISOString())+'::timestamptz',salesAvailable:query.route?'false':'true'};
    return blocks[0][1].replace(/(?<!:):([A-Za-z]\w*)/g,(_,name) => {
        if (!(name in args)) throw new Error('Unexpected SQL parameter: '+name);
        return parameters ? parameters[name] : args[name];
    });
}
/** Benchmarks the actual HTTP response body; precision/scope validation happens after timing it. */
async function request(state,authorization,scenario,endpoint) {
    const query = scenario.query, params = new URLSearchParams({from:query.from,to:query.to});
    if(query.route) params.set('routeId',query.route);
    const started = performance.now();
    const response = await fetch(state.api+'/api/v1/reports/'+endpoint+'?'+params,{headers:{Authorization:'Bearer '+authorization,'X-Organization-Id':query.org},signal:AbortSignal.timeout(30000)});
    const body = await response.text(), latency = performance.now()-started;
    if(response.status!==200) throw new Error('Benchmark HTTP '+response.status);
    const actual = JSON.parse(body);
    assert.equal(actual.organizationId,query.org);
    assert.equal(actual.timezone,'America/Sao_Paulo');
    assert.equal(actual.from,query.from); assert.equal(actual.to,query.to);
    assert.equal(actual.definitionVersion,endpoint==='analytics'?'analytics-1':'operations-1');
    assertReport(actual,scenario[endpoint]);
    return latency;
}

/** Progressive volumes reload the same owned schema; named development resources cannot be selected. */
export async function benchmark(directory,state,{volumes=[1000,10000,50000],seed=669850,samples=100,warmup=10,concurrencies=[1,4]}={}) {
    if (!Number.isSafeInteger(samples)||samples<20||samples>10000||!Number.isSafeInteger(warmup)||warmup<1||warmup>100||!concurrencies.every(c=>Number.isSafeInteger(c)&&c>=1&&c<=32)) throw new Error('Invalid sampling configuration.');
    guard(state);
    const info = JSON.parse(docker(['info','--format','{{json .}}']));
    const result = {scenarioVersion:'synthetic-reports-1',startedAt:new Date().toISOString(),sourceCommit:execFileSync('git',['rev-parse','HEAD'],{cwd:root,encoding:'utf8'}).trim(),
        backendImageId:state.backendImageId,databaseImageId:state.databaseImageId,redisImageId:state.redisImageId,openapiSha256:state.openapiSha256,
        environment:{node:process.version,os:os.platform()+' '+os.release(),architecture:os.arch(),docker:info.ServerVersion,dockerCpus:info.NCPU,dockerMemoryBytes:info.MemTotal,
            backendCpus:2,backendMemory:'1GiB',heap:'256-512MiB',databaseCpus:2,databaseMemory:'1GiB',sharedBuffers:'128MB',workMem:'4MB',connectionPool:10},
        configuration:{volumes,seed,samples,warmup,concurrencies,model:'closed-loop loopback HTTP, reused client, nearest-rank percentiles',period:'2026-06-01 through 2026-09-30 inclusive America/Sao_Paulo'},migrationHistory:sql(state,"SELECT version||':'||checksum FROM flyway_schema_history WHERE success ORDER BY installed_rank"),runs:[],status:'running'};
    const checkpoint = () => fs.writeFileSync(path.join(directory,'results.json'),JSON.stringify(result,null,2));
    try {
        for (const executions of volumes) {
            const fixture = seedRun(directory,state,{seed,executions}), authorization = token(directory,fixture.actor);
            result.runs.push({executions,sqlSha256:fixture.sqlSha256,counts:fixture.counts,seedMs:fixture.preparationMs,measurements:[],plans:[]});
            const run = result.runs.at(-1);
            checkpoint();
            for (const scenario of fixture.scenarios) for (const endpoint of ['operations','analytics']) {
                // Validation before measurement is mandatory; incorrect totals abort the baseline.
                await request(state,authorization,scenario,endpoint);
                for (let i=0;i<warmup;i++) await request(state,authorization,scenario,endpoint);
                const plans = [];
                for(let i=0;i<3;i++) plans.push(JSON.parse(sql(state,'EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) '+productionSql(endpoint,scenario.query)))[0]);
                run.plans.push({scenario:scenario.query.name,endpoint,observations:plans});
                checkpoint();
                for(const concurrency of concurrencies) {
                    let next=0,errors=0; const times=[],failures=[],started=performance.now();
                    await Promise.all(Array.from({length:concurrency},async()=>{
                        while(next++<samples) try { times.push(await request(state,authorization,scenario,endpoint)); }
                        catch(error) { errors++; failures.push(error.message); }
                    }));
                    const metrics=statistics(times,performance.now()-started,errors);
                    run.measurements.push({scenario:scenario.query.name,endpoint,concurrency,...metrics,failures});
                    checkpoint();
                    console.log(JSON.stringify({executions,scenario:scenario.query.name,endpoint,concurrency,...metrics}));
                    if(errors) throw new Error('Benchmark recorded HTTP or correctness failures.');
                }
            }
        }
        result.completedAt=new Date().toISOString(); result.status='passed';
    } catch(error) { result.status='failed';result.error=error.message;throw error; }
    finally { fs.writeFileSync(path.join(directory,'results.json'),JSON.stringify(result,null,2)); }
    return result;
}

if(process.argv[1] && path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
    const {values,positionals}=parseArgs({allowPositionals:true,options:{volumes:{type:'string',default:'1000,10000,50000'},seed:{type:'string',default:'669850'},samples:{type:'string',default:'100'},warmup:{type:'string',default:'10'},concurrency:{type:'string',default:'1,4'}}});
    const {directory,state}=readRun(positionals[0]);
    await benchmark(directory,state,{volumes:values.volumes.split(',').map(Number),seed:Number(values.seed),samples:Number(values.samples),warmup:Number(values.warmup),concurrencies:values.concurrency.split(',').map(Number)});
}
