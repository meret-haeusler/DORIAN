package utils;

import datastructure.Fasta;
import dorian.dorian;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class VCFFileWriter {

    /**
     * Writes a vcf file from a given list of variant contexts
     * @param vcf_out       Path to VCF output file
     * @param vcfHeader     VCFHeader object for given sample
     * @param variant_calls List of variant contexts
     */
    public static void writeVCFFile(String vcf_out, VCFHeader vcfHeader, List<VariantContext> variant_calls) {
        // Create vcf writer
        VCFEncoder vcf_writer = new VCFEncoder(vcfHeader, true, true);

        // Create file writer
        try (FileWriter writer = new FileWriter(vcf_out, false)) {
            BufferedWriter bw = new BufferedWriter(writer);

            // Write VCF header to the file
            for (VCFHeaderLine headerline : vcfHeader.getMetaDataInSortedOrder()) {
                bw.write("##" + headerline);
                bw.newLine();
            }

            // Write column identifiers
            writeColumnHeader(bw, vcfHeader);

            // Write each VariantContext to the VCF file
            for (VariantContext variant : variant_calls) {
                vcf_writer.write(bw, variant);
                bw.newLine();
            }

            bw.close();

        } catch (Exception e) {
            dorian.logger.error(e.getMessage());
        }
    }


    /**
     * Writes the VCF column header to the specified output file
     * @param bw        Writer for current file
     * @param vcfHeader VCF header of output data
     * @throws IOException Throws exception if fails
     */
    public static void writeColumnHeader(BufferedWriter bw, VCFHeader vcfHeader) throws IOException {
        bw.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
        for (String sample : vcfHeader.getSampleNamesInOrder()) {
            bw.write("\t" + sample);
        }
        bw.newLine();
    }


    /**
     * Creates a header for a VCF file using reference and sample information
     * @param ref           Reference as Fasta object
     * @param sample_name   Name of analysed sample
     * @return  VCFHeader for given sample
     */
    public static VCFHeader defaultHeader(Fasta ref, String sample_name){

        VCFHeaderVersion version = VCFHeaderVersion.valueOf("VCF4_2");

        Map<String, String> contig_map =  Map.of(
                "ID", ref.getHeader().split(" ")[0].replace(">", ""),
                "length", Integer.toString(ref.getSequence().length()));
        VCFHeaderLine contig = new VCFContigHeaderLine(contig_map,0);
        VCFHeaderLine ad = new VCFFormatHeaderLine("AD", VCFHeaderLineCount.R, VCFHeaderLineType.Integer,
                "Allelic depths for the ref and alt alleles in the order listed");
        VCFHeaderLine dp = new VCFFormatHeaderLine("DP", 1, VCFHeaderLineType.Integer,
                "Approximate read depth (reads with MQ=255 or with bad mates are filtered)");
        VCFHeaderLine cor_mode_filter = new VCFInfoHeaderLine("COR_MODE", 1, VCFHeaderLineType.String,
                "Used correction mode: " + dorian.cor_mode.getModeName());
        VCFHeaderLine cov_1_filter = new VCFInfoHeaderLine("MIN_COV_1", 2, VCFHeaderLineType.String,
                "Minimal coverage filter (incl. N): " + dorian.cov);
        VCFHeaderLine freq_filter = new VCFInfoHeaderLine("MIN_FREQ", 3, VCFHeaderLineType.String,
                "Minimal base frequency filter: " + dorian.freq);

        Set<VCFHeaderLine> meta_data = new HashSet<>(Arrays.asList(contig, ad, dp, cor_mode_filter, cov_1_filter, freq_filter));

        VCFHeader header = new VCFHeader(meta_data, new ArrayList<>(Collections.singleton(sample_name)));

        header.setVCFHeaderVersion(version);

        return header;
    }

}
