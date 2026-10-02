import assert from 'node:assert/strict';
import { readFile, readdir, stat } from 'node:fs/promises';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { pages, getVersion } from './source.mjs';

const root = resolve(fileURLToPath(new URL('../dist/', import.meta.url)));
const base = '/composium/';
const origin = 'https://oleginvoke.github.io';
const version = getVersion(await readFile(new URL('../../README.md', import.meta.url), 'utf8'));
async function files(directory) {
  const result = [];
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = resolve(directory, entry.name);
    if (entry.isDirectory()) result.push(...await files(path));
    else if (entry.name.endsWith('.html')) result.push(path);
  }
  return result;
}
const htmlFiles = await files(root);
const html = new Map(await Promise.all(htmlFiles.map(async path => [path, await readFile(path, 'utf8')])));
let links = 0;
for (const [path, content] of html) {
  assert.match(content, /<html lang="en">/, `Language missing: ${path}`);
  assert.equal((content.match(/<h1[\s>]/g) || []).length, 1, `Expected one h1: ${path}`);
  assert.match(content, /<meta name="description" content="[^"]+">/, `Description missing: ${path}`);
  assert.match(content, /<link rel="canonical" href="https:\/\/oleginvoke.github.io\/composium\//, `Canonical missing: ${path}`);
  const structured = content.match(/<script type="application\/ld\+json">(.*?)<\/script>/s);
  assert.ok(structured, `Structured data missing: ${path}`);
  JSON.parse(structured[1]);
  const ids = [...content.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]);
  assert.equal(new Set(ids).size, ids.length, `Duplicate heading or element ids: ${path}`);
  for (const match of content.matchAll(/\b(?:href|src)="([^"]+)"/g)) {
    const target = new URL(match[1].replace(/&amp;/g, '&'), `${origin}${base}${path.slice(root.length + 1).replaceAll('\\', '/')}`);
    if (target.origin !== origin) continue;
    assert.ok(target.pathname.startsWith(base), `Asset escapes project prefix: ${target}`);
    let destination = resolve(root, '.' + decodeURIComponent(target.pathname.slice(base.length - 1)));
    assert.ok(destination.startsWith(root), `Asset escapes build output: ${target}`);
    if ((await stat(destination)).isDirectory()) destination = resolve(destination, 'index.html');
    assert.ok(await stat(destination), `Broken local link: ${target}`);
    if (target.hash) {
      const destinationHtml = html.get(destination);
      assert.ok(destinationHtml?.includes(`id="${decodeURIComponent(target.hash.slice(1))}"`), `Broken anchor ${target} in ${path}`);
    }
    links++;
  }
}
const homepage = await readFile(resolve(root, 'index.html'), 'utf8');
assert.ok(homepage.includes(`composium:${version}`), 'Homepage installation version differs from README.');
assert.ok(!homepage.includes('og:image'), 'No social image was requested.');
const sitemap = await readFile(resolve(root, 'sitemap.xml'), 'utf8');
assert.equal((sitemap.match(/<loc>/g) || []).length, pages.length + 1);
for (const page of pages) assert.ok(sitemap.includes(`${origin}${base}docs/${page.slug}/`));
assert.equal(htmlFiles.length, pages.length + 2);
console.log(`Checked ${htmlFiles.length} HTML pages and ${links} local links/anchors: metadata, structured data, sitemap, assets, and README version are valid.`);
