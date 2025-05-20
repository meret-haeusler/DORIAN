package datastructure;

public enum CorrectionMode {
    NO_COR("no correction", "no-cor"),
    SILENCING("silencing", "sil"),
    WEIGHTING("weighting", "wei")
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

}
