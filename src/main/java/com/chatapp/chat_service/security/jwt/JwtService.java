package com.chatapp.chat_service.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    // 👇 Bạn có thể load từ file config (application.yml)
    private static final String SECRET_KEY = "Fzq8zZPO1YcV0aRCFMCYdxnDAhJ4TLWit5RGkZllNFA=";
    private static final long EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 ngày

    private final Key key;

    public JwtService() {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
    }

    public String generateToken(String username, UUID userId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }


    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }


    public UUID getUserIdFromToken(String token) {
        String idStr = getClaims(token).get("userId", String.class);
        return UUID.fromString(idStr);
    }
}
