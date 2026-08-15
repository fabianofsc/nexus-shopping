UPDATE payment_attempts SET provider = 'PAYMENT_SERVICE' WHERE provider = 'LOGGING_PROVIDER';

DROP TABLE payment_provider_dispatches;
