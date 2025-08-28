package com.alejandro.microservices.promptgeneratorsaas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    
    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnly}")
    private String jwtSecret;
    
    @Value("${jwt.access.expiration:900}") // 15 minutes default
    private int accessTokenExpiration;
    
    @Value("${jwt.refresh.expiration:604800}") // 7 days default
    private int refreshTokenExpiration;
    
    @Value("${jwt.issuer:prompt-generator-saas}")
    private String jwtIssuer;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(userDetails.getUsername());
    }

    public String generateAccessToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "ACCESS");
        return createToken(claims, username, accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateRefreshToken(userDetails.getUsername());
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "REFRESH");
        return createToken(claims, username, refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, int expirationSeconds) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(jwtIssuer)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000L))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateAccessToken(String token) {
        try {
            final String issuer = extractIssuer(token);
            final String type = extractTokenType(token);
            
            return !isTokenExpired(token) && 
                   jwtIssuer.equals(issuer) && 
                   "ACCESS".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean validateRefreshToken(String token) {
        try {
            final String issuer = extractIssuer(token);
            final String type = extractTokenType(token);
            
            return !isTokenExpired(token) && 
                   jwtIssuer.equals(issuer) && 
                   "REFRESH".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            final String issuer = extractIssuer(token);
            
            return (username.equals(userDetails.getUsername()) && 
                    !isTokenExpired(token) && 
                    jwtIssuer.equals(issuer));
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public int getRefreshExpirySeconds() {
        return refreshTokenExpiration;
    }
}
