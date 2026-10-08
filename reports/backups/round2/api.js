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
    sendResetCode(email) { return this.request('/auth/password/send-code', { method: 'POST', body: JSON.stringify({ email }) }); },
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
