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
package org.helm.notation2.tools;

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.helm.chemtoolkit.CTKException;
import org.helm.notation2.Attachment;
import org.helm.notation2.Chemistry;
import org.helm.notation2.Monomer;
import org.helm.notation2.MonomerFactory;
import org.helm.notation2.MonomerStore;
import org.helm.notation2.exception.BuilderMoleculeException;
import org.helm.notation2.exception.ChemistryException;
import org.helm.notation2.exception.MonomerException;
import org.helm.notation2.exception.NotationException;
import org.helm.notation2.exception.ParserException;
import org.helm.notation2.tools.Images;
import org.jdom2.JDOMException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ImagesTest {

  @Test
  public void TestGenerationImageOfMonomer() throws BuilderMoleculeException, CTKException, FileNotFoundException, IOException, ChemistryException {
    Monomer monomer = MonomerFactory.getInstance().getMonomerStore().getMonomer("RNA", "P");
    byte[] result = Images.generateImageofMonomer(monomer, false);
    if (!Files.exists(Paths.get("test-output"))) {
      Files.createDirectories(Paths.get("test-output"));
    }
    try (FileOutputStream out = new FileOutputStream("test-output" + File.separator + "MonomerTestPicture.png")) {
      out.write(result);
    }
  }

  @Test
  public void TestGenerationImageOfHELMNotation() throws ParserException, JDOMException, BuilderMoleculeException, CTKException, IOException, NotationException, ChemistryException {
    String notation = "RNA1{R(U)P}|RNA2{R(U)P.R(G)}|RNA3{R(C)P.R(A)}|CHEM1{[MCC]}$RNA1,CHEM1,3:R2-1:R1|RNA2,RNA3,5:pair-2:pair|RNA2,RNA3,2:pair-5:pair$$$";
    if (Chemistry.getInstance().getChemistry().equals("org.helm.chemtoolkit.chemaxon.ChemaxonManipulator")) {
      byte[] result = Images.generateImageHELMMolecule(HELM2NotationUtils.readNotation(notation));
      if (!Files.exists(Paths.get("test-output"))) {
        Files.createDirectories(Paths.get("test-output"));
      }
      try (FileOutputStream out = new FileOutputStream("test-output" + File.separator + "TestGenerationImageOfHELMNotationComplex.png")) {
        out.write(result);
      }
    }
  }

  @Test
  public void TestGenerationImageOfHELMNotationPEPTIDEComplex() throws ParserException, JDOMException, BuilderMoleculeException, CTKException, IOException, NotationException, ChemistryException {
    String notation = "PEPTIDE1{D.F.D}|PEPTIDE2{C}|PEPTIDE3{E.D}$PEPTIDE3,PEPTIDE1,2:R3-1:R3|PEPTIDE2,PEPTIDE1,1:R3-3:R3$$$";
    // String notation =
    // "PEPTIDE1{D.F.D}|PEPTIDE2{C}$PEPTIDE2,PEPTIDE1,1:R3-3:R3$$$";

    byte[] result = Images.generateImageHELMMolecule(HELM2NotationUtils.readNotation(notation));
    if (!Files.exists(Paths.get("test-output"))) {
      Files.createDirectories(Paths.get("test-output"));
    }
    try (FileOutputStream out = new FileOutputStream("test-output" + File.separator + "TestGenerationImageOfHELMNotationPEPTIDeComplex.png")) {
      out.write(result);
    }
  }

  @Test
  public void TestGenerationImageOfHELMNotationProblemCase() throws ParserException, JDOMException, BuilderMoleculeException, CTKException, IOException, NotationException, ChemistryException {
    String notation = "RNA1{R(A)P.R(G)}$$$$";
    byte[] result = Images.generateImageHELMMolecule(HELM2NotationUtils.readNotation(notation));
    if (!Files.exists(Paths.get("test-output"))) {
      Files.createDirectories(Paths.get("test-output"));
    }
    try (FileOutputStream out = new FileOutputStream("test-output" + File.separator + "TestGenerationImageOfHELMNotationSimple.png")) {
      out.write(result);
    }

  }

  @Test
  public void TestGenerationImageOfHELMNotationSimpleCase() throws ParserException, JDOMException, BuilderMoleculeException, CTKException, IOException, NotationException, ChemistryException {
    String notation = "PEPTIDE1{G.G.G}$$$$";
    byte[] result = Images.generateImageHELMMolecule(HELM2NotationUtils.readNotation(notation));
    if (!Files.exists(Paths.get("test-output"))) {
      Files.createDirectories(Paths.get("test-output"));
    }
    try (FileOutputStream out = new FileOutputStream("test-output" + File.separator + "TestGenerationImageOfHELMNotationSimpleCase.png")) {
      out.write(result);
    }
  }
  

  @Test
  public void TestGenerationImageOfHELMNotationExtendedAttachments() throws ParserException, JDOMException, BuilderMoleculeException, CTKException, IOException, NotationException, ChemistryException, MonomerException {
	  Monomer testExtended = new Monomer();
	  testExtended.setPolymerType("CHEM");
	  testExtended.setMonomerType("Undefined");
	  testExtended.setCanSMILES("[H:1]CC(C[H:2])C(C[H:3])CC[H:4]");
	  testExtended.setName("testEx");
	  testExtended.setAlternateId("testEx");
	  List<Attachment> attachments = new ArrayList<Attachment>();
	    Attachment att = new Attachment();
	    att.setAlternateId("R1-OH");
	    att.setLabel("R1");
	    att.setCapGroupName("OH");
	    att.setCapGroupSMILES("[*:1][OH]");
	    attachments.add( att);
	    
	    att = new Attachment();
	    att.setAlternateId("R2-OH");
	    att.setLabel("R2");
	    att.setCapGroupName("OH");
	    att.setCapGroupSMILES("[*:2][OH]");
	    attachments.add( att);

	    att = new Attachment();
	    att.setAlternateId("R3-OH");
	    att.setLabel("R3");
	    att.setCapGroupName("OH");
	    att.setCapGroupSMILES("[*:3][OH]");
	    attachments.add( att);
	    
	    att = new Attachment();
	    att.setAlternateId("R4-N");
	    att.setLabel("R4");
	    att.setCapGroupName("N");
	    att.setCapGroupSMILES("[*:4][N]");
	    attachments.add(att);
	    
	   
	    testExtended.setAttachmentList(attachments);
	    MonomerFactory.getInstance().getMonomerStore().addMonomer(testExtended);
	  
	  String notation = "CHEM1{[testEx]}$$$$";
    byte[] result = Images.generateImageHELMMolecule(HELM2NotationUtils.readNotation(notation));
    if (!Files.exists(Paths.get("test-output"))) {
      Files.createDirectories(Paths.get("test-output"));
    }
    try (FileOutputStream out = new FileOutputStream("test-output" + File.separator + "TestGenerationImageOfHELMNotationExtendedAttachments.png")) {
      out.write(result);
    }
  }
  
  @Test
  public void TestGenerationImageOfHELMNotationExtendedAttachmentsR() throws ParserException, JDOMException, BuilderMoleculeException, CTKException, IOException, NotationException, ChemistryException, MonomerException {
	 
	  String notation = "PEPTIDE1{[PEN]}$$$$";
    byte[] result = Images.generateImageHELMMolecule(HELM2NotationUtils.readNotation(notation));
    if (!Files.exists(Paths.get("test-output"))) {
      Files.createDirectories(Paths.get("test-output"));
    }
    try (FileOutputStream out = new FileOutputStream("test-output" + File.separator + "TestGenerationImageOfHELMNotationExtendedAttachmentsR.png")) {
      out.write(result);
    }
  }
  
  @Test
  public void TestGenerationImageOfHELMNotationExtendedAttachmentsUsingStandardAttachmentList() throws ParserException, JDOMException, BuilderMoleculeException, CTKException, IOException, NotationException, ChemistryException, MonomerException {
	  Monomer testExtended = new Monomer();
	  testExtended.setPolymerType("CHEM");
	  testExtended.setMonomerType("Undefined");
	  testExtended.setCanSMILES("[H:9]CC(C[H:10])C(C[H:11])CCC[H:12]");
	  testExtended.setName("testEx2");
	  testExtended.setAlternateId("testEx2");
	  List<Attachment> attachments = new ArrayList<Attachment>();
	    Attachment att = new Attachment();
	    att.setAlternateId("R9-H");
	    att.setLabel("R9");
	    att.setCapGroupName("H");
	    att.setCapGroupSMILES("[*:9][H]");
	    attachments.add( att);
	    
	    att = new Attachment();
	    att.setAlternateId("R10-H");
	    att.setLabel("R10");
	    att.setCapGroupName("H");
	    att.setCapGroupSMILES("[*:10][H]");
	    attachments.add( att);

	    att = new Attachment();
	    att.setAlternateId("R11-H");
	    att.setLabel("R11");
	    att.setCapGroupName("H");
	    att.setCapGroupSMILES("[*:11][H]");
	    attachments.add( att);
	    
	    att = new Attachment();
	    att.setAlternateId("R12-H");
	    att.setLabel("R12");
	    att.setCapGroupName("H");
	    att.setCapGroupSMILES("[*:12][H]");
	    attachments.add(att);
	    
	   
	    testExtended.setAttachmentList(attachments);
	    MonomerFactory.getInstance().getMonomerStore().addMonomer(testExtended);
	  
	  String notation = "CHEM1{[testEx2]}|CHEM2{[sDBL]}$CHEM1,CHEM2,1:R9-1:R3$$$V2.0";
    byte[] result = Images.generateImageHELMMolecule(HELM2NotationUtils.readNotation(notation));
    System.out.println(result.toString());
    if (!Files.exists(Paths.get("test-output"))) {
      Files.createDirectories(Paths.get("test-output"));
    }
    try (FileOutputStream out = new FileOutputStream("test-output" + File.separator + "TestGenerationImageOfHELMNotationExtendedAttachments.png")) {
      out.write(result);
    }
    
    Assert.assertEquals(SMILES.getSMILESForAll(HELM2NotationUtils.readNotation(notation)),"C(C(C[H])C(C[H])CCC[H])OCCCCC(NCC(CNC(CCCCO[H])=O)O[H])=O");
    
    
  }

}
