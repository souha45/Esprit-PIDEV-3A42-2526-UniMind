package org.example.enums;

public enum StatutTraitement {
    EN_COURS,
    TERMINE,
    SUSPENDU;

    /**
     * Retourne la valeur au format Symfony
     */
    public String getValeurSymfony() {
        switch (this) {
            case EN_COURS:
                return "en cours";
            case TERMINE:
                return "termine";
            case SUSPENDU:
                return "suspendu";
            default:
                return this.name().toLowerCase();
        }
    }

    /**
     * Retourne l'enum à partir de la valeur Symfony
     */
    public static StatutTraitement fromValeurSymfony(String valeur) {
        if (valeur == null) return EN_COURS;
        switch (valeur.toLowerCase()) {
            case "en cours":
                return EN_COURS;
            case "termine":
                return TERMINE;
            case "suspendu":
                return SUSPENDU;
            default:
                return EN_COURS;
        }
    }
}