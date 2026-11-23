(() => {
  try {
    const seen = new Set();
    document.querySelectorAll('a[href*="Product.html?"]').forEach((link) => {
      try { const id = new URL(link.href).searchParams.get('sku'); const image = link.querySelector('img'); if (id && image && !seen.has(id)) { seen.add(id); image.style.viewTransitionName = `product-${id.replace(/[^a-zA-Z0-9_-]/g, '-')}`; } link.addEventListener('pointerdown', () => { try { document.querySelectorAll('[style*="view-transition-name: product-"]').forEach((node) => { node.style.viewTransitionName = ''; }); if (id && image) image.style.viewTransitionName = `product-${id.replace(/[^a-zA-Z0-9_-]/g, '-')}`; } catch (_) {} }); } catch (_) {}
    });
    const hero = document.querySelector('#productImage'); const sku = new URL(location.href).searchParams.get('sku');
    if (hero && sku) { document.querySelectorAll('[style*="view-transition-name: product-"]').forEach((node) => { node.style.viewTransitionName = ''; }); hero.style.viewTransitionName = `product-${sku.replace(/[^a-zA-Z0-9_-]/g, '-')}`; }
  } catch (_) {}
  try {
    const badge = document.querySelector('.cart-link b');
    if (badge) { const count = Number.parseInt(badge.textContent, 10) || 0; const before = Number.parseInt(sessionStorage.getItem('cart-count') || '', 10); if (Number.isFinite(before) && count > before) badge.classList.add('is-bumping'); sessionStorage.setItem('cart-count', String(count)); }
  } catch (_) {}
  try {
    document.querySelectorAll('.inline-qty button').forEach((button) => button.addEventListener('click', () => { try { const input = button.parentElement.querySelector('input'); if (input) { input.classList.remove('qty-changing'); void input.offsetWidth; input.classList.add('qty-changing'); } } catch (_) {} }));
  } catch (_) {}
  try {
    document.querySelectorAll('[data-qty-step]').forEach((button) => button.addEventListener('click', () => { try { const input = button.parentElement.querySelector('input'); const step = Number(button.dataset.qtyStep); const value = Math.min(99, Math.max(1, (Number(input.value) || 1) + step)); input.value = String(value); input.dispatchEvent(new Event('change', { bubbles: true })); } catch (_) {} }));
  } catch (_) {}
  try {
    document.querySelectorAll('.flash, .flash-message, [data-flash]').forEach((flash) => { try { const close = document.createElement('button'); close.type = 'button'; close.className = 'motion-close'; close.setAttribute('aria-label', '关闭提示'); close.textContent = '×'; const dismiss = () => { try { flash.classList.add('is-leaving'); setTimeout(() => flash.remove(), 190); } catch (_) {} }; close.addEventListener('click', dismiss); flash.append(close); if (flash.getAttribute('role') !== 'alert' && !flash.classList.contains('is-error')) setTimeout(dismiss, 4000); } catch (_) {} });
  } catch (_) {}
})();
