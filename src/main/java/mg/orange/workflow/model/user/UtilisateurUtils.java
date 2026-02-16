package mg.orange.workflow.model.user;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class UtilisateurUtils {

    private UtilisateurUtils() {}

    /**
     * Hash un mot de passe avec BCrypt (12 rounds)
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être null ou vide");
        }
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    /**
     * Vérifie un mot de passe en clair contre un hash BCrypt
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword);
        return result.verified;
    }

    /**
     * Vérifie si un hash utilise BCrypt (commence par $2a$, $2b$, ou $2y$)
     */
    public static boolean isBCryptHash(String hashedPassword) {
        return hashedPassword != null &&
               (hashedPassword.startsWith("$2a$") ||
                hashedPassword.startsWith("$2b$") ||
                hashedPassword.startsWith("$2y$"));
    }

}
