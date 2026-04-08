package org.example.enums;

public enum StatutParticipation {
    EN_ATTENTE("en_attente"),
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
        for (StatutParticipation statut : StatutParticipation.values()) {
            if (statut.dbValue.equals(value)) {
                return statut;
            }
        }
        throw new IllegalArgumentException("Valeur StatutParticipation inconnue: " + value);
    }
}