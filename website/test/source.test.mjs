import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { splitReadme, getVersion, pages } from '../scripts/source.mjs';

test('README sections ignore headings inside fenced examples', () => {
  const source = '# Library\n\n## Installation\n\n```markdown\n## Example heading\n```\n\n## Usage\nHello\n';
  const sections = splitReadme(source);
  assert.equal(sections.size, 2);
  assert.match(sections.get('Installation'), /## Example heading/);
  assert.equal(sections.get('Usage'), '## Usage\nHello\n');
});

test('every README topic is assigned to exactly one documentation page', async () => {
  const sections = splitReadme(await readFile(new URL('../../README.md', import.meta.url), 'utf8'));
  const assigned = pages.flatMap(page => page.sections);
  assert.equal(new Set(assigned).size, assigned.length);
  assert.deepEqual([...assigned].sort(), [...sections.keys()].sort());
});

test('installation version comes from README and must be explicit', () => {
  assert.equal(getVersion('Version: `1.3.0-alpha02` (pre-release)'), '1.3.0-alpha02');
  assert.throws(() => getVersion('No version'), /version/i);
});
