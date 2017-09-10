package cn.intret.app.picgo.model;


public enum ViewMode {
    GRID_VIEW,
    LIST_VIEW,
    UNKNOWN;

    public static ViewMode fromString(String string) {
        if (string == null) {
            return UNKNOWN;
        }
        switch (string) {
            case "GRID_VIEW": return GRID_VIEW;
            case "LIST_VIEW": return LIST_VIEW;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        switch (this) {
            case LIST_VIEW: return "LIST_VIEW";
            case GRID_VIEW: return "GRID_VIEW";
        }
        return "UNKNOWN";
    }
}
