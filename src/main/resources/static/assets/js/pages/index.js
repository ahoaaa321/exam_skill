let plans = [];
let activeTrade = '';   // 工种 chips 当前筛选（空 = 全部）
const el = id => document.getElementById(id);
// 转义统一使用 api.js 提供的全局 escHtml

// 公告区加载骨架占位（数据到达后由 load() 清理）
const clearNoticeSkeleton = window.skeletonRows ? skeletonRows(el('notice-list'), 4, 2) : null;

async function load() {
    updateNavAuth();
    try {
        plans = await API.plans();
        const notices = await API.announcements();
        const trades = await API.trades();
        const countEl = el('plan-count');
        countEl.dataset.count = plans.length;
        animateCount(countEl, plans.length);
        if (clearNoticeSkeleton) clearNoticeSkeleton();
        el('notice-list').innerHTML = notices.map((n, i) =>
            `<div class="notice-item anim-in" style="--i:${i % 10}"><b>${escHtml(n.title)}</b><br><span class="meta">${escHtml(n.content)}</span></div>`
        ).join('');
        const icons = ['⚡','🔥','🔧','💼','💻','🚗','💆','🍳','🍵','👶','🏥','🛡️','🏠','🛒','📦','🥗','💊','📷','💇','🥟'];
        el('trade-list').innerHTML = trades.map((t, i) =>
            `<div class="trade-card anim-in" style="--i:${i % 20}" onclick="document.getElementById('plans').scrollIntoView({behavior:'smooth'})">
                <div class="trade-icon">${icons[i % icons.length]}</div>
                <div class="trade-name">${escHtml(t.trade_name)}</div>
                <div class="trade-cat">${escHtml(t.trade_category || '职业技能')}</div>
            </div>`
        ).join('');
        // Hero 统计数字滚动（一次性进场动画，reduced-motion 时 animCount 直接落定）
        animCount(el('hero-stat-plans'), plans.length);
        animCount(el('hero-stat-trades'), trades.length);
        animCount(el('hero-stat-regs'), plans.reduce((s, p) => s + (p.currentCount || 0), 0));
        renderTradeChips();
        renderPlans();
    } catch (e) { toast(e.message, 'error'); }
}

function renderPlans() {
    const q = el('search').value.toLowerCase();
    const level = el('level').value;
    const sort = el('sort').value;
    const anim = renderPlans.first !== false;   // 首次渲染播放入场动画，筛选重渲染不播
    renderPlans.first = false;
    let filtered = plans.filter(p =>
        (p.planName + p.tradeName).toLowerCase().includes(q) && (!level || p.levelName === level)
        && (!activeTrade || p.tradeName === activeTrade)
    );
    switch (sort) {
        case 'fee-asc': filtered.sort((a, b) => a.fee - b.fee); break;
        case 'fee-desc': filtered.sort((a, b) => b.fee - a.fee); break;
        case 'time-asc': filtered.sort((a, b) => new Date(a.registerEndTime) - new Date(b.registerEndTime)); break;
        case 'time-desc': filtered.sort((a, b) => new Date(b.registerEndTime) - new Date(a.registerEndTime)); break;
        case 'name-asc': filtered.sort((a, b) => a.planName.localeCompare(b.planName, 'zh')); break;
        case 'hot-desc': filtered.sort((a, b) => (b.currentCount || 0) - (a.currentCount || 0)); break;
    }
    if (!filtered.length) {
        el('plan-list').innerHTML = '<div class="notice">暂无匹配的考试计划</div>';
        return;
    }
    el('plan-list').innerHTML = filtered.map((p, i) => {
        const pct = p.maxCandidates > 0 ? Math.min(100, Math.round(p.currentCount / p.maxCandidates * 100)) : 0;
        // 倒计时徽章：>7 天不显示；3-7 天琥珀；≤3 天红(.badge-urgent)；已截止灰暗提示
        const daysLeft = p.registerEndTime ? Math.ceil((new Date(p.registerEndTime) - Date.now()) / 86400000) : null;
        let badge = '';
        if (daysLeft !== null) {
            if (daysLeft < 0) badge = '<span class="badge-countdown badge-urgent">已截止</span>';
            else if (daysLeft === 0) badge = '<span class="badge-countdown badge-urgent">今日截止</span>';
            else if (daysLeft <= 3) badge = `<span class="badge-countdown badge-urgent">距截止 ${daysLeft} 天</span>`;
            else if (daysLeft <= 7) badge = `<span class="badge-countdown">距截止 ${daysLeft} 天</span>`;
        }
        return `<article class="card plan-card${anim ? ' anim-in' : ''}"${anim ? ` style="--i:${i % 12}"` : ''}>
            <div class="plan-head">
                <div class="label">报名计划 #${String(p.id).padStart(3,'0')}</div>
                ${badge}
            </div>
            <h3>${escHtml(p.planName)} <span class="tag">${escHtml(p.levelName)}</span></h3>
            <div class="meta">
                认定工种：${escHtml(p.tradeName)}<br>
                报名时间：${p.registerStartTime?.substring(0,10)} 至 ${p.registerEndTime?.substring(0,10)}<br>
                考试时间：${p.examTime?.substring(0,16).replace('T',' ')}<br>
                报名费用：¥${p.fee}
            </div>
            <div class="plan-progress">
                <div class="plan-progress-meta"><span>报名热度</span><span>${p.currentCount} / ${p.maxCandidates || '不限'}</span></div>
                <div class="heat-bar${pct > 90 ? ' hot' : ''}" style="margin-top:4px"><div class="heat-bar-fill" style="width:${pct}%"></div></div>
            </div>
            <div class="plan-actions">
                <button class="btn" onclick="detail(${p.id})">查看详情</button>
                <button class="btn btn-primary" onclick="applyPlan(${p.id})">我要报名</button>
            </div>
        </article>`;
    }).join('');
}

/* ===== 工种 chips 筛选 ===== */
function renderTradeChips() {
    const box = el('trade-chips');
    if (!box) return;
    const names = [...new Set(plans.map(p => p.tradeName).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'zh'));
    box.innerHTML = ['全部', ...names].map(n => {
        const val = n === '全部' ? '' : n;
        return `<button type="button" class="chip${(activeTrade || '全部') === n ? ' active' : ''}" data-trade="${escHtml(val)}" onclick="setTradeFilter(this.dataset.trade)">${escHtml(n)}</button>`;
    }).join('');
}

window.setTradeFilter = function (name) {
    activeTrade = name || '';
    el('trade-chips')?.querySelectorAll('.chip').forEach(c => c.classList.toggle('active', (c.dataset.trade || '') === activeTrade));
    renderPlans();
};

function detail(id) {
    const p = plans.find(x => x.id === id);
    el('modal').hidden = false;
    el('modal').innerHTML = `<div class="modal"><div class="panel" style="margin:20px;max-width:680px;max-height:85vh;overflow-y:auto">
        <div class="eyebrow">PLAN DETAIL</div>
        <h2>${escHtml(p.planName)}</h2>
        <div class="meta">
            工种：${escHtml(p.tradeName)}　等级：${escHtml(p.levelName)}<br>
            报名时间：${p.registerStartTime?.substring(0,16).replace('T',' ')} 至 ${p.registerEndTime?.substring(0,16).replace('T',' ')}<br>
            考试时间：${p.examTime?.substring(0,16).replace('T',' ')}<br>
            考试地点：${escHtml(p.examLocation || '以准考证安排为准')}<br>
            报名上限：${p.maxCandidates || '不限'}人　费用：¥${p.fee}<br>
            报名条件：${escHtml(p.conditionDesc || '身体健康，具备相应职业技能基础。')}
        </div>

        <div class="callout callout-amber">
            <div class="callout-head"><span class="callout-icon">📌</span><h3>报考必要性分析</h3></div>
            <p>取得「${escHtml(p.tradeName)}」${escHtml(p.levelName)}职业技能等级证书，是从事相关岗位的<strong>准入证明</strong>与<strong>能力背书</strong>。证书全国通用、可在线查验，有助于提升职场竞争力、获得岗位晋升与薪资提升机会。当前计划报名人数已达 <strong>${p.currentCount}</strong> 人，建议尽早报名锁定考位。</p>
        </div>

        <div class="review-list">
            <div class="review-head"><span class="callout-icon">⭐</span><h3>学员评价</h3><span>综合评分 4.8 / 5.0</span></div>
            <div class="review-card"><div class="review-stars">★★★★★</div><b>张同学</b> <span class="meta">· 已通过</span><br><span class="meta">报名流程很顺畅，审核通过后很快就安排了考场，证书查询也很方便。</span></div>
            <div class="review-card"><div class="review-stars">★★★★☆</div><b>李同学</b> <span class="meta">· 已通过</span><br><span class="meta">考试内容贴合实际工作，拿到证书后公司给涨了工资，很值得。</span></div>
            <div class="review-card"><div class="review-stars">★★★★★</div><b>王同学</b> <span class="meta">· 备考中</span><br><span class="meta">平台功能齐全，从报名到查成绩一站式完成，体验很好。</span></div>
        </div>

        <div class="callout callout-indigo" style="margin-top:16px">
            <div class="callout-head"><span class="callout-icon">🤖</span><h3>AI 智能问答</h3></div>
            <div id="ai-chat-box" class="ai-chat-box"></div>
            <div style="display:flex;gap:8px">
                <input id="ai-question" placeholder="输入您的问题，如：报名条件是什么？" style="flex:1" onkeydown="if(event.key==='Enter')askAI()">
                <button class="btn btn-primary" onclick="askAI()">提问</button>
            </div>
            <div class="ai-quick-actions">
                <button class="btn btn-sm" onclick="quickAsk('报名条件是什么？')">报名条件</button>
                <button class="btn btn-sm" onclick="quickAsk('如何缴费？')">如何缴费</button>
                <button class="btn btn-sm" onclick="quickAsk('准考证什么时候打印？')">准考证打印</button>
                <button class="btn btn-sm" onclick="quickAsk('证书如何领取？')">证书领取</button>
            </div>
        </div>

        <div class="plan-actions" style="margin-top:20px">
            <button class="btn btn-primary" onclick="applyPlan(${p.id})">立即报名</button>
            <button class="btn" onclick="closeModal()">关闭</button>
        </div>
    </div></div>`;
}

function quickAsk(q) {
    el('ai-question').value = q;
    askAI();
}

async function askAI() {
    const input = el('ai-question');
    const q = input.value.trim();
    if (!q) { toast('请输入问题', 'error'); return; }
    const box = el('ai-chat-box');
    box.innerHTML += `<div class="ai-row"><b>您：</b>${escHtml(q)}</div>`;
    box.innerHTML += `<div class="ai-row" style="color:var(--muted)"><b>AI：</b>思考中...</div>`;
    input.value = '';
    box.scrollTop = box.scrollHeight;
    try {
        const res = await API.aiQa(q);
        const msgs = box.querySelectorAll('.ai-row');
        if (msgs.length) msgs[msgs.length - 1].outerHTML = `<div class="ai-row"><b>AI：</b>${escHtml(res.answer)}</div>`;
        box.scrollTop = box.scrollHeight;
    } catch (e) {
        const msgs = box.querySelectorAll('.ai-row');
        if (msgs.length) msgs[msgs.length - 1].outerHTML = `<div class="ai-row" style="color:var(--danger)"><b>AI：</b>抱歉，服务暂不可用</div>`;
    }
}

function openLoginSelector() {
    el('modal').hidden = false;
    el('modal').innerHTML = `<div class="modal"><div class="panel" style="margin:20px">
        <div class="eyebrow">SELECT ROLE</div>
        <h2 style="text-align:center">选择登录身份</h2>
        <p class="meta" style="text-align:center;margin-bottom:24px">请选择您的身份进入对应功能页面</p>
        <div class="role-grid">
            <div class="role-card" onclick="location.href='/auth/login.html'">
                <div class="role-icon">🎓</div><h3>考生</h3><p class="meta">报名、缴费、准考证、成绩</p>
            </div>
            <div class="role-card" onclick="location.href='/auth/admin-login.html'">
                <div class="role-icon">👨‍💼</div><h3>管理员</h3><p class="meta">计划管理、资格审核</p>
            </div>
            <div class="role-card" onclick="location.href='/auth/exam-staff-login.html'">
                <div class="role-icon">📋</div><h3>考务人员</h3><p class="meta">考场配置、编排、准考证</p>
            </div>
            <div class="role-card" onclick="location.href='/auth/super-admin-login.html'">
                <div class="role-icon">⚙️</div><h3>超级管理员</h3><p class="meta">用户管理、系统配置、日志</p>
            </div>
        </div>
        <div style="text-align:center;margin-top:18px">
            <a href="/auth/register.html" style="color:var(--primary);text-decoration:none">还没有账号？立即注册 →</a>
        </div>
        <button class="btn" style="width:100%;margin-top:16px" onclick="closeModal()">返回浏览</button>
    </div></div>`;
}

function closeModal() { el('modal').hidden = true; }

/* ===== 导航栏登录态 ===== */
function updateNavAuth() {
    const u = getUser();
    const btn = el('nav-auth');
    if (!btn) return;
    if (u && u.token) {
        const roleMap = { 0: '考生', 1: '管理员', 2: '考务', 3: '超管' };
        const dashMap = { 0: '/student/dashboard.html', 1: '/admin/admin.html', 2: '/staff/exam-staff.html', 3: '/super-admin/super-admin.html' };
        btn.textContent = (u.realName || u.username) + ' · 进入' + (roleMap[u.role] || '');
        btn.onclick = (e) => { e.preventDefault(); location.href = dashMap[u.role] || '/student/dashboard.html'; };
    } else {
        btn.textContent = '登录 / 注册';
        btn.onclick = (e) => { e.preventDefault(); openLoginSelector(); };
    }
}

/* ===== 在线报名申请 ===== */
function applyPlan(planId) {
    const user = getUser();
    if (!user || !user.token) {
        sessionStorage.setItem('pendingPlanId', planId);
        toast('请先登录考生账号');
        setTimeout(() => { location.href = '/auth/login.html?redirect=/'; }, 700);
        return;
    }
    if (user.role !== 0) {
        toast('报名功能请使用考生账号登录', 'error');
        return;
    }
    openApplyForm(planId);
}

function openApplyForm(planId) {
    const p = plans.find(x => x.id === planId);
    if (!p) return;
    el('modal').hidden = false;
    el('modal').innerHTML = `<div class="modal"><div class="panel" style="margin:20px;max-width:560px">
        <div class="eyebrow">ONLINE APPLICATION</div>
        <h2>报名申请</h2>
        <div class="apply-plan-info">
            <b>${p.planName}</b>　<span class="tag">${p.levelName}</span><br>
            认定工种：${p.tradeName}　|　考试时间：${p.examTime?.substring(0,16).replace('T',' ')}　|　费用：¥${p.fee}
        </div>
        <form id="apply-form" class="form" onsubmit="submitApply(${p.id});return false">
            <div class="form-group">
                <label class="form-label">从事本职业年限（年）</label>
                <input id="ap-years" type="number" min="0" max="60" value="0" required>
            </div>
            <div class="form-group">
                <label class="form-label">文化程度</label>
                <select id="ap-edu" required>
                    <option value="">请选择</option>
                    <option>初中</option><option>高中</option><option selected>中专/中技</option>
                    <option>大专</option><option>本科</option><option>硕士及以上</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">紧急联系人</label>
                <input id="ap-contact" placeholder="请输入紧急联系人姓名" required>
            </div>
            <div class="form-group">
                <label class="form-label">紧急联系电话</label>
                <input id="ap-phone" type="tel" placeholder="请输入11位手机号" required maxlength="11">
            </div>
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">确认提交报名</button>
                <button type="button" class="btn" onclick="closeModal()">取消</button>
            </div>
        </form>
    </div></div>`;
}

async function submitApply(planId) {
    const workYears = parseInt(el('ap-years').value) || 0;
    const education = el('ap-edu').value;
    const emergencyContact = el('ap-contact').value.trim();
    const emergencyPhone = el('ap-phone').value.trim();
    if (!emergencyContact) { toast('请填写紧急联系人', 'error'); return; }
    if (!/^1[3-9]\d{9}$/.test(emergencyPhone)) { toast('紧急联系电话格式不正确', 'error'); return; }
    try {
        await API.apply({ planId, workYears, education, emergencyContact, emergencyPhone });
        sessionStorage.removeItem('pendingPlanId');
        el('modal').innerHTML = `<div class="modal"><div class="panel apply-success" style="margin:20px">
            <div class="apply-success-circle"><svg viewBox="0 0 52 52"><polyline points="14 27 23 36 38 19"/></svg></div>
            <h2>报名提交成功</h2>
            <p class="meta" style="margin:10px 0 22px">资格审核结果将通过站内消息通知您，请留意个人中心。</p>
            <button class="btn btn-primary" style="width:100%" onclick="location.href='/student/dashboard.html'">前往我的报名</button>
        </div></div>`;
    } catch (e) { toast(e.message, 'error'); }
}

/* ===== 证书公开查验 ===== */
async function verifyCert() {
    const no = el('cert-no').value.trim();
    if (!no) { toast('请输入证书编号', 'error'); return; }
    try {
        const c = await API.verifyCertificate(no);
        const revoked = c.status === 2;
        el('cert-result').innerHTML = `
            <div class="cert-result">
                <div class="cert-result-head">
                    <div class="cert-badge ${revoked ? 'revoked' : 'ok'}">${revoked ? '✕' : '✓'}</div>
                    <div><h3>${revoked ? '该证书已作废' : '证书真实有效'}</h3>
                    <span class="meta">编号：${escHtml(c.certificate_no)}</span></div>
                </div>
                <div class="cert-grid meta">
                    <div><b>持证人：</b>${escHtml(c.real_name)}</div>
                    <div><b>工种：</b>${escHtml(c.trade_name)}</div>
                    <div><b>等级：</b>${escHtml(c.level_name)}</div>
                    <div><b>认定计划：</b>${escHtml(c.plan_name || '-')}</div>
                    <div><b>发证日期：</b>${(c.issued_at || '').substring(0,10)}</div>
                    <div><b>发证机构：</b>${escHtml(c.issuer || '-')}</div>
                </div>
            </div>`;
    } catch (e) {
        el('cert-result').innerHTML = `<div class="notice" style="color:#b91c1c">⚠ ${escHtml(e.message)}</div>`;
    }
}

function animateCount(el, target) {
    if (!el) return;
    el.dataset.counted = '1';
    let cur = 0;
    const step = Math.max(1, Math.ceil(target / 40));
    const tick = () => {
        cur += step;
        if (cur >= target) { el.textContent = target; }
        else { el.textContent = cur; requestAnimationFrame(tick); }
    };
    tick();
}

// Animate all stats on load
document.querySelectorAll('.stat-num[data-count]').forEach(el => {
    if (!el.dataset.counted) animateCount(el, parseInt(el.dataset.count) || 0);
});

el('search').oninput = renderPlans;
el('level').onchange = renderPlans;
el('sort').onchange = renderPlans;

/* ===== Hero fading carousel ===== */
(function(){
  const slides = document.querySelectorAll('.hero-slide');
  const textLayers = document.querySelectorAll('.hero-text-layer');
  const dots = document.querySelectorAll('#hero-dots .dot');
  if (slides.length < 2) return;
  let idx = 0;
  const INTERVAL = 5000;
  function goTo(i) {
    slides[idx].classList.remove('active');
    if (textLayers[idx]) textLayers[idx].classList.remove('active');
    if (dots[idx]) dots[idx].classList.remove('active');
    idx = (i + slides.length) % slides.length;
    slides[idx].classList.add('active');
    if (textLayers[idx]) textLayers[idx].classList.add('active');
    if (dots[idx]) dots[idx].classList.add('active');
  }
  const timer = setInterval(() => goTo(idx + 1), INTERVAL);
  dots.forEach((d, i) => d.addEventListener('click', () => { goTo(i); }));
})();

load().then(() => {
    const pid = sessionStorage.getItem('pendingPlanId');
    const u = getUser();
    if (pid && u && u.token && u.role === 0) {
        sessionStorage.removeItem('pendingPlanId');
        setTimeout(() => openApplyForm(parseInt(pid)), 350);
    }
});

/* ===== 滚动进度条 + 回到顶部 ===== */
(function(){
  const bar = document.getElementById('scroll-progress');
  const btn = document.getElementById('back-to-top');
  function onScroll() {
    const st = window.scrollY || document.documentElement.scrollTop;
    const h = document.documentElement.scrollHeight - window.innerHeight;
    const pct = h > 0 ? (st / h) * 100 : 0;
    if (bar) bar.style.width = pct + '%';
    if (btn) btn.classList.toggle('visible', st > 400);
  }
  window.addEventListener('scroll', onScroll, { passive: true });
  if (btn) btn.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
  onScroll();
})();

/* ===== Apple-style scroll animations ===== */
const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach(e => {
        if (e.isIntersecting) {
            e.target.classList.add('reveal-visible');
            e.target.querySelectorAll('[data-count]').forEach(countEl => {
                if (!countEl.dataset.counted) {
                    countEl.dataset.counted = '1';
                    const target = parseInt(countEl.dataset.count) || 0;
                    let cur = 0;
                    const step = Math.max(1, Math.ceil(target / 40));
                    const tick = () => {
                        cur += step;
                        if (cur >= target) { countEl.textContent = target; }
                        else { countEl.textContent = cur; requestAnimationFrame(tick); }
                    };
                    tick();
                }
            });
            const textEl = e.target.querySelector('[data-text]');
            if (textEl && !textEl.dataset.counted) {
                textEl.dataset.counted = '1';
                textEl.textContent = textEl.dataset.text;
            }
        }
    });
}, { threshold: 0.15 });
document.querySelectorAll('.reveal').forEach(el => revealObserver.observe(el));

/* ===== Navbar scroll state ===== */
let ticking = false;
window.addEventListener('scroll', () => {
    if (!ticking) {
        requestAnimationFrame(() => {
            const topbar = document.getElementById('topbar');
            if (window.scrollY > 20) topbar.classList.add('scrolled');
            else topbar.classList.remove('scrolled');
            ticking = false;
        });
        ticking = true;
    }
}, { passive: true });

/* ===== Smooth scroll for nav links ===== */
document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', e => {
        e.preventDefault();
        const target = document.querySelector(link.getAttribute('href'));
        if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
});

/* ===== Parallax blob on hero ===== */
const hero = document.querySelector('.hero');
if (hero) {
    window.addEventListener('mousemove', e => {
        const blobs = hero.querySelectorAll('.blob');
        const x = (e.clientX / window.innerWidth - 0.5) * 20;
        const y = (e.clientY / window.innerHeight - 0.5) * 20;
        blobs.forEach((b, i) => {
            const factor = (i + 1) * 0.5;
            b.style.transform = `translate(${x * factor}px, ${y * factor}px)`;
        });
    });
}

/* ===== Ctrl+K 命令面板（全局工具 cmdk，标签由 cmdk 内部 escHtml 转义） ===== */
if (window.cmdk) {
    const jump = id => () => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
    cmdk([
        { label: '查看考试计划', hint: '跳转', action: jump('plans') },
        { label: '通知公告', hint: '跳转', action: jump('notice') },
        { label: '证书查验', hint: '跳转', action: jump('verify') },
        { label: '帮助中心', hint: '跳转', action: jump('faq') },
        { label: '登录 / 注册', hint: '入口', action: () => openLoginSelector() },
        { label: '前往考生工作台', hint: '跳转', action: () => { location.href = '/student/dashboard.html'; } }
    ]);
}
