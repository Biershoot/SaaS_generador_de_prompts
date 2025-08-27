# 🚀 Deployment Guide - Prompt Generator SaaS

Esta guía te ayudará a desplegar tu aplicación Spring Boot en diferentes entornos usando Docker, Kubernetes y CI/CD.

## 📋 Tabla de Contenidos

1. [Estrategias de Despliegue](#estrategias-de-despliegue)
2. [Desarrollo Local](#desarrollo-local)
3. [Configuración de AWS](#configuración-de-aws)
4. [Despliegue en Kubernetes](#despliegue-en-kubernetes)
5. [CI/CD con GitHub Actions](#cicd-con-github-actions)
6. [Monitoreo y Logs](#monitoreo-y-logs)
7. [Troubleshooting](#troubleshooting)

## 🎯 Estrategias de Despliegue

### Opción 1: MVP (Rápido y Barato)
- **ECS Fargate**: Serverless, fácil de configurar
- **Render/Railway**: Plataformas PaaS simples
- **Vercel/Netlify**: Para frontend + API routes

### Opción 2: Producción Escalable
- **EKS (AWS)**: Kubernetes gestionado en AWS
- **GKE (GCP)**: Kubernetes gestionado en Google Cloud
- **AKS (Azure)**: Kubernetes gestionado en Azure

## 🏠 Desarrollo Local

### Prerrequisitos
- Docker y Docker Compose
- Java 21
- Maven

### Ejecutar con Docker Compose

```bash
# Clonar el repositorio
git clone <your-repo>
cd prompt-generator-saas

# Configurar variables de entorno
cp .env.example .env
# Editar .env con tus API keys

# Ejecutar la aplicación
docker-compose up -d

# Ver logs
docker-compose logs -f app

# Detener servicios
docker-compose down
```

### Variables de Entorno Necesarias

```bash
# Database
DATASOURCE_URL=jdbc:mysql://db:3306/prompt_saas
DATASOURCE_USERNAME=prompt_user
DATASOURCE_PASSWORD=prompt_pass

# JWT
JWT_SECRET=your-super-secret-jwt-key

# Stripe (test keys)
STRIPE_SECRET_KEY=sk_test_xxxxxxxxxxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxxx
STRIPE_PUBLISHABLE_KEY=pk_test_xxxxxxxxxxxxxxxxxxxxxx

# AI Providers
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxx
CLAUDE_API_KEY=sk-ant-xxxxxxxxxxxxxxxxxxxxxx
STABLE_DIFFUSION_API_KEY=xxxxxxxxxxxxxxxxxxxxxx
```

## ☁️ Configuración de AWS

### 1. Crear ECR Repository

```bash
aws ecr create-repository \
    --repository-name prompt-generator-saas \
    --region us-east-1
```

### 2. Configurar IAM User para CI/CD

```bash
# Crear usuario IAM
aws iam create-user --user-name github-actions

# Adjuntar políticas necesarias
aws iam attach-user-policy \
    --user-name github-actions \
    --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser

aws iam attach-user-policy \
    --user-name github-actions \
    --policy-arn arn:aws:iam::aws:policy/AmazonECS-FullAccess
```

### 3. Crear Access Keys

```bash
aws iam create-access-key --user-name github-actions
```

### 4. Configurar RDS (Base de Datos)

```bash
# Crear subnet group
aws rds create-db-subnet-group \
    --db-subnet-group-name prompt-saas-subnet-group \
    --db-subnet-group-description "Subnet group for Prompt SaaS" \
    --subnet-ids subnet-xxxxxxxxx subnet-yyyyyyyyy

# Crear instancia RDS
aws rds create-db-instance \
    --db-instance-identifier prompt-saas-db \
    --db-instance-class db.t3.micro \
    --engine mysql \
    --engine-version 8.0.35 \
    --master-username admin \
    --master-user-password YourSecurePassword123! \
    --allocated-storage 20 \
    --db-subnet-group-name prompt-saas-subnet-group \
    --vpc-security-group-ids sg-xxxxxxxxx
```

## 🐳 Despliegue en Kubernetes

### Prerrequisitos
- Cluster de Kubernetes (EKS, GKE, o local)
- kubectl configurado
- Helm (opcional)

### 1. Configurar Secrets

```bash
# Crear secrets desde archivo
kubectl apply -f k8s/secret.yaml

# O crear secrets manualmente
kubectl create secret generic prompt-secrets \
    --from-literal=DATASOURCE_URL="jdbc:mysql://your-rds-endpoint:3306/prompt_saas" \
    --from-literal=DATASOURCE_USERNAME="admin" \
    --from-literal=DATASOURCE_PASSWORD="YourSecurePassword123!" \
    --from-literal=JWT_SECRET="your-super-secret-jwt-key" \
    --from-literal=STRIPE_SECRET_KEY="sk_test_xxxxxxxxxxxxxxxxxxxxxx" \
    --from-literal=STRIPE_WEBHOOK_SECRET="whsec_xxxxxxxxxxxxxxxxxxxxxx" \
    --from-literal=OPENAI_API_KEY="sk-xxxxxxxxxxxxxxxxxxxxxx" \
    -n prompt
```

### 2. Desplegar Aplicación

```bash
# Crear namespace
kubectl apply -f k8s/namespace.yaml

# Aplicar configuraciones
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# Desplegar aplicación
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml

# Verificar despliegue
kubectl get pods -n prompt
kubectl get services -n prompt
kubectl get ingress -n prompt
```

### 3. Usar Script de Despliegue

```bash
# Hacer ejecutable el script
chmod +x scripts/deploy.sh

# Desplegar
./scripts/deploy.sh production latest
```

## 🔄 CI/CD con GitHub Actions

### 1. Configurar Secrets en GitHub

Ve a tu repositorio → Settings → Secrets and variables → Actions

Agrega los siguientes secrets:

```bash
# AWS Configuration
AWS_ACCESS_KEY_ID=AKIAXXXXXXXXXXXXXXXX
AWS_SECRET_ACCESS_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
AWS_ACCOUNT_ID=123456789012
AWS_ECR_REPO=prompt-generator-saas

# Application Secrets
STRIPE_SECRET_KEY=sk_test_xxxxxxxxxxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxxx
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxx
CLAUDE_API_KEY=sk-ant-xxxxxxxxxxxxxxxxxxxxxx
STABLE_DIFFUSION_API_KEY=xxxxxxxxxxxxxxxxxxxxxx

# Database (if using external DB)
DATASOURCE_URL=jdbc:mysql://your-rds-endpoint:3306/prompt_saas
DATASOURCE_USERNAME=admin
DATASOURCE_PASSWORD=YourSecurePassword123!

# JWT
JWT_SECRET=your-super-secret-jwt-key

# Kubernetes (si usas K8s)
KUBECONFIG=<base64-encoded-kubeconfig>
```

### 2. Configurar Webhook de Stripe

En el dashboard de Stripe:
1. Ve a Developers → Webhooks
2. Agrega endpoint: `https://api.your-domain.com/api/webhooks/stripe`
3. Selecciona eventos:
   - `checkout.session.completed`
   - `customer.subscription.created`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.payment_succeeded`
   - `invoice.payment_failed`

### 3. Pipeline de Despliegue

El pipeline se ejecuta automáticamente en:
- Push a `main` → Despliegue a producción
- Push a `develop` → Despliegue a staging
- Pull Request → Tests y security scan

## 📊 Monitoreo y Logs

### Health Checks

```bash
# Verificar health de la aplicación
curl https://api.your-domain.com/actuator/health

# Verificar readiness
curl https://api.your-domain.com/actuator/health/readiness

# Verificar liveness
curl https://api.your-domain.com/actuator/health/liveness
```

### Logs

```bash
# Ver logs de pods
kubectl logs -f deployment/prompt-app -n prompt

# Ver logs de un pod específico
kubectl logs -f <pod-name> -n prompt

# Ver logs de los últimos 100 líneas
kubectl logs --tail=100 deployment/prompt-app -n prompt
```

### Métricas

```bash
# Ver métricas de Prometheus
curl https://api.your-domain.com/actuator/prometheus

# Ver información de la aplicación
curl https://api.your-domain.com/actuator/info
```

## 🔧 Troubleshooting

### Problemas Comunes

#### 1. Pods no inician

```bash
# Verificar eventos
kubectl describe pod <pod-name> -n prompt

# Verificar logs
kubectl logs <pod-name> -n prompt

# Verificar configuración
kubectl describe deployment prompt-app -n prompt
```

#### 2. Problemas de conectividad a la base de datos

```bash
# Verificar conectividad desde pod
kubectl exec -it <pod-name> -n prompt -- mysql -h <db-host> -u <username> -p

# Verificar variables de entorno
kubectl exec -it <pod-name> -n prompt -- env | grep DATASOURCE
```

#### 3. Problemas con Stripe

```bash
# Verificar configuración de Stripe
kubectl exec -it <pod-name> -n prompt -- env | grep STRIPE

# Verificar webhook endpoint
curl -X POST https://api.your-domain.com/api/webhooks/stripe \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
```

#### 4. Problemas de memoria/CPU

```bash
# Ver uso de recursos
kubectl top pods -n prompt

# Ver métricas detalladas
kubectl describe hpa prompt-hpa -n prompt
```

### Comandos Útiles

```bash
# Reiniciar deployment
kubectl rollout restart deployment/prompt-app -n prompt

# Escalar manualmente
kubectl scale deployment prompt-app --replicas=3 -n prompt

# Ver estado del rollout
kubectl rollout status deployment/prompt-app -n prompt

# Revertir a versión anterior
kubectl rollout undo deployment/prompt-app -n prompt
```

## 🔒 Seguridad

### Checklist de Seguridad

- [ ] Secrets configurados correctamente
- [ ] HTTPS habilitado en producción
- [ ] CORS configurado apropiadamente
- [ ] Rate limiting habilitado
- [ ] Security headers configurados
- [ ] Base de datos con SSL
- [ ] Logs de auditoría habilitados
- [ ] Monitoreo de seguridad configurado

### Mejores Prácticas

1. **Nunca** committear secrets al repositorio
2. Usar variables de entorno para configuración
3. Implementar rotación de secrets
4. Monitorear logs de seguridad
5. Mantener dependencias actualizadas
6. Usar imágenes base seguras
7. Implementar network policies en K8s

## 📈 Escalabilidad

### Auto-scaling

El HPA configurado escala automáticamente basado en:
- CPU usage > 70%
- Memory usage > 80%

### Optimizaciones

1. **JVM Tuning**: Configurado en Dockerfile
2. **Connection Pooling**: HikariCP optimizado
3. **Caching**: Redis opcional
4. **CDN**: Para assets estáticos
5. **Load Balancing**: Nginx Ingress

## 🆘 Soporte

Para problemas específicos:

1. Revisar logs de la aplicación
2. Verificar configuración de Kubernetes
3. Comprobar conectividad de red
4. Validar secrets y configmaps
5. Revisar métricas de recursos

---

**Nota**: Asegúrate de reemplazar todos los placeholders (`<your-domain>`, `<ECR_REGISTRY>`, etc.) con tus valores reales antes de desplegar.
