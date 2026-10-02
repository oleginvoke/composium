# Composium website

A static English homepage and seven documentation pages for GitHub Pages.
The documentation content and library version are read directly from the root
`README.md` on every build. No backend or Android build is required.

## Local development

Use Node.js 24 and the pnpm version declared in `package.json`.

```sh
cd website
pnpm install --frozen-lockfile
pnpm test
pnpm build
pnpm check
pnpm dev
```

Open `http://127.0.0.1:4173/composium/`. The preview uses the same `/composium/`
path prefix as GitHub Pages. Rebuild and reload after editing source files.

## Editing

- Edit the root `README.md` to update documentation and the library version.
- Map new README level-two headings to a page in `scripts/source.mjs`. A build
  fails if a topic is missing, rather than silently dropping documentation.
- Edit homepage copy and layout in `scripts/build.mjs`.
- Edit styles, browser interactions, and the favicon in `public/`.
- `demo-poster.png` is the first frame of the existing `assets/composium-demo.gif`.
  The original GIF is copied at build time and starts only when Play is pressed.
- `dist/` is generated output and is ignored by Git.

## Publishing to GitHub Pages

1. Open the repository's **Settings → Pages**.
2. Set **Build and deployment → Source** to **GitHub Actions**.
3. Push these files to `master`, or run **Build and deploy documentation** from
   the Actions tab after merging. Pull requests only build and validate the site.
4. Confirm the deployment succeeded in Actions and open
   `https://oleginvoke.github.io/composium/`.

The workflow automatically rebuilds when the README, website, demo, or workflow
changes. Only the website output is published; repository files and local
configuration are excluded.

If the repository name, owner, or domain changes, update `origin` and `base` in
`scripts/build.mjs`, the preview prefix in `scripts/serve.mjs`, and the expected
URLs in `scripts/check.mjs` together.

## Search discovery

Every content page is rendered as complete HTML with a unique title,
description, canonical URL, and structured data. Pages link to one another and
are listed in `https://oleginvoke.github.io/composium/sitemap.xml`.

After publishing:

1. Add the website URL to the GitHub repository's About / Website field and
   link it from the root README.
2. Add a URL-prefix property for `https://oleginvoke.github.io/composium/` in
   Google Search Console, verify ownership, and submit `sitemap.xml`.
3. Request indexing of the homepage and getting-started page using URL
   Inspection. Share useful examples that link back to the documentation.

Ownership verification needs the repository owner's Search Console account.
If Google provides an HTML verification file, include it in the build output
at the exact requested location. A project-level `robots.txt` cannot control
the whole `oleginvoke.github.io` origin: crawlers look for robots at the origin
root, so submit the sitemap directly. Indexing and search placement are not
guaranteed by creating or submitting the site.
