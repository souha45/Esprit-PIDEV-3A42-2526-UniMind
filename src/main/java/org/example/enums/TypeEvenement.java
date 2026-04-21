package org.example.enums;

public enum TypeEvenement {
    ATELIER("atelier"),
    WEBINAIRE("webinaire"),
    GROUPE_PAROLE("groupe_parole"),
    CONFERENCE("conference"),
    JOURNEE_THEMATIQUE("journee_thematique"),
    ACTIVITE_SPORTIVE("activite_sportive"),
    FORMATION("formation");

    private final String dbValue;

    TypeEvenement(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static TypeEvenement fromDb(String value) {
        if (value == null) return null;
        for (TypeEvenement type : TypeEvenement.values()) {
            if (type.dbValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Valeur TypeEvenement inconnue: " + value);
    }
}