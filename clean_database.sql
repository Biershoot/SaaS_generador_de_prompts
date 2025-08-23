-- Script para limpiar completamente la base de datos MySQL
-- Ejecuta este script en tu cliente MySQL

-- Eliminar la base de datos si existe
DROP DATABASE IF EXISTS prompt_saas;

-- Crear la base de datos nuevamente
CREATE DATABASE prompt_saas;

-- Usar la base de datos
USE prompt_saas;

-- Verificar que la base de datos está completamente vacía
SHOW TABLES;

-- Verificar que no hay tablas de Flyway
SELECT 'Base de datos prompt_saas creada y lista para migraciones' as status;
