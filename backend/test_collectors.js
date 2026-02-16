const http = require('http');

const options = {
    hostname: 'localhost',
    port: 5000,
    path: '/api/admin/collectors', // Checking admin route as well
    method: 'GET'
};

const req = http.request(options, (res) => {
    console.log(`GET /api/admin/collectors STATUS: ${res.statusCode}`);
    let data = '';
    res.on('data', (chunk) => data += chunk);
    res.on('end', () => console.log('RESPONSE:', data.substring(0, 100)));
});

req.on('error', (e) => console.error("Request error:", e.message));
req.end();

const options2 = {
    hostname: 'localhost',
    port: 5000,
    path: '/api/collectors', // Checking public route
    method: 'GET'
};

const req2 = http.request(options2, (res) => {
    console.log(`GET /api/collectors STATUS: ${res.statusCode}`);
    let data = '';
    res.on('data', (chunk) => data += chunk);
    res.on('end', () => console.log('RESPONSE:', data.substring(0, 100)));
});
req2.on('error', (e) => console.error("Request error:", e.message));
req2.end();
