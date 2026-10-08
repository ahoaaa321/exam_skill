/* ===== 控制台模块快捷导航：精准定位 + 滚动高亮（各控制台页共用） ===== */
(function () {
  var nav = document.getElementById('module-nav');
  if (!nav) return;
  var chips = Array.prototype.slice.call(nav.querySelectorAll('.module-chip'));
  var sections = chips
      .map(function (c) { return document.getElementById(c.dataset.target); })
      .filter(Boolean);
  var navInner = nav.querySelector('.module-nav-inner') || nav;
  var clickLockUntil = 0;   // 点击跳转后的短暂锁定期，避免滚动过程中高亮抖动
  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var scrollingEl = document.scrollingElement || document.documentElement;
  var rafId = null;
  function stickyOffset() {
    // 顶栏 68px + 模块条高度，再留 12px 呼吸间距
    var navH = nav.getBoundingClientRect().height;
    return 68 + navH + 12;
  }

  /* 自定义滚动动画：固定时长、距离无关，用计时器时间轴驱动（后台标签/rAF 节流时依然准确）。
     每帧根据目标区块实时位置重算，可自动吸收异步数据渲染导致的布局位移 */
  function scrollToSection(sec, done) {
    if (rafId) clearTimeout(rafId);
    var prevBehavior = scrollingEl.style.scrollBehavior;
    scrollingEl.style.scrollBehavior = 'auto';
    function finish() { scrollingEl.style.scrollBehavior = prevBehavior; if (done) done(); }
    if (reduceMotion) {
      scrollingEl.scrollTop = Math.max(0, sec.getBoundingClientRect().top + scrollingEl.scrollTop - stickyOffset());
      finishSettle(); return;
    }
    var startY = scrollingEl.scrollTop;
    var startTarget = Math.max(0, sec.getBoundingClientRect().top + startY - stickyOffset());
    var diff = startTarget - startY;
    if (Math.abs(diff) < 2) { finishSettle(); return; }
    var duration = 480;
    var startTime = Date.now();
    function ease(t) { return t < .5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2; }
    function step() {
      var p = Math.min(1, (Date.now() - startTime) / duration);
      // 以起点位移量做动画，再叠加区块在动画期间的实时位移修正
      var liveTarget = Math.max(0, sec.getBoundingClientRect().top + scrollingEl.scrollTop - stickyOffset());
      var correction = (liveTarget - (startY + diff)) * p;
      scrollingEl.scrollTop = startY + diff * ease(p) + correction;
      if (p < 1) { rafId = setTimeout(step, 16); }
      else { scrollingEl.scrollTop = liveTarget; rafId = null; finishSettle(); }
    }
    step();

    /* 落点自校正：图表/表格异步渲染可能在动画结束后继续推挤布局，
       800ms 内若用户未手动滚动则做两次静默回补 */
    var interrupted = false;
    function onInterrupt() { interrupted = true; }
    window.addEventListener('wheel', onInterrupt, { passive: true, once: true });
    window.addEventListener('touchmove', onInterrupt, { passive: true, once: true });
    window.addEventListener('keydown', onInterrupt, { once: true });
    function repin() {
      if (interrupted) return;
      var delta = sec.getBoundingClientRect().top - stickyOffset();
      if (Math.abs(delta) > 2) scrollingEl.scrollTop = Math.max(0, scrollingEl.scrollTop + delta);
      setTimeout(spy, 40);   // 回补后重算高亮（重排可能改变了当前模块）
    }
    function finishSettle() {
      finish();
      setTimeout(repin, 250);
      setTimeout(repin, 650);
    }
  }

  function setActive(id, scrollChip) {
    chips.forEach(function (c) { c.classList.toggle('active', c.dataset.target === id); });
    if (scrollChip) {
      var active = nav.querySelector('.module-chip.active');
      if (active) {
        var targetLeft = active.offsetLeft - navInner.clientWidth / 2 + active.clientWidth / 2;
        navInner.scrollTo({ left: targetLeft });
      }
    }
  }

  function goTo(id, pushHash) {
    var sec = document.getElementById(id);
    if (!sec) return;
    setActive(id, true);
    if (pushHash !== false) history.replaceState(null, '', '#' + id);
    clickLockUntil = Date.now() + 620;
    scrollToSection(sec);
  }

  chips.forEach(function (chip) {
    chip.addEventListener('click', function (e) {
      e.preventDefault();
      goTo(chip.dataset.target);
    });
  });

  /* scroll-spy：取视口顶部参考线下方最近的一个模块 */
  function spy() {
    if (Date.now() < clickLockUntil) return;
    var line = stickyOffset() + 6;   // 比定位线略宽，定位后立即判定为当前模块
    var current = sections[0] ? sections[0].id : null;
    for (var i = 0; i < sections.length; i++) {
      if (sections[i].getBoundingClientRect().top <= line) current = sections[i].id;
    }
    setActive(current, false);
  }
  var ticking = false;
  window.addEventListener('scroll', function () {
    if (!ticking) {
      ticking = true;
      setTimeout(function () { spy(); ticking = false; }, 60);
    }
  }, { passive: true });

  /* 打开页面时若带 hash，精准跳到对应模块；异步数据加载完成后再校正一次 */
  if (location.hash) {
    var hashId = location.hash.slice(1);
    if (document.getElementById(hashId)) {
      setTimeout(function () { goTo(hashId, false); }, 120);
      window.addEventListener('load', function () {
        setTimeout(function () {
          var sec = document.getElementById(hashId);
          if (sec && Math.abs(sec.getBoundingClientRect().top - stickyOffset()) > 4) goTo(hashId, false);
        }, 400);
      });
    }
  } else {
    spy();
  }
})();
