ALTER TABLE orders ADD COLUMN payment_attempt_reference VARCHAR(255);
ALTER TABLE orders ADD COLUMN payment_provider_transaction_id VARCHAR(255);

ALTER TABLE notifications ADD COLUMN notification_key VARCHAR(255);
ALTER TABLE notifications ADD COLUMN sending_lease_until TIMESTAMP;
ALTER TABLE notifications ADD COLUMN sending_lease_token VARCHAR(64);

UPDATE notifications
SET notification_key = CONCAT('legacy:', CAST(id AS VARCHAR(32)))
WHERE notification_key IS NULL;

ALTER TABLE notifications ALTER COLUMN notification_key SET NOT NULL;
ALTER TABLE notifications ADD CONSTRAINT uq_notifications_notification_key UNIQUE (notification_key);

ALTER TABLE notifications DROP CONSTRAINT notifications_status_check;
ALTER TABLE notifications
    ADD CONSTRAINT notifications_status_check
    CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED'));
