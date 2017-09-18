package cn.intret.app.picgo.model;

public enum SortOrder {
    DESC,
    ASC,
    UNKNOWN;
    public static SortOrder fromString(String string) {
        if (string == null) {
            return UNKNOWN;
        }
        switch (string) {
            case "DESC": return DESC;
            case "ASC": return ASC;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        switch (this) {
            case ASC: return "ASC";
            case DESC: return "DESC";
        }
        return "UNKNOWN";
    }
}
