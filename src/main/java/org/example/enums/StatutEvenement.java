package org.example.enums;

public enum StatutEvenement {
    A_VENIR("a_venir"),
    EN_COURS("en_cours"),
    TERMINE("termine"),
    ANNULE("annule");

    private final String dbValue;

    StatutEvenement(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static StatutEvenement fromDb(String value) {
        if (value == null) return null;
        for (StatutEvenement statut : StatutEvenement.values()) {
            if (statut.dbValue.equals(value)) {
                return statut;
            }
        }
        throw new IllegalArgumentException("Valeur StatutEvenement inconnue: " + value);
    }
}