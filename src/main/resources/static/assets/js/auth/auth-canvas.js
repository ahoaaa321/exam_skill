/* ============================================================
 * 认证页 AI 动画背景
 * 基于 AI 生成底图，Canvas 实现 Ken Burns 运镜 + 漂浮光粒子
 * 60fps 无缝循环，替代视频文件（无虚化、无外部依赖）
 * ============================================================ */
(function () {
    const canvas = document.querySelector('.auth-canvas-bg');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const img = new Image();
    img.src = canvas.dataset.src || '/assets/img/auth-bg.png';

    let W = 0, H = 0, dpr = Math.min(window.devicePixelRatio || 1, 2);
    const particles = [];
    const PARTICLE_COUNT = 46;
    let t0 = performance.now();

    function resize() {
        const rect = canvas.getBoundingClientRect();
        W = rect.width;
        H = rect.height;
        canvas.width = Math.round(W * dpr);
        canvas.height = Math.round(H * dpr);
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    }

    function initParticles() {
        particles.length = 0;
        for (let i = 0; i < PARTICLE_COUNT; i++) {
            particles.push({
                x: Math.random() * W,
                y: Math.random() * H,
                r: 0.6 + Math.random() * 2.4,
                vx: (Math.random() - 0.5) * 0.15,
                vy: -0.1 - Math.random() * 0.35,
                a: 0.15 + Math.random() * 0.45,
                tw: Math.random() * Math.PI * 2,
                tws: 0.5 + Math.random() * 1.2
            });
        }
    }

    function drawImageCover(img, zoom, panX, panY) {
        // cover 缩放
        const iw = img.naturalWidth, ih = img.naturalHeight;
        const scale = Math.max(W / iw, H / ih) * zoom;
        const dw = iw * scale, dh = ih * scale;
        const dx = (W - dw) / 2 + panX;
        const dy = (H - dh) / 2 + panY;
        ctx.drawImage(img, dx, dy, dw, dh);
    }

    function frame(now) {
        const t = (now - t0) / 1000;
        ctx.clearRect(0, 0, W, H);

        if (img.complete && img.naturalWidth > 0) {
            // Ken Burns 运镜：30s 一个呼吸周期，缩放 1.0 -> 1.12，缓慢平移
            const cycle = t / 30;
            const zoom = 1.0 + 0.11 * (0.5 - 0.5 * Math.cos(cycle * Math.PI * 2));
            const panX = Math.sin(cycle * Math.PI * 2) * (W * 0.025);
            const panY = Math.cos(cycle * Math.PI * 2 * 0.7) * (H * 0.018);
            drawImageCover(img, zoom, panX, panY);
        } else {
            // 图片未加载时的兜底渐变
            const g = ctx.createLinearGradient(0, 0, W, H);
            g.addColorStop(0, '#0f172a');
            g.addColorStop(0.5, '#1e293b');
            g.addColorStop(1, '#0f172a');
            ctx.fillStyle = g;
            ctx.fillRect(0, 0, W, H);
        }

        // 漂浮光粒子
        for (const p of particles) {
            p.x += p.vx;
            p.y += p.vy;
            p.tw += 0.02 * p.tws;
            if (p.y < -10) { p.y = H + 10; p.x = Math.random() * W; }
            if (p.x < -10) p.x = W + 10;
            if (p.x > W + 10) p.x = -10;
            const alpha = p.a * (0.55 + 0.45 * Math.sin(p.tw));
            const grad = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r * 4);
            grad.addColorStop(0, `rgba(255,210,140,${alpha})`);
            grad.addColorStop(0.4, `rgba(120,180,255,${alpha * 0.5})`);
            grad.addColorStop(1, 'rgba(0,0,0,0)');
            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.r * 4, 0, Math.PI * 2);
            ctx.fill();
        }

        requestAnimationFrame(frame);
    }

    function start() {
        resize();
        initParticles();
        requestAnimationFrame(frame);
    }

    window.addEventListener('resize', () => { resize(); initParticles(); });

    if (img.complete && img.naturalWidth > 0) {
        start();
    } else {
        img.onload = start;
        img.onerror = start;
    }
})();
