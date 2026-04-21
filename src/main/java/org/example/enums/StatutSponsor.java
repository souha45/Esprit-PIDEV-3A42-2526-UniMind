package org.example.enums;

public enum StatutSponsor {
    EN_ATTENTE("en_attente"),
    CONFIRME("confirme"),
    REFUSE("refuse"),
    ANNULE("annule");

    private final String dbValue;

    StatutSponsor(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static StatutSponsor fromDb(String value) {
        if (value == null) return null;
        for (StatutSponsor statut : StatutSponsor.values()) {
            if (statut.dbValue.equals(value)) {
                return statut;
            }
        }
        throw new IllegalArgumentException("Valeur StatutSponsor inconnue: " + value);
    }
}