let currentSlide = 0;
const totalSlides = 3;
let wrapper, indicatorsContainer;
const countdownElements = [];

function setCookie(name, value, days) {
    const date = new Date();
    date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
    const expires = 'expires=' + date.toUTCString();
    document.cookie = name + '=' + encodeURIComponent(value) + ';' + expires + ';path=/';
}

function getCookie(name) {
    const nameEQ = name + '=';
    const ca = document.cookie.split(';');
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) === ' ') c = c.substring(1, c.length);
        if (c.indexOf(nameEQ) === 0) {
            return decodeURIComponent(c.substring(nameEQ.length, c.length));
        }
    }
    return null;
}

function deleteCookie(name) {
    document.cookie = name + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;path=/';
}

function checkLogin() {
    const isLoggedIn = getCookie('isLoggedIn');
    return isLoggedIn === 'true';
}

function getUsername() {
    return getCookie('username') || '';
}

function updateHeader() {
    const userInfoDiv = document.getElementById('userInfo');
    const isLoggedIn = checkLogin();
    const username = getUsername();
    
    if (isLoggedIn) {
        userInfoDiv.innerHTML = `
            <span class="user-name">欢迎，${username}</span>
            <button class="logout-btn" onclick="handleLogout()">退出</button>
        `;
    } else {
        userInfoDiv.innerHTML = `
            <a href="login.html" class="login-link">登录</a>
        `;
    }
}

function handleLogout() {
    if (confirm('确定要退出登录吗？')) {
        deleteCookie('isLoggedIn');
        deleteCookie('username');
        deleteCookie('userInfo');
        updateHeader();
        alert('已退出登录');
    }
}

function requireLogin(callback) {
    if (checkLogin()) {
        callback();
    } else {
        if (confirm('请先登录才能参与拼团，是否前往登录页？')) {
            window.location.href = 'login.html';
        }
    }
}

function initIndicators() {
    for (let i = 0; i < totalSlides; i++) {
        const indicator = document.createElement('div');
        indicator.className = 'indicator' + (i === 0 ? ' active' : '');
        indicator.onclick = () => goToSlide(i);
        indicatorsContainer.appendChild(indicator);
    }
}

function updateIndicators() {
    const indicators = document.querySelectorAll('.indicator');
    indicators.forEach((indicator, index) => {
        indicator.classList.toggle('active', index === currentSlide);
    });
}

function goToSlide(index) {
    currentSlide = index;
    wrapper.style.transform = `translateX(-${currentSlide * 100}%)`;
    updateIndicators();
}

function nextSlide() {
    currentSlide = (currentSlide + 1) % totalSlides;
    goToSlide(currentSlide);
}

function openPaymentModal(amount, type) {
    requireLogin(() => {
        document.getElementById('payAmount').textContent = amount;
        document.getElementById('paymentType').textContent = type;
        document.getElementById('paymentModal').classList.add('active');
    });
}

function closePaymentModal() {
    document.getElementById('paymentModal').classList.remove('active');
}

function formatTime(seconds) {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    const hStr = hours.toString().padStart(2, '0');
    const mStr = minutes.toString().padStart(2, '0');
    const sStr = secs.toString().padStart(2, '0');
    
    return `${hStr}:${mStr}:${sStr}`;
}

function initCountdown() {
    const elements = document.querySelectorAll('.countdown');
    elements.forEach((el, index) => {
        const totalSeconds = parseInt(el.getAttribute('data-total')) || 349;
        countdownElements.push({
            element: el,
            totalSeconds: totalSeconds,
            remainingSeconds: totalSeconds
        });
        el.textContent = formatTime(totalSeconds);
    });
}

function updateCountdowns() {
    countdownElements.forEach((countdown, index) => {
        countdown.remainingSeconds--;
        
        if (countdown.remainingSeconds < 0) {
            countdown.remainingSeconds = countdown.totalSeconds;
        }
        
        countdown.element.textContent = formatTime(countdown.remainingSeconds);
    });
}

function confirmPayment() {
    const amount = document.getElementById('payAmount').textContent;
    const paymentType = document.getElementById('paymentType').textContent;
    const username = getUsername();
    
    const orderInfo = {
        orderId: 'ORDER_' + Date.now(),
        username: username,
        amount: amount,
        type: paymentType,
        time: new Date().toLocaleString()
    };
    
    console.log('结算信息：', orderInfo);
    
    alert(`支付成功！\n订单号：${orderInfo.orderId}\n金额：¥${amount}\n类型：${paymentType}`);
    
    closePaymentModal();
}

document.addEventListener('DOMContentLoaded', function() {
    wrapper = document.getElementById('carouselWrapper');
    indicatorsContainer = document.getElementById('indicators');
    
    updateHeader();
    initIndicators();
    initCountdown();
    setInterval(nextSlide, 3000);
    setInterval(updateCountdowns, 1000);
    
    document.getElementById('paymentModal').addEventListener('click', function(e) {
        if (e.target === this) {
            closePaymentModal();
        }
    });
});
