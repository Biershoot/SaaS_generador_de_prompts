-- Script para limpiar la base de datos MySQL
-- Ejecuta este script en tu cliente MySQL

-- Eliminar la base de datos si existe
DROP DATABASE IF EXISTS prompt_saas;

-- Crear la base de datos nuevamente
CREATE DATABASE prompt_saas;

-- Usar la base de datos
USE prompt_saas;

-- Verificar que la base de datos está vacía
SHOW TABLES;
