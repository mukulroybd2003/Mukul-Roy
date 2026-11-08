<?php
declare(strict_types=1);

namespace MateChat\Config;

class Config {
    private static array $env = [];

    public static function load(string $path = __DIR__ . '/../.env'): void {
        if (!file_exists($path)) {
            $path = __DIR__ . '/../.env.example';
        }
        if (file_exists($path)) {
            $lines = file($path, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
            foreach ($lines as $line) {
                $line = trim($line);
                if ($line === '' || str_starts_with($line, '#')) {
                    continue;
                }
                if (str_contains($line, '=')) {
                    [$k, $v] = explode('=', $line, 2);
                    self::$env[trim($k)] = trim(trim($v), '"\'');
                }
            }
        }
    }

    public static function get(string $key, mixed $default = null): mixed {
        return self::$env[$key] ?? getenv($key) ?: $default;
    }
}
