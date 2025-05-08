const express = require('express');
const { exec } = require('child_process');
const app = express();
const port = process.env.PORT || 8080;

// Start our Java application in the background
function startJavaApp() {
    console.log('Starting Java application in the background...');
    const javaProcess = exec('java -jar app.jar');

    javaProcess.stdout.on('data', (data) => {
        console.log(`Java app stdout: ${data}`);
    });

    javaProcess.stderr.on('data', (data) => {
        console.error(`Java app stderr: ${data}`);
    });

    javaProcess.on('close', (code) => {
        console.log(`Java app process exited with code ${code}`);
    });
}

// Simple endpoint for health checks
app.get('/', (req, res) => {
    res.send('Proxy server is running!');
});

// Start the server
app.listen(port, '0.0.0.0', () => {
    console.log(`Proxy server listening at http://0.0.0.0:${port}`);
    startJavaApp();
});