# MATE CHAT – INSTALLATION & LOCAL SETUP

## Prerequisites
- Android Studio Ladybug / Meerkat or CLI Gradle 8+ with JDK 17+
- PHP 8.2+ with `pdo`, `pdo_mysql`, `openssl`, `json` extensions
- MySQL 8.0+
- Node.js 18+ & npm for the Website

---

## 1. Database Setup (MySQL 8+)
```bash
# Log in to MySQL
mysql -u root -p

# Import the complete production schema
mysql -u root -p < mate-chat-database/schema.sql
```

---

## 2. PHP Backend Setup
```bash
cd mate-chat-backend

# Copy environment configuration
cp .env.example .env

# Configure database credentials in .env
nano .env

# Install Composer dependencies
composer install

# Start local PHP REST development server
php -S 0.0.0.0:8000 -t public/

# In a separate terminal, launch the WebSocket server
php websocket/server.php
```

---

## 3. Website Setup (React + TypeScript)
```bash
cd mate-chat-website

# Install dependencies
npm install

# Start Vite development server
npm run dev
# The website will be available at http://localhost:5173
```

---

## 4. Android Application Build
The root repository is already configured with Gradle Kotlin DSL:
```bash
# Verify unit and Robolectric tests
gradle :app:testDebugUnitTest

# Assemble Debug APK
gradle :app:assembleDebug
```
The output APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
