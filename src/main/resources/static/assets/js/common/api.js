// 全局 HTML 转义工具：所有写入 innerHTML 的动态文本必须经此函数，防止存储型/反射型 XSS
window.escHtml = function (s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
};

const API = {
    baseURL: '',

    async request(path, options = {}) {
        const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
        const token = this.getToken();
        if (token) headers['Authorization'] = 'Bearer ' + token;
        const response = await fetch(this.baseURL + '/api' + path, { headers, ...options });
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(body.message || '请求失败');
        }
        if (body.code !== undefined && body.code !== 200) {
            throw new Error(body.message || '操作失败');
        }
        return body.data !== undefined ? body.data : body;
    },

    getToken() {
        try { return JSON.parse(localStorage.getItem('exam-user') || '{}').token; } catch { return null; }
    },

    // ============ Auth ============
    login(data) { return this.request('/auth/login', { method: 'POST', body: JSON.stringify(data) }); },
    register(data) { return this.request('/auth/register', { method: 'POST', body: JSON.stringify(data) }); },
    sendRegisterCode(email) { return this.request('/auth/register/send-code', { method: 'POST', body: JSON.stringify({ email }) }); },
    sendResetCode(email, username) { return this.request('/auth/password/send-code', { method: 'POST', body: JSON.stringify({ email, username }) }); },
    resetPassword(data) { return this.request('/auth/password/reset', { method: 'POST', body: JSON.stringify(data) }); },
    resetPasswordByToken(data) { return this.request('/auth/password/reset-by-token', { method: 'POST', body: JSON.stringify(data) }); },
    changePassword(data) { return this.request('/auth/password/change', { method: 'POST', body: JSON.stringify(data) }); },

    // ============ Public ============
    plans() { return this.request('/public/plans'); },
    announcements() { return this.request('/public/announcements'); },
    trades() { return this.request('/public/trades'); },
    levels() { return this.request('/public/levels'); },
    verifyCertificate(no) { return this.request('/public/certificates/verify?no=' + encodeURIComponent(no)); },

    // ============ Exam plans ============
    allPlans() { return this.request('/admin/plans'); },
    adminPlans() { return this.allPlans(); },
    createPlan(data) { return this.request('/admin/plans', { method: 'POST', body: JSON.stringify(data) }); },
    updatePlan(id, data) { return this.request('/admin/plans/' + id, { method: 'PUT', body: JSON.stringify(data) }); },
    deletePlan(id) { return this.request('/admin/plans/' + id, { method: 'DELETE' }); },
    updatePlanStatus(id, status) { return this.request('/admin/plans/' + id + '/status?status=' + status, { method: 'PUT' }); },

    // ============ Registrations ============
    registrations() { return this.request('/registrations'); },
    myRegistrations() { return this.request('/my/registrations'); },
    apply(data) { return this.request('/registrations', { method: 'POST', body: JSON.stringify(data) }); },
    cancelRegistration(id) { return this.request('/my/registrations/' + id + '/cancel', { method: 'PUT' }); },
    timeline(id) { return this.request('/registrations/' + id + '/timeline'); },
    auditRecords(id) { return this.request('/registrations/' + id + '/audit-records'); },
    audit(id, approved, reason, secondAudit) { return this.request('/registrations/' + id + '/audit', { method: 'PUT', body: JSON.stringify({ approved, reason, secondAudit }) }); },
    pay(id, payMethod) { return this.request('/registrations/' + id + '/pay?payMethod=' + (payMethod || 'wechat'), { method: 'PUT' }); },
    confirmPay(id) { return this.request('/admin/registrations/' + id + '/confirm-pay', { method: 'PUT' }); },
    payments() { return this.request('/admin/payments'); },
    ticket(id) { return this.request('/registrations/' + id + '/ticket'); },

    // ============ Arrangements ============
    arrange() { return this.request('/arrangements', { method: 'POST' }); },
    resetArrangements() { return this.request('/arrangements/reset', { method: 'POST' }); },
    arrangements() { return this.request('/arrangements'); },
    updateArrangement(id, data) { return this.request('/arrangements/' + id, { method: 'PUT', body: JSON.stringify(data) }); },
    statistics() { return this.request('/statistics'); },

    // ============ Materials ============
    addMaterial(regId, data) { return this.request('/registrations/' + regId + '/materials', { method: 'POST', body: JSON.stringify(data) }); },
    getMaterials(regId) { return this.request('/registrations/' + regId + '/materials'); },
    deleteMaterial(id) { return this.request('/materials/' + id, { method: 'DELETE' }); },

    // ============ Sign-in ============
    signins() { return this.request('/signins'); },
    doSignin(arrId, type) { return this.request('/signins/' + arrId + '/signin?type=' + (type || 1), { method: 'PUT' }); },
    markAbsent(arrId) { return this.request('/signins/' + arrId + '/absent', { method: 'PUT' }); },
    // 考生自助签到（数据仅限本人，服务端做归属校验）
    mySignins() { return this.request('/my/signins'); },
    mySignin(arrId) { return this.request('/my/signins/' + arrId + '/signin', { method: 'PUT' }); },

    // ============ Rooms ============
    rooms() { return this.request('/rooms'); },
    createRoom(data) { return this.request('/rooms', { method: 'POST', body: JSON.stringify(data) }); },
    updateRoom(id, data) { return this.request('/rooms/' + id, { method: 'PUT', body: JSON.stringify(data) }); },
    toggleRoomStatus(id, status) { return this.request('/rooms/' + id + '/status?status=' + status, { method: 'PUT' }); },
    deleteRoom(id) { return this.request('/rooms/' + id, { method: 'DELETE' }); },

    // ============ Scores ============
    myScores() { return this.request('/scores/my'); },
    allScores() { return this.request('/scores'); },
    scoreStats() { return this.request('/scores/stats'); },
    saveScore(data) { return this.request('/scores', { method: 'POST', body: JSON.stringify(data) }); },
    publishScore(id) { return this.request('/scores/' + id + '/publish', { method: 'PUT' }); },
    scoreChangeLogs(id) { return this.request('/scores/' + id + '/change-logs'); },

    // ============ Certificates ============
    myCertificates() { return this.request('/certificates/my'); },
    allCertificates() { return this.request('/admin/certificates'); },
    issueCertificate(data) { return this.request('/admin/certificates', { method: 'POST', body: JSON.stringify(data) }); },
    revokeCertificate(id) { return this.request('/admin/certificates/' + id + '/revoke', { method: 'PUT' }); },

    // ============ Arrangement config ============
    getArrConfig(planId) { return this.request('/arrangements/config/' + planId); },
    saveArrConfig(planId, data) { return this.request('/arrangements/config/' + planId, { method: 'PUT', body: JSON.stringify(data) }); },

    // ============ Invigilators ============
    invigilators() { return this.request('/invigilators'); },
    addInvigilator(data) { return this.request('/invigilators', { method: 'POST', body: JSON.stringify(data) }); },
    deleteInvigilator(id) { return this.request('/invigilators/' + id, { method: 'DELETE' }); },

    // ============ System ============
    me() { return this.request('/system/me'); },
    updateProfile(data) { return this.request('/system/me/profile', { method: 'PUT', body: JSON.stringify(data) }); },
    messages() { return this.request('/system/messages'); },
    unreadCount() { return this.request('/system/messages/unread-count'); },
    readMessage(id) { return this.request('/system/messages/' + id + '/read', { method: 'PUT' }); },
    readAllMessages() { return this.request('/system/messages/read-all', { method: 'PUT' }); },
    allAnnouncements() { return this.request('/system/announcements'); },
    createAnnouncement(data) { return this.request('/system/announcements', { method: 'POST', body: JSON.stringify(data) }); },
    updateAnnouncementStatus(id, status) { return this.request('/system/announcements/' + id + '/status?status=' + status, { method: 'PUT' }); },
    users() { return this.request('/system/users'); },
    createUser(data) { return this.request('/system/users', { method: 'POST', body: JSON.stringify(data) }); },
    toggleUserStatus(id, status) { return this.request('/system/users/' + id + '/status?status=' + status, { method: 'PUT' }); },
    updateUserRole(id, role) { return this.request('/system/users/' + id + '/role?role=' + role, { method: 'PUT' }); },
    resetUserPassword(id) { return this.request('/system/users/' + id + '/reset-password', { method: 'PUT' }); },
    configs() { return this.request('/system/configs'); },
    updateConfig(data) { return this.request('/system/configs', { method: 'PUT', body: JSON.stringify(data) }); },
    databaseHealth() { return this.request('/system/database-health'); },
    allTrades() { return this.request('/system/trades'); },
    createTrade(data) { return this.request('/system/trades', { method: 'POST', body: JSON.stringify(data) }); },
    updateTrade(id, data) { return this.request('/system/trades/' + id, { method: 'PUT', body: JSON.stringify(data) }); },
    deleteTrade(id) { return this.request('/system/trades/' + id, { method: 'DELETE' }); },
    toggleTradeStatus(id, status) { return this.request('/system/trades/' + id + '/status?status=' + status, { method: 'PUT' }); },
    allLevels() { return this.request('/system/levels'); },
    createLevel(data) { return this.request('/system/levels', { method: 'POST', body: JSON.stringify(data) }); },
    updateLevel(id, data) { return this.request('/system/levels/' + id, { method: 'PUT', body: JSON.stringify(data) }); },
    deleteLevel(id) { return this.request('/system/levels/' + id, { method: 'DELETE' }); },
    toggleLevelStatus(id, status) { return this.request('/system/levels/' + id + '/status?status=' + status, { method: 'PUT' }); },
    logs(operator, type) { return this.request('/system/logs?operator=' + (operator || '') + '&type=' + (type || '')); },
    loginLogs(limit) { return this.request('/system/login-logs?limit=' + (limit || 100)); },

    // ============ 统计 ============
    userRegistrationMonthly(year) { return this.request('/system/stats/user-registration?year=' + year); },
    registrationCategorySummary() { return this.request('/system/stats/registration-category'); },

    // ============ 公告删除/导入 ============
    deleteAnnouncement(id) { return this.request('/system/announcements/' + id, { method: 'DELETE' }); },
    importAnnouncements(items) { return this.request('/system/announcements/import', { method: 'POST', body: JSON.stringify({ items }) }); },

    // ============ 售后服务 ============
    myAfterSalesTickets() { return this.request('/system/after-sales'); },
    allAfterSalesTickets() { return this.request('/system/after-sales/all'); },
    createAfterSalesTicket(data) { return this.request('/system/after-sales', { method: 'POST', body: JSON.stringify(data) }); },
    replyAfterSalesTicket(id, reply) { return this.request('/system/after-sales/' + id + '/reply', { method: 'PUT', body: JSON.stringify({ reply }) }); },
    updateAfterSalesStatus(id, status) { return this.request('/system/after-sales/' + id + '/status?status=' + status, { method: 'PUT' }); },

    // ============ AI 智能问答 ============
    aiQa(question) { return this.request('/system/ai-qa', { method: 'POST', body: JSON.stringify({ question }) }); }
};

function saveUser(user) { localStorage.setItem('exam-user', JSON.stringify(user)); }
function getUser() { try { return JSON.parse(localStorage.getItem('exam-user')); } catch { return null; } }
function getToken() { return API.getToken(); }
function logout() { localStorage.removeItem('exam-user'); location.href = '/'; }
// 用运行时赋值而非函数声明，避免与页面内联脚本的 const el 产生全局标识符冲突
if (!window.el) {
    window.el = function (id) { return document.getElementById(id); };
}
function toast(message, type = 'info') {
    const el = document.createElement('div');
    el.className = 'toast toast-' + type;
    el.textContent = message;
    document.body.append(el);
    setTimeout(() => { el.style.opacity = '0'; el.style.transform = 'translateY(10px)'; setTimeout(() => el.remove(), 300); }, 2600);
}

// 滚动动画
document.addEventListener('DOMContentLoaded', () => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const style = document.createElement('style');
    style.textContent = '.scroll-scene{opacity:0;transform:translateY(30px);transition:opacity .7s ease,transform .7s ease}.scroll-scene.visible{opacity:1;transform:none}';
    document.head.appendChild(style);
    const observer = new IntersectionObserver(entries => entries.forEach(entry => {
        if (entry.isIntersecting) { entry.target.classList.add('visible'); observer.unobserve(entry.target); }
    }), { threshold: .1 });
    document.querySelectorAll('main > section, main > .grid, main > .panel, footer').forEach((el, i) => {
        if (i > 0) { el.classList.add('scroll-scene'); observer.observe(el); }
    });
});

// ===== Innovation Utils =====
// 全局创新工具（2026-10）：数字滚动 / 骨架屏 / Ctrl+K 命令面板。只追加，不改动以上既有函数。

// 数字滚动：从 0 滚动到 target（requestAnimationFrame + easeOutCubic，千分位格式化）；
// prefers-reduced-motion 时直接设置最终文本
window.animCount = function (el, target, duration = 900) {
    if (!el) return;
    target = Math.max(0, Number(target) || 0);
    const fmt = n => n.toLocaleString('zh-CN');
    const reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    // 后台标签页 rAF 暂停，动画会冻结在初始值 → 直接落终值
    if (reduced || document.hidden || !window.requestAnimationFrame || !(duration > 0)) { el.textContent = fmt(target); return; }
    const t0 = performance.now();
    const tick = now => {
        const p = Math.min(1, (now - t0) / duration);
        const eased = 1 - Math.pow(1 - p, 3); // easeOutCubic
        el.textContent = fmt(Math.round(eased * target));
        if (p < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
};

// 骨架表格行：向 container 注入 rows×cols 骨架占位（.skeleton-card），返回清理函数；
// 数据到达后调用清理函数移除占位，再渲染真实内容
window.skeletonRows = function (container, rows = 5, cols = 4) {
    if (!container) return () => { };
    const box = document.createElement('div');
    box.className = 'skeleton-card';
    let html = '<div class="skeleton skeleton-line" style="width:32%;height:18px"></div>';
    for (let r = 0; r < rows; r++) {
        html += '<div class="skeleton-row">';
        for (let c = 0; c < cols; c++) html += '<div class="skeleton skeleton-line"></div>';
        html += '</div>';
    }
    box.innerHTML = html;
    container.replaceChildren(box);
    return () => { if (box.isConnected) box.remove(); };
};

// Ctrl+K 命令面板：items = [{ label, hint, action }]；
// Esc / 点击遮罩关闭；↑↓ 导航（.selected 高亮）；Enter 执行后关闭；输入实时按 label 过滤；
// 同一页面重复调用复用已建面板（window._cmdkInstance）；返回 { open, destroy }
window.cmdk = function (items, onSelect) {
    if (window._cmdkInstance) { window._cmdkInstance._update(items, onSelect); return window._cmdkInstance; }

    let currentItems = Array.isArray(items) ? items : [];
    let selectCb = typeof onSelect === 'function' ? onSelect : null;
    let filtered = [];
    let selIdx = -1;

    const overlay = document.createElement('div');
    overlay.className = 'cmdk-overlay';
    overlay.innerHTML =
        '<div class="cmdk-panel" role="dialog" aria-label="命令面板">' +
        '<input class="cmdk-input" type="text" placeholder="搜索命令…" autocomplete="off">' +
        '<div class="cmdk-list"></div>' +
        '<div class="cmdk-foot">' +
        '<span><kbd class="cmdk-kbd">↑</kbd><kbd class="cmdk-kbd">↓</kbd> 导航</span>' +
        '<span><kbd class="cmdk-kbd">Enter</kbd> 执行</span>' +
        '<span><kbd class="cmdk-kbd">Esc</kbd> 关闭</span>' +
        '</div>' +
        '</div>';
    document.body.appendChild(overlay);
    const input = overlay.querySelector('.cmdk-input');
    const list = overlay.querySelector('.cmdk-list');

    const isOpen = () => overlay.classList.contains('open');

    function renderList() {
        const q = input.value.trim().toLowerCase();
        filtered = currentItems.filter(it => !q || String((it && it.label) || '').toLowerCase().includes(q));
        selIdx = filtered.length ? 0 : -1;
        list.innerHTML = filtered.length
            ? filtered.map((it, i) =>
                '<div class="cmdk-item' + (i === selIdx ? ' selected' : '') + '" data-i="' + i + '" role="option">' +
                '<span class="cmdk-item-label">' + escHtml(it.label) + '</span>' +
                (it.hint ? '<kbd class="cmdk-kbd">' + escHtml(it.hint) + '</kbd>' : '') +
                '</div>').join('')
            : '<div class="cmdk-empty">没有匹配的命令</div>';
    }

    function highlight() {
        list.querySelectorAll('.cmdk-item').forEach((node, i) => {
            node.classList.toggle('selected', i === selIdx);
            if (i === selIdx) node.scrollIntoView({ block: 'nearest' });
        });
    }

    function move(delta) {
        if (!filtered.length) return;
        selIdx = (selIdx + delta + filtered.length) % filtered.length;
        highlight();
    }

    function exec(i) {
        const it = filtered[i];
        if (!it) return;
        close();
        if (typeof it.action === 'function') it.action();
        else if (selectCb) selectCb(it);
    }

    function open() {
        if (isOpen()) { input.focus(); return; }
        input.value = '';
        renderList();
        overlay.classList.add('open');
        setTimeout(() => input.focus(), 50);
    }

    function close() { overlay.classList.remove('open'); }

    function onDocKey(e) {
        if ((e.ctrlKey || e.metaKey) && (e.key === 'k' || e.key === 'K')) {
            e.preventDefault();
            isOpen() ? close() : open();
            return;
        }
        if (!isOpen()) return;
        if (e.key === 'Escape') { e.preventDefault(); close(); }
        else if (e.key === 'ArrowDown') { e.preventDefault(); move(1); }
        else if (e.key === 'ArrowUp') { e.preventDefault(); move(-1); }
        else if (e.key === 'Enter') { e.preventDefault(); exec(selIdx); }
    }

    document.addEventListener('keydown', onDocKey);
    input.addEventListener('input', renderList);
    list.addEventListener('click', e => {
        const item = e.target.closest('.cmdk-item');
        if (item) exec(parseInt(item.dataset.i, 10));
    });
    overlay.addEventListener('mousedown', e => { if (e.target === overlay) close(); });

    const instance = {
        open,
        _update(nextItems, nextOnSelect) {
            if (Array.isArray(nextItems)) currentItems = nextItems;
            if (typeof nextOnSelect === 'function') selectCb = nextOnSelect;
            if (isOpen()) renderList();
        },
        destroy() {
            document.removeEventListener('keydown', onDocKey);
            overlay.remove();
            if (window._cmdkInstance === instance) window._cmdkInstance = null;
        }
    };
    window._cmdkInstance = instance;
    return instance;
};

/* ===== 页面切入/切出过渡 =====
   入场：main 顶层区块 stagger 淡入上移（.page-enter + --d 延迟，样式在 theme.css）
   离场：拦截同源链接点击，整页快速淡出（.page-leaving）后再跳转；
        bfcache 返回时由 pageshow 清除离场状态，避免页面停留在透明 */
(function initPageTransitions() {
    if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    // 入场 stagger
    const applyEnter = () => {
        const main = document.querySelector('main');
        if (!main || document.body.classList.contains('page-enter-done')) return;
        document.body.classList.add('page-enter-done');
        const blocks = [...main.children].filter(n => /^(SECTION|DIV|ASIDE)$/.test(n.tagName));
        const step = blocks.length > 8 ? 45 : 65;
        blocks.forEach((n, i) => {
            if (n.classList.contains('hero')) return; // hero 已有专属入场动画，避免叠加
            n.classList.add('page-enter');
            n.style.setProperty('--d', Math.min(i, 8) * step + 'ms');
        });
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', applyEnter);
    else applyEnter();

    // 离场：拦截同源链接（跳过修饰键/新窗口/下载/锚点/外链/同页链接）
    document.addEventListener('click', e => {
        if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
        const a = e.target.closest ? e.target.closest('a[href]') : null;
        if (!a || a.target === '_blank' || a.hasAttribute('download')) return;
        const raw = a.getAttribute('href') || '';
        if (/^(#|javascript:|mailto:|tel:)/i.test(raw)) return;
        let url;
        try { url = new URL(a.href); } catch (_) { return; }
        if (url.origin !== location.origin) return;
        if (url.pathname === location.pathname && url.search === location.search) return; // 同页锚点，平滑滚动即可
        e.preventDefault();
        document.body.classList.add('page-leaving');
        setTimeout(() => { location.href = url.href; }, 200);
    });

    // bfcache 恢复/返回时清除离场状态
    window.addEventListener('pageshow', () => document.body.classList.remove('page-leaving'));
})();
