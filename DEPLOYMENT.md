# MATE CHAT – PRODUCTION DEPLOYMENT & DEVOPS

## Production Architecture

```
[Clients: Android APK / React Web]
       │
       ▼ (HTTPS / WSS via Cloudflare / Nginx Reverse Proxy)
[Nginx Web Server]
       ├── /api/v1/*   ──► PHP-FPM 8.2 (REST API)
       ├── /ws         ──► Ratchet WebSocket Daemon (Supervisor)
       └── /admin      ──► Secure Admin Portal (Static SPA)
       │
       ▼
[MySQL 8.0 Primary Database] (InnoDB, utf8mb4)
```

---

## 1. Nginx Reverse Proxy & SSL Configuration
```nginx
server {
    listen 443 ssl http2;
    server_name api.matechat.app;

    ssl_certificate /etc/letsencrypt/live/api.matechat.app/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.matechat.app/privkey.pem;

    root /var/www/mate-chat/backend/public;
    index index.php;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/var/run/php/php8.2-fpm.sock;
    }

    # WebSocket Real-Time Gateway
    location /ws {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400;
    }
}
```

---

## 2. Supervisor Configuration for WebSocket Service
Create `/etc/supervisor/conf.d/mate-chat-ws.conf`:
```ini
[program:mate-chat-websocket]
command=php /var/www/mate-chat/backend/websocket/server.php
autostart=true
autorestart=true
user=www-data
redirect_stderr=true
stdout_logfile=/var/log/mate-chat-ws.log
```
Run `supervisorctl reread && supervisorctl update`.
