package datastructure;

public enum CorrectionMode {
    NO_COR("NoCorrection", "nc"),
    SILENCING("Silencing", "s"),
    WEIGHTING("Weighting", "w")
    ;

    private final String mode_name;
    private final String short_name;

    CorrectionMode(String mode_name, String short_name){
        this.mode_name = mode_name;
        this.short_name = short_name;
    }

    public String getModeName() {
        return mode_name;
    }

    public String getShortName() {
        return short_name;
    }

    public static CorrectionMode fromShortName(String short_name) {
        for (CorrectionMode mode : CorrectionMode.values()) {
            if (mode.getShortName().equals(short_name)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("No CorrectionMode found for short name: " + short_name);
    }

}
