(() => {
  try {
    document.querySelectorAll('.flash-message').forEach((flash) => {
      try {
        const close = document.createElement('button');
        close.type = 'button'; close.className = 'flash-close'; close.setAttribute('aria-label', '关闭提示'); close.textContent = '×';
        const dismiss = () => { try { flash.classList.add('is-leaving'); window.setTimeout(() => flash.remove(), 190); } catch (_) {} };
        close.addEventListener('click', dismiss); flash.append(close);
        if (!flash.classList.contains('is-error') && flash.getAttribute('role') !== 'alert') window.setTimeout(dismiss, 4000);
      } catch (_) {}
    });
  } catch (_) {}
  try {
    document.addEventListener('submit', (event) => {
      try {
        const action = event.target && event.target.action ? new URL(event.target.action) : null;
        const match = action && action.pathname.match(/\/admin\/(products|categories|customers|orders)\/(\d+)/);
        if (match) sessionStorage.setItem('admin-motion-target', `row-${match[1]}-${match[2]}`);
      } catch (_) {}
    }, true);
  } catch (_) {}
  try {
    if (document.querySelector('.flash-message:not(.is-error)')) {
      const target = sessionStorage.getItem('admin-motion-target'); sessionStorage.removeItem('admin-motion-target');
      const row = target ? document.getElementById(target) : null;
      if (row) { history.replaceState(null, '', `#${target}`); row.scrollIntoView({ block: 'center' }); }
    }
  } catch (_) {}
})();
