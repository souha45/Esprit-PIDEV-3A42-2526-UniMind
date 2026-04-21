package org.example.enums;

public enum TypeSponsor {
    MUTUELLE("mutuelle"),
    ENTREPRISE("entreprise"),
    ASSOCIATION("association"),
    FONDATION("fondation");

    private final String dbValue;

    TypeSponsor(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static TypeSponsor fromDb(String value) {
        if (value == null) return null;
        for (TypeSponsor type : TypeSponsor.values()) {
            if (type.dbValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Valeur TypeSponsor inconnue: " + value);
    }
}