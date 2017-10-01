package cn.intret.app.picgo.model.user;

public enum SortWay {
    NAME,
    SIZE,
    DATE,
    UNKNOWN;
    public static SortWay fromString(String string) {
        if (string == null) {
            return UNKNOWN;
        }
        switch (string) {
            case "NAME": return NAME;
            case "SIZE": return SIZE;
            case "DATE": return DATE;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        switch (this) {
            case NAME: return "NAME";
            case SIZE: return "SIZE";
            case DATE: return "DATE";
        }
        return "UNKNOWN";
    }
}
