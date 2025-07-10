## DORIAN
<table>
    <tr>
        <td>
DORIAN (<b>D</b>amage-aware gen<b>O</b>me <b>R</b>econstruct<b>I</b>on for <b>AN</b>cient data) is a genome reconstruction tool designed for ancient data. DORIAN implements damage-aware genome reconstruction with different methods to detect and correct damaged positions prior to consensus base calling. <br>
DORIAN allows two modes to detect and correct damaged positions, respectively.  
        </td>
        <td>
            <img src="media/DORIAN.jpg" alt="DORIAN Logo" width="750"/>
        </td>
    </tr>
</table>

### Damage detection
* Polarization-Based identifies damaged positions using the reference base.
* Polarization-Free identifies damaged positions using ancient specific damage patterns in the mapping reads.

### Damage correction
* Silencing replaces damaged positions with a non-informative ('N') base call.
* Weighting performs a weighting on the affected bases to increase or decrease their influence on the base call bases on the severity of the damage observed.
* No correction: Consensus base calls are merely made based on coverage and base frequency.  
  If ```--correction nc``` is specified, ```--detection``` parameter must not be specified.


## Software Requirements
* Java 22.0.1
* Apache Maven 3.9.10

### OS Requirements
DORIAN was tested on the following systems:
* macOS: Sequoia (15.5)
* Linux: Ubuntu (22.04.5)

## Installation
```
git clone git@github.com:meret-haeusler/DORIAN.git
cd DORIAN
mvn clean compile assembly:single
```
The compiled jar file can be found in ```DORIAN/target```. A precompiled executable jar file is also available in the ```DORIAN/out/artifacts/DORIAN_jar``` folder of the repository.

## Usage
`````
java -jar <path/to/file>DORIAN.jar [options]

 -h,--help                      Print help message

 -b,--bam <FILE>                BAM file of mapped reads
 -r,--reference <FILE>          Reference genome
 -o,--out <PATH>                Path to output directory (must already exist)

 -c,--cov <INT>                 Minimum coverage for consensus calling
 -f,--freq <DOUBLE>             Minimum frequency for consensus calling

--correction <STRING>           Damage correction mode: 
                                    s  (Silencing)
                                    w  (Weighting)
                                    nc (no correction)
--detection <STRING>            Damage detection mode (only for correction modes s and w):
                                    pb (Polarization-Based)
                                    pf (Polarization-Free)  

 Only for correction mode w:                               
 --dp3 <FILE>                   Path to DamageProfile of 3' end
 --dp5 <FILE>                   Path to DamageProfile of 5' end
 --dp_file <FILE>               Path to tsv file specifying a DamageProfile for each read group (details below)
 
 Optional:
 --bed                          Writes a BED file to --out (ROI table in IGV format for corrected positions)
 --vcf                          Writes a VCF file to --out
`````

### --dp_file parameter
The --dp_file parameter allows the user to specify different DamageProfiles for the read groups in the BAM file. 
This is useful when different read groups have different damage patterns, e.g. combined samples with and without UDG treatment.  
The file should be a tsv file that contains the read group ID and the paths to the DamageProfile files for the 5' and 3' ends of the respective read groups:
```
read_group_id    path/to/dp5.txt    path/to/dp3.txt
```
- The read group ID can be obtained from the BAM file using the command:
```samtools view -H <path/to/bam> | grep '^@RG'``` (use only the string after ```ID:```) 
- The path can either be absolute or relative to the directory where DORIAN is run.
- An example file is provided at `test_data/dp_file.tsv`.

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

> Only for runs where ```--correction```is either ```w```  or ```s```.

File that can be loaded to IGV ([Interactive Genome Viewer](https://igv.org)) together with the BAM and reference file to closer inspect the corrected positions. This highlights the positions on which a correction was performed as well as the two previous and following positions.

</details>


<details>
<summary>VCF</summary>
File similar to VCF files generated in GATK's UnifiedGenotyper or HalotypeCaller. 

If ```--correction``` is ```w```, weights which are not a whole number are rounded to the next integer in the AD tag of the VCF file.
</details>



## Test Data and Example Commands 
A small dummy dataset is provided in the ```test_data``` folder of the repository. It contains a BAM file, a reference genome, and DamageProfiles for both 5' and 3' ends.

### Example commands
* Run DORIAN with Polarization-Based damage detection and Silencing correction using a minimal coverage of 3 and a minimal frequency of 0.66:  
  ```java -jar DORIAN.jar -b test_data/test_reads.bam -r test_data/test_genome.fasta -o test_out -c 3 -f 0.66 --correction s --detection pb```
* Run DORIAN with Polarization-Free damage detection and Silencing correction using a minimal coverage of 3 and a minimal frequency of 0.66. Include a VCF and BED file to the output:  
  ```java -jar DORIAN.jar -b test_data/test_reads.bam -r test_data/test_genome.fasta -o test_out -c 3 -f 0.66 --correction s --detection pf --bed --vcf```
* Run DORIAN with Polarization-Based damage detection and Weighting correction using a minimal coverage of 3 and a minimal frequency of 0.66. Use the same DamageProfiles for all read groups:  
  ```java -jar DORIAN.jar -b test_data/test_reads.bam -r test_data/test_genome.fasta -o test_out -c 3 -f 0.66 --correction w --detection pb --dp3 test_data/dp3.txt --dp5 test_data/dp5.txt```  
* Run DORIAN with Polarization-Free damage detection and Weighting correction using a minimal coverage of 3 and a minimal frequency of 0.66. Use different DamageProfiles for each read group as specified in the ```test_data/dp_file.tsv``` file and include a BED file to the output:  
  ```java -jar DORIAN.jar -b test_data/test_reads.bam -r test_data/test_genome.fasta -o test_out -c 3 -f 0.66 --correction w --detection pf --dp_file test_data/dp_file.tsv```  