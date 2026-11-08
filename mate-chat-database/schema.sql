-- =====================================================================
-- MATE CHAT - COMPLETE PRODUCTION DATABASE SCHEMA
-- Database: mate_chat
-- Engine: InnoDB, Charset: utf8mb4, Collation: utf8mb4_unicode_ci
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `mate_chat`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `mate_chat`;

SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1. ACCOUNT & AUTHENTICATION
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `refresh_tokens`;
DROP TABLE IF EXISTS `user_sessions`;
DROP TABLE IF EXISTS `devices`;
DROP TABLE IF EXISTS `user_profiles`;
DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `email` VARCHAR(191) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `status` ENUM('active', 'suspended', 'pending_verification', 'deactivated') NOT NULL DEFAULT 'pending_verification',
  `email_verified_at` TIMESTAMP NULL DEFAULT NULL,
  `two_factor_enabled` TINYINT(1) NOT NULL DEFAULT 0,
  `two_factor_secret` VARCHAR(128) NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`),
  KEY `idx_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_profiles` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `full_name` VARCHAR(128) NOT NULL,
  `avatar_url` VARCHAR(512) NULL DEFAULT NULL,
  `cover_url` VARCHAR(512) NULL DEFAULT NULL,
  `bio` VARCHAR(255) NULL DEFAULT NULL,
  `about` TEXT NULL DEFAULT NULL,
  `phone` VARCHAR(32) NULL DEFAULT NULL,
  `date_of_birth` DATE NULL DEFAULT NULL,
  `gender` ENUM('unspecified', 'female', 'male', 'other') NOT NULL DEFAULT 'unspecified',
  `is_online` TINYINT(1) NOT NULL DEFAULT 0,
  `last_seen` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `custom_status` VARCHAR(128) NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  KEY `idx_profiles_online_lastseen` (`is_online`, `last_seen`),
  CONSTRAINT `fk_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `devices` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `device_uuid` VARCHAR(128) NOT NULL,
  `device_name` VARCHAR(128) NOT NULL,
  `platform` ENUM('android', 'ios', 'web', 'desktop') NOT NULL,
  `app_version` VARCHAR(32) NOT NULL DEFAULT '1.0.0',
  `is_trusted` TINYINT(1) NOT NULL DEFAULT 1,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `last_ip` VARCHAR(45) NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_device_uuid` (`user_id`, `device_uuid`),
  KEY `idx_devices_user` (`user_id`),
  CONSTRAINT `fk_devices_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `device_id` BIGINT UNSIGNED NOT NULL,
  `session_token` VARCHAR(128) NOT NULL,
  `ip_address` VARCHAR(45) NOT NULL,
  `user_agent` VARCHAR(255) NULL DEFAULT NULL,
  `expires_at` TIMESTAMP NOT NULL,
  `revoked_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_token` (`session_token`),
  KEY `idx_sessions_user_dev` (`user_id`, `device_id`),
  CONSTRAINT `fk_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sessions_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refresh_tokens` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL,
  `token_hash` VARCHAR(64) NOT NULL,
  `expires_at` TIMESTAMP NOT NULL,
  `is_revoked` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_hash` (`token_hash`),
  KEY `idx_refresh_tokens_session` (`session_id`),
  CONSTRAINT `fk_refresh_session` FOREIGN KEY (`session_id`) REFERENCES `user_sessions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. END-TO-END ENCRYPTION (E2EE) SECURITY INFRASTRUCTURE
-- (Server stores ONLY public identity keys, signed prekeys & one-time prekeys)
-- (Private keys are NEVER stored on server)
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `group_epochs`;
DROP TABLE IF EXISTS `key_bundles`;
DROP TABLE IF EXISTS `one_time_prekeys`;
DROP TABLE IF EXISTS `signed_prekeys`;
DROP TABLE IF EXISTS `identity_keys`;
DROP TABLE IF EXISTS `device_keys`;

CREATE TABLE `device_keys` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `device_id` BIGINT UNSIGNED NOT NULL,
  `public_identity_key` TEXT NOT NULL,
  `registration_id` INT UNSIGNED NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_devkey_device` (`device_id`),
  KEY `idx_devkeys_user` (`user_id`),
  CONSTRAINT `fk_devkeys_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_devkeys_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `identity_keys` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `public_key` TEXT NOT NULL,
  `key_fingerprint` VARCHAR(64) NOT NULL,
  `verified_by_user` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_idkeys_user` (`user_id`),
  CONSTRAINT `fk_idkeys_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `signed_prekeys` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `device_id` BIGINT UNSIGNED NOT NULL,
  `key_id` INT UNSIGNED NOT NULL,
  `public_key` TEXT NOT NULL,
  `signature` TEXT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_signed_prekey_dev` (`device_id`, `key_id`),
  CONSTRAINT `fk_signed_prekey_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `one_time_prekeys` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `device_id` BIGINT UNSIGNED NOT NULL,
  `key_id` INT UNSIGNED NOT NULL,
  `public_key` TEXT NOT NULL,
  `is_consumed` TINYINT(1) NOT NULL DEFAULT 0,
  `consumed_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_otp_device_consumed` (`device_id`, `is_consumed`),
  CONSTRAINT `fk_otp_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `key_bundles` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `device_id` BIGINT UNSIGNED NOT NULL,
  `identity_key` TEXT NOT NULL,
  `signed_prekey` TEXT NOT NULL,
  `signed_prekey_signature` TEXT NOT NULL,
  `last_rotated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keybundle_device` (`device_id`),
  CONSTRAINT `fk_keybundle_dev` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `group_epochs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT UNSIGNED NOT NULL,
  `epoch_number` INT UNSIGNED NOT NULL DEFAULT 1,
  `public_epoch_state` TEXT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_epoch_group` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 3. FRIENDS & SOCIAL GRAPH
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `blocked_users`;
DROP TABLE IF EXISTS `friendships`;
DROP TABLE IF EXISTS `friend_requests`;

CREATE TABLE `friend_requests` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `sender_id` BIGINT UNSIGNED NOT NULL,
  `receiver_id` BIGINT UNSIGNED NOT NULL,
  `status` ENUM('pending', 'accepted', 'rejected', 'cancelled') NOT NULL DEFAULT 'pending',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friend_req` (`sender_id`, `receiver_id`),
  KEY `idx_friend_req_receiver` (`receiver_id`, `status`),
  CONSTRAINT `fk_freq_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_freq_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `friendships` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `friend_id` BIGINT UNSIGNED NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friendship` (`user_id`, `friend_id`),
  KEY `idx_friendships_friend` (`friend_id`),
  CONSTRAINT `fk_friendship_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_friendship_friend` FOREIGN KEY (`friend_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `blocked_users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `blocker_id` BIGINT UNSIGNED NOT NULL,
  `blocked_id` BIGINT UNSIGNED NOT NULL,
  `reason` VARCHAR(255) NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_block_pair` (`blocker_id`, `blocked_id`),
  CONSTRAINT `fk_block_blocker` FOREIGN KEY (`blocker_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_block_blocked` FOREIGN KEY (`blocked_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 4. MESSAGING & CONVERSATIONS
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `pinned_messages`;
DROP TABLE IF EXISTS `saved_messages`;
DROP TABLE IF EXISTS `message_reactions`;
DROP TABLE IF EXISTS `message_receipts`;
DROP TABLE IF EXISTS `message_attachments`;
DROP TABLE IF EXISTS `messages`;
DROP TABLE IF EXISTS `conversation_members`;
DROP TABLE IF EXISTS `conversations`;

CREATE TABLE `conversations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `type` ENUM('direct', 'group') NOT NULL DEFAULT 'direct',
  `title` VARCHAR(128) NULL DEFAULT NULL,
  `avatar_url` VARCHAR(512) NULL DEFAULT NULL,
  `created_by` BIGINT UNSIGNED NOT NULL,
  `last_message_id` BIGINT UNSIGNED NULL DEFAULT NULL,
  `last_message_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `disappearing_seconds` INT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_conv_last_msg` (`last_message_at`),
  CONSTRAINT `fk_conv_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `conversation_members` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `conversation_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `role` ENUM('owner', 'admin', 'moderator', 'member') NOT NULL DEFAULT 'member',
  `is_muted` TINYINT(1) NOT NULL DEFAULT 0,
  `is_archived` TINYINT(1) NOT NULL DEFAULT 0,
  `is_pinned` TINYINT(1) NOT NULL DEFAULT 0,
  `last_read_message_id` BIGINT UNSIGNED NULL DEFAULT NULL,
  `unread_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `joined_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conv_user` (`conversation_id`, `user_id`),
  KEY `idx_conv_member_user` (`user_id`, `is_archived`, `is_pinned`),
  CONSTRAINT `fk_cm_conv` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cm_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `conversation_id` BIGINT UNSIGNED NOT NULL,
  `sender_id` BIGINT UNSIGNED NOT NULL,
  `sender_device_id` BIGINT UNSIGNED NOT NULL,
  `message_type` ENUM('text', 'image', 'video', 'audio', 'voice_note', 'document', 'system') NOT NULL DEFAULT 'text',
  `encrypted_payload` MEDIUMTEXT NOT NULL,
  `iv` VARCHAR(64) NOT NULL,
  `auth_tag` VARCHAR(64) NOT NULL,
  `key_fingerprint` VARCHAR(64) NOT NULL,
  `reply_to_id` BIGINT UNSIGNED NULL DEFAULT NULL,
  `is_edited` TINYINT(1) NOT NULL DEFAULT 0,
  `is_deleted_everyone` TINYINT(1) NOT NULL DEFAULT 0,
  `expires_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_msg_conv_time` (`conversation_id`, `created_at`),
  KEY `idx_msg_sender` (`sender_id`),
  CONSTRAINT `fk_msg_conv` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_msg_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `message_attachments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `message_id` BIGINT UNSIGNED NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `file_size_bytes` BIGINT UNSIGNED NOT NULL,
  `mime_type` VARCHAR(128) NOT NULL,
  `encrypted_file_path` VARCHAR(512) NOT NULL,
  `encryption_metadata` TEXT NOT NULL,
  `thumbnail_path` VARCHAR(512) NULL DEFAULT NULL,
  `waveform_data` TEXT NULL DEFAULT NULL,
  `duration_seconds` INT UNSIGNED NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_attach_msg` (`message_id`),
  CONSTRAINT `fk_attach_msg` FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `message_receipts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `message_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `device_id` BIGINT UNSIGNED NOT NULL,
  `status` ENUM('sent', 'delivered', 'read') NOT NULL DEFAULT 'delivered',
  `delivered_at` TIMESTAMP NULL DEFAULT NULL,
  `read_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receipt_msg_dev` (`message_id`, `device_id`),
  KEY `idx_receipt_user` (`user_id`),
  CONSTRAINT `fk_rec_msg` FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rec_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `message_reactions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `message_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `emoji` VARCHAR(32) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reaction` (`message_id`, `user_id`, `emoji`),
  CONSTRAINT `fk_react_msg` FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_react_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `saved_messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `message_id` BIGINT UNSIGNED NOT NULL,
  `note` VARCHAR(255) NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_saved_user_msg` (`user_id`, `message_id`),
  CONSTRAINT `fk_saved_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_saved_msg` FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `pinned_messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `conversation_id` BIGINT UNSIGNED NOT NULL,
  `message_id` BIGINT UNSIGNED NOT NULL,
  `pinned_by` BIGINT UNSIGNED NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pin_conv_msg` (`conversation_id`, `message_id`),
  CONSTRAINT `fk_pin_conv` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pin_msg` FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 5. GROUPS & PERMISSIONS
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `group_invitations`;
DROP TABLE IF EXISTS `group_members`;
DROP TABLE IF EXISTS `groups`;

CREATE TABLE `groups` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `conversation_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `description` TEXT NULL DEFAULT NULL,
  `icon_url` VARCHAR(512) NULL DEFAULT NULL,
  `invite_code` VARCHAR(64) NOT NULL,
  `is_public` TINYINT(1) NOT NULL DEFAULT 0,
  `only_admins_message` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_conv` (`conversation_id`),
  UNIQUE KEY `uk_group_invite` (`invite_code`),
  CONSTRAINT `fk_grp_conv` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `group_members` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `role` ENUM('owner', 'admin', 'moderator', 'member') NOT NULL DEFAULT 'member',
  `can_post` TINYINT(1) NOT NULL DEFAULT 1,
  `can_invite` TINYINT(1) NOT NULL DEFAULT 1,
  `joined_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grp_member` (`group_id`, `user_id`),
  KEY `idx_grp_members_user` (`user_id`),
  CONSTRAINT `fk_gm_grp` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_gm_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `group_invitations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT UNSIGNED NOT NULL,
  `invited_by` BIGINT UNSIGNED NOT NULL,
  `invitee_id` BIGINT UNSIGNED NOT NULL,
  `status` ENUM('pending', 'accepted', 'rejected', 'expired') NOT NULL DEFAULT 'pending',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grp_invite` (`group_id`, `invitee_id`),
  CONSTRAINT `fk_gi_grp` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_gi_by` FOREIGN KEY (`invited_by`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_gi_invitee` FOREIGN KEY (`invitee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 6. VOICE & VIDEO CALLS (WEBRTC)
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `call_participants`;
DROP TABLE IF EXISTS `calls`;

CREATE TABLE `calls` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `channel_name` VARCHAR(128) NOT NULL,
  `caller_id` BIGINT UNSIGNED NOT NULL,
  `call_type` ENUM('audio', 'video') NOT NULL DEFAULT 'audio',
  `is_group` TINYINT(1) NOT NULL DEFAULT 0,
  `status` ENUM('initiating', 'ringing', 'connected', 'ended', 'missed', 'rejected', 'busy') NOT NULL DEFAULT 'initiating',
  `started_at` TIMESTAMP NULL DEFAULT NULL,
  `ended_at` TIMESTAMP NULL DEFAULT NULL,
  `duration_seconds` INT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_channel` (`channel_name`),
  KEY `idx_calls_caller` (`caller_id`),
  CONSTRAINT `fk_call_caller` FOREIGN KEY (`caller_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `call_participants` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `call_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `status` ENUM('invited', 'accepted', 'declined', 'missed', 'left') NOT NULL DEFAULT 'invited',
  `joined_at` TIMESTAMP NULL DEFAULT NULL,
  `left_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cp_call_user` (`call_id`, `user_id`),
  KEY `idx_cp_user` (`user_id`),
  CONSTRAINT `fk_cp_call` FOREIGN KEY (`call_id`) REFERENCES `calls` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cp_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 7. NOTIFICATIONS & DEVICE TOKENS (FIREBASE CLOUD MESSAGING)
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `device_tokens`;
DROP TABLE IF EXISTS `notifications`;

CREATE TABLE `notifications` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `actor_id` BIGINT UNSIGNED NULL DEFAULT NULL,
  `type` ENUM('new_message', 'friend_request', 'request_accepted', 'group_invite', 'incoming_call', 'missed_call', 'mention', 'system') NOT NULL,
  `title` VARCHAR(128) NOT NULL,
  `body` VARCHAR(255) NOT NULL,
  `payload_json` TEXT NULL DEFAULT NULL,
  `is_read` TINYINT(1) NOT NULL DEFAULT 0,
  `read_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notif_user_unread` (`user_id`, `is_read`, `created_at`),
  CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `device_tokens` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `device_id` BIGINT UNSIGNED NOT NULL,
  `fcm_token` VARCHAR(512) NOT NULL,
  `platform` ENUM('android', 'ios', 'web') NOT NULL,
  `last_refreshed_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_device_fcm` (`user_id`, `device_id`),
  KEY `idx_fcm_token` (`fcm_token`(191)),
  CONSTRAINT `fk_dt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_dt_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 8. SOCIAL: ADDA ROOMS & USER PRESENCE
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `user_presence`;
DROP TABLE IF EXISTS `adda_members`;
DROP TABLE IF EXISTS `addas`;

CREATE TABLE `addas` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(128) NOT NULL,
  `topic` VARCHAR(255) NULL DEFAULT NULL,
  `host_id` BIGINT UNSIGNED NOT NULL,
  `is_live` TINYINT(1) NOT NULL DEFAULT 1,
  `speaker_count` INT UNSIGNED NOT NULL DEFAULT 1,
  `listener_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `started_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ended_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_adda_live` (`is_live`),
  CONSTRAINT `fk_adda_host` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `adda_members` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `adda_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `role` ENUM('host', 'co_host', 'speaker', 'listener') NOT NULL DEFAULT 'listener',
  `is_muted` TINYINT(1) NOT NULL DEFAULT 1,
  `joined_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adda_member` (`adda_id`, `user_id`),
  CONSTRAINT `fk_am_adda` FOREIGN KEY (`adda_id`) REFERENCES `addas` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_am_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_presence` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `status` ENUM('online', 'away', 'busy', 'offline') NOT NULL DEFAULT 'offline',
  `custom_text` VARCHAR(128) NULL DEFAULT NULL,
  `current_screen` VARCHAR(64) NULL DEFAULT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_presence_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 9. ADMIN PANEL & ROLE-BASED ACCESS CONTROL
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `admin_activity_logs`;
DROP TABLE IF EXISTS `admin_role_permissions`;
DROP TABLE IF EXISTS `admin_permissions`;
DROP TABLE IF EXISTS `admin_users`;
DROP TABLE IF EXISTS `admin_roles`;

CREATE TABLE `admin_roles` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `slug` VARCHAR(64) NOT NULL,
  `description` VARCHAR(255) NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `admin_permissions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `slug` VARCHAR(64) NOT NULL,
  `module` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `admin_role_permissions` (
  `role_id` BIGINT UNSIGNED NOT NULL,
  `permission_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`),
  CONSTRAINT `fk_arp_role` FOREIGN KEY (`role_id`) REFERENCES `admin_roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_arp_perm` FOREIGN KEY (`permission_id`) REFERENCES `admin_permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `admin_users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT UNSIGNED NOT NULL,
  `username` VARCHAR(64) NOT NULL,
  `email` VARCHAR(191) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `full_name` VARCHAR(128) NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `last_login_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`),
  UNIQUE KEY `uk_admin_email` (`email`),
  CONSTRAINT `fk_admin_role` FOREIGN KEY (`role_id`) REFERENCES `admin_roles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `admin_activity_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_id` BIGINT UNSIGNED NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(64) NOT NULL,
  `target_id` BIGINT UNSIGNED NULL DEFAULT NULL,
  `ip_address` VARCHAR(45) NOT NULL,
  `details_json` TEXT NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_logs_admin` (`admin_id`),
  CONSTRAINT `fk_aal_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 10. MODERATION & ABUSE PREVENTION
-- (Admin never has access to plaintext private keys or encrypted messages)
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `moderation_actions`;
DROP TABLE IF EXISTS `report_evidence`;
DROP TABLE IF EXISTS `reports`;

CREATE TABLE `reports` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `reporter_id` BIGINT UNSIGNED NOT NULL,
  `reported_user_id` BIGINT UNSIGNED NOT NULL,
  `target_type` ENUM('user', 'group', 'adda_room') NOT NULL,
  `target_id` BIGINT UNSIGNED NOT NULL,
  `reason` ENUM('spam', 'harassment', 'hate_speech', 'inappropriate_media', 'impersonation', 'other') NOT NULL,
  `description` TEXT NULL DEFAULT NULL,
  `status` ENUM('pending', 'reviewing', 'resolved', 'dismissed') NOT NULL DEFAULT 'pending',
  `reviewed_by` BIGINT UNSIGNED NULL DEFAULT NULL,
  `resolution_notes` TEXT NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reports_status` (`status`),
  CONSTRAINT `fk_rep_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rep_reported` FOREIGN KEY (`reported_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rep_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `report_evidence` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `report_id` BIGINT UNSIGNED NOT NULL,
  `evidence_type` ENUM('voluntary_decrypted_text', 'screenshot', 'attachment_link') NOT NULL,
  `content` TEXT NOT NULL,
  `submitted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_evidence_report` (`report_id`),
  CONSTRAINT `fk_re_report` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `moderation_actions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `report_id` BIGINT UNSIGNED NULL DEFAULT NULL,
  `moderator_id` BIGINT UNSIGNED NOT NULL,
  `target_user_id` BIGINT UNSIGNED NOT NULL,
  `action` ENUM('warn', 'suspend_temp', 'ban_permanent', 'delete_group', 'dismiss') NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `duration_hours` INT UNSIGNED NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_ma_moderator` FOREIGN KEY (`moderator_id`) REFERENCES `admin_users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ma_user` FOREIGN KEY (`target_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 11. SYSTEM SETTINGS & AUDIT LOGS
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `audit_logs`;
DROP TABLE IF EXISTS `login_activity`;
DROP TABLE IF EXISTS `app_settings`;

CREATE TABLE `app_settings` (
  `setting_key` VARCHAR(64) NOT NULL,
  `setting_value` TEXT NOT NULL,
  `description` VARCHAR(255) NULL DEFAULT NULL,
  `is_public` TINYINT(1) NOT NULL DEFAULT 0,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `login_activity` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NULL DEFAULT NULL,
  `email_attempted` VARCHAR(191) NOT NULL,
  `ip_address` VARCHAR(45) NOT NULL,
  `user_agent` VARCHAR(255) NULL DEFAULT NULL,
  `status` ENUM('success', 'failed_password', 'account_locked', 'suspicious_ip') NOT NULL,
  `attempted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_login_ip_time` (`ip_address`, `attempted_at`),
  KEY `idx_login_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `audit_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `entity_type` VARCHAR(64) NOT NULL,
  `entity_id` BIGINT UNSIGNED NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `actor_id` BIGINT UNSIGNED NULL DEFAULT NULL,
  `payload` TEXT NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_entity` (`entity_type`, `entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- INITIAL SEED DATA
-- ---------------------------------------------------------------------

INSERT INTO `app_settings` (`setting_key`, `setting_value`, `description`, `is_public`) VALUES
('app_name', 'MATE CHAT', 'Application Name', 1),
('app_tagline', 'Connect • Chat • Call • Share', 'Tagline', 1),
('maintenance_mode', '0', 'Put site into maintenance mode', 1),
('max_upload_size_mb', '50', 'Maximum attachment upload size in megabytes', 1),
('e2ee_enforced', '1', 'Enforce End-to-End Encryption for all private chats', 1),
('webrtc_stun_servers', '["stun:stun.l.google.com:19302","stun:stun1.l.google.com:19302"]', 'Default STUN servers', 1);

INSERT INTO `admin_roles` (`id`, `name`, `slug`, `description`) VALUES
(1, 'Super Admin', 'super_admin', 'Full access to all system functions and admin users'),
(2, 'Administrator', 'administrator', 'Management of users, groups, settings, and moderation'),
(3, 'Moderator', 'moderator', 'Review reports and voluntary evidence submissions'),
(4, 'Support', 'support', 'Account recovery and user assistance');

INSERT INTO `admin_users` (`id`, `role_id`, `username`, `email`, `password_hash`, `full_name`, `is_active`) VALUES
(1, 1, 'superadmin', 'admin@matechat.app', '$2y$12$eA8i46nJgJ5K7nZ8Y9V0uu31X0v2E8D7F9B1C3A5E7G9I1K3M5O7Q', 'System Super Administrator', 1);

-- Demo Seed Users
INSERT INTO `users` (`id`, `username`, `email`, `password_hash`, `status`, `email_verified_at`) VALUES
(1, 'alex_mate', 'alex@matechat.app', '$2y$12$eA8i46nJgJ5K7nZ8Y9V0uu31X0v2E8D7F9B1C3A5E7G9I1K3M5O7Q', 'active', NOW()),
(2, 'sophia_code', 'sophia@matechat.app', '$2y$12$eA8i46nJgJ5K7nZ8Y9V0uu31X0v2E8D7F9B1C3A5E7G9I1K3M5O7Q', 'active', NOW()),
(3, 'marcus_design', 'marcus@matechat.app', '$2y$12$eA8i46nJgJ5K7nZ8Y9V0uu31X0v2E8D7F9B1C3A5E7G9I1K3M5O7Q', 'active', NOW()),
(4, 'emma_sound', 'emma@matechat.app', '$2y$12$eA8i46nJgJ5K7nZ8Y9V0uu31X0v2E8D7F9B1C3A5E7G9I1K3M5O7Q', 'active', NOW());

INSERT INTO `user_profiles` (`user_id`, `full_name`, `bio`, `about`, `is_online`, `last_seen`, `custom_status`) VALUES
(1, 'Alex Rivers', 'Building next-gen secure communication 🚀', 'Software architect passionate about decentralized protocols and cryptography.', 1, NOW(), 'In a zone'),
(2, 'Sophia Zhang', 'Security & Crypto Engineer 🔐', 'Full-stack developer and open source advocate.', 1, NOW(), 'Available'),
(3, 'Marcus Vance', 'Product & Visual Designer ✨', 'Crafting delightful mobile & web interfaces.', 0, DATE_SUB(NOW(), INTERVAL 15 MINUTE), 'Away'),
(4, 'Emma Watson', 'Podcaster & Audio Enthusiast 🎙️', 'Host of Tech Talk Adda room. Join the conversation!', 1, NOW(), 'Hosting Adda');

INSERT INTO `friendships` (`user_id`, `friend_id`) VALUES
(1, 2), (2, 1),
(1, 3), (3, 1),
(1, 4), (4, 1);

INSERT INTO `conversations` (`id`, `type`, `title`, `created_by`, `disappearing_seconds`) VALUES
(1, 'direct', NULL, 1, 0),
(2, 'direct', NULL, 1, 0),
(3, 'group', 'MATE Core Engineering', 1, 0);

INSERT INTO `conversation_members` (`conversation_id`, `user_id`, `role`) VALUES
(1, 1, 'member'), (1, 2, 'member'),
(2, 1, 'member'), (2, 3, 'member'),
(3, 1, 'owner'), (3, 2, 'admin'), (3, 3, 'member'), (3, 4, 'member');

INSERT INTO `groups` (`conversation_id`, `name`, `description`, `invite_code`, `is_public`) VALUES
(3, 'MATE Core Engineering', 'Official engineering channel for E2EE protocol and mobile architecture.', 'mate-eng-2026', 1);

INSERT INTO `addas` (`id`, `title`, `topic`, `host_id`, `is_live`, `speaker_count`, `listener_count`) VALUES
(1, 'Cybersecurity & Cryptography Cafe', 'Signal protocol, post-quantum cryptography, and MLS group ratchets', 1, 1, 3, 28),
(2, 'Kotlin Multiplatform & Compose Tech Adda', 'Jetpack Compose UI patterns, M3 design system, and state hoisting', 2, 1, 2, 15);
