/** Regression gates for deterministic fixtures, independent math and sampling. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { generate,identity } from './generate.mjs';
import { expected,ratio,assertReport } from './oracle.mjs';
import { statistics,productionSql } from './run.mjs';

test('same seed and volume reproduce byte-identical SQL and identities',()=>{
    const a=generate({seed:669850,executions:100}),b=generate({seed:669850,executions:100});
    assert.equal(a.sql,b.sql);assert.equal(a.digest,b.digest);
    assert.notEqual(a.digest,generate({seed:669851,executions:100}).digest);
    assert.equal(identity(669850,'execution',1),identity(669850,'execution',1));
});
test('fixtures include mixed states, periods, completeness, activity, duplicates and reversals',()=>{
    const {data}=generate({executions:1000});
    assert.deepEqual(new Set(data.executions.map(e=>e.status)),new Set(['COMPLETED','PLANNED','IN_PROGRESS','CANCELLED']));
    assert.ok(data.executions.some(e=>e.km===null)&&data.executions.some(e=>e.km===0n));
    assert.ok(new Set(data.executions.map(e=>new Date(e.finish).getUTCMonth())).size>=4);
    assert.ok(data.receipts.some(c=>!c.active)&&data.receipts.some(c=>!c.execution));
    assert.ok(data.executions.some(e=>new Set(e.sections.map(s=>s.key)).size<e.sections.length));
    assert.deepEqual(new Set(data.sales.map(s=>s.status)),new Set(['DRAFT','POSTED','REVERSED']));
    const hot=data.executions.filter(e=>e.org===data.refs[0].org).length;
    assert.ok(hot>700&&hot<900);
});
test('linked receipts reference completed executions with the same organization',()=>{
    const {data}=generate({executions:100});
    const executions=new Map(data.executions.map(e=>[e.id,e]));
    for(const c of data.receipts.filter(c=>c.execution)){
        const e=executions.get(c.execution);assert.equal(e.status,'COMPLETED');assert.equal(e.org,c.org);assert.equal(e.route,c.route);
    }
});
test('oracle uses weighted cohorts and sale state at the original business period',()=>{
    const finish=Date.parse('2026-06-02T03:00:00Z');
    const data={executions:[
        {id:'a',org:'o',route:'r',status:'COMPLETED',finish,seconds:3600n,km:10000n,plannedKm:10000n,plannedSeconds:3600n,sections:[{key:'a:1',covered:true},{key:'a:1',covered:true}]},
        {id:'b',org:'o',route:'r',status:'COMPLETED',finish,seconds:7200n,km:30000n,plannedKm:20000n,plannedSeconds:3600n,sections:[{key:'b:1',covered:false}]},
    ],receipts:[{org:'o',route:'r',execution:'a',time:finish-86400000,active:true,kg:200000n},{org:'o',route:'r',execution:'b',time:finish,active:true,kg:1000000n}],
        sales:[{org:'o',time:finish,active:true,status:'POSTED',cents:1234n},{org:'o',time:finish,active:true,status:'REVERSED',cents:9999n}]};
    const report=expected(data,{org:'o',from:'2026-06-02',to:'2026-06-02'},'analytics');
    assert.equal(report.hourlyProductivity.value,'40');assert.equal(report.distanceProductivity.value,'3');
    assert.equal(report.coverage.percentage,'50');assert.equal(report.coverage.duplicateRows,1);
    assert.equal(report.distanceComparison.percentageDeviation,'33.333333');assert.equal(report.revenue.amount,'12.34');
    assert.equal(expected(data,{org:'o',from:'2026-06-02',to:'2026-06-02'},'operations').collectedWeightKg,'100');
    assert.equal(expected(data,{org:'o',route:'r',from:'2026-06-02',to:'2026-06-02'},'analytics').revenue.amount,null);
});
test('exact ratios avoid zero division, preserve scale and round signed HALF_UP',()=>{
    assert.equal(ratio(1200000n,40000n,-1),'3');
    assert.equal(ratio(-1n,6n),'-0.166667');assert.equal(ratio(1n,0n),null);assert.equal(ratio(null,1n),null);
    assert.equal(ratio(0n,1n),'0');
});
test('response checks reject incorrect totals and a null silently changed to zero',()=>{
    assertReport({value:'1.0000',missing:null},{value:'1',missing:null});
    assert.throws(()=>assertReport({value:'2'},{value:'1'}));
    assert.throws(()=>assertReport({value:'0'},{value:null}));
});
test('nearest-rank percentiles and throughput include recorded failures',()=>{
    const r=statistics(Array.from({length:100},(_,i)=>i+1),2000,2);
    assert.equal(r.p50Ms,50);assert.equal(r.p95Ms,95);assert.equal(r.p99Ms,99);assert.equal(r.throughputRequestsPerSecond,51);assert.equal(r.errors,2);
});
test('prepared plan diagnostics preserve the maintained SQL with typed parameter positions',()=>{
    const q={org:identity(669850,'org',0),from:'2026-06-01',to:'2026-09-30'};
    const positions={org:'$1',municipality:'$2',route:'$3',vehicle:'$4',start:'$5',end:'$6',salesAvailable:'$7'};
    const prepared=productionSql('analytics',q,positions);
    assert.ok(prepared.includes('organization_id=$1'));
    assert.ok(prepared.includes('CAST($3 AS uuid)'));
    assert.ok(prepared.includes('AND $7'));
    assert.ok(!prepared.includes(':org'));
    assert.ok(productionSql('analytics',q).includes("'2026-06-01T03:00:00Z'::timestamptz"));
    const names=[];
    const expanded=productionSql('analytics',q,name=>{names.push(name);return '$'+names.length;});
    assert.ok(names.filter(name=>name==='org').length>1);
    assert.equal([...expanded.matchAll(/\$\d+/g)].length,names.length);
    assert.equal(new Set([...expanded.matchAll(/\$\d+/g)].map(match=>match[0])).size,names.length);
});
