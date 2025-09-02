# Prompt Generator SaaS

Plataforma SaaS para crear, organizar y escalar la generación de prompts de IA con planes de suscripción y pagos recurrentes. Diseñada para equipos que necesitan estandarizar prompts, controlar costos y ofrecer capacidades premium a sus usuarios finales.

## ¿Qué problema resuelve?

- Estandariza prompts y flujos de generación en un solo backend.
- Controla acceso y límites por plan con suscripciones recurrentes (Stripe).
- Expone API unificada para múltiples proveedores de IA (OpenAI, Claude, Stable Diffusion).
- Reduce fricción de seguridad: JWT, RBAC, rate limiting, validación y webhooks verificados.

## Características clave

- Autenticación JWT y autorización por roles (USER/ADMIN).
- Suscripciones: Free, Premium, Pro; upgrades/downgrades; cancelación y estados.
- Integración con Stripe Checkout y webhooks firmados.
- Proveedores de IA intercambiables vía `AIServiceFactory`.
- Observabilidad: health checks, logging estructurado y manejo centralizado de errores.

## Demo rápida (local)

Requisitos: Java 17+, MySQL 8+, cuenta de Stripe (modo test).

```bash
# 1) Clonar
git clone https://github.com/Biershoot/SaaS_generador_de_prompts.git
cd SaaS_generador_de_prompts

# 2) Base de datos
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS prompt_saas;"

# 3) Variables de entorno (ejemplo)
setx DB_URL "jdbc:mysql://localhost:3306/prompt_saas"
setx DB_USERNAME "root"
setx DB_PASSWORD "your_password"
setx JWT_SECRET "dev-jwt-secret"
setx STRIPE_SECRET_KEY "sk_test_xxxxxxxxxxxxxxxxx"
setx STRIPE_PUBLISHABLE_KEY "pk_test_xxxxxxxxxxxxx"
setx STRIPE_WEBHOOK_SECRET "whsec_xxxxxxxxxxxxxxx"

# 4) Ejecutar
./mvnw spring-boot:run
```

La API quedará disponible en `http://localhost:8080`. Swagger: `http://localhost:8080/swagger-ui.html`.

## Configuración esencial

Variables mínimas:

```bash
DB_URL=jdbc:mysql://localhost:3306/prompt_saas
DB_USERNAME=<user>
DB_PASSWORD=<pass>

JWT_SECRET=<secure-secret>
JWT_EXPIRATION=18000
JWT_ISSUER=prompt-generator-saas

STRIPE_SECRET_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

OPENAI_API_KEY=<optional>
CLAUDE_API_KEY=<optional>
STABLE_DIFFUSION_API_KEY=<optional>
```

## Endpoints principales

- Autenticación: `/auth/register`, `/auth/login`, `/auth/refresh`
- Prompts: `GET/POST/PUT/DELETE /api/prompts`
- IA: `POST /api/ai/generate`, `POST /api/ai/chat`, `POST /api/ai/image`
- Suscripciones: `GET /api/subscriptions/plans`, `GET /api/subscriptions/my-subscription`, `POST /api/subscriptions/create-checkout-session`, `POST /api/subscriptions/upgrade`, `POST /api/subscriptions/cancel`, `GET /api/subscriptions/features`
- Webhooks: `POST /api/webhooks/stripe`

## Arquitectura (alto nivel)

Backend Spring Boot con capas de `controller`, `service`, `repository`, `security` e integración con Stripe y proveedores de IA. Migraciones con Flyway y MySQL como almacenamiento principal.

## Seguridad

- JWT con expiración y issuer validado.
- RBAC, rate limiting por IP, CORS configurado, validación Bean Validation.
- Verificación de firma de webhooks Stripe.

## Despliegue

```bash
./mvnw clean package -Pprod
java -jar target/prompt-generator-saas-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Configura claves LIVE de Stripe, `JWT_SECRET` robusto, CORS y base de datos gestionada (con SSL) antes de producción.

## Documentación adicional

- Guía de Stripe: `STRIPE_SETUP.md`
- Sistema de pagos/suscripciones: `SUBSCRIPTION_PAYMENT_GUIDE.md`
- Guía de seguridad: `SECURITY_GUIDELINES.md`

## Licencia

MIT.

## Enlaces

Repositorio: https://github.com/Biershoot/SaaS_generador_de_prompts
