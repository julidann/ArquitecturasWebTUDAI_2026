-- Una base por microservicio (mismo motor, esquemas separados).
-- Se ejecuta solo la primera vez que se crea el volumen de PostgreSQL.
CREATE DATABASE eventos_db;
CREATE DATABASE reservas_db;
CREATE DATABASE compras_db;
CREATE DATABASE pagos_db;
