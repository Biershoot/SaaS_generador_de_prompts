# Security Guidelines and Best Practices

## Overview

This document outlines the security measures implemented in the Prompt Generator SaaS application and provides guidelines for maintaining security in development and production environments.

## Security Features Implemented

### 1. Authentication & Authorization

#### JWT Token Security
- **Configurable Secret Key**: JWT secret is now configurable via environment variables
- **Token Expiration**: Configurable token expiration (5 hours dev, 1 hour prod)
- **Token Issuer Validation**: Validates token issuer to prevent token reuse
- **Secure Token Generation**: Uses HMAC-SHA256 for token signing

#### Spring Security Configuration
- **Role-based Access Control**: Implemented for all endpoints
- **Method-level Security**: `@PreAuthorize` annotations on sensitive operations
- **Stateless Sessions**: No server-side session storage
- **CORS Configuration**: Properly configured for cross-origin requests

### 2. Input Validation & Sanitization

#### DTO Validation
- **Bean Validation**: All DTOs use `@Valid` annotations
- **Pattern Validation**: Regex patterns for URL and ID validation
- **Required Field Validation**: `@NotBlank` for required fields
- **Custom Validation Messages**: User-friendly error messages

#### Payment Validation
- **Price ID Validation**: Validates Stripe price ID format
- **URL Validation**: Validates success and cancel URLs
- **User Authorization**: Validates user permissions for payment operations

### 3. Rate Limiting

#### API Rate Limiting
- **Per-IP Rate Limiting**: 60 requests per minute per IP
- **Endpoint-specific Limits**: Different limits for different endpoints
- **Rate Limit Headers**: X-RateLimit-* headers in responses
- **Graceful Degradation**: Returns 429 status when limit exceeded

### 4. Error Handling & Logging

#### Global Exception Handler
- **Consistent Error Responses**: Standardized error response format
- **Security-conscious Logging**: No sensitive data in logs
- **Proper HTTP Status Codes**: Appropriate status codes for different errors
- **Structured Logging**: JSON-formatted logs for better analysis

#### Security Logging
- **Authentication Events**: Logged with appropriate levels
- **Payment Events**: Detailed logging for payment operations
- **Access Denied Events**: Logged for security monitoring
- **Rate Limit Violations**: Logged for abuse detection

### 5. Headers & Security Headers

#### Security Headers
- **HSTS**: HTTP Strict Transport Security enabled
- **Frame Options**: Prevents clickjacking attacks
- **Content Type Options**: Prevents MIME type sniffing
- **XSS Protection**: Basic XSS protection headers

### 6. Database Security

#### SQL Injection Prevention
- **JPA/Hibernate**: Uses parameterized queries
- **Input Validation**: All inputs validated before database operations
- **Connection Security**: SSL enabled for database connections

### 7. Payment Security

#### Stripe Integration Security
- **Webhook Signature Verification**: Validates Stripe webhook signatures
- **Secure API Keys**: Environment variable configuration
- **Customer Data Protection**: Minimal customer data storage
- **Payment Validation**: Validates all payment operations

## Environment Configuration

### Development Environment

```yaml
# application.yml
jwt:
  secret: your-super-secret-jwt-key-change-in-production
  expiration: 18000 # 5 hours
  issuer: prompt-generator-saas

logging:
  level:
    com.alejandro.microservices.promptgeneratorsaas: INFO
    org.springframework.security: INFO
```

### Production Environment

```yaml
# application-prod.yml
jwt:
  secret: ${JWT_SECRET} # Must be set
  expiration: 3600 # 1 hour
  issuer: prompt-generator-saas

logging:
  level:
    com.alejandro.microservices.promptgeneratorsaas: WARN
    org.springframework.security: WARN
```

## Required Environment Variables

### Production Environment Variables

```bash
# JWT Configuration
JWT_SECRET=your-super-secure-jwt-secret-key-here

# Database Configuration
DB_URL=jdbc:mysql://your-db-host:3306/prompt_saas
DB_USERNAME=your-db-username
DB_PASSWORD=your-secure-db-password

# Stripe Configuration
STRIPE_SECRET_KEY=sk_live_your_live_stripe_secret_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret
STRIPE_PUBLISHABLE_KEY=pk_live_your_live_stripe_publishable_key

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com

# AI Provider Keys
OPENAI_API_KEY=your_openai_api_key
CLAUDE_API_KEY=your_claude_api_key
STABLE_DIFFUSION_API_KEY=your_stability_api_key
```

## Security Checklist

### Before Deployment

- [ ] **Environment Variables**: All sensitive data moved to environment variables
- [ ] **JWT Secret**: Strong, unique JWT secret configured
- [ ] **Database Security**: Database credentials secured
- [ ] **Stripe Keys**: Live Stripe keys configured
- [ ] **CORS Configuration**: Proper CORS origins configured
- [ ] **Logging Level**: Production logging level set to WARN
- [ ] **HTTPS**: SSL certificate configured
- [ ] **Rate Limiting**: Rate limiting enabled and configured
- [ ] **Security Headers**: All security headers enabled

### Regular Security Maintenance

- [ ] **Dependency Updates**: Regular security updates for dependencies
- [ ] **Log Monitoring**: Monitor security logs for suspicious activity
- [ ] **Access Reviews**: Regular review of user access and permissions
- [ ] **Backup Security**: Secure backup procedures in place
- [ ] **Incident Response**: Incident response plan documented

## Security Monitoring

### Key Metrics to Monitor

1. **Authentication Failures**: Monitor for brute force attempts
2. **Rate Limit Violations**: Monitor for API abuse
3. **Payment Failures**: Monitor for payment fraud
4. **Access Denied Events**: Monitor for unauthorized access attempts
5. **Database Connection Failures**: Monitor for database attacks

### Log Analysis

```bash
# Monitor authentication failures
grep "Authentication error" logs/application.log

# Monitor rate limit violations
grep "Rate limit exceeded" logs/application.log

# Monitor payment errors
grep "Payment error" logs/application.log
```

## Incident Response

### Security Incident Response Plan

1. **Detection**: Monitor logs and alerts
2. **Assessment**: Evaluate the scope and impact
3. **Containment**: Isolate affected systems
4. **Eradication**: Remove the threat
5. **Recovery**: Restore normal operations
6. **Lessons Learned**: Document and improve

### Contact Information

- **Security Team**: security@yourcompany.com
- **Emergency Contact**: +1-XXX-XXX-XXXX
- **Stripe Support**: https://support.stripe.com

## Compliance

### GDPR Compliance

- **Data Minimization**: Only collect necessary data
- **User Consent**: Clear consent mechanisms
- **Data Portability**: Export user data capability
- **Right to Deletion**: User data deletion capability
- **Data Encryption**: Encrypt data in transit and at rest

### PCI DSS Compliance (for payments)

- **Card Data**: No card data stored in application
- **Stripe Compliance**: Leverage Stripe's PCI DSS compliance
- **Secure Communication**: HTTPS for all payment operations
- **Access Control**: Strict access controls for payment data

## Security Testing

### Recommended Security Tests

1. **Penetration Testing**: Regular security assessments
2. **Vulnerability Scanning**: Automated vulnerability scans
3. **Code Security Review**: Regular code security reviews
4. **Dependency Scanning**: Automated dependency vulnerability scans
5. **API Security Testing**: Test API endpoints for vulnerabilities

### Security Testing Tools

- **OWASP ZAP**: Web application security scanner
- **SonarQube**: Code quality and security analysis
- **Dependabot**: Automated dependency updates
- **Snyk**: Vulnerability scanning for dependencies

## Conclusion

This application implements industry-standard security practices and should be regularly reviewed and updated to maintain security posture. Always follow the principle of defense in depth and regularly update security measures based on emerging threats.
