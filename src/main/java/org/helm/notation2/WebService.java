/**
 * *****************************************************************************
 * Copyright C 2015, The Pistoia Alliance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *****************************************************************************
 */
package org.helm.notation2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.helm.chemtoolkit.CTKException;
import org.helm.notation.MonomerException;
import org.helm.notation.MonomerFactory;
import org.helm.notation.MonomerLoadingException;
import org.helm.notation.MonomerStore;
import org.helm.notation.NotationException;
import org.helm.notation.model.Monomer;
import org.helm.notation.tools.xHelmNotationParser;
import org.helm.notation2.calculation.ExtinctionCoefficient;
import org.helm.notation2.exception.BuilderMoleculeException;
import org.helm.notation2.exception.ConnectionNotationException;
import org.helm.notation2.exception.ExtinctionCoefficientException;
import org.helm.notation2.exception.FastaFormatException;
import org.helm.notation2.exception.GroupingNotationException;
import org.helm.notation2.exception.HELM1FormatException;
import org.helm.notation2.exception.HELM2HandledException;
import org.helm.notation2.exception.ParserException;
import org.helm.notation2.exception.PeptideUtilsException;
import org.helm.notation2.exception.PolymerIDsException;
import org.helm.notation2.exception.ValidationException;
import org.helm.notation2.parser.ConverterHELM1ToHELM2;
import org.helm.notation2.parser.ParserHELM2;
import org.helm.notation2.parser.exceptionparser.ExceptionState;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebService class containing all required methods for the web-service
 *
 * @author hecht
 */
public class WebService {

  /** The Logger for this class */
  private static final Logger LOG = LoggerFactory.getLogger(WebService.class);

  /**
   * method to get the XHELMRootElement of a document as a string
   *
   * @param resource xhelm input
   * @return XHELMRootElement
   * @throws JDOMException
   * @throws IOException
   */
  private Element getXHELMRootElement(String resource) throws JDOMException,
      IOException {

    ByteArrayInputStream stream = new ByteArrayInputStream(resource.getBytes());
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(stream);

    return doc.getRootElement();
  }

  /**
   * method to combine the new MonomerStore to the existing one, in case of
   * xHELM as input
   *
   * @param monomerStore MonomerStore
   * @throws MonomerLoadingException
   * @throws IOException
   * @throws MonomerException
   */
  private void updateMonomerStore(MonomerStore monomerStore) throws MonomerLoadingException, IOException, MonomerException {
    for (Monomer monomer : monomerStore.getAllMonomersList()) {
      MonomerFactory.getInstance().getMonomerStore().addNewMonomer(monomer);
      // save monomer db to local file after successful update //
      MonomerFactory.getInstance().saveMonomerCache();
    }
  }

  /**
   * method to read the HELM string, the HELM can be in version 1 or 2, or in
   * Xhelm format
   *
   * @param notation HELM input
   * @return ContainerHELM2
   * @throws ParserException
   * @throws IOException
   * @throws JDOMException
   * @throws MonomerException
   */
  private ContainerHELM2 readNotation(String notation) throws ParserException, JDOMException, IOException, MonomerException {
    /* xhelm notation */
    if (notation.contains("<Xhelm>")) {
      LOG.info("xhelm is used as input");
      String xhelm = notation;
      Element xHELMRootElement = getXHELMRootElement(xhelm);

      notation = xHelmNotationParser.getHELMNotationString(xHELMRootElement);
      MonomerStore store = xHelmNotationParser.getMonomerStore(xHELMRootElement);
      updateMonomerStore(store);

    }
    /* HELM1-Format -> */
    if (!(notation.contains("V2.0") || notation.contains("v2.0"))) {
      if (notation.endsWith("$")) {
        LOG.info("Convert HELM1 into HELM2");
        notation = new ConverterHELM1ToHELM2().doConvert(notation);
        LOG.info("Conversion was successful: " + notation);
      } else {
        LOG.info("Wrong HELM Input");
        throw new ParserException("HELMNotation is not valid");
      }
    }
    /* parses the HELM notation and generates the necessary notation objects */
    ParserHELM2 parser = new ParserHELM2();
    try {
      LOG.info("Parse HELM2");
      parser.parse(notation);
      LOG.info("Parsing was successful");
    } catch (ExceptionState | IOException | JDOMException e) {
      throw new ParserException("HELMNotation is not valid");
    }
    return new ContainerHELM2(parser.getHELM2Notation(), new InterConnections());
  }

  /**
   * method to validate the HELM-String input
   *
   * @param helm input HELM string
   * @return ContainerHELM2
   * @throws ValidationException if the HELM input is not valid
   */
  private ContainerHELM2 validate(String helm) throws ValidationException {

    try {
      /* Read */
      ContainerHELM2 containerhelm2 = readNotation(helm);

      /* Validate */
      LOG.info("Validation of HELM is starting");
      Validation.validateNotationObjects(containerhelm2);
      LOG.info("Validation was successful");

      return containerhelm2;

    } catch (MonomerException | GroupingNotationException | ConnectionNotationException | PolymerIDsException | ParserException | JDOMException | IOException e) {
      LOG.info("Validation was not successful");
      LOG.error(e.getMessage());
      throw new ValidationException(e.getMessage());
    }
  }

  /**
   * method to validate the input HELM-String
   *
   * @param helm input HELM-string
   * @throws ValidationException if the input HELM is not valid
   * @throws MonomerLoadingException if the MonomerFactory can not be refreshed
   */
  public void validateHELM(String helm) throws ValidationException, MonomerLoadingException {
    validate(helm);
    setMonomerFactoryToDefault(helm);
  }

  /**
   * method to convert the input HELM into canonical HELM
   *
   * @param notation HELM input
   * @return canonical HELM
   * @throws HELM1FormatException if HELM input contains HELM2 features
   * @throws NotationException if the notation objects can not be built
   * @throws ValidationException if the HELM input is not valid
   * @throws MonomerLoadingException if the MonomerFactory can not be loaded
   */
  public String convertStandardHELMToCanonicalHELM(String notation) throws HELM1FormatException, NotationException, ValidationException, MonomerLoadingException {
    String result = HELM1Utils.getCanonical(validate(notation).getHELM2Notation());
    setMonomerFactoryToDefault(notation);
    return result;
  }

  /**
   * method to convert the input HELM into a standard HELM
   *
   * @param notation HELM input
   * @return standard HELM
   * @throws HELM1FormatException if the HELM input contains HELM2 features
   * @throws NotationException if the notation objects can not be built
   * @throws ValidationException if the HELM input is not valid
   * @throws MonomerLoadingException if the MonomerFactory can not be loaded
   * @throws CTKException
   */
  public String convertIntoStandardHELM(String notation) throws HELM1FormatException, NotationException, ValidationException, MonomerLoadingException, CTKException {
    String result = HELM1Utils.getStandard(validate(notation).getHELM2Notation());
    setMonomerFactoryToDefault(notation);
    return result;
  }

  /**
   * method to calculate from a non-ambiguous HELM string the extinction
   * coefficient
   *
   * @param notation HELM input
   * @return extinction coefficient from the HELM input
   * @throws ExtinctionCoefficientException if the extinction coefficient can
   *           not be calculated
   * @throws ValidationException if the HELM input is not valid
   * @throws MonomerLoadingException if the MonomerFactory can not be refreshed
   */
  public Float calculateExtinctionCoefficient(String notation) throws ExtinctionCoefficientException, ValidationException, MonomerLoadingException {
    Float result = ExtinctionCoefficient.getInstance().calculate(validate(notation).getHELM2Notation());
    setMonomerFactoryToDefault(notation);
    return result;
  }

  /**
   * method to generate FASTA-Formats for all rna and peptide sequences from an
   * HELM input
   *
   * @param notation HELM input
   * @return FASTA containing all rna and peptide sequences
   * @throws ValidationException if the HELM input is not valid
   * @throws FastaFormatException if the FASTA-sequences can not be built
   * @throws MonomerLoadingException if the MonomerFactory can not be loaded
   */
  public String generateFasta(String notation) throws FastaFormatException, ValidationException, MonomerLoadingException {
    String result = FastaFormat.generateFasta(validate(notation).getHELM2Notation());
    setMonomerFactoryToDefault(notation);
    return result;
  }

  /**
   * method to generate HELM from a FASTA containing rna/dna sequences
   *
   * @param notation FASTA containing rna/dna sequences
   * @return HELM
   * @throws FastaFormatException if the input is not valid
   * @throws JDOMException
   * @throws IOException
   * @throws org.helm.notation2.parser.exceptionparser.NotationException
   */
  public String generateHELMFromFastaNucleotide(String notation) throws FastaFormatException, org.helm.notation2.parser.exceptionparser.NotationException, IOException, JDOMException {
    String result = new ContainerHELM2(FastaFormat.generateRNAPolymersFromFastaFormatHELM1(notation), new InterConnections()).getHELM2Notation().toHELM2();
    setMonomerFactoryToDefault(notation);
    return result;
  }

  /**
   * method to generate HELM from a FASTA containing peptide sequence(s)
   *
   * @param notation FASTA containing peptide sequence(s)
   * @return HELM
   * @throws FastaFormatException if the FASTA input is not valid
   * @throws MonomerLoadingException if the MonomerFactory can not be loaded
   */
  public String generateHELMFromFastaPeptide(String notation) throws FastaFormatException, MonomerLoadingException {
    String result = new ContainerHELM2(FastaFormat.generatePeptidePolymersFromFASTAFormatHELM1(notation), new InterConnections()).getHELM2Notation().toHELM2();
    setMonomerFactoryToDefault(notation);
    return result;
  }

  /**
   * method to calculate from a non-ambiguous HELM input the molecular weight
   *
   * @param notation HELM input
   * @return moleuclar weight from the HELM input
   * @throws MonomerLoadingException if the MonomerFactory can not be refreshed
   * @throws ValidationException if the HELM input is not valid
   * @throws BuilderMoleculeException if the molecule for the calculation can
   *           not be built
   * @throws CTKException
   */
  public Double calculateMolecularWeight(String notation) throws MonomerLoadingException, BuilderMoleculeException, CTKException, ValidationException {
    Double result = MoleculeInformation.getMolecularWeight(validate(notation).getHELM2Notation());
    setMonomerFactoryToDefault(notation);
    return result;
  }

  /**
   * method to calculate from a non-ambiguous HELM input the molecular formula
   *
   * @param notation HELM input
   * @return molecular formula from the HELM input
   * @throws ValidationException if the HELM input is not valid
   * @throws BuilderMoleculeException if the molecule for the calculation can
   *           not be built
   * @throws CTKException
   * @throws MonomerLoadingException if the MonomerFactory can not be refreshed
   */
  public String getMolecularFormula(String notation) throws BuilderMoleculeException, CTKException, ValidationException, MonomerLoadingException {
    String result = MoleculeInformation.getMolecularFormular(validate(notation).getHELM2Notation());
    setMonomerFactoryToDefault(notation);
    return result;
  }

  /**
   * method to calculate froma non-ambiguous HELM input the molecular
   * properties: molecular formula, molecular weight, exact mass, extinction
   * coefficient
   *
   * @param notation
   * @return
   * @throws BuilderMoleculeException
   * @throws CTKException
   * @throws ExtinctionCoefficientException
   * @throws ValidationException
   * @throws MonomerLoadingException
   */
  public List<String> getMolecularProperties(String notation) throws BuilderMoleculeException, CTKException, ExtinctionCoefficientException, ValidationException, MonomerLoadingException {
    List<String> result = MoleculeInformation.getMoleculeProperties(validate(notation).getHELM2Notation());
    setMonomerFactoryToDefault(notation);
    return result;

  }

  /**
   * method to read a single peptide sequence and generates HELM
   *
   * @param peptide peptide sequence
   * @return HELM
   * @throws org.helm.notation2.parser.exceptionparser.NotationException if the
   *           notation object can not be built
   * @throws FastaFormatException if the peptide sequence is not in the right
   *           format
   */
  public String readPeptide(String peptide) throws org.helm.notation2.parser.exceptionparser.NotationException, FastaFormatException {
    return SequenceConverter.readPeptide(peptide).getHELM2Notation().toHELM2();
  }

  /**
   * method to read a single rna sequence and generates HELM
   *
   * @param rna rna sequence
   * @return HELM
   * @throws org.helm.notation2.parser.exceptionparser.NotationException if the
   *           notation object can not be built
   * @throws FastaFormatException if the rna-sequence is not in the right format
   *           HELM
   * @throws JDOMException
   * @throws IOException
   */
  public String readRNA(String rna) throws org.helm.notation2.parser.exceptionparser.NotationException, FastaFormatException, IOException, JDOMException {
    return SequenceConverter.readRNA(rna).getHELM2Notation().toHELM2();
  }

  /**
   * method to generate a HELM molecule
   *
   * @param notation HELM string
   * @return generated molecule image in byte[]
   * @throws ValidationException if the HELM string is not valid
   * @throws BuilderMoleculeException if the molecule can't be built
   * @throws CTKException
   * @throws IOException
   * @throws MonomerException if the MonomerFactory can not be loaded
   */
  public byte[] generateImageForHELMMolecule(String notation) throws BuilderMoleculeException, CTKException, IOException, ValidationException {
    byte[] result = Images.generateImageHELMMolecule(validate(notation).getHELM2Notation());
    setMonomerFactoryToDefault(notation);
    return result;
  }

  /**
   * method to generate an image for a monomer
   *
   * @param monomer Monomer Input
   * @return generated molecule image in byte[]
   * @throws BuilderMoleculeException if the molecule can not be built
   * @throws CTKException
   */
  public byte[] generateImageForMonomer(Monomer monomer, boolean showRgroups) throws BuilderMoleculeException, CTKException {
    return Images.generateImageofMonomer(monomer, showRgroups);
  }

  /**
   * method to generate JSON-Output for the HELM
   *
   * @param helm HELM input
   * @return HELM as JSON-objects
   * @throws ValidationException if the HELM input is not valid
   * @throws MonomerLoadingException if the MonomerFactory can not be refreshed
   */
  public String generateJSON(String helm) throws ValidationException, MonomerLoadingException {
    String result = validate(helm).toJSON();
    setMonomerFactoryToDefault(helm);
    return result;
  }

  /**
   * method to set the MonomerFactory to the default one, this is only done in
   * case of xHELM input
   *
   * @param helm input HELM
   * @throws MonomerLoadingException if the MonomerFactory can not be loaded
   */
  private void setMonomerFactoryToDefault(String helm) throws MonomerLoadingException {
    if (helm.contains("<Xhelm>")) {
      LOG.info("Refresh local Monomer Store in case of Xhelm");
      MonomerFactory.refreshMonomerCache();
    }
  }

  /**
   * method to generate the natural analogue sequence for all peptide-sequences
   * from an HELM input
   *
   * @param notation input HELM
   * @return natural analogue peptide sequences, divided by white spaces
   * @throws org.helm.notation2.parser.exceptionparser.NotationException if the
   *           input complex notation contains non-peptide polymer(s)
   * @throws HELM2HandledException if the HELM input contains HELM2 features
   * @throws ValidationException if the input HELM is not valid
   * @throws MonomerLoadingException if the MonomerFactory can not be refreshed
   * @throws PeptideUtilsException if the polymer is not a peptide
   */
  public String generateNaturalAnalogSequencePeptide(String notation) throws org.helm.notation2.parser.exceptionparser.NotationException, HELM2HandledException, ValidationException,
      MonomerLoadingException, PeptideUtilsException {
    String result = SequenceConverter.getPeptideNaturalAnalogSequenceFromNotation(validate(notation).getHELM2Notation());
    setMonomerFactoryToDefault(notation);
    return result;
  }

  /**
   * method to generate the natural analogue sequence for all rna-sequences from
   * an HELM input
   *
   * @param notation HELM input
   * @return natural analogue rna sequences, divided by white spaces
   * @throws org.helm.notation2.parser.exceptionparser.NotationException if the
   *           input complex notation contains non-nucleid acid polymer(s)
   * @throws HELM2HandledException if HELM input contains HELM2 features
   * @throws ValidationException if the HELM input is not valid
   * @throws MonomerLoadingException if the MonomerFactory can not be refreshed
   */
  public String generateNaturalAnalogSequenceRNA(String notation) throws org.helm.notation2.parser.exceptionparser.NotationException, HELM2HandledException, ValidationException,
      MonomerLoadingException {
    String result = SequenceConverter.getNucleotideNaturalAnalogSequenceFromNotation(validate(notation).getHELM2Notation());
    setMonomerFactoryToDefault(notation);
    return result;
  }

}
