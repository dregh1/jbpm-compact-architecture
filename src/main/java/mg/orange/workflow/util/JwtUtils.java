package mg.orange.workflow.util;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.build.Jwt;
import mg.orange.workflow.exception.ApiException;
import mg.orange.workflow.model.user.Utilisateur;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class JwtUtils {

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "orange-workflow")
    String issuer;

    @ConfigProperty(name = "jwt.access.token.expiration", defaultValue = "900") // 15 minutes
    long accessTokenExpirationSeconds;

    @ConfigProperty(name = "jwt.refresh.token.expiration", defaultValue = "604800") // 7 jours
    long refreshTokenExpirationSeconds;

    private final JWTParser parser;

    @Inject
    public JwtUtils(JWTParser parser) {
        this.parser = parser;
    }

    /**
     * Génère un token d'accès JWT
     */
    public String generateAccessToken(Utilisateur user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTokenExpirationSeconds);

        return Jwt.issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .groups(getUserRoles(user))
                .claim("email", user.getEmail())
                .claim("nomComplet", user.getNomComplet())
                .issuedAt(now)
                .expiresAt(expiration)
                .sign();
    }

    /**
     * Génère un token de rafraîchissement
     */
    public String generateRefreshToken(Utilisateur user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(refreshTokenExpirationSeconds);

        return Jwt.issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiresAt(expiration)
                .sign();
    }

    /**
     * Génère à la fois un token d'accès et un token de rafraîchissement
     */
    public TokenPair generateTokenPair(Utilisateur user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Valide un token JWT
     */
    public boolean validateToken(String token) {
        try {
            // Parser le token spécifique passé en paramètre
            parser.parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrait l'ID utilisateur du token
     */
    public Long getUserIdFromToken(String token) {
        try {
            JsonWebToken parsedToken = parser.parse(token);
            String subject = parsedToken.getSubject();
            return subject != null ? Long.valueOf(subject) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrait l'email du token
     */
    public String getEmailFromToken(String token) {
        try {
            JsonWebToken parsedToken = parser.parse(token);
            return parsedToken.getClaim("email");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Vérifie si le token est expiré
     */
    public boolean isTokenExpired(String token) {
        try {
            JsonWebToken parsedToken = parser.parse(token);
            long expirationTime = parsedToken.getExpirationTime();
            Instant expiration = Instant.ofEpochSecond(expirationTime);
            return expiration != null && Instant.now().isAfter(expiration);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extrait la date d'expiration du token
     */
    public java.util.Date getExpirationDateFromToken(String token) {
        try {
            JsonWebToken parsedToken = parser.parse(token);
            long expirationTime = parsedToken.getExpirationTime();
            return new java.util.Date(expirationTime * 1000);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calcule la durée de validité restante du token en secondes
     */
    public long getRemainingValiditySeconds(String token) {
        try {
            JsonWebToken parsedToken = parser.parse(token);
            long expirationTime = parsedToken.getExpirationTime();
            Instant expiration = Instant.ofEpochSecond(expirationTime);
            if (expiration == null) return 0;

            Duration remaining = Duration.between(Instant.now(), expiration);
            return Math.max(0, remaining.getSeconds());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Rafraîchit un token d'accès en utilisant un refresh token valide
     */
    public String refreshAccessToken(String refreshToken, Utilisateur user) {
        if (!validateToken(refreshToken)) {
            throw new ApiException("Token de rafraîchissement invalide", 401);
        }

        if (isTokenExpired(refreshToken)) {
            throw new ApiException("Token de rafraîchissement expiré", 401);
        }

        // Vérifier que le refresh token appartient bien à l'utilisateur
        Long tokenUserId = getUserIdFromToken(refreshToken);
        if (!user.getId().equals(tokenUserId)) {
            throw new ApiException("Token de rafraîchissement ne correspond pas à l'utilisateur", 401);
        }

        return generateAccessToken(user);
    }

    /**
     * Récupère les rôles de l'utilisateur sous forme de Set
     */
    private Set<String> getUserRoles(Utilisateur user) {
        // Ici, vous pouvez adapter selon votre logique de rôles
        // Pour l'exemple, on suppose que l'utilisateur a des rôles sous forme de chaîne
        Set<String> roles = new HashSet<>();

        // Ajouter le rôle par défaut
        roles.add("USER");

        // Si l'utilisateur a des rôles spécifiques, les ajouter
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            // Supposons que les rôles sont stockés sous forme de chaîne séparée par des virgules
            String[] userRoles = user.getRoles().split(",");
            roles.addAll(Arrays.asList(userRoles));
        }

        return roles;
    }

    /**
     * Classe interne pour représenter une paire de tokens
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
