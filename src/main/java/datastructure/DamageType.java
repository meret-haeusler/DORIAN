package datastructure;

public enum DamageType {
    CT, GA, NONE;

    public boolean needsCorrection() {
        return equals(CT) || equals(GA);
    }
}


