const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const app = express();
const port = process.env.PORT || 8080;

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'UP' });
});

// Proxy all other requests to the Spring Boot app
app.use('/', createProxyMiddleware({
    target: 'http://localhost:8081',
    changeOrigin: true,
    onError: (err, req, res) => {
        console.error('Proxy error:', err);
        res.status(503).json({ status: 'DOWN', message: 'Application starting...' });
    }
}));

app.listen(port, '0.0.0.0', () => {
    console.log(`Proxy listening on port ${port}`);
});