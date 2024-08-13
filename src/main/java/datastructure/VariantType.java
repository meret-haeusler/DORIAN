package datastructure;

public enum VariantType {
        CT, GA, NONE;

    public boolean isVariant(){
        return equals(CT) || equals(GA);
    }
}


