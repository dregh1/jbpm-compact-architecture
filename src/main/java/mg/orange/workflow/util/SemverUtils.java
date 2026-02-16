package mg.orange.workflow.util;

import java.util.regex.Pattern;

/**
 * Utilitaires pour la manipulation des versions selon Semantic Versioning 2.0 (X.Y.Z uniquement)
 */
public class SemverUtils {

    /**
     * Pattern regex pour valider le format semver X.Y.Z strict (pas de pre-release)
     * Exemples valides: 1.0.0, 2.1.3, 0.0.1
     */
    private static final Pattern SEMVER_PATTERN = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");

    /**
     * Valide si une chaîne représente une version semver valide X.Y.Z
     * @param version La version à valider
     * @return true si valide, false sinon
     */
    public static boolean isValidSemver(String version) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }
        return SEMVER_PATTERN.matcher(version.trim()).matches();
    }

    /**
     * Valide une version semver et lance une exception si invalide
     * @param version La version à valider
     * @throws IllegalArgumentException si la version est invalide
     */
    public static void validateSemver(String version) {
        if (!isValidSemver(version)) {
            throw new IllegalArgumentException("Format de version semver invalide: '" + version +
                    "'. Format attendu: X.Y.Z (ex: 1.0.0)");
        }
    }

    /**
     * Incrémente la version majeure (X+1.Y.0)
     * @param currentVersion Version actuelle
     * @return Nouvelle version majeure
     */
    public static String bumpMajor(String currentVersion) {
        validateSemver(currentVersion);
        SemverComponents comp = parseSemver(currentVersion);
        return (comp.major + 1) + ".0.0";
    }

    /**
     * Incrémente la version mineure (X.Y+1.0)
     * @param currentVersion Version actuelle
     * @return Nouvelle version mineure
     */
    public static String bumpMinor(String currentVersion) {
        validateSemver(currentVersion);
        SemverComponents comp = parseSemver(currentVersion);
        return comp.major + "." + (comp.minor + 1) + ".0";
    }

    /**
     * Incrémente la version de patch (X.Y.Z+1)
     * @param currentVersion Version actuelle
     * @return Nouvelle version de patch
     */
    public static String bumpPatch(String currentVersion) {
        validateSemver(currentVersion);
        SemverComponents comp = parseSemver(currentVersion);
        return comp.major + "." + comp.minor + "." + (comp.patch + 1);
    }

    /**
     * Définit une version spécifique (pour migrations ou initialisations)
     * @param major Numéro majeur
     * @param minor Numéro mineur
     * @param patch Numéro de patch
     * @return Version formatée X.Y.Z
     */
    public static String setVersion(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Les numéros de version ne peuvent pas être négatifs");
        }
        return major + "." + minor + "." + patch;
    }

    /**
     * Parse une version semver en ses composants
     */
    private static SemverComponents parseSemver(String version) {
        String[] parts = version.trim().split("\\.");
        return new SemverComponents(
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2])
        );
    }

    /**
     * Classe interne pour représenter les composants d'une version semver
     */
    private static class SemverComponents {
        final int major;
        final int minor;
        final int patch;

        SemverComponents(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }
    }

    /**
     * Compare deux versions semver
     * @return négatif si v1 < v2, 0 si v1 == v2, positif si v1 > v2
     */
    public static int compareVersions(String v1, String v2) {
        validateSemver(v1);
        validateSemver(v2);

        SemverComponents c1 = parseSemver(v1);
        SemverComponents c2 = parseSemver(v2);

        int majorCompare = Integer.compare(c1.major, c2.major);
        if (majorCompare != 0) return majorCompare;

        int minorCompare = Integer.compare(c1.minor, c2.minor);
        if (minorCompare != 0) return minorCompare;

        return Integer.compare(c1.patch, c2.patch);
    }

    /**
     * Version par défaut pour l'initialisation
     */
    public static final String DEFAULT_VERSION = "1.0.0";

    /**
     * Version d'initialisation pour données existantes
     */
    public static final String MIGRATION_VERSION = "0.1.0";
}
