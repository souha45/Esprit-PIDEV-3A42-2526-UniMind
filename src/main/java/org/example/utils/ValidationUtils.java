package org.example.utils;

public class ValidationUtils {

    //EMAIL valide ou pas
    public static boolean isEmailValide(String email) {
        return email != null && email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    // MOT DE PASSE (min 8, 1 majuscule, 1 chiffre)
    public static boolean isPasswordValide(String password) {
        return password != null
                && password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*[0-9].*");
    }

    // TELEPHONE (exactement 8 chiffres)
    public static boolean isTelephoneValide(String tel) {
        return tel != null && tel.matches("^[0-9]{8}$");
    }

    // CIN (exactement 8 chiffres)
    public static boolean isCinValide(String cin) {
        return cin != null && cin.matches("^[0-9]{8}$");
    }

    //  CHAMP NON VIDE
    public static boolean isNonVide(String valeur) {
        return valeur != null && !valeur.trim().isEmpty();
    }

    // NOM / PRENOM (lettres uniquement, 2 à 50 caractères)
    public static boolean isNomValide(String nom) {
        return nom != null && nom.trim().matches("^[a-zA-ZÀ-ÿ\\s\\-]{2,50}$");
    }

    // CONFIRMATION MOT DE PASSE
    public static boolean isPasswordConfirme(String password, String confirm) {
        return password != null && password.equals(confirm);
    }

    // MESSAGES D'ERREUR
    public static String messageEmail() {
        return "Email invalide (ex: user@esprit.tn)";
    }

    public static String messagePassword() {
        return "Min 8 caractères, 1 majuscule, 1 chiffre";
    }

    public static String messageTelephone() {
        return "Téléphone : 8 chiffres";
    }

    public static String messageCin() {
        return "CIN : 8 chiffres obligatoires";
    }

    public static String messageNom() {
        return "Nom/Prénom : lettres uniquement, 2 à 50 caractères";
    }

    public static String messagePasswordConfirm() {
        return "Les mots de passe ne correspondent pas";
    }
}
