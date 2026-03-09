package it.smartcommunitylab.playandgo.engine.util;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.time.Instant;

@Component
public class JwtTokenUtil {
    private static Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    /**
     * Decodifica e valida un token JWT usando un endpoint JWKS
     * @param token il token JWT
     * @param jwksEndpoint l'endpoint JWKS (es: https://idp.example.com/.well-known/jwks.json)
     * @return Jwt con i claims
     * @throws JwtException se il token non è valido o scaduto
     */
    public Jwt validateAndGetClaimsWithJwks(String token, String jwksEndpoint) throws JwtException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksEndpoint).build();
        Jwt jwt = decoder.decode(token);
        
        // Verifica che il token non sia scaduto
        if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isBefore(Instant.now())) {
            logger.error("Token scaduto. Data di scadenza: {}", jwt.getExpiresAt());
            throw new JwtException("Token scaduto");
        }
        
        logger.debug("Token validato con successo usando JWKS endpoint: {}", jwksEndpoint);
        return jwt;
    }


    /**
     * Verifica se il token è valido usando endpoint JWKS senza lanciare eccezioni
     */
    public boolean isTokenValid(String token, String jwksEndpoint) {
        try {
            validateAndGetClaimsWithJwks(token, jwksEndpoint);
            return true;
        } catch (Exception e) {
            logger.warn("Token non valido: " + e.getMessage());
            return false;
        }
    }

    /**
     * Estrae il subject (sub) dal token usando endpoint JWKS
     */
    public String getSubject(String token, String jwksEndpoint) throws JwtException {
        Jwt jwt = validateAndGetClaimsWithJwks(token, jwksEndpoint);
        return jwt.getSubject();
    }

    /**
     * Estrae l'issuer (iss) dal token usando endpoint JWKS
     */
    public String getIssuer(String token, String jwksEndpoint) throws JwtException {
        Jwt jwt = validateAndGetClaimsWithJwks(token, jwksEndpoint);
        return jwt.getIssuer().toExternalForm();
    }

    /**
     * Estrae l'audience (aud) dal token usando endpoint JWKS
     */
    public java.util.Collection<String> getAudience(String token, String jwksEndpoint) throws JwtException {
        Jwt jwt = validateAndGetClaimsWithJwks(token, jwksEndpoint);
        return jwt.getAudience();
    }

    /**
     * Estrae tutti i claims dal token usando endpoint JWKS
     */
    public Map<String, Object> getAllClaims(String token, String jwksEndpoint) throws JwtException {
        Jwt jwt = validateAndGetClaimsWithJwks(token, jwksEndpoint);
        return jwt.getClaims();
    }

    /**
     * Estrae uno specifico claim dal token usando endpoint JWKS
     */
    public Object getClaim(String token, String jwksEndpoint, String claimName) throws JwtException {
        Jwt jwt = validateAndGetClaimsWithJwks(token, jwksEndpoint);
        return jwt.getClaims().get(claimName);
    }
}


