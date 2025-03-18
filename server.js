const express = require('express');
const cors = require('cors');
const app = express();
const port = process.env.PORT || 3000;
const host = '0.0.0.0';

// Enable CORS
app.use(cors({
    origin: ['https://your-site.netlify.app', 'http://localhost:3000'],
    methods: ['GET', 'POST', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'X-API-Key']
}));

app.use(express.json());

// Serve static files from 'public' directory
app.use(express.static('public'));

// In-memory storage for notifications
let notifications = [];

// Simple API key authentication
const API_KEY = 'safety-monitor-123';

// Test endpoint (no authentication)
app.get('/test', (req, res) => {
    res.json({ message: 'Server is running!' });
});

// API authentication middleware
app.use('/api', (req, res, next) => {
    const apiKey = req.headers['x-api-key'];
    if (!apiKey || apiKey !== API_KEY) {
        return res.status(401).json({ error: 'Unauthorized' });
    }
    next();
});

// Protected API endpoints
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
    console.log('\nAvailable endpoints:');
    console.log('  - GET  /test     : Server test');
    console.log('  - GET  /api/notifications  : Retrieve notifications');
    console.log('  - POST /api/notifications  : Submit notification data');
}); 