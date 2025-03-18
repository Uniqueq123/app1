# Safety Monitor App

An Android application that monitors notifications with a Node.js backend server.

## Project Structure

- `app/` - Android application
  - Monitors device notifications
  - Sends notification data to server
  - Requires notification access permission

- `server/` - Node.js backend server
  - Receives and stores notifications
  - Provides API endpoints for notification data
  - Protected with API key authentication

## Setup Instructions

### Android App Setup
1. Open the project in Android Studio
2. Configure `AppConfig.java` with your server URL
3. Build and run the app on your device
4. Grant notification access permission when prompted

### Server Setup
1. Navigate to the server directory:
   ```bash
   cd server
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the server:
   ```bash
   npm start
   ```

## API Endpoints

- `GET /test` - Test server connection
- `GET /api/notifications` - Retrieve all notifications
- `POST /api/notifications` - Submit a new notification

## Security
- API endpoints are protected with API key authentication
- Ensure proper API key configuration in both app and server 