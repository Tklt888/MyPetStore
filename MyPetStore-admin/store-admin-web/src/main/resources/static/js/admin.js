(() => {
    const dialog = document.querySelector('#confirm-dialog');
    if (!dialog || typeof dialog.showModal !== 'function') return;
    const title = dialog.querySelector('[data-dialog-title]');
    const message = dialog.querySelector('[data-dialog-message]');
    const confirmButton = dialog.querySelector('[data-dialog-confirm]');
    let pendingForm = null;

    document.addEventListener('submit', event => {
        const form = event.target.closest('form[data-confirm]');
        if (!form || form.dataset.confirmed === 'true') return;
        event.preventDefault();
        pendingForm = form;
        title.textContent = form.dataset.confirmTitle || '确认操作';
        message.textContent = form.dataset.confirm || '确定继续执行此操作吗？';
        confirmButton.textContent = form.dataset.confirmLabel || '确认';
        dialog.showModal();
    });

    confirmButton.addEventListener('click', () => {
        if (!pendingForm) return;
        const form = pendingForm;
        form.dataset.confirmed = 'true';
        pendingForm = null;
        dialog.close('confirm');
        form.requestSubmit();
    });
    dialog.addEventListener('close', () => { if (dialog.returnValue !== 'confirm') pendingForm = null; });
})();
