/* ===== Mouse-following gradient orbs (shared across auth pages) ===== */
(function () {
  const orbs = document.querySelectorAll('.auth-orb');
  if (!orbs.length) return;
  const base = Array.from(orbs).map(o => ({
    el: o,
    x: parseFloat(getComputedStyle(o).left) || 0,
    y: parseFloat(getComputedStyle(o).top) || 0
  }));
  let mx = window.innerWidth / 2, my = window.innerHeight / 2;
  let curX = mx, curY = my, raf = null;

  window.addEventListener('mousemove', e => {
    mx = e.clientX; my = e.clientY;
    if (!raf) raf = requestAnimationFrame(update);
  }, { passive: true });

  function update() {
    curX += (mx - curX) * 0.06;
    curY += (my - curY) * 0.06;
    base.forEach((b, i) => {
      const depth = 0.025 + i * 0.015;
      const dx = (curX - window.innerWidth / 2) * depth;
      const dy = (curY - window.innerHeight / 2) * depth;
      b.el.style.transform = `translate3d(${dx}px, ${dy}px, 0)`;
    });
    if (Math.abs(curX - mx) > 0.5 || Math.abs(curY - my) > 0.5) {
      raf = requestAnimationFrame(update);
    } else { raf = null; }
  }
})();
