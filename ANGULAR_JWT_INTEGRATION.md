# 🔐 Integración JWT Angular + Spring Boot

## 📋 Resumen del Flujo de Seguridad

1. **Login**: Angular hace POST `/auth/login` → Backend devuelve access token + establece refresh token en cookie HttpOnly
2. **Requests**: Angular usa access token en `Authorization: Bearer ...` para llamadas API
3. **Token Expired**: Si access token expira (401), Angular llama automáticamente a POST `/auth/refresh`
4. **Refresh**: Backend valida refresh token de la cookie y devuelve nuevo access token
5. **Logout**: Angular llama POST `/auth/logout` → Backend invalida refresh token y borra cookie

## 🚀 Backend (Spring Boot) - ✅ Implementado

### Endpoints Disponibles

```http
POST /auth/login     # Login con credenciales
POST /auth/refresh   # Renovar access token
POST /auth/logout    # Cerrar sesión
GET  /auth/validate  # Validar token actual
```

### Configuración JWT

```yaml
jwt:
  secret: ${JWT_SECRET:your-super-secret-jwt-key-change-in-production}
  access:
    expiration: 900 # 15 minutos
  refresh:
    expiration: 604800 # 7 días
  issuer: prompt-generator-saas
```

## 🎯 Frontend (Angular) - Implementación

### 1. Servicio de Autenticación

```typescript
// auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../environments/environment';

export interface AuthResponse {
  accessToken: string;
  username: string;
  role: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    // Verificar si hay un usuario guardado al iniciar
    const savedUser = localStorage.getItem('currentUser');
    if (savedUser) {
      this.currentUserSubject.next(JSON.parse(savedUser));
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credentials)
      .pipe(
        tap(response => {
          // Guardar access token en localStorage
          localStorage.setItem('accessToken', response.accessToken);
          localStorage.setItem('currentUser', JSON.stringify(response));
          this.currentUserSubject.next(response);
        })
      );
  }

  refreshToken(): Observable<{ accessToken: string }> {
    return this.http.post<{ accessToken: string }>(`${this.API_URL}/refresh`, {})
      .pipe(
        tap(response => {
          localStorage.setItem('accessToken', response.accessToken);
          const currentUser = this.currentUserSubject.value;
          if (currentUser) {
            currentUser.accessToken = response.accessToken;
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            this.currentUserSubject.next(currentUser);
          }
        })
      );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/logout`, {})
      .pipe(
        tap(() => {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('currentUser');
          this.currentUserSubject.next(null);
        })
      );
  }

  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }

  getCurrentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }
}
```

### 2. Interceptor HTTP para JWT

```typescript
// jwt.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getAccessToken();
    
    if (token) {
      request = this.addToken(request, token);
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !request.url.includes('/auth/refresh')) {
          return this.handle401Error(request, next);
        }
        return throwError(() => error);
      })
    );
  }

  private addToken(request: HttpRequest<any>, token: string): HttpRequest<any> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap((response: any) => {
          this.isRefreshing = false;
          this.refreshTokenSubject.next(response.accessToken);
          return next.handle(this.addToken(request, response.accessToken));
        }),
        catchError((error) => {
          this.isRefreshing = false;
          this.authService.logout();
          return throwError(() => error);
        })
      );
    } else {
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => next.handle(this.addToken(request, token)))
      );
    }
  }
}
```

### 3. Guard de Autenticación

```typescript
// auth.guard.ts
import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): boolean {
    if (this.authService.isAuthenticated()) {
      return true;
    }

    this.router.navigate(['/login']);
    return false;
  }
}
```

### 4. Configuración del Módulo

```typescript
// app.module.ts
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AppComponent } from './app.component';
import { JwtInterceptor } from './interceptors/jwt.interceptor';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    HttpClientModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
```

### 5. Componente de Login

```typescript
// login.component.ts
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  template: `
    <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
      <div>
        <label for="username">Username:</label>
        <input id="username" type="text" formControlName="username">
      </div>
      <div>
        <label for="password">Password:</label>
        <input id="password" type="password" formControlName="password">
      </div>
      <button type="submit" [disabled]="loginForm.invalid">Login</button>
    </form>
  `
})
export class LoginComponent {
  loginForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.authService.login(this.loginForm.value).subscribe({
        next: () => {
          this.router.navigate(['/dashboard']);
        },
        error: (error) => {
          console.error('Login failed:', error);
        }
      });
    }
  }
}
```

## 🔧 Configuración de Entorno

### environment.ts
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

### environment.prod.ts
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://your-production-domain.com'
};
```

## 🛡️ Recomendaciones de Seguridad

### ✅ Implementado en Backend
- **Access Token**: 15 minutos de vida útil
- **Refresh Token**: 7 días de vida útil, almacenado en cookie HttpOnly
- **Token Rotation**: Refresh tokens se rotan en cada uso
- **Secure Cookies**: Configurados para producción con HTTPS
- **CORS**: Configurado correctamente para Angular

### 🔒 Mejores Prácticas
1. **Nunca almacenar refresh tokens en localStorage**
2. **Usar HttpOnly cookies para refresh tokens**
3. **Implementar token rotation**
4. **Validar tokens en cada request**
5. **Manejar errores 401 automáticamente**
6. **Logout automático en errores de refresh**

### 🚀 Próximos Pasos
1. **Implementar el código Angular** en tu proyecto
2. **Configurar las rutas protegidas** con AuthGuard
3. **Probar el flujo completo** de login → refresh → logout
4. **Configurar variables de entorno** para producción
5. **Implementar manejo de errores** más robusto

## 📝 Notas Importantes

- El refresh token se maneja automáticamente por el navegador en las cookies HttpOnly
- El access token se almacena en localStorage para uso en requests
- El interceptor maneja automáticamente la renovación de tokens
- La configuración está lista para producción con HTTPS
