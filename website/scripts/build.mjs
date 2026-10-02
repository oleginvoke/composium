import { readFile, writeFile, mkdir, cp, rm } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import MarkdownIt from 'markdown-it';
import Prism from 'prismjs';
import 'prismjs/components/prism-kotlin.js';
import { pages, splitReadme, getVersion } from './source.mjs';

const root = fileURLToPath(new URL('../../', import.meta.url));
const output = resolve(root, 'website/dist');
const origin = 'https://oleginvoke.github.io';
const base = '/composium/';
const repository = 'https://github.com/oleginvoke/composium';
const source = await readFile(resolve(root, 'README.md'), 'utf8');
const version = getVersion(source);
const sections = splitReadme(source);
const assigned = pages.flatMap(page => page.sections);
if (assigned.length !== new Set(assigned).size || assigned.length !== sections.size || assigned.some(section => !sections.has(section))) {
  throw new Error('Every README section must be assigned to exactly one documentation page. Update scripts/source.mjs.');
}
const escape = value => String(value).replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
const slugify = value => value.toLowerCase().replace(/[^a-z0-9\s-]/g, '').trim().replace(/\s+/g, '-');
const link = path => `${base}${path}`;
const arrow = '<span aria-hidden="true">↗</span>';
const github = '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M12 .8a11.2 11.2 0 0 0-3.54 21.83c.56.1.76-.24.76-.54v-2.1c-3.12.67-3.78-1.32-3.78-1.32-.51-1.3-1.24-1.65-1.24-1.65-1.02-.7.08-.68.08-.68 1.13.08 1.73 1.16 1.73 1.16 1 1.72 2.62 1.22 3.26.93.1-.73.4-1.22.71-1.5-2.49-.28-5.11-1.25-5.11-5.55 0-1.22.44-2.23 1.16-3.02-.12-.28-.5-1.42.11-2.96 0 0 .94-.3 3.08 1.15a10.7 10.7 0 0 1 5.62 0c2.14-1.45 3.08-1.15 3.08-1.15.61 1.54.23 2.68.11 2.96.72.79 1.16 1.8 1.16 3.02 0 4.31-2.62 5.27-5.12 5.54.4.35.76 1.03.76 2.08v3.1c0 .3.2.65.77.54A11.2 11.2 0 0 0 12 .8Z"/></svg>';
const logo = '<svg viewBox="0 0 32 32" width="32" height="32" fill="none" aria-hidden="true"><rect x="2" y="2" width="28" height="28" rx="9" fill="#163d34"/><path d="m16 7 9 5-9 5-9-5 9-5Z" fill="#d9f99d"/><path d="m7 16 9 5 9-5M7 21l9 5 9-5" stroke="#d9f99d" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';

const md = new MarkdownIt({ html: false, linkify: true });
md.renderer.rules.fence = (tokens, index) => {
  const token = tokens[index];
  const language = token.info.trim().split(/\s/)[0];
  const highlighted = Prism.languages[language] ? Prism.highlight(token.content, Prism.languages[language], language) : escape(token.content);
  return `<div class="code-block"><div class="code-toolbar"><span>${escape(language || 'Code')}</span><button type="button" class="copy-button" aria-label="Copy code example">Copy</button></div><pre><code class="language-${escape(language)}">${highlighted}</code></pre></div>`;
};
function code(value, language = 'kotlin') { return md.render(`\`\`\`${language}\n${value}\n\`\`\``); }
function renderMarkdown(value) {
  const tokens = md.parse(value, {});
  const headings = [];
  const ids = new Map();
  tokens.forEach((token, index) => {
    if (token.type !== 'heading_open') return;
    const inline = tokens[index + 1];
    const text = inline.children?.filter(child => child.type === 'text' || child.type === 'code_inline').map(child => child.content).join('') || inline.content;
    const stem = slugify(text);
    const count = ids.get(stem) || 0;
    ids.set(stem, count + 1);
    const id = count ? `${stem}-${count}` : stem;
    token.attrSet('id', id);
    headings.push({ id, title: text, depth: Number(token.tag.slice(1)) });
  });
  return { html: md.renderer.render(tokens, md.options, {}), headings };
}
function navigation(active = '') {
  let group;
  return pages.map(page => {
    const heading = group !== page.group ? `<p class="nav-group">${escape(page.group)}</p>` : '';
    group = page.group;
    return `${heading}<a href="${link(`docs/${page.slug}/`)}"${active === page.slug ? ' class="active" aria-current="page"' : ''}>${escape(page.title)}</a>`;
  }).join('');
}
function shell({ title, description, path = '', content, docs = false, noindex = false }) {
  const canonical = `${origin}${link(path)}`;
  const structured = path ? { '@context': 'https://schema.org', '@type': 'TechArticle', headline: title, description, url: canonical, inLanguage: 'en', isPartOf: { '@type': 'WebSite', name: 'Composium', url: `${origin}${base}` } } : { '@context': 'https://schema.org', '@type': 'SoftwareSourceCode', name: 'Composium', description, codeRepository: repository, programmingLanguage: 'Kotlin', runtimePlatform: 'Android / Jetpack Compose', license: `${repository}/blob/master/LICENSE`, version, url: canonical };
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${escape(title)}</title><meta name="description" content="${escape(description)}"><meta name="theme-color" content="#163d34">${noindex ? '<meta name="robots" content="noindex">' : ''}
<meta name="google-site-verification" content="m8LUDa-PXHL-mkHHA3IkZeDu6Iyc7RbwyMSdY1kcYYg" />
<link rel="canonical" href="${canonical}"><link rel="icon" type="image/svg+xml" href="${link('assets/favicon.svg')}"><link rel="stylesheet" href="${link('assets/style.css')}">
<meta property="og:type" content="website"><meta property="og:site_name" content="Composium"><meta property="og:title" content="${escape(title)}"><meta property="og:description" content="${escape(description)}"><meta property="og:url" content="${canonical}">
<meta name="twitter:card" content="summary"><meta name="twitter:title" content="${escape(title)}"><meta name="twitter:description" content="${escape(description)}">
<script type="application/ld+json">${JSON.stringify(structured).replace(/</g, '\\u003c')}</script><script src="${link('assets/site.js')}" defer></script></head>
<body class="${docs ? 'docs-page' : 'home-page'}"><a class="skip-link" href="#main">Skip to content</a>
<header class="site-header"><div class="header-inner"><a class="brand" href="${base}" aria-label="Composium home">${logo}<span>composium<span class="brand-dot">.</span></span></a>
<button class="menu-toggle" type="button" aria-expanded="false" aria-controls="main-nav">Menu <span aria-hidden="true">☰</span></button>
<nav id="main-nav" class="main-nav" aria-label="Main navigation"><a href="${link('')}#features">Features</a><a href="${link('docs/getting-started/')}"${docs ? ' aria-current="true"' : ''}>Documentation</a><a class="github-link" href="${repository}">${github} GitHub ${arrow}</a></nav></div></header>
${content}
<footer class="site-footer"><div class="footer-inner"><a class="brand footer-brand" href="${base}">${logo}<span>composium.</span></a><p>Made for Jetpack Compose. Built in the open.</p><div><a href="${repository}/blob/master/LICENSE">Apache 2.0</a><a href="https://central.sonatype.com/artifact/io.github.oleginvoke/composium">Maven Central ${arrow}</a><a href="${repository}">GitHub ${arrow}</a></div></div></footer>
<p id="copy-status" class="sr-only" role="status" aria-live="polite"></p></body></html>`;
}

const install = `plugins {
    id("com.google.devtools.ksp") version "<ksp-version>"
}

dependencies {
    implementation("io.github.oleginvoke:composium:${version}")
    ksp("io.github.oleginvoke:composium-processor:${version}")
}`;
const example = `import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import oleginvoke.com.composium.ComposiumScene
import oleginvoke.com.composium.scene

@ComposiumScene
internal val buttonScene by scene(
    group = "Buttons",
    name = "Primary",
) { contentPadding ->
    val enabled: Boolean by param(true)
    val label: String by param("Continue")

    Box(Modifier.padding(contentPadding)) {
        Button(onClick = {}, enabled = enabled) {
            Text(label)
        }
    }
}`;
const home = `<main id="main">
<section class="hero wrap"><div class="hero-copy"><p class="eyebrow"><span class="status-dot"></span> STORYBOOK FOR ANDROID JETPACK COMPOSE</p>
<h1>Your Compose UI.<br><em>Ready to explore.</em></h1><p class="hero-description">Turn your components into an interactive, in-app catalog. Explore real UI states, tweak parameters, and give your design system a place to live.</p>
<div class="hero-actions"><a class="button button-primary" href="${link('docs/getting-started/')}">Get started <span aria-hidden="true">→</span></a><a class="button button-text" href="${repository}">View on GitHub ${arrow}</a></div>
<p class="release"><span>v${version}</span> <span class="release-label">Pre-release</span><span class="release-separator">/</span> Available on Maven Central</p>
<div class="hero-note"><span class="mono">ComposiumScreen()</span><p>One composable. Anywhere in your app.</p></div></div>
<figure class="demo"><div class="demo-top"><span><span class="status-dot"></span> THE REAL THING</span><span>Android sample</span></div><div class="demo-stage"><div class="demo-label"><span>YOUR COMPONENTS</span><strong>A living<br>UI catalog.</strong><span class="demo-marker" aria-hidden="true">↘</span></div><div class="phone"><img id="demo-image" src="${link('assets/demo-poster.png')}" data-animation="${link('assets/composium-demo.gif')}" data-poster="${link('assets/demo-poster.png')}" alt="Composium Android catalog with nested scene groups, thumbnails, and a sample button" width="480" height="1004" fetchpriority="high"></div></div><figcaption><span>Browse scenes. Change values. See what happens.</span><button type="button" class="demo-toggle" aria-pressed="false"><span aria-hidden="true">▶</span> Play demo</button></figcaption></figure></section>
<div class="trust-strip wrap"><span>Native Jetpack Compose</span><span>KSP discovery · optional</span><span>Android minSdk 24</span><span>Apache 2.0 · open source</span></div>
<section id="features" class="features wrap"><div class="section-heading"><p class="eyebrow">A WORKSPACE FOR YOUR UI</p><h2>Less setup.<br>More exploration.</h2><p>Keep the components, states, and edge cases you care about together—inside the app you already build.</p></div>
<div class="feature-grid"><article><span class="feature-number">01 / CATALOG</span><h3>Every state, in one place.</h3><p>Organize scenes into searchable, nested groups. Automatic thumbnails and custom badges make your catalog easy to browse.</p><a href="${link('docs/scenes/')}">Explore scenes <span aria-hidden="true">→</span></a></article><article><span class="feature-number">02 / CONTROLS</span><h3>Change it. See it.</h3><p>Edit text, toggle booleans, and pick enum or sealed options at runtime. Bring your own choices for custom types.</p><a href="${link('docs/parameters/')}">Meet the parameters <span aria-hidden="true">→</span></a></article><article><span class="feature-number">03 / ENVIRONMENT</span><h3>Check the edge cases.</h3><p>Try dark theme, larger fonts, different display sizes, and RTL. Review your UI under the conditions your users experience.</p><a href="${link('docs/theming/')}">Adjust the environment <span aria-hidden="true">→</span></a></article></div></section>
<section id="quick-start" class="quick-start"><div class="wrap"><div class="section-heading"><p class="eyebrow">FROM COMPONENT TO CATALOG</p><h2>Compose code.<br>A whole new playground.</h2><p>Scenes are ordinary Compose code, with a few controls added. Your components keep their existing API.</p></div>
<div class="quick-start-grid"><div class="steps"><article><span class="step-number">1</span><div><h3>Add the dependencies</h3><p>Use <code>google()</code> and <code>mavenCentral()</code>, set <code>compileSdk 36</code>, and add the library and KSP processor.</p></div></article><article><span class="step-number">2</span><div><h3>Describe a scene</h3><p>Wrap your component in <code>scene { }</code>. Declare values with <code>param()</code> and let Composium build the controls.</p></div></article><article><span class="step-number">3</span><div><h3>Open your catalog</h3><p>Embed <code>ComposiumScreen()</code> in a debug route, activity, or internal tools screen. KSP discovers your annotated scenes.</p></div></article><a class="button button-primary" href="${link('docs/getting-started/')}">Follow the full setup <span aria-hidden="true">→</span></a><p class="setup-note">Prefer manual registration? <a href="${link('docs/getting-started/#quick-start-without-ksp')}">Skip KSP.</a></p></div>
<div class="example-stack"><div class="example-heading"><span class="file-dot"></span> build.gradle.kts</div>${code(install)}<p class="code-note">Choose a KSP2 version compatible with your Kotlin and AGP. <a href="${link('docs/getting-started/#requirements')}">See requirements →</a></p><details class="scene-example"><summary>See a complete button scene <span aria-hidden="true">+</span></summary>${code(example)}</details></div></div></div></section>
<section class="use-cases wrap"><div class="section-heading"><p class="eyebrow">ONE CATALOG, MANY REASONS</p><h2>For the people<br>behind the pixels.</h2></div><div class="use-case-list"><a href="${link('docs/use-cases/#design-system-catalog')}"><span>01</span><div><h3>Design systems</h3><p>A living reference for components, tokens, and their variations.</p></div><span aria-hidden="true">↗</span></a><a href="${link('docs/use-cases/#qa-handoff')}"><span>02</span><div><h3>QA & visual review</h3><p>Make loading, error, empty, and disabled states easy to reproduce.</p></div><span aria-hidden="true">↗</span></a><a href="${link('docs/use-cases/#local-experimentation')}"><span>03</span><div><h3>Everyday development</h3><p>A focused sandbox for the UI you are working on right now.</p></div><span aria-hidden="true">↗</span></a></div></section>
<section class="closing wrap"><div><p class="eyebrow">START WITH ONE SCENE</p><h2>Give your UI room to play.</h2><p>Small integration. A catalog that grows with your app.</p></div><a class="button button-primary" href="${link('docs/getting-started/')}">Build your first catalog <span aria-hidden="true">→</span></a></section></main>`;

// The directory is a fixed build output inside website/, never an input or user path.
await rm(output, { recursive: true, force: true });
await mkdir(resolve(output, 'assets'), { recursive: true });
await cp(resolve(root, 'website/public'), resolve(output, 'assets'), { recursive: true });
await cp(resolve(root, 'website/demo-poster.png'), resolve(output, 'assets/demo-poster.png'));
await cp(resolve(root, 'assets/composium-demo.gif'), resolve(output, 'assets/composium-demo.gif'));
await writeFile(resolve(output, 'index.html'), shell({ title: 'Composium — Storybook for Android Jetpack Compose', description: 'Build an interactive, in-app UI catalog for Android Jetpack Compose. Explore component states, tweak live parameters, and review your design system with Composium.', content: home }));

for (const [index, page] of pages.entries()) {
  const markdown = page.sections.map(section => sections.get(section)).join('\n\n');
  const rendered = renderMarkdown(markdown);
  const previous = pages[index - 1];
  const next = pages[index + 1];
  const toc = rendered.headings.map(heading => `<a class="toc-depth-${heading.depth}" href="#${heading.id}">${escape(heading.title)}</a>`).join('');
  const content = `<div class="docs-layout"><aside class="docs-sidebar"><div class="sidebar-title">DOCUMENTATION <span>v${version}</span></div><nav aria-label="Documentation">${navigation(page.slug)}</nav><a class="source-link" href="${repository}/blob/master/README.md">Read the source ${arrow}</a></aside>
<main id="main" class="docs-main"><details class="mobile-docs-nav"><summary>Documentation <span aria-hidden="true">⌄</span></summary><nav aria-label="Documentation pages">${navigation(page.slug)}</nav></details><p class="eyebrow">${escape(page.group)}</p><h1>${escape(page.title)}</h1><p class="doc-intro">${escape(page.description)}</p><div class="prose">${rendered.html}</div><div class="doc-edit"><span>Documentation follows the project README.</span><a href="${repository}/edit/master/README.md">Improve this page ${arrow}</a></div><nav class="doc-pagination" aria-label="Documentation pagination">${previous ? `<a href="${link(`docs/${previous.slug}/`)}"><span>← Previous</span><strong>${escape(previous.title)}</strong></a>` : '<span></span>'}${next ? `<a class="next" href="${link(`docs/${next.slug}/`)}"><span>Next →</span><strong>${escape(next.title)}</strong></a>` : ''}</nav></main>
<aside class="docs-toc"><p class="eyebrow">ON THIS PAGE</p><nav aria-label="On this page">${toc}</nav></aside></div>`;
  const directory = resolve(output, 'docs', page.slug);
  await mkdir(directory, { recursive: true });
  await writeFile(resolve(directory, 'index.html'), shell({ title: `${page.title} · Composium documentation`, description: page.description, path: `docs/${page.slug}/`, content, docs: true }));
}

const urls = ['', ...pages.map(page => `docs/${page.slug}/`)];
await writeFile(resolve(output, 'sitemap.xml'), `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls.map(path => `  <url><loc>${origin}${link(path)}</loc></url>`).join('\n')}\n</urlset>\n`);
// On a project Pages site this file lives under /composium/. Crawlers only
// discover robots.txt at the origin root; submit sitemap.xml directly instead.
await writeFile(resolve(output, 'robots.txt'), `User-agent: *\nAllow: /\nSitemap: ${origin}${base}sitemap.xml\n`);
await writeFile(resolve(output, '.nojekyll'), '');
await writeFile(resolve(output, '404.html'), shell({ title: 'Page not found · Composium', description: 'Find your way back to the Composium documentation.', path: '404.html', noindex: true, content: `<main id="main" class="not-found wrap"><p class="eyebrow">404 / PAGE NOT FOUND</p><h1>Let’s get you<br>back to the catalog.</h1><p>This page may have moved. The documentation is a good place to start.</p><a class="button button-primary" href="${link('docs/getting-started/')}">Open documentation →</a></main>` }));
console.log(`Built Composium ${version}: homepage + ${pages.length} documentation pages in website/dist.`);
