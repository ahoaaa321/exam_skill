/* 验证码按钮倒计时：每秒刷新，结束后恢复原文字并可再次点击 */
function startCountdown(btn, seconds, resetText) {
    let left = seconds;
    btn.disabled = true;
    btn.textContent = left + 's 后重发';
    const timer = setInterval(() => {
        left--;
        if (left <= 0) {
            clearInterval(timer);
            btn.disabled = false;
            btn.textContent = resetText;
        } else {
            btn.textContent = left + 's 后重发';
        }
    }, 1000);
}

function bindLogin(role) {
    document.querySelector('#login-form').addEventListener('submit', async event => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        const submit = event.currentTarget.querySelector('button[type="submit"], button:not([type])');
        if (submit) submit.disabled = true;
        try {
            const user = await API.login({ username: form.get('username'), password: form.get('password') });
            if (role && user.role !== role) throw new Error('该账号不属于当前登录入口');
            saveUser(user);
            let next = role === 0 ? '/student/dashboard.html' : role === 1 ? '/admin/admin.html' : role === 2 ? '/staff/exam-staff.html' : '/super-admin/super-admin.html';
            const redirect = new URLSearchParams(location.search).get('redirect');
            if (redirect && redirect.startsWith('/')) next = redirect;
            window.location.replace(next);
        } catch (error) {
            if (submit) submit.disabled = false;
            toast(error.message, 'error');
        }
    });
}

function bindRegister() {
    const form = document.querySelector('#register-form');
    const sendCodeBtn = document.querySelector('#send-code-btn');
    const pwdInput = document.querySelector('#pwd-input');
    const confirmPwdInput = document.querySelector('#confirm-pwd-input');
    const confirmTip = document.querySelector('#confirm-tip');

    // 密码强度检测
    if (pwdInput) {
        pwdInput.addEventListener('input', () => {
            const pwd = pwdInput.value;
            const { score, text, color } = calcPasswordStrength(pwd);
            const fill = document.getElementById('strength-fill');
            const txt = document.getElementById('strength-text');
            if (fill) fill.style.cssText = `width:${score}%;background:${color}`;
            if (txt) { txt.textContent = text; txt.style.color = color; }
            checkConfirm();
        });
    }

    // 确认密码实时校验
    function checkConfirm() {
        if (!confirmPwdInput) return;
        const pwd = pwdInput.value;
        const confirm = confirmPwdInput.value;
        if (!confirm) { confirmTip.style.display = 'none'; return; }
        if (pwd === confirm) {
            confirmTip.textContent = '✓ 两次密码一致';
            confirmTip.style.color = '#34c759';
            confirmTip.style.display = 'block';
        } else {
            confirmTip.textContent = '✗ 两次输入的密码不一致';
            confirmTip.style.color = '#ef4444';
            confirmTip.style.display = 'block';
        }
    }
    if (confirmPwdInput) {
        confirmPwdInput.addEventListener('input', checkConfirm);
    }

    function calcPasswordStrength(pwd) {
        let score = 0;
        if (pwd.length >= 8) score += 25;
        if (pwd.length >= 12) score += 15;
        if (/[a-z]/.test(pwd)) score += 15;
        if (/[A-Z]/.test(pwd)) score += 15;
        if (/\d/.test(pwd)) score += 15;
        if (/[^a-zA-Z0-9]/.test(pwd)) score += 15;
        if (score <= 30) return { score, text: '弱', color: '#ef4444' };
        if (score <= 60) return { score, text: '中', color: '#f59e0b' };
        if (score <= 85) return { score, text: '强', color: '#10b981' };
        return { score: 100, text: '极强', color: '#2dd4bf' };
    }

    if (sendCodeBtn) {
        sendCodeBtn.addEventListener('click', async () => {
            const email = form.querySelector('[name="email"]').value;
            if (!email) { toast('请先填写邮箱', 'error'); return; }
            // 立即给出「发送中」反馈，避免等待邮件接口期间界面无响应
            sendCodeBtn.disabled = true;
            const originalText = sendCodeBtn.textContent;
            sendCodeBtn.textContent = '发送中…';
            try {
                await API.sendRegisterCode(email);
                toast('验证码已发送至邮箱，请查收');
                startCountdown(sendCodeBtn, 60, originalText);
            } catch (e) {
                sendCodeBtn.disabled = false;
                sendCodeBtn.textContent = originalText;
                toast(e.message, 'error');
            }
        });
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        const f = new FormData(form);
        if (f.get('password') !== f.get('confirmPassword')) {
            toast('两次输入的密码不一致', 'error');
            return;
        }
        const submit = form.querySelector('button[type="submit"], button:not([type])');
        const username = f.get('username');
        const password = f.get('password');
        if (submit) { submit.disabled = true; submit.textContent = '注册中…'; }
        try {
            const result = await API.register({
                username,
                password,
                name: f.get('name'),
                idCard: f.get('idCard'),
                phone: f.get('phone'),
                email: f.get('email'),
                code: f.get('code')
            });
            // 恢复令牌仅显示一次：暂存后到考生中心用横幅提醒用户保存
            if (result && result.recoveryToken) {
                sessionStorage.setItem('exam-recovery-token', result.recoveryToken);
            }
            // 注册成功后自动登录，直接进入已登录页面（带参数确保拉取最新页面而非缓存）
            const user = await API.login({ username, password });
            saveUser(user);
            const redirect = new URLSearchParams(location.search).get('redirect');
            const target = redirect && redirect.startsWith('/') ? redirect : '/student/dashboard.html';
            window.location.replace(target + (target.includes('?') ? '&' : '?') + 'welcome=1');
        } catch (error) {
            if (submit) { submit.disabled = false; submit.textContent = '完成注册'; }
            toast(error.message, 'error');
        }
    });
}

function bindForgotPassword() {
    const form = document.querySelector('#recovery-form');
    const tokenForm = document.querySelector('#token-form');
    if (!form && !tokenForm) return;
    const sendCodeBtn = document.querySelector('#send-code-btn');

    if (sendCodeBtn) {
        sendCodeBtn.addEventListener('click', async () => {
            const email = form.querySelector('[name="email"]').value;
            const username = form.querySelector('[name="username"]').value.trim();
            if (!email) { toast('请填写邮箱', 'error'); return; }
            sendCodeBtn.disabled = true;
            const originalText = sendCodeBtn.textContent;
            sendCodeBtn.textContent = '发送中…';
            try {
                await API.sendResetCode(email, username);
                toast('验证码已发送至邮箱，请查收');
                startCountdown(sendCodeBtn, 60, originalText);
            } catch (e) {
                sendCodeBtn.disabled = false;
                sendCodeBtn.textContent = originalText;
                toast(e.message, 'error');
            }
        });
    }

    if (form) {
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const f = new FormData(form);
            if (f.get('newPassword') !== f.get('confirmPassword')) {
                toast('两次输入的密码不一致', 'error');
                return;
            }
            try {
                await API.resetPassword({ email: f.get('email'), username: f.get('username'), code: f.get('code'), newPassword: f.get('newPassword') });
                toast('密码重置成功，请登录');
                setTimeout(() => location.href = '/auth/login.html', 1000);
            } catch (e) { toast(e.message, 'error'); }
        });
    }

    if (tokenForm) {
        tokenForm.addEventListener('submit', async event => {
            event.preventDefault();
            const f = new FormData(tokenForm);
            if (f.get('newPassword') !== f.get('confirmPassword')) {
                toast('两次输入的密码不一致', 'error');
                return;
            }
            try {
                await API.resetPasswordByToken({ token: f.get('token'), newPassword: f.get('newPassword') });
                toast('密码重置成功，请登录');
                setTimeout(() => location.href = '/auth/login.html', 1000);
            } catch (e) { toast(e.message, 'error'); }
        });
    }
}

function switchTab(tab) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    event.target.classList.add('active');
    document.getElementById('recovery-form').hidden = tab !== 'email';
    document.getElementById('token-form').hidden = tab !== 'token';
}
