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

function goBack() {
    window.location.href = 'index.html';
}

function handleLogin() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    if (!username || !password) {
        alert('请输入用户名和密码');
        return;
    }
    
    const userInfo = {
        username: username,
        loginTime: new Date().toISOString()
    };
    
    setCookie('userInfo', JSON.stringify(userInfo), 7);
    setCookie('isLoggedIn', 'true', 7);
    setCookie('username', username, 7);
    
    alert('登录成功！欢迎 ' + username);
    
    window.location.href = 'index.html';
}

function checkLoginStatus() {
    const isLoggedIn = getCookie('isLoggedIn');
    if (isLoggedIn === 'true') {
        window.location.href = 'index.html';
    }
}

document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();
    
    document.getElementById('password').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            handleLogin();
        }
    });
});
