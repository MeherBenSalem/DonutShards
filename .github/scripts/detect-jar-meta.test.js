#!/usr/bin/env node
'use strict';

const assert = require('assert');
const { parseName, detectRequired, loadSupported } = require('./detect-jar-meta.js');

const supported = { loaders: ['paper', 'folia', 'purpur'], gameVersions: ['1.20.1', '1.20.4', '1.21.1', '26.1.2', '26.2', '26.3'] };
let failed = 0;

function test(name, fn) {
  try {
    fn();
    console.log('ok', name);
  } catch (e) {
    failed += 1;
    console.error('FAIL', name, '-', e.message);
  }
}

test('range name covers every supported version', () => {
  const got = detectRequired('releases/donutshards-paper-1.20.1-26.3-2.0.0.jar', supported);
  assert.deepStrictEqual(got, { loader: 'paper', loaders: ['paper', 'folia', 'purpur'], gameVersions: supported.gameVersions, version: '2.0.0' });
});

test('single calendar version with a patch number', () => {
  assert.deepStrictEqual(detectRequired('donutshards-folia-26.1.2-2.0.0.jar', supported).gameVersions, ['26.1.2']);
});

test('pre-release version suffix', () => {
  assert.strictEqual(detectRequired('donutshards-paper-1.20.1-26.3-2.1.0-beta.1.jar', supported).version, '2.1.0-beta.1');
});

test('legacy 1.x name still parses', () => {
  assert.deepStrictEqual(parseName('DonutShards-1.5.2-paper-folia-mc1.20.1-26.3.jar'), { loader: 'paper', from: '1.20.1', to: '26.3', version: '1.5.2' });
});

test('a stale range is refused', () => {
  assert.throws(() => detectRequired('donutshards-paper-1.20.1-26.2-2.0.0.jar', supported), /lists 1\.20\.1-26\.3/);
});

test('an unsupported single version is refused', () => {
  assert.throws(() => detectRequired('donutshards-paper-1.19.4-2.0.0.jar', supported), /does not list/);
});

test('a loader missing from the list is refused', () => {
  assert.throws(() => detectRequired('donutshards-spigot-1.20.1-26.3-2.0.0.jar', supported), /not in supported/);
});

test('unrecognised names are refused instead of defaulting', () => {
  assert.throws(() => detectRequired('donutshards-mystery.jar', supported), /Unrecognised/);
  assert.throws(() => detectRequired('DonutShards-2.0.0-sources.jar', supported), /Unrecognised/);
});

test('the repository list loads and its loaders are known', () => {
  const real = loadSupported();
  assert.ok(real.gameVersions.includes('1.20.1') && real.gameVersions.includes('26.3'));
  assert.ok(!real.loaders.includes('spigot') && !real.loaders.includes('bukkit'), 'Spigot/Bukkit cannot run DonutShards');
});

if (failed) {
  console.error(`${failed} case(s) failed`);
  process.exit(1);
}
console.log('all detect-jar-meta cases passed');
