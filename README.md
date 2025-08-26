# 🚀 Prompt Generator SaaS

Una aplicación SaaS completa para generar prompts de IA con sistema de suscripciones y pagos recurrentes.

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Arquitectura](#-arquitectura)
- [Tecnologías](#-tecnologías)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [API Endpoints](#-api-endpoints)
- [Sistema de Suscripciones](#-sistema-de-suscripciones)
- [Seguridad](#-seguridad)
- [Despliegue](#-despliegue)
- [Documentación](#-documentación)
- [Contribución](#-contribución)
- [Licencia](#-licencia)

## ✨ Características

### 🤖 Generación de Prompts
- **Múltiples Proveedores de IA**: OpenAI, Claude, Stable Diffusion
- **Prompts Personalizados**: Crear y guardar prompts personalizados
- **Templates Predefinidos**: Biblioteca de templates para diferentes casos de uso
- **Historial de Prompts**: Seguimiento completo de prompts generados

### 💳 Sistema de Suscripciones
- **Planes Flexibles**: Free, Premium, Pro con diferentes límites
- **Pagos Recurrentes**: Integración completa con Stripe
- **Upgrades/Downgrades**: Cambio de planes con validación
- **Gestión de Estados**: Activo, Cancelado, Vencido, Impago

### 🔐 Seguridad Avanzada
- **Autenticación JWT**: Tokens seguros y configurables
- **Autorización RBAC**: Control de acceso basado en roles
- **Rate Limiting**: Protección contra abuso de API
- **Validación de Entrada**: Validación robusta de datos
- **Headers de Seguridad**: Protección contra ataques comunes

### 📊 Monitoreo y Logging
- **Logging Estructurado**: Logs detallados para debugging
- **Métricas de Seguridad**: Monitoreo de eventos de seguridad
- **Error Handling**: Manejo centralizado de errores
- **Health Checks**: Endpoints de salud del sistema

## 🏗️ Arquitectura

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Spring Boot   │    │   Database      │
│   (React/Vue)   │◄──►│   Application   │◄──►│   (MySQL)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   Stripe API    │
                       │   (Payments)    │
                       └─────────────────┘
```

### Componentes Principales

1. **Controllers**: Manejo de requests HTTP y respuestas
2. **Services**: Lógica de negocio y operaciones
3. **Repositories**: Acceso a datos y persistencia
4. **Security**: Autenticación, autorización y validación
5. **Payment**: Integración con Stripe para pagos
6. **Webhooks**: Manejo de eventos de Stripe

## 🛠️ Tecnologías

### Backend
- **Spring Boot 3.x**: Framework principal
- **Spring Security**: Seguridad y autenticación
- **Spring Data JPA**: Persistencia de datos
- **MySQL**: Base de datos principal
- **Stripe Java SDK**: Integración de pagos
- **JWT**: Autenticación stateless
- **Lombok**: Reducción de boilerplate
- **Maven**: Gestión de dependencias

### Base de Datos
- **MySQL 8.0+**: Base de datos relacional
- **Flyway**: Migraciones de base de datos
- **H2**: Base de datos en memoria para tests

### Seguridad
- **Spring Security**: Framework de seguridad
- **JWT**: JSON Web Tokens
- **BCrypt**: Hashing de contraseñas
- **Rate Limiting**: Protección contra abuso

## 🚀 Instalación

### Prerrequisitos
- Java 17 o superior
- MySQL 8.0 o superior
- Maven 3.6+
- Cuenta de Stripe (para pagos)

### 1. Clonar el Repositorio
```bash
git clone https://github.com/Biershoot/SaaS_generador_de_prompts.git
cd SaaS_generador_de_prompts
```

### 2. Configurar Base de Datos
```sql
CREATE DATABASE prompt_saas;
CREATE USER 'prompt_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON prompt_saas.* TO 'prompt_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configurar Variables de Entorno
```bash
# Database
DB_URL=jdbc:mysql://localhost:3306/prompt_saas
DB_USERNAME=prompt_user
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your-super-secure-jwt-secret-key-here
JWT_EXPIRATION=18000
JWT_ISSUER=prompt-generator-saas

# Stripe (Test Keys)
STRIPE_SECRET_KEY=sk_test_xxxxxxxxxxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxxx
STRIPE_PUBLISHABLE_KEY=pk_test_xxxxxxxxxxxxxxxxxxxxxx

# AI Providers
OPENAI_API_KEY=your_openai_api_key
CLAUDE_API_KEY=your_claude_api_key
STABLE_DIFFUSION_API_KEY=your_stability_api_key

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
```

### 4. Compilar y Ejecutar
```bash
# Compilar
./mvnw clean compile

# Ejecutar tests
./mvnw test

# Ejecutar aplicación
./mvnw spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8080`

## ⚙️ Configuración

### Configuración de Stripe

1. **Crear Cuenta Stripe**: Registrarse en [stripe.com](https://stripe.com)
2. **Obtener API Keys**: Desde el dashboard de Stripe
3. **Configurar Webhooks**: 
   - URL: `https://yourapp.com/api/webhooks/stripe`
   - Eventos: `checkout.session.completed`, `customer.subscription.*`, `invoice.payment_*`
4. **Crear Productos y Precios**: Para cada plan de suscripción

### Configuración de AI Providers

1. **OpenAI**: Obtener API key desde [platform.openai.com](https://platform.openai.com)
2. **Claude**: Obtener API key desde [console.anthropic.com](https://console.anthropic.com)
3. **Stable Diffusion**: Obtener API key desde [stability.ai](https://stability.ai)

## 📡 API Endpoints

### Autenticación
```http
POST /auth/register          # Registro de usuario
POST /auth/login            # Inicio de sesión
POST /auth/refresh          # Renovar token JWT
```

### Gestión de Prompts
```http
GET    /api/prompts         # Obtener prompts del usuario
POST   /api/prompts         # Crear nuevo prompt
PUT    /api/prompts/{id}    # Actualizar prompt
DELETE /api/prompts/{id}    # Eliminar prompt
```

### Generación de IA
```http
POST /api/ai/generate       # Generar contenido con IA
POST /api/ai/chat          # Chat con IA
POST /api/ai/image         # Generar imagen
```

### Suscripciones y Pagos
```http
GET    /api/subscriptions/plans              # Obtener planes disponibles
GET    /api/subscriptions/my-subscription    # Obtener suscripción actual
POST   /api/subscriptions/create-checkout-session  # Crear sesión de pago
POST   /api/subscriptions/upgrade            # Actualizar a plan superior
POST   /api/subscriptions/cancel             # Cancelar suscripción
GET    /api/subscriptions/features           # Obtener características disponibles
```

### Gestión de Usuarios
```http
GET    /api/users/profile   # Obtener perfil de usuario
PUT    /api/users/profile   # Actualizar perfil
GET    /api/users/stats     # Obtener estadísticas de uso
```

### Webhooks
```http
POST /api/webhooks/stripe   # Webhook de Stripe (público)
```

## 💳 Sistema de Suscripciones

### Planes Disponibles

| Plan | Precio | Límite de Prompts | Características |
|------|--------|-------------------|-----------------|
| **Free** | $0/mes | 10 prompts | Acceso básico |
| **Premium** | $9.99/mes | 100 prompts | Prompts personalizados |
| **Pro** | $19.99/mes | Ilimitado | Soporte prioritario |

### Estados de Suscripción
- **ACTIVE**: Suscripción activa y pagada
- **CANCELED**: Suscripción cancelada
- **PAST_DUE**: Pago vencido
- **UNPAID**: Pago fallido

### Flujo de Suscripción
1. Usuario se registra → Suscripción FREE automática
2. Usuario selecciona plan → Checkout Session de Stripe
3. Usuario completa pago → Webhook activa suscripción
4. Usuario accede a características según su plan

## 🔐 Seguridad

### Características de Seguridad Implementadas

#### Autenticación y Autorización
- ✅ JWT tokens seguros y configurables
- ✅ Control de acceso basado en roles (RBAC)
- ✅ Tokens con expiración configurable
- ✅ Validación de issuer y claims

#### Protección de Datos
- ✅ Validación de entrada con Bean Validation
- ✅ Sanitización de datos de entrada
- ✅ Encriptación de contraseñas con BCrypt
- ✅ Headers de seguridad HTTP

#### Protección de API
- ✅ Rate limiting (60 req/min por IP)
- ✅ CORS configurado correctamente
- ✅ Validación de webhooks de Stripe
- ✅ Logging de eventos de seguridad

#### Configuración de Seguridad
- ✅ Variables de entorno para datos sensibles
- ✅ Configuración separada para desarrollo y producción
- ✅ Deshabilitación de características de desarrollo en producción
- ✅ Logging de seguridad comprehensivo

### Puntuación de Seguridad: **8.6/10** (Excelente)

## 🚀 Despliegue

### Despliegue en Producción

1. **Configurar Variables de Entorno de Producción**
```bash
# Usar claves live de Stripe
STRIPE_SECRET_KEY=sk_live_xxxxxxxxxxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxxx
STRIPE_PUBLISHABLE_KEY=pk_live_xxxxxxxxxxxxxxxxxxxxxx

# JWT más restrictivo en producción
JWT_EXPIRATION=3600  # 1 hora
JWT_SECRET=your-super-secure-production-jwt-secret

# CORS para dominio de producción
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

2. **Configurar Base de Datos de Producción**
```bash
# Usar MySQL de producción con SSL
DB_URL=jdbc:mysql://your-db-host:3306/prompt_saas?useSSL=true
DB_USERNAME=your_production_user
DB_PASSWORD=your_secure_password
```

3. **Configurar Webhook en Stripe Dashboard**
- URL: `https://yourdomain.com/api/webhooks/stripe`
- Eventos: `checkout.session.completed`, `customer.subscription.*`, `invoice.payment_*`

4. **Desplegar Aplicación**
```bash
# Compilar para producción
./mvnw clean package -Pprod

# Ejecutar con perfil de producción
java -jar target/prompt-generator-saas-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Verificación Post-Despliegue

1. **Verificar Health Check**
```bash
curl -X GET https://yourdomain.com/actuator/health
```

2. **Verificar Configuración de Stripe**
```bash
curl -X GET https://yourdomain.com/api/subscriptions/plans \
  -H "Authorization: Bearer <jwt_token>"
```

3. **Probar Webhook**
```bash
curl -X POST https://yourdomain.com/api/webhooks/stripe \
  -H "Stripe-Signature: whsec_..." \
  -H "Content-Type: application/json" \
  -d '{"test": "webhook"}'
```

## 📚 Documentación

### Documentación Técnica
- [Guía de Suscripciones y Pagos](SUBSCRIPTION_PAYMENT_GUIDE.md)
- [Guía de Seguridad](SECURITY_GUIDELINES.md)
- [Resumen de Mejoras de Seguridad](SECURITY_IMPROVEMENTS_SUMMARY.md)
- [Configuración de Stripe](STRIPE_SETUP.md)

### Documentación de API
- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (desarrollo)
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

### Ejemplos de Uso

#### Crear una Suscripción
```bash
# 1. Registrar usuario
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# 2. Iniciar sesión
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# 3. Crear checkout session
curl -X POST http://localhost:8080/api/subscriptions/create-checkout-session \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"priceId":"price_premium_monthly","successUrl":"http://localhost:3000/success","cancelUrl":"http://localhost:3000/cancel"}'
```

#### Generar un Prompt
```bash
curl -X POST http://localhost:8080/api/ai/generate \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Write a blog post about AI","provider":"openai","model":"gpt-4"}'
```

## 🤝 Contribución

### Cómo Contribuir

1. **Fork el repositorio**
2. **Crear una rama para tu feature**
   ```bash
   git checkout -b feature/nueva-funcionalidad
   ```
3. **Hacer commit de tus cambios**
   ```bash
   git commit -m "feat: agregar nueva funcionalidad"
   ```
4. **Push a la rama**
   ```bash
   git push origin feature/nueva-funcionalidad
   ```
5. **Crear un Pull Request**

### Estándares de Código

- **Java**: Seguir convenciones de Spring Boot
- **Documentación**: JavaDoc para todas las clases públicas
- **Tests**: Cobertura mínima del 80%
- **Commits**: Usar convenciones de Conventional Commits
- **Seguridad**: Revisar vulnerabilidades antes de merge

### Estructura del Proyecto

```
src/
├── main/
│   ├── java/
│   │   └── com/alejandro/microservices/promptgeneratorsaas/
│   │       ├── config/          # Configuraciones
│   │       ├── controller/      # Controladores REST
│   │       ├── dto/            # Data Transfer Objects
│   │       ├── entity/         # Entidades JPA
│   │       ├── exception/      # Excepciones personalizadas
│   │       ├── repository/     # Repositorios de datos
│   │       ├── security/       # Configuración de seguridad
│   │       └── service/        # Lógica de negocio
│   └── resources/
│       ├── application.yml     # Configuración principal
│       ├── application-prod.yml # Configuración de producción
│       └── db/migration/       # Migraciones de base de datos
└── test/                       # Tests unitarios e integración
```

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo [LICENSE](LICENSE) para más detalles.

## 🆘 Soporte

### Problemas Comunes

1. **Error de Conexión a Base de Datos**
   - Verificar que MySQL esté ejecutándose
   - Verificar credenciales en `application.yml`
   - Verificar que la base de datos exista

2. **Error de Stripe**
   - Verificar que las API keys sean correctas
   - Verificar que el webhook esté configurado
   - Revisar logs para errores específicos

3. **Error de Autenticación**
   - Verificar que el JWT secret esté configurado
   - Verificar que el token no haya expirado
   - Verificar el formato del token

### Contacto

- **Issues**: [GitHub Issues](https://github.com/Biershoot/SaaS_generador_de_prompts/issues)
- **Documentación**: Ver archivos de documentación en el repositorio
- **Seguridad**: Reportar vulnerabilidades por email

---

**¡Gracias por usar Prompt Generator SaaS! 🎉**

Desarrollado con ❤️ por Alejandro
