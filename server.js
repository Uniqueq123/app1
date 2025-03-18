const express = require('express');
const cors = require('cors');
const app = express();
const port = process.env.PORT || 3000;
const host = '0.0.0.0';

// Enable CORS
app.use(cors({
    origin: '*',
    methods: ['GET', 'POST', 'OPTIONS'],
    allowedHeaders: ['Content-Type']
}));

app.use(express.json());

// Serve static files from 'public' directory
app.use(express.static('public'));

// In-memory storage for notifications
let notifications = [];

// Store device statuses
let deviceStatuses = [];

// Redirect root to login page
app.get('/', (req, res) => {
    res.redirect('/login.html');
});

// Test endpoint
app.get('/test', (req, res) => {
    res.json({ message: 'Server is running!' });
});

// Get notifications endpoint (no API key required anymore)
app.get('/api/notifications', (req, res) => {
    console.log('Current notifications:', notifications);
    res.json(notifications);
});

// POST endpoint to receive notification data
app.post('/api/notifications', (req, res) => {
    console.log('Received notification request:', {
        headers: req.headers,
        body: req.body
    });

    const notification = {
        id: Date.now(),
        timestamp: new Date().toISOString(),
        ...req.body
    };
    
    notifications.push(notification);
    console.log('Stored notification:', notification);
    
    res.status(201).json({
        message: 'Notification received successfully',
        notification
    });
});

// Device status endpoint
app.post('/api/device-status', (req, res) => {
    const deviceStatus = {
        ...req.body,
        lastSeen: new Date().toISOString()
    };
    
    // Update or add device status
    const index = deviceStatuses.findIndex(d => d.deviceId === deviceStatus.deviceId);
    if (index >= 0) {
        deviceStatuses[index] = deviceStatus;
    } else {
        deviceStatuses.push(deviceStatus);
    }
    
    console.log('Device status updated:', deviceStatus);
    res.status(200).json({ message: 'Status updated' });
});

// Get active devices
app.get('/api/devices', (req, res) => {
    // Remove devices not seen in last 24 hours
    const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
    deviceStatuses = deviceStatuses.filter(device => 
        new Date(device.lastSeen) > oneDayAgo
    );
    
    res.json(deviceStatuses);
});

// Start the server
app.listen(port, host, () => {
    console.log(`Server running at http://${host}:${port}`);
    console.log(`Local access: http://localhost:${port}`);
    console.log(`Network access: http://192.168.8.132:${port}`);
    console.log('\nAvailable endpoints:');
    console.log('  - GET  /         : HTML landing page');
    console.log('  - GET  /test     : Server test');
    console.log('  - GET  /api/notifications  : Retrieve notifications');
    console.log('  - POST /api/notifications  : Submit notification data');
    console.log('  - POST /api/device-status  : Submit device status');
    console.log('  - GET  /api/devices      : Retrieve active devices');
}); 