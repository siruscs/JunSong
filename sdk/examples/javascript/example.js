/**
 * JunSong开放平台 JavaScript/Node.js SDK 使用示例
 *
 * 演示如何使用HMAC-SHA256签名调用开放API
 */
const crypto = require('crypto');
const http = require('http');

const APP_KEY = 'js_d30da74ff5b14d5cb6fa2bf97789d1dc';
const APP_SECRET = 'f487dccac151496f8b9d33e6977d80aceec4591be3c64d4188d8dfb9b4d3d707';

function callOpenApi(method, path, body = '') {
    const timestamp = Date.now().toString();
    const nonce = crypto.randomBytes(16).toString('hex');
    const signStr = method + '/openapi/v1' + path + timestamp + nonce + body;
    const signature = crypto.createHmac('sha256', APP_SECRET).update(signStr).digest('hex');

    const options = {
        hostname: 'localhost',
        port: 8081,
        path: `/openapi/v1${path}`,
        method: method,
        headers: {
            'X-App-Key': APP_KEY,
            'X-App-Timestamp': timestamp,
            'X-App-Nonce': nonce,
            'X-App-Signature': signature,
        },
    };

    const req = http.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => (data += chunk));
        res.on('end', () => {
            console.log(`状态码: ${res.statusCode}`);
            console.log(`X-API-Version: ${res.headers['x-api-version'] || '无'}`);
            console.log(`X-RateLimit-Limit: ${res.headers['x-ratelimit-limit'] || '无'}`);
            console.log(`X-RateLimit-Remaining: ${res.headers['x-ratelimit-remaining'] || '无'}`);
            console.log(`响应: ${data}`);
        });
    });
    req.end();
}

console.log('=== 查询应用列表 ===');
callOpenApi('GET', '/app/list');
