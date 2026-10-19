(() => {
  const form = document.getElementById('adminLoginForm');
  const message = document.getElementById('adminLoginMessage');
  const submit = document.getElementById('adminLoginSubmit');

  form.addEventListener('submit', async event => {
    event.preventDefault();
    message.textContent = '';
    submit.disabled = true;
    try {
      const response = await fetch('/api/auth/admin/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify(Object.fromEntries(new FormData(form)))
      });
      const result = await response.json();
      if (!response.ok) {
        message.className = 'small text-danger';
        message.textContent = response.status === 403
          ? 'This account does not have administrator access.'
          : response.status === 401
            ? 'Email or password is incorrect.'
            : result.error || 'Sign in failed. Please try again.';
        return;
      }
      if (result.role !== 'ADMIN') {
        message.className = 'small text-danger';
        message.textContent = 'This account does not have administrator access.';
        return;
      }
      sessionStorage.setItem('cw_role', result.role);
      sessionStorage.setItem('cw_name', result.name);
      location.assign('/admin.html');
    } catch {
      message.className = 'small text-danger';
      message.textContent = 'Could not reach the server. Please try again.';
    } finally {
      submit.disabled = false;
    }
  });
})();
