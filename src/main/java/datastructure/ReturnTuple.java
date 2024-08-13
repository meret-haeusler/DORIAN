package datastructure;

import htsjdk.variant.variantcontext.VariantContext;

import java.util.List;

public class ReturnTuple {

    private final StringBuilder seq;
    private final List<VariantContext> variants;

    public ReturnTuple(StringBuilder seq, List<VariantContext> variants) {
        this.seq = seq;
        this.variants = variants;
    }

    // Getter
    public StringBuilder getSeq() {
        return seq;
    }

    public List<VariantContext> getVariants() {
        return variants;
    }
}
