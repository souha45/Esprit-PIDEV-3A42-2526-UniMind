package org.example.enums;

public enum TypeContribution {
    FINANCIER("financier"),
    MATERIEL("materiel"),
    LOGISTIQUE("logistique"),
    COMMUNICATION("communication"),
    AUTRE("autre");

    private final String dbValue;

    TypeContribution(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static TypeContribution fromDb(String value) {
        if (value == null) return null;
        for (TypeContribution type : TypeContribution.values()) {
            if (type.dbValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Valeur TypeContribution inconnue: " + value);
    }
}