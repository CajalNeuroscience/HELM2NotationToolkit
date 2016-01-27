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

import java.io.IOException;

import org.helm.notation2.exception.ParserException;
import org.helm.notation2.parser.ConverterHELM1ToHELM2;
import org.helm.notation2.parser.ParserHELM2;
import org.helm.notation2.parser.exceptionparser.ExceptionState;
import org.helm.notation2.parser.exceptionparser.NotationException;
import org.helm.notation2.parser.notation.polymer.MonomerNotation;
import org.helm.notation2.parser.notation.polymer.MonomerNotationUnit;
import org.helm.notation2.parser.notation.polymer.PolymerNotation;
import org.jdom2.JDOMException;

import org.testng.annotations.Test;

/**
 * ChangeObjectsTest
 *
 * @author hecht
 * @version $Id$
 */
public class ChangeObjectsTest {

  private ContainerHELM2 readNotation(String notation) throws ParserException, JDOMException {
    /* HELM1-Format -> */
    if (!(notation.contains("V2.0"))) {
      notation = new ConverterHELM1ToHELM2().doConvert(notation);
    }
    /* parses the HELM notation and generates the necessary notation objects */
    ParserHELM2 parser = new ParserHELM2();
    try {
      parser.parse(notation);
    } catch (ExceptionState | IOException e) {
      throw new ParserException(e.getMessage());
    }
    ContainerHELM2 containerhelm2 = new ContainerHELM2(parser.getHELM2Notation(), new InterConnections());
    return containerhelm2;
  }

  @Test
  public void testSimpleMethods() throws ParserException, JDOMException, IOException, NotationException {
    String notation = "PEPTIDE1{A.G}|PEPTIDE2{L.G}$$$$V2.0";
    ContainerHELM2 containerhelm2 = readNotation(notation);

    ChangeObjects.deleteMonomerNotation(1, containerhelm2.getAllPolymers().get(0));
    // ChangeObjects.changePolymerNotation(0,
    // ChangeObjects.deleteMonomerNotation(containerhelm2.getAllPolymers().get(0),
    // 1), containerhelm2);

    MonomerNotation monomerNotation = new MonomerNotationUnit("D", "PEPTIDE");
    monomerNotation.setCount("3");
    ChangeObjects.addMonomerNotation(0, containerhelm2.getAllPolymers().get(0), monomerNotation);
    ChangeObjects.changeMonomerNotation(1, containerhelm2.getHELM2Notation().getListOfPolymers().get(0), monomerNotation);
    ChangeObjects.deleteMonomerNotation(1, containerhelm2.getHELM2Notation().getListOfPolymers().get(0));
    try {
      ChangeObjects.deleteMonomerNotation(0, containerhelm2.getHELM2Notation().getListOfPolymers().get(0));
    } catch (NotationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    PolymerNotation polymer = containerhelm2.getHELM2Notation().getListOfPolymers().get(1);
    ChangeObjects.deletePolymerNotation(1, containerhelm2);

    System.out.println(containerhelm2.getHELM2Notation().toHELM2());

    ChangeObjects.deletePolymerNotation(0, containerhelm2);
    ChangeObjects.addPolymerNotation(0, polymer, containerhelm2);
    ChangeObjects.addPolymerNotation(0, polymer, containerhelm2);

    System.out.println(containerhelm2.getHELM2Notation().toHELM2());
    // containerhelm2 = ChangeObjects.deletePolymerNotation(1, containerhelm2);

    // ChangeObjects.changePolymerNotation(0,
    // ChangeObjects.deleteMonomerNotation(containerhelm2.getAllPolymers().get(0),
    // 0), containerhelm2);
  }

}
