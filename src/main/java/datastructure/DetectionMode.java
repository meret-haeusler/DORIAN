package datastructure;

public enum DetectionMode {
    NO_COR("no correction", "no-cor"),
    BASED("Polarization-Based", "pb"),
    FREE("Polarization-Free", "pf")
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

    public static DetectionMode fromShortName(String short_name) {
        for (DetectionMode mode : DetectionMode.values()) {
            if (mode.getDetectionModeShort().equals(short_name)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("No DetectionMode found for short name: " + short_name);
    }
}
