let currentSlide = 0;
const totalSlides = 3;
let wrapper, indicatorsContainer;
const countdownElements = [];
let currentOutTradeNo = '';
let currentPaymentType = '';
let currentTeamId = '';
const REFRESH_INTERVAL = 5000;
let refreshIntervalId = null;

function randomNumeric(length) {
    let result = '';
    for (let i = 0; i < length; i++) {
        result += Math.floor(Math.random() * 10);
    }
    return result;
}

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
        fetchGroupBuyMarketConfig();
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

async function openPaymentModal(amount, type, teamId = '') {
    requireLogin(async () => {
        document.getElementById('payAmount').textContent = amount;
        document.getElementById('paymentType').textContent = type;
        currentPaymentType = type;
        currentTeamId = teamId;
        
        if (type === '开团购买' || type === '抢单拼团') {
            const username = getUsername();
            let userId;
            if (username) {
                userId = username;
            } else {
                userId = "lipeicheng";
                console.warn('无法获取当前登录用户名，使用默认 userId: lipeicheng');
            }
            
            currentOutTradeNo = randomNumeric(12);
            
            const requestData = {
                userId: userId,
                teamId: teamId,
                activityId: 100123,
                goodsId: "9890001",
                source: "s01",
                channel: "c01",
                outTradeNo: currentOutTradeNo,
                notifyUrl: "http://117.72.145.235:8091/api/v1/test/group_buy_notify"
            };
            
            try {
                const response = await fetch('http://117.72.145.235:8091/api/v1/gbm/trade/lock_market_pay_order', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(requestData)
                });
                
                const result = await response.json();
                
                if (result.code === '0000') {
                    console.log('锁单成功：', result.data);
                    document.getElementById('paymentModal').classList.add('active');
                } else {
                    alert('锁单失败：' + result.info);
                }
            } catch (error) {
                console.error('锁单接口调用失败：', error);
                alert('锁单失败，请稍后重试');
            }
        } else {
            document.getElementById('paymentModal').classList.add('active');
        }
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

function parseTimeToSeconds(timeStr) {
    if (!timeStr) return 0;
    const parts = timeStr.split(':').map(Number);
    if (parts.length === 3) {
        return parts[0] * 3600 + parts[1] * 60 + parts[2];
    } else if (parts.length === 2) {
        return parts[0] * 60 + parts[1];
    }
    return 0;
}

function initCountdown() {
    countdownElements.length = 0;
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
    const elementsToRemove = [];
    
    countdownElements.forEach((countdown, index) => {
        countdown.remainingSeconds--;
        
        if (countdown.remainingSeconds <= 0) {
            elementsToRemove.push(index);
        } else {
            countdown.element.textContent = formatTime(countdown.remainingSeconds);
        }
    });
    
    for (let i = elementsToRemove.length - 1; i >= 0; i--) {
        const index = elementsToRemove[i];
        const countdown = countdownElements[index];
        const groupItem = countdown.element.closest('.group-item');
        
        if (groupItem) {
            groupItem.style.transition = 'opacity 0.3s, transform 0.3s';
            groupItem.style.opacity = '0';
            groupItem.style.transform = 'translateX(-100%)';
            
            setTimeout(() => {
                try {
                    if (groupItem.parentNode) {
                        groupItem.remove();
                    }
                } catch (error) {
                    console.error('删除拼团项时出错:', error);
                }
                
                const groupList = document.querySelector('.group-list');
                if (groupList) {
                    const remainingItems = groupList.querySelectorAll('.group-item');
                    if (remainingItems.length === 0) {
                        groupList.innerHTML = '<div class="empty-team">小伙伴，赶紧去开团吧，做村里最靓的仔。</div>';
                    }
                }
            }, 300);
        }
        
        countdownElements.splice(index, 1);
    }
}

async function confirmPayment() {
    const amount = document.getElementById('payAmount').textContent;
    const paymentType = document.getElementById('paymentType').textContent;
    const username = getUsername();
    
    if (currentPaymentType === '开团购买' || currentPaymentType === '抢单拼团') {
        let userId;
        if (username) {
            userId = username;
        } else {
            userId = "lipeicheng";
            console.warn('无法获取当前登录用户名，使用默认 userId: lipeicheng');
        }
        
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        const outTradeTime = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        
        const requestData = {
            source: "s01",
            channel: "c01",
            userId: userId,
            outTradeNo: currentOutTradeNo,
            outTradeTime: outTradeTime
        };
        
        try {
            const response = await fetch('http://117.72.145.235:8091/api/v1/gbm/trade/settlement_market_pay_order', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            });
            
            const result = await response.json();
            
            if (result.code === '0000') {
                console.log('结算成功：', result.data);
                alert(`支付成功！\n订单号：${currentOutTradeNo}\n金额：¥${amount}\n类型：${paymentType}`);
                closePaymentModal();
                await fetchGroupBuyMarketConfig();
            } else {
                alert('结算失败：' + result.info);
            }
        } catch (error) {
            console.error('结算接口调用失败：', error);
            alert('结算失败，请稍后重试');
        }
    } else {
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
        await fetchGroupBuyMarketConfig();
    }
}

async function refreshDataSilently() {
    const paymentModal = document.getElementById('paymentModal');
    if (paymentModal && paymentModal.classList.contains('active')) {
        return;
    }
    
    try {
        await fetchGroupBuyMarketConfig();
    } catch (error) {
        console.warn('静默刷新数据失败:', error);
    }
}

async function fetchGroupBuyMarketConfig() {
    const username = getUsername();
    
    let userId;
    if (username) {
        userId = username;
    } else {
        userId = "lipeicheng";
        console.warn('无法获取当前登录用户名，使用默认 userId: lipeicheng');
    }
    
    const requestData = {
        userId: userId,
        source: "s01",
        channel: "c01",
        goodsId: "9890001"
    };
    
    try {
        const response = await fetch('http://117.72.145.235:8091/api/v1/gbm/index/query_group_buy_market_config', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });
        
        const result = await response.json();
        
        if (result.code === '0000') {
            renderPage(result.data);
        }
    } catch (error) {
        console.error('API 调用失败:', error);
    }
}

function renderPage(data) {
    const { goods, teamList, teamStatistic } = data;
    
    renderPromoBanner(goods, teamStatistic);
    renderTeamList(teamList, goods);
    renderPriceButtons(goods);
}

function renderPromoBanner(goods, teamStatistic) {
    const promoBanner = document.querySelector('.promo-banner');
    if (promoBanner) {
        promoBanner.textContent = `大促优惠 | 直降 ¥${goods.deductionPrice}，${teamStatistic.allTeamUserCount}人再抢，参与马上抢到`;
    }
}

function renderTeamList(teamList, goods) {
    const groupList = document.querySelector('.group-list');
    if (!groupList) return;
    
    if (!teamList || teamList.length === 0) {
        groupList.innerHTML = '<div class="empty-team">小伙伴，赶紧去开团吧，做村里最靓的仔。</div>';
        return;
    }
    
    groupList.innerHTML = teamList.map((team, index) => {
        const remainingCount = team.targetCount - team.lockCount;
        const userFirstChar = team.userId.charAt(0).toUpperCase();
        const totalSeconds = parseTimeToSeconds(team.validTimeCountdown);
        return `
            <div class="group-item">
                <div class="avatar">${userFirstChar}</div>
                <div class="group-info">
                    <div class="group-user">${team.userId}</div>
                    <div class="group-time">拼单即将结束 <span class="countdown" id="countdown${index + 1}" data-total="${totalSeconds}">${team.validTimeCountdown}</span></div>
                    <div class="group-remaining">还差 ${remainingCount} 人</div>
                </div>
                <button class="join-btn" onclick="openPaymentModal(${goods.payPrice}, '抢单拼团', '${team.teamId}')">立即抢单</button>
            </div>
        `;
    }).join('');
    
    initCountdown();
}

function renderPriceButtons(goods) {
    const singleBuyPrice = document.getElementById('singleBuyPrice');
    const groupBuyPrice = document.getElementById('groupBuyPrice');
    const singleBuyBtn = document.getElementById('singleBuyBtn');
    const groupBuyBtn = document.getElementById('groupBuyBtn');
    
    if (singleBuyPrice) {
        singleBuyPrice.textContent = goods.originalPrice;
    }
    
    if (groupBuyPrice) {
        groupBuyPrice.textContent = goods.payPrice;
    }
    
    if (singleBuyBtn) {
        singleBuyBtn.setAttribute('onclick', `openPaymentModal(${goods.originalPrice}, '单独购买')`);
    }
    
    if (groupBuyBtn) {
        groupBuyBtn.setAttribute('onclick', `openPaymentModal(${goods.payPrice}, '开团购买')`);
    }
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
    
    fetchGroupBuyMarketConfig();
});
