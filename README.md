## DORIAN
DORIAN (**D**amage-aware gen**O**me **R**econstruct**I**on for **AN**cient data) is a genome reconstruction tool designed for ancient data. DORIAN implements  three damage-aware reconstruction methods where positions that show ancient specific damage patterns are corrected prior to base calling. 

* Reference-based Silencing identifies damaged positions using the reference sequence and replaces damaged bases with a non-informative base call.
* Reference-free Silencing identifies damages positions by specific damage patterns in the mapping reads and replaces the damaged bases with a non-informative base call.
* Reference-free Weighting uses also uses the ancient specific damage pattern to identify damaged positions and performs a weighting on the affected bases to increase or decrease their influence on the base call bases on the severity of the damage observed.

In addition, a state-of-the-art genome reconstruction is implemented that bases the base calls merely on coverage and base frequencies. 

## Dependencies
The current version of DORIAN is build with ```JDK 15``` and ```Maven 3.12.1```. A precompiled excutable jar file is in the ```DORIAN/out/artifacts/DORIAN_jar``` folder of the repository.

## Usage
`````
java -jar <path/to/file>DORIAN.jar [options]

 -h,--help                      Print help message

 -b,--bam <FILE>                BAM file of mapped reads
 -r,--ref-file <FILE>           Reference genome
 -o,--out <PATH>                Path to output directory

 -c,--coverage <INT>            Minimum coverage for consensus calling
 -f,--minfreq <DOUBLE>          Minimum frequency for consensus calling

 -m,--mode <INT>                Correction modes:
                                1=no correction
                                2=ref-based silencing
                                3=ref-free silencing
                                4=ref-free weighting

 Only for ref-free weighting:                               
 -dp3,--damageprofile3 <FILE>   Path to DamageProfile of 3' end
 -dp5,--damageprofile5 <FILE>   Path to DamageProfile of 5' end
`````

## Output Files
<details>
<summary>Log</summary>
The log file contains a listing of the specified cli parameters for the given run. In addition, it lists all positions that were considered for correction (in <code>no correction</code> mode, all positions are listed) and some general information on the position.

* <code>CHROM</code>: The name of the reference sequence
* <code>POS</code>: The position in the reference sequence
* <code>REF</code>: The reference base at <code>POS</code>
* <code>COV</code>: The read coverage observed at <code>POS</code> prior correction
* <code>ALLELE_COUNTS_PRIOR</code>: Counts of observed bases prior correction
* <code>ALLELE_COUNTS_CORRECTED</code>: Counts of corrected bases (excluding N's)
* <code>BASE_CALL</code>: Final base call for the position as included in the Fasta
* <code>BASE_FREQ</code>: Frequency with which the <code>BASE_CALL</code> was made (always <code>-1.0</code> for N's, as a N is only called if the coverage or frequency for another base call is too low)
</details>


<details>
<summary>Fasta</summary>
Reconstructed sequence of the input sample. As header, the sample name as specified in the BAM file name and the chosen correction mode are used
</details>


<details>
<summary>BED</summary>

> Only for Reference-based Silencing, Reference-free Silencing and Reference-free Weighting runs.

File that can be loaded to IGV ([Interactive Genome Viewer](https://igv.org)) together with the BAM and reference file to closer inspect the corrected positions. This highlights the positions on which a correction was performed as well as the two previous and following positions.

</details>


<details>
<summary>VCF</summary>
File similar to VCF files generated in GATK's UnifiedGenotyper or HalotypeCaller. 

In Reference-free Weighting, weights that are not a whole number are rounded to the next integer in the AD tag of the VCF file.
</details>
