package datastructure;

public enum CorrectionMode {
    DAM_SIL("silence damage", "silence-dam"),
    WCC("weighted-correction w/o reference upvote", "wc-NoUpvote"),
    WCC_UPVOTE("weighted-correction with reference upvote", "wc-WithUpvote"),
    NO_COR("no correction", "no-cor")
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

    public boolean needsCorrection(){
        return equals(WCC) || equals(WCC_UPVOTE);
    }

}
