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
    res.json(notifications);
});

// POST endpoint to receive notification data
app.post('/api/notifications', (req, res) => {
    const notification = {
        id: Date.now(),
        timestamp: new Date().toISOString(),
        ...req.body
    };
    
    notifications.push(notification);
    console.log('Received notification:', notification);
    
    res.status(201).json({
        message: 'Notification received successfully',
        notification
    });
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
}); 