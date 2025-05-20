package datastructure;

public enum DetectionMode {
    NO_COR("no correction", "no-cor"),
    BASED("Polarization-Based", "based"),
    FREE("Polarization-Free", "free")
    ;

    private final String detectionMode;
    private final String detectionModeShort;

    DetectionMode(String detectionMode, String detectionModeShort){
        this.detectionMode = detectionMode;
        this.detectionModeShort = detectionModeShort;
    }

    public String getDetectionMode() {
        return detectionMode;
    }

    public String getDetectionModeShort() {
        return detectionModeShort;
    }
}
