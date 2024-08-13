package datastructure;

public enum CorrectionMode {
    NO_COR("no correction", "no-cor"),
    REFBASED_SIL("reference-based silencing", "ref-based_sil"),
    REFFREE_SIL("reference-free silencing", "ref-free_sil"),
    REFFREE_WEI("reference-free weighting", "ref-free_weighting")
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

    public boolean needsDP(){
        return equals(REFFREE_WEI);
    }

}
