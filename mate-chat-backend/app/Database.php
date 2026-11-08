<?php
declare(strict_types=1);

namespace MateChat;

use PDO;
use PDOException;
use MateChat\Config\Config;

class Database {
    private static ?PDO $instance = null;

    public static function getConnection(): PDO {
        if (self::$instance === null) {
            Config::load();
            $host = Config::get('DB_HOST', '127.0.0.1');
            $port = Config::get('DB_PORT', '3306');
            $db   = Config::get('DB_DATABASE', 'mate_chat');
            $user = Config::get('DB_USERNAME', 'mate_user');
            $pass = Config::get('DB_PASSWORD', '');

            $dsn = "mysql:host={$host};port={$port};dbname={$db};charset=utf8mb4";
            $options = [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
            ];

            try {
                self::$instance = new PDO($dsn, $user, $pass, $options);
            } catch (PDOException $e) {
                // In production, log error and do not expose credentials
                error_log("Database connection failed: " . $e->getMessage());
                throw new \RuntimeException("Database connection unavailable", 500);
            }
        }
        return self::$instance;
    }
}
