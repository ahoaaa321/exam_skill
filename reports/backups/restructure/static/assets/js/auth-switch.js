/* ===== 登录页「其他身份登录」下拉切换（四页共用） ===== */
(function () {
  document.querySelectorAll('.auth-switch').forEach(function (sw) {
    var btn = sw.querySelector('.auth-switch-btn');
    if (!btn) return;
    btn.setAttribute('aria-haspopup', 'true');
    btn.setAttribute('aria-expanded', 'false');

    function close() {
      sw.classList.remove('open');
      btn.setAttribute('aria-expanded', 'false');
    }

    btn.addEventListener('click', function (e) {
      e.stopPropagation();
      var open = sw.classList.toggle('open');
      btn.setAttribute('aria-expanded', String(open));
    });

    sw.addEventListener('click', function (e) { e.stopPropagation(); });
    document.addEventListener('click', function () {
      if (sw.classList.contains('open')) close();
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && sw.classList.contains('open')) close();
    });
  });
})();
