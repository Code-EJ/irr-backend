/** Prepares disposable Docker infrastructure with real Flyway migrations. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { parseArgs } from 'node:util';
import { base, docker, cleanup } from './common.mjs';

const { values } = parseArgs({ options: { image: { type:'string',default:'irr-identity-backend' } } });
const image = JSON.parse(docker(['image','inspect',values.image]))[0];
const postgres = JSON.parse(docker(['image','inspect','postgres:16-alpine']))[0];
const redis = JSON.parse(docker(['image','inspect','redis:8.2-alpine']))[0];
const project = 'irr-bench-' + crypto.randomBytes(6).toString('hex');
const directory = path.join(base,project), purpose = 'synthetic-report-benchmark';
fs.mkdirSync(directory,{recursive:true,mode:0o700});
const keys = crypto.generateKeyPairSync('rsa',{modulusLength:2048,publicKeyEncoding:{type:'spki',format:'pem'},privateKeyEncoding:{type:'pkcs8',format:'pem'}});
fs.writeFileSync(path.join(directory,'private.pem'),keys.privateKey,{mode:0o600});
const labels = {'irr.purpose':purpose};
const config = { name:project,services:{
    database:{image:postgres.Id,labels,environment:{POSTGRES_DB:'irr_benchmark',POSTGRES_USER:'irr_benchmark',POSTGRES_PASSWORD:'isolated-benchmark-only'},volumes:['database:/var/lib/postgresql/data'],mem_limit:'1g',cpus:2,command:['postgres','-c','shared_buffers=128MB','-c','work_mem=4MB','-c','max_connections=40'],healthcheck:{test:['CMD-SHELL','pg_isready -U irr_benchmark -d irr_benchmark'],interval:'2s',timeout:'2s',retries:30}},
    redis:{image:redis.Id,labels,command:['redis-server','--requirepass','isolated-benchmark-redis'],mem_limit:'128m',cpus:1,healthcheck:{test:['CMD-SHELL','REDISCLI_AUTH=isolated-benchmark-redis redis-cli ping'],interval:'2s',timeout:'2s',retries:30}},
    backend:{image:image.Id,labels,mem_limit:'1g',cpus:2,environment:{JAVA_TOOL_OPTIONS:'-Xms256m -Xmx512m',DB_HOST:'database',DB_PORT:'5432',DB_NAME:'irr_benchmark',DB_USER:'irr_benchmark',DB_PASSWORD:'isolated-benchmark-only',REDIS_HOST:'redis',REDIS_PORT:'6379',REDIS_PASSWORD:'isolated-benchmark-redis',RSA_PUBLIC_KEY:keys.publicKey,RSA_PRIVATE_KEY:keys.privateKey,STORAGE_DIRECTORY:'/data/uploads',DEV_BOOTSTRAP_ENABLED:'false',BOOTSTRAP_ADMIN_ENABLED:'false',MAIL_PROVIDER:'disabled',IRR_ATTACHMENTS_CLEANUP_ENABLED:'false',SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:'10'},ports:['127.0.0.1::9191'],depends_on:{database:{condition:'service_healthy'},redis:{condition:'service_healthy'}},healthcheck:{test:['CMD','curl','--fail','--silent','http://localhost:9191/actuator/health/readiness'],interval:'2s',timeout:'3s',start_period:'60s',retries:60}},
},networks:{default:{labels}},volumes:{database:{labels}}};
const file = path.join(directory,'compose.json');
fs.writeFileSync(file,JSON.stringify(config,null,2),{mode:0o600});
const state = {project,purpose,createdAt:new Date().toISOString(),backendImageId:image.Id,databaseImageId:postgres.Id,redisImageId:redis.Id};
try {
    docker(['compose','-f',file,'-p',project,'up','-d','--wait','--wait-timeout','180','--pull','never']);
    for (const kind of ['database','backend','redis']) state[kind] = docker(['compose','-f',file,'-p',project,'ps','-q',kind]);
    const container = JSON.parse(docker(['inspect',state.backend]))[0];
    const port = container.NetworkSettings.Ports['9191/tcp']?.[0];
    if (!port || port.HostIp !== '127.0.0.1') throw new Error('Benchmark API must publish only to loopback.');
    state.api = 'http://127.0.0.1:' + port.HostPort;
    const spec = await (await fetch(state.api + '/v3/api-docs')).json();
    if (!spec.paths?.['/api/v1/reports/analytics']?.get || !spec.paths?.['/api/v1/reports/operations']?.get || Object.keys(spec.paths).some(endpoint => !endpoint.startsWith('/api/v1/'))) throw new Error('Benchmark image must expose the supported v1 report contract.');
    delete spec.servers;
    const normalize = value => Array.isArray(value) ? value.map(normalize) : value && typeof value === 'object' ? Object.fromEntries(Object.keys(value).sort().map(key => [key,normalize(value[key])])) : value;
    state.openapiSha256 = crypto.createHash('sha256').update(JSON.stringify(normalize(spec))).digest('hex');
    fs.writeFileSync(path.join(directory,'run.json'),JSON.stringify(state,null,2));
    console.log('Isolated benchmark ready: ' + directory);
} catch (error) {
    fs.writeFileSync(path.join(directory,'preparation-failure.json'),JSON.stringify({project,error:error.message}));
    cleanup(state); throw error;
}
