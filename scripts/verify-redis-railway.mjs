/** Verifies the secret-safe Redis trial startup. @author Enzo Ribas (https://github.com/oEnzoRibas). */
import {spawnSync} from 'node:child_process';
import {randomBytes} from 'node:crypto';
import assert from 'node:assert/strict';
const image=process.argv[2];
if(!image||image.startsWith('-'))throw new Error('Supply the built Redis Railway image.');
const suffix=randomBytes(8).toString('hex');
const container=`irr-redis-railway-check-${suffix}`,volume=`irr-redis-railway-check-${suffix}`;
const password=randomBytes(32).toString('base64url');
function docker(args,env={}){
  const result=spawnSync('docker',args,{encoding:'utf8',timeout:60000,env:{...process.env,...env}});
  if(result.error||result.status!==0)throw new Error('Docker verification command failed; secret-bearing output withheld.');
  return result.stdout.trim();
}
async function ready(){
  for(let attempt=0;attempt<30;attempt++){
    try{if(docker(['exec','-e','REDISCLI_AUTH',container,'redis-cli','ping'],{REDISCLI_AUTH:password})==='PONG')return;}catch{}
    await new Promise(resolve=>setTimeout(resolve,250));
  }
  throw new Error('Authenticated Redis did not become ready.');
}
docker(['volume','create','--label','irr.purpose=redis-railway-verification',volume]);
try{
  docker(['run','-d','--name',container,'--network','none','--memory','125m','--mount',`type=volume,src=${volume},dst=/data`,'-e','REDIS_PASSWORD',image],{REDIS_PASSWORD:password});
  await ready();
  assert.match(docker(['exec',container,'redis-cli','ping']),/NOAUTH/);
  const status=docker(['exec',container,'cat','/proc/1/status']);
  assert.match(status,/Uid:\s+(?!0\s)\d+\s+/);
  const argumentsText=docker(['exec',container,'cat','/proc/1/cmdline']);
  assert.ok(!argumentsText.includes(password));
  const processEnvironment=docker(['exec','--user','redis',container,'cat','/proc/1/environ']);
  assert.ok(!processEnvironment.includes('REDIS_PASSWORD='));
  docker(['exec','-e','REDISCLI_AUTH',container,'redis-cli','set','irr:trial:probe','retained'],{REDISCLI_AUTH:password});
  // Graceful restart flushes AOF and proves the persistent /data mount is used.
  docker(['restart',container]);await ready();
  assert.equal(docker(['exec','-e','REDISCLI_AUTH',container,'redis-cli','get','irr:trial:probe'],{REDISCLI_AUTH:password}),'retained');
  const policy=docker(['exec','-e','REDISCLI_AUTH',container,'redis-cli','config','get','maxmemory-policy'],{REDISCLI_AUTH:password});
  assert.ok(policy.includes('noeviction'));
  console.log('PASS: default Redis startup, authentication, non-root PID 1, secret-safe arguments/environment, AOF restart and noeviction.');
}finally{
  spawnSync('docker',['rm','-f',container],{stdio:'ignore'});
  spawnSync('docker',['volume','rm',volume],{stdio:'ignore'});
}
