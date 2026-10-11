/** @author Enzo Ribas (https://github.com/oEnzoRibas) */
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {validateInitialData} from '../validate-initial-data.mjs';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const fixture = () => ({version:1,sourceBatchId:'synthetic-validation-only',organizations:[{key:'org',name:'Synthetic unit',organizationType:'Synthetic fixture'}],users:[{key:'user',fullName:'Synthetic person',email:'fixture@example.invalid',userRole:'REPRESENTATIVE'}],memberships:[{organizationKey:'org',userKey:'user',role:'MEMBER'}],openingStock:{decision:'not-required'}});
test('accepts linked foundation data without writes and produces a stable fingerprint', () => {
  const value=fixture(), report=validateInitialData(value);
  assert.equal(report.valid,true); assert.equal(report.writesPerformed,0);
  assert.equal(report.contentSha256,validateInitialData({...value,version:1}).contentSha256);
});
test('rejects normalized duplicate emails and duplicate external keys', () => {
  const value=fixture();value.users.push({...value.users[0],email:'FIXTURE@EXAMPLE.INVALID'});
  const report=validateInitialData(value);assert.equal(report.valid,false);assert.equal(report.errors.length,2);
  assert.ok(!JSON.stringify(report).includes('FIXTURE@EXAMPLE.INVALID'));
});
test('rejects missing references and duplicated memberships', () => {
  const value=fixture();value.memberships.push({...value.memberships[0]});value.memberships[0].organizationKey='missing';
  assert.equal(validateInitialData(value).valid,false);
  value.memberships[0].organizationKey='org';assert.equal(validateInitialData(value).valid,false);
});
test('does not accept credentials or stock quantities as foundation input', () => {
  const value=fixture();value.users[0].password='private-fixture-only';value.openingStock.quantity=10;
  const report=validateInitialData(value);assert.equal(report.valid,false);assert.ok(!JSON.stringify(report).includes('private-fixture-only'));
});
test('flags global administration and pending stock for separate review', () => {
  const value=fixture();value.users[0].userRole='ADMINISTRATOR';value.openingStock.decision='pending';
  const report=validateInitialData(value);assert.equal(report.valid,true);assert.equal(report.warnings.length,2);
});
test('rejects invalid roots and destination identifiers', () => {
  assert.equal(validateInitialData(null).valid,false);
  const value=fixture();value.users[0].existingId='not-a-uuid';assert.equal(validateInitialData(value).valid,false);
});

test('rejects coerced keys, missing organization types and duplicate destination UUIDs', () => {
  const value=fixture();value.sourceBatchId=123;value.organizations[0].key=123;
  delete value.organizations[0].organizationType;
  assert.equal(validateInitialData(value).valid,false);
  const linked=fixture();linked.users[0].existingId='abcdef01-1234-1234-1234-123456789abc';
  linked.users.push({...linked.users[0],key:'second',email:'second@example.invalid',existingId:linked.users[0].existingId.toUpperCase()});
  assert.ok(validateInitialData(linked).errors.some(e=>e.message==='Duplicate destination identity.'));
});

test('CLI rejects malformed private input without echoing its contents', () => {
  const directory=fs.mkdtempSync(path.join(os.tmpdir(),'irr-data-validation-'));
  try {
    const file=path.join(directory,'invalid.json');
    fs.writeFileSync(file,'private-client-value-not-json');
    const result=spawnSync(process.execPath,[fileURLToPath(new URL('../validate-initial-data.mjs',import.meta.url)),file],{encoding:'utf8'});
    assert.equal(result.status,1);
    assert.ok(result.stderr.includes('No writes performed'));
    assert.ok(!(result.stdout+result.stderr).includes('private-client-value'));
  } finally {
    const resolved=path.resolve(directory), temporaryRoot=path.resolve(os.tmpdir());
    if (!resolved.startsWith(temporaryRoot+path.sep) || !path.basename(resolved).startsWith('irr-data-validation-')) throw new Error('Refusing cleanup outside the validation temporary directory.');
    fs.rmSync(resolved,{recursive:true,force:true});
  }
});

