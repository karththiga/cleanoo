const http = require('http');

// 1. Get All Pickups to find an ID
const optionsList = {
    hostname: 'localhost',
    port: 5000,
    path: '/api/pickups',
    method: 'GET'
};

const reqList = http.request(optionsList, (res) => {
    let data = '';
    res.on('data', (chunk) => data += chunk);
    res.on('end', () => {
        try {
            const json = JSON.parse(data);
            if (json.data && json.data.length > 0) {
                const id = json.data[0]._id;
                console.log(`Testing GET /api/pickups/${id}...`);
                testSinglePickup(id);
            } else {
                console.log("No pickups found to test.");
            }
        } catch (e) {
            console.error("Failed to parse list response:", e.message);
        }
    });
});
reqList.end();

function testSinglePickup(id) {
    const options = {
        hostname: 'localhost',
        port: 5000,
        path: `/api/pickups/${id}`,
        method: 'GET'
    };

    const req = http.request(options, (res) => {
        console.log(`GET /api/pickups/${id} STATUS: ${res.statusCode}`);
        if (res.statusCode === 404) {
            console.log("CONFIRMED: Endpoint missing (404)");
        } else {
            console.log("Endpoint exists?");
        }
    });
    req.end();
}
