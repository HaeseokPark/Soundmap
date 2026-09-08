-- MySQL 8: apply ONCE before the first feature deployment if using ddl-auto=validate.
-- Do not apply after Hibernate update already created these objects.
CREATE TABLE saved_tracks (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  track_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_saved_user_track UNIQUE(user_id,track_id),
  CONSTRAINT fk_saved_track FOREIGN KEY(track_id) REFERENCES tracks(id)
);
CREATE TABLE music_check_ins (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  track_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  publicly_shared BIT NOT NULL,
  INDEX idx_checkin_user_time(user_id,created_at),
  INDEX idx_checkin_time(created_at),
  CONSTRAINT fk_checkin_track FOREIGN KEY(track_id) REFERENCES tracks(id)
);
CREATE TABLE user_notifications (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  actor_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  title VARCHAR(200) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  read_at DATETIME(6) NULL,
  INDEX idx_notification_user(user_id,id)
);
CREATE TABLE push_subscriptions (
  id VARCHAR(64) NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  endpoint VARCHAR(2048) NOT NULL,
  public_key VARCHAR(200) NOT NULL,
  auth VARCHAR(100) NOT NULL,
  INDEX idx_push_user(user_id)
);
ALTER TABLE chat_messages ADD COLUMN track_id BIGINT NULL;
ALTER TABLE chat_messages ADD CONSTRAINT fk_message_track FOREIGN KEY(track_id) REFERENCES tracks(id);
