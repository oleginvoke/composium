import { createServer } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { resolve, sep, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(fileURLToPath(new URL('../dist/', import.meta.url)));
const base = '/composium';
const types = { '.html': 'text/html; charset=utf-8', '.css': 'text/css', '.js': 'text/javascript', '.svg': 'image/svg+xml', '.gif': 'image/gif', '.png': 'image/png', '.xml': 'application/xml', '.txt': 'text/plain' };
createServer(async (request, response) => {
  try {
    const url = new URL(request.url, 'http://localhost');
    if (url.pathname === '/' || url.pathname === base) {
      response.writeHead(302, { Location: `${base}/` }).end();
      return;
    }
    if (!url.pathname.startsWith(`${base}/`)) throw new Error('Not found');
    let path = resolve(root, '.' + decodeURIComponent(url.pathname.slice(base.length)));
    if (path !== root && !path.startsWith(root + sep)) throw new Error('Not found');
    if ((await stat(path)).isDirectory()) path = resolve(path, 'index.html');
    response.writeHead(200, { 'Content-Type': types[extname(path)] || 'application/octet-stream', 'Cache-Control': 'no-store' });
    response.end(await readFile(path));
  } catch {
    response.writeHead(404, { 'Content-Type': 'text/plain' }).end('Not found');
  }
}).listen(4173, '127.0.0.1', () => console.log('Composium preview: http://127.0.0.1:4173/composium/'));
