const http = require('http');
const fs = require('fs');
const path = require('path');

const boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW';
const householdId = '6953bf0e6d8f8ae3567d7520';
// Using the image we found in uploads
const imagePath = 'c:\\Users\\jeyal\\Downloads\\Projects\\WasteManagement\\backend\\uploads\\household.jpg';

const jsonData = JSON.stringify({
    household: householdId,
    wasteType: 'Plastic',
    address: 'Test Generated Pickup',
    // Random coordinates for map implementation if needed
    latitude: 6.9271,
    longitude: 79.8612
});

try {
    const fileContent = fs.readFileSync(imagePath);
    const filename = path.basename(imagePath);

    let bodyParts = [];

    // Part 1: data
    bodyParts.push(Buffer.from(`--${boundary}\r\n`));
    bodyParts.push(Buffer.from(`Content-Disposition: form-data; name="data"\r\n\r\n`));
    bodyParts.push(Buffer.from(`${jsonData}\r\n`));

    // Part 2: image
    bodyParts.push(Buffer.from(`--${boundary}\r\n`));
    bodyParts.push(Buffer.from(`Content-Disposition: form-data; name="image"; filename="${filename}"\r\n`));
    bodyParts.push(Buffer.from(`Content-Type: image/jpeg\r\n\r\n`));
    bodyParts.push(fileContent);
    bodyParts.push(Buffer.from(`\r\n--${boundary}--\r\n`));

    const postData = Buffer.concat(bodyParts);

    const options = {
        hostname: 'localhost',
        port: 5000,
        path: '/api/pickups/upload',
        method: 'POST',
        headers: {
            'Content-Type': `multipart/form-data; boundary=${boundary}`,
            'Content-Length': postData.length
        }
    };

    const req = http.request(options, (res) => {
        console.log(`STATUS: ${res.statusCode}`);
        let data = '';
        res.on('data', (chunk) => data += chunk);
        res.on('end', () => console.log('RESPONSE: ' + data));
    });

    req.on('error', (e) => {
        console.error(`REQUEST ERROR: ${e.message}`);
    });

    req.write(postData);
    req.end();

} catch (err) {
    console.error("FILE ERROR:", err.message);
}
