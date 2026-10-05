-- Runs once, when the PostgreSQL volume is first created (docker-entrypoint-initdb.d).
-- wallet-event-consumer shares this PostgreSQL server but owns a separate database and user.
CREATE USER consumer WITH PASSWORD 'consumer';
CREATE DATABASE consumer OWNER consumer;
