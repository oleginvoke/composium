const menu = document.querySelector('.menu-toggle');
const navigation = document.querySelector('.main-nav');
menu?.addEventListener('click', () => {
  const expanded = menu.getAttribute('aria-expanded') !== 'true';
  menu.setAttribute('aria-expanded', String(expanded));
  navigation.classList.toggle('is-open', expanded);
});
navigation?.addEventListener('click', event => {
  if (!event.target.closest('a')) return;
  menu.setAttribute('aria-expanded', 'false');
  navigation.classList.remove('is-open');
});
document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && menu?.getAttribute('aria-expanded') === 'true') {
    menu.setAttribute('aria-expanded', 'false');
    navigation.classList.remove('is-open');
    menu.focus();
  }
});

document.querySelectorAll('.copy-button').forEach(button => {
  button.addEventListener('click', async () => {
    const value = button.closest('.code-block').querySelector('code').textContent;
    const status = document.getElementById('copy-status');
    try {
      await navigator.clipboard.writeText(value);
      button.textContent = 'Copied!';
      status.textContent = 'Code copied to clipboard.';
    } catch {
      button.textContent = 'Select code';
      const selection = window.getSelection();
      const range = document.createRange();
      range.selectNodeContents(button.closest('.code-block').querySelector('code'));
      selection.removeAllRanges();
      selection.addRange(range);
      status.textContent = 'Clipboard unavailable. Code selected; use your copy shortcut.';
    }
    clearTimeout(button.resetTimer);
    button.resetTimer = setTimeout(() => { button.textContent = 'Copy'; }, 2400);
  });
});

const demo = document.getElementById('demo-image');
const toggle = document.querySelector('.demo-toggle');
toggle?.addEventListener('click', () => {
  const play = toggle.getAttribute('aria-pressed') !== 'true';
  demo.src = play ? demo.dataset.animation : demo.dataset.poster;
  toggle.setAttribute('aria-pressed', String(play));
  toggle.innerHTML = play ? '<span aria-hidden="true">Ⅱ</span> Stop demo' : '<span aria-hidden="true">▶</span> Play demo';
});

const headings = document.querySelectorAll('.prose h2[id], .prose h3[id]');
if (headings.length && 'IntersectionObserver' in window) {
  const observer = new IntersectionObserver(entries => {
    const entry = entries.find(item => item.isIntersecting);
    if (!entry) return;
    document.querySelectorAll('.docs-toc a').forEach(anchor => {
      const active = anchor.hash === `#${entry.target.id}`;
      anchor.classList.toggle('active', active);
      if (active) anchor.setAttribute('aria-current', 'location');
      else anchor.removeAttribute('aria-current');
    });
  }, { rootMargin: '-90px 0px -65% 0px' });
  headings.forEach(heading => observer.observe(heading));
}
