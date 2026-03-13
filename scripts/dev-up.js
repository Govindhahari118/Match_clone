#!/usr/bin/env node

const { spawn } = require('node:child_process');
const os = require('node:os');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');

const args = process.argv.slice(2);
const noDocker = args.includes('--no-docker');
const isWindows = os.platform() === 'win32';
const rootDir = path.resolve(__dirname, '..');
const runDir = path.join(rootDir, '.run');
const pidFile = path.join(runDir, 'frontend-dev.pid');
const logFile = path.join(runDir, 'frontend-dev.log');

function run(cmd, cmdArgs, opts = {}) {
  const child = spawn(cmd, cmdArgs, {
    stdio: 'inherit',
    shell: false,
    ...opts,
  });
  child.on('exit', (code) => process.exit(code ?? 0));
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function checkLocalhost8000() {
  return new Promise((resolve) => {
    const req = http.get('http://127.0.0.1:8000', { timeout: 1800 }, (res) => {
      res.resume();
      resolve(true);
    });
    req.on('timeout', () => {
      req.destroy();
      resolve(false);
    });
    req.on('error', () => resolve(false));
  });
}

function readExistingPid() {
  try {
    if (!fs.existsSync(pidFile)) return null;
    const pid = Number(fs.readFileSync(pidFile, 'utf8').trim());
    return Number.isFinite(pid) && pid > 0 ? pid : null;
  } catch {
    return null;
  }
}

function isProcessAlive(pid) {
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    return false;
  }
}

async function ensureFrontendOnLinux() {
  const alreadyUp = await checkLocalhost8000();
  if (alreadyUp) {
    console.log('[dev-up] Frontend already reachable at http://localhost:8000');
    return;
  }

  fs.mkdirSync(runDir, { recursive: true });

  const existingPid = readExistingPid();
  if (existingPid && isProcessAlive(existingPid)) {
    console.log(`[dev-up] Found existing frontend PID ${existingPid}. Waiting for readiness...`);
  } else {
    const outFd = fs.openSync(logFile, 'a');
    const errFd = fs.openSync(logFile, 'a');

    const child = spawn('npm', ['run', 'dev', '--prefix', 'frontend'], {
      cwd: rootDir,
      detached: true,
      stdio: ['ignore', outFd, errFd],
      shell: false,
    });

    fs.writeFileSync(pidFile, String(child.pid));
    child.unref();
    console.log(`[dev-up] Started frontend (PID ${child.pid}).`);
    console.log(`[dev-up] Logs: ${path.relative(rootDir, logFile)}`);
  }

  for (let i = 0; i < 30; i += 1) {
    if (await checkLocalhost8000()) {
      console.log('[dev-up] Frontend is up at http://localhost:8000');
      return;
    }
    await wait(1000);
  }

  console.error('[dev-up] Frontend did not become ready within 30 seconds.');
  console.error(`[dev-up] Check logs: ${path.relative(rootDir, logFile)}`);
  process.exit(1);
}

async function main() {
  if (isWindows) {
    const psArgs = [
      '-ExecutionPolicy',
      'Bypass',
      '-File',
      './scripts/dev-up.ps1',
    ];
    if (noDocker) psArgs.push('-NoDocker');
    run('powershell', psArgs);
    return;
  }

  console.log('[dev-up] Non-Windows environment detected.');
  if (!noDocker) {
    console.log('[dev-up] Docker-backed backend bootstrap is skipped on Linux in this script.');
    console.log('[dev-up] If docker is available, start infra/backend manually.');
  }

  await ensureFrontendOnLinux();
}

main();
