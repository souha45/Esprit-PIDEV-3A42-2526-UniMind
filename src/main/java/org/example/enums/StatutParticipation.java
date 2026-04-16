package org.example.enums;

public enum StatutParticipation {
    EN_ATTENTE("attente"),
    CONFIRME("confirme"),
    ANNULE("annule");

    private final String dbValue;

    StatutParticipation(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static StatutParticipation fromDb(String value) {
        if (value == null) return null;
        // Gérer les anciennes valeurs pour compatibilité
        if (value.equals("en_attente")) {
            value = "attente";
        }
        for (StatutParticipation statut : StatutParticipation.values()) {
            if (statut.dbValue.equals(value)) {
                return statut;
            }
        }
        throw new IllegalArgumentException("Valeur StatutParticipation inconnue: " + value);
    }
}