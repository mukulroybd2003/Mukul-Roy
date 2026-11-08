<?php
declare(strict_types=1);

// MATE CHAT - REST API ENTRYPOINT
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

spl_autoload_register(function ($class) {
    $prefix = 'MateChat\\';
    $baseDir = __DIR__ . '/../app/';
    $len = strlen($prefix);
    if (strncmp($prefix, $class, $len) !== 0) {
        return;
    }
    $relativeClass = substr($class, $len);
    $file = $baseDir . str_replace('\\', '/', $relativeClass) . '.php';
    if (file_exists($file)) {
        require_once $file;
    }
});

require_once __DIR__ . '/../config/config.php';
\MateChat\Config\Config::load();

$uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$method = $_SERVER['REQUEST_METHOD'];

// Health check
if ($uri === '/api/v1/health' || $uri === '/health') {
    echo json_encode([
        'status' => 'healthy',
        'app' => 'MATE CHAT REST API',
        'version' => '1.0.0',
        'timestamp' => date('c'),
        'e2ee' => 'enforced'
    ]);
    exit;
}

// Router
try {
    if ($uri === '/api/v1/auth/register' && $method === 'POST') {
        (new \MateChat\Controllers\AuthController())->register();
        exit;
    }

    if ($uri === '/api/v1/auth/login' && $method === 'POST') {
        (new \MateChat\Controllers\AuthController())->login();
        exit;
    }

    // Authenticated routes
    $session = \MateChat\Middleware\AuthMiddleware::authenticate();

    if ($uri === '/api/v1/auth/logout' && $method === 'POST') {
        (new \MateChat\Controllers\AuthController())->logout($session);
        exit;
    }

    if ($uri === '/api/v1/conversations' && $method === 'GET') {
        (new \MateChat\Controllers\ConversationController())->list($session);
        exit;
    }

    if ($uri === '/api/v1/conversations/direct' && $method === 'POST') {
        (new \MateChat\Controllers\ConversationController())->createDirect($session);
        exit;
    }

    if (preg_match('#^/api/v1/conversations/(\d+)/messages$#', $uri, $m) && $method === 'GET') {
        (new \MateChat\Controllers\MessageController())->getMessages($session, (int)$m[1]);
        exit;
    }

    if ($uri === '/api/v1/messages' && $method === 'POST') {
        (new \MateChat\Controllers\MessageController())->sendMessage($session);
        exit;
    }

    if ($uri === '/api/v1/keys/bundle' && $method === 'POST') {
        (new \MateChat\Controllers\KeysController())->uploadBundle($session);
        exit;
    }

    if (preg_match('#^/api/v1/keys/bundle/(\d+)$#', $uri, $m) && $method === 'GET') {
        (new \MateChat\Controllers\KeysController())->fetchBundle($session, (int)$m[1]);
        exit;
    }

    if ($uri === '/api/v1/admin/stats' && $method === 'GET') {
        (new \MateChat\Controllers\AdminController())->getDashboardStats();
        exit;
    }

    if ($uri === '/api/v1/admin/users' && $method === 'GET') {
        (new \MateChat\Controllers\AdminController())->listUsers();
        exit;
    }

    if (preg_match('#^/api/v1/admin/users/(\d+)/status$#', $uri, $m) && $method === 'POST') {
        (new \MateChat\Controllers\AdminController())->toggleUserStatus((int)$m[1]);
        exit;
    }

    // Default 404
    http_response_code(404);
    echo json_encode(['error' => 'Endpoint not found', 'path' => $uri]);
} catch (\Throwable $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Internal server error: ' . $e->getMessage()]);
}
