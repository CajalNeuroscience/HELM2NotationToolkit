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
package org.helm.notation2.wsadapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.helm.notation2.Chemistry;

/**
 *
 * Singleton {@code MonomerStoreConfiguration} stores the webservice
 * configuration, e.g. URI to REST API.
 *
 * @author <a href="mailto:lanig@quattro-research.com">Marco Lanig</a>
 * @version $Id$
 */
public class MonomerStoreConfiguration {
  /** Path to local config file containing monomer store configuration. */
  private static final String CONFIG_FILE_PATH = System.getProperty("user.home") + System.getProperty("file.separator") + ".helm" + System.getProperty("file.separator")
      + "MonomerStoreConfig.properties";

  private static final String USE_WEBSERVICE = "use.webservice";

  private static final String UPDATE_AUTOMATIC = "update.automatic";

  private static final String WEBSERVICE_MONOMERS_URL = "webservice.monomers.url";

  private static final String WEBSERVICE_MONOMERS_PATH = "webservice.monomers.path";

  private static final String WEBSERVICE_MONOMERS_PUT_PATH = "webservice.monomers.put.path";

  private static final String WEBSERVICE_EDITOR_CATEGORIZATION_URL = "webservice.editor.categorization.url";

  private static final String WEBSERVICE_EDITOR_CATEGORIZATION_PATH = "webservice.editor.categorization.path";

  private static final String WEBSERVICE_NUCLEOTIDES_URL = "webservice.nucleotides.url";

  private static final String WEBSERVICE_NUCLEOTIDES_PATH = "webservice.nucleotides.path";

  private static final String WEBSERVICE_NUCLEOTIDES_PUT_PATH = "webservice.nucleotides.put.path";
 
  private static final String USE_EXTERNAL_MONOMERS = "use.external.monomers";

  private static final String EXTERNAL_MONOMERS_PATH = "external.monomers.path";

  private static final String USE_EXTERNAL_NUCLEOTIDES = "use.external.nucleotides";

  private static final String EXTERNAL_NUCLEOTIDES_PATH = "external.nucleotides.path";
  
  private static final String USE_EXTERNAL_ATTACHMENTS = "use.external.attachments";
  
  private static final String EXTERNAL_ATTACHMENTS_PATH = "external.attachments.path";

  private static MonomerStoreConfiguration _instance;

  private boolean isUseWebservice;

  private boolean isUpdateAutomatic;

  private boolean isUseExternalMonomers;

  private boolean isUseExternalNucleotides;
  
  private boolean isUseExternalAttachments;

  private String externalMonomersPath;

  private String externalNucleotidesPath;
  
  private String externalAttachmentsPath;

  private String webserviceMonomersURL;

  private String webserviceMonomersPath;

  private String webserviceMonomersPutPath;

  private String webserviceNucleotidesURL;

  private String webserviceNucleotidesPath;

  private String webserviceNucleotidesPutPath;
  
  private String webserviceEditorCategorizationURL;

  private String webserviceEditorCategorizationPath;

  /**
   * Private singleton constructor that initializes
   * {@code MonomerStoreConfiguration} using local config file.
   */
  private MonomerStoreConfiguration() {
    refresh();
  }

  /**
   * Resets the configuration to default values.
   */
  private void resetConfigToDefault() {
    isUseWebservice = true;
    isUpdateAutomatic = true;
    isUseExternalMonomers = false;
    isUseExternalNucleotides = true;
    setUseExternalAttachments(false);
    webserviceMonomersURL = "http://localhost:8080/HELM2MonomerService/rest";
    webserviceMonomersPath = "monomer/";
    webserviceMonomersPutPath = "monomer/";
    webserviceEditorCategorizationURL = "http://localhost:8080";
    webserviceEditorCategorizationPath = "";
    externalNucleotidesPath = "";
    externalMonomersPath = "";
    setExternalAttachmentsPath("");
  }

  /**
   * Creates singleton instance of {@code MonomerStoreConfiguration} and returns
   * it.
   *
   * @return instance
   */
  public static MonomerStoreConfiguration getInstance() {
    if (_instance == null) {
      _instance = new MonomerStoreConfiguration();
    }
    return _instance;
  }

  /**
   * Returns whether webservice should be used or not.
   *
   * @return true/false
   */
  public boolean isUseWebservice() {
    return isUseWebservice;
  }

  /**
   * Returns whether all webservices should be fetched at all times, or be
   * refreshed manually.
   *
   * @return true=automatic refresh, false=manual refresh
   */
  public boolean isUpdateAutomatic() {
    return isUpdateAutomatic;
  }
  

  /**
   * Sets whether all webservices should be fetched at all times, or be
   * refreshed manually.
   *
   * @param isUpdateAutomatic true=automatic refresh, false=manual refresh
   */
  public void setUpdateAutomatic(boolean isUpdateAutomatic) {
    this.isUpdateAutomatic = isUpdateAutomatic;
  }

  /**
   * Returns URL without path to monomer resource.
   *
   * @return URL as String
   */
  public String getWebserviceMonomersURL() {
    return webserviceMonomersURL;
  }

  /**
   * Returns path to monomer resource.
   *
   * @return path as String
   */
  public String getWebserviceMonomersPath() {
    return webserviceMonomersPath;
  }

  /**
   * Returns URL without path to nucleotide resource.
   *
   * @return URL as string
   */
  public String getWebserviceNucleotidesURL() {
    return webserviceNucleotidesURL;
  }

  /**
   * Returns path to nucleotide resource.
   *
   * @return path as String
   */
  public String getWebserviceNucleotidesPath() {
    return webserviceNucleotidesPath;
  }

  /**
   * Returns URL and Path to monomer resource combined.
   *
   * @return full URL as String
   */
  public String getWebserviceMonomersFullURL() {
    return webserviceMonomersURL + "/" + webserviceMonomersPath;
  }

  /**
   * Returns URL and Path to nucleotide resource combined.
   *
   * @return full URL as String
   */
  public String getWebserviceNucleotidesFullURL() {
    return webserviceNucleotidesURL + "/" + webserviceNucleotidesPath;
  }

  /**
   * Returns URL and Path to nucleotide PUT resource combined.
   *
   * @return full URL as String
   */
  public String getWebserviceNucleotidesPutFullURL() {
    return webserviceNucleotidesURL + "/" + webserviceNucleotidesPutPath;
  }

  /**
   * Returns URL and Path to monomer PUT resource combined.
   *
   * @return full URL as String
   */
  public String getWebserviceMonomersPutFullURL() {
    return webserviceMonomersURL + "/" + webserviceMonomersPutPath;
  }

  /**
   * Returns URL without path to monomer categorization resource. It contains
   * the monomer categorization used in the HELMEditor.
   *
   * @return URL as string
   */
  public String getWebserviceEditorCategorizationURL() {
    return webserviceEditorCategorizationURL;
  }

  /**
   * Returns path to monomer categorization resource.
   *
   * @return path as String
   */
  public String getWebserviceEditorCategorizationPath() {
    return webserviceEditorCategorizationPath;
  }

  /**
   * Returns URL and Path to monomer categorization resource combined.
   *
   * @return full URL as String
   */
  public String getWebserviceEditorCategorizationFullURL() {
    return webserviceEditorCategorizationURL + "/"
        + webserviceEditorCategorizationPath;
  }

  public boolean isUseExternalMonomers() {
    return isUseExternalMonomers;
  }

  public boolean isUseExternalNucleotides() {
    return isUseExternalNucleotides;
  }

  public String getExternalNucleotidesPath() {
    return externalNucleotidesPath;
  }

  public String getExternalMonomersPath() {
    return externalMonomersPath;
  }
  


  /**
   * Refreshes the configuration using the local properties file.
   */
  public void refresh() {
    String configFilePath = System.getenv("HELM_CONFIG_FILE_PATH");
    if (configFilePath == null || configFilePath.isEmpty()) {
        configFilePath = CONFIG_FILE_PATH;
    }
    File configFile = new File(configFilePath);

    if (!configFile.exists()) {
      BufferedWriter writer = null;
      BufferedReader reader = null;
      try {
        configFile.createNewFile();
        InputStream in = Chemistry.class.getResourceAsStream("/org/helm/notation2/resources/MonomerStoreConfig.properties");
        reader = new BufferedReader(new InputStreamReader(in));

        writer = new BufferedWriter(new FileWriter(configFile));
        String line;
        while ((line = reader.readLine()) != null) {
          writer.write(line + System.getProperty("line.separator"));
        }

      } catch (Exception e) {
        resetConfigToDefault();
        e.printStackTrace();

      } finally {
        try {
          if (writer != null) {
            writer.close();
          }
          if (reader != null) {
            reader.close();
          }
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    try

    {
      PropertiesConfiguration conf = new PropertiesConfiguration(CONFIG_FILE_PATH);
      isUseWebservice = conf.getBoolean(USE_WEBSERVICE);
      isUpdateAutomatic = conf.getBoolean(UPDATE_AUTOMATIC);
      webserviceMonomersURL = conf.getString(WEBSERVICE_MONOMERS_URL);
      webserviceMonomersPath = conf.getString(WEBSERVICE_MONOMERS_PATH);
      webserviceMonomersPutPath = conf.getString(WEBSERVICE_MONOMERS_PUT_PATH);
      webserviceNucleotidesURL = conf.getString(WEBSERVICE_NUCLEOTIDES_URL);
      webserviceNucleotidesPath = conf.getString(WEBSERVICE_NUCLEOTIDES_PATH);
      webserviceNucleotidesPutPath = conf.getString(WEBSERVICE_NUCLEOTIDES_PUT_PATH);
      webserviceEditorCategorizationURL = conf.getString(WEBSERVICE_EDITOR_CATEGORIZATION_URL);
      webserviceEditorCategorizationPath = conf.getString(WEBSERVICE_EDITOR_CATEGORIZATION_PATH);
 
      /* load from external xml file */
      isUseExternalMonomers = conf.getBoolean(USE_EXTERNAL_MONOMERS);
      externalMonomersPath = conf.getString(EXTERNAL_MONOMERS_PATH);
      isUseExternalNucleotides = conf.getBoolean(USE_EXTERNAL_NUCLEOTIDES);
      externalNucleotidesPath = conf.getString(EXTERNAL_NUCLEOTIDES_PATH);
      isUseExternalAttachments = conf.getBoolean(USE_EXTERNAL_ATTACHMENTS);
      externalAttachmentsPath = conf.getString(EXTERNAL_ATTACHMENTS_PATH);

    } catch (ConfigurationException | NoSuchElementException e) {
      resetConfigToDefault();
      e.printStackTrace();
    }

  }

  /**
   *
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    String lineSep = System.getProperty("line.separator");
    String res =
        String.format("Webservice configuration:%sUse Webservice: %s%sUpdate Automatic: %s%sGetMonomers: %s%sPutMonomers: %s%sGetNucleotides: %s%sPutNucleotides: %s%sCategorization config: %s%sUse external monomer xml file: %s%sMonomer xml file: %s%sUse external nucleotide xml file: %s%sNucleotide xml file:%s", lineSep, isUseWebservice(), lineSep, isUpdateAutomatic(), lineSep, getWebserviceMonomersFullURL(), lineSep, getWebserviceMonomersPutFullURL(), lineSep, getWebserviceNucleotidesFullURL(), lineSep, getWebserviceNucleotidesPutFullURL(), lineSep, getWebserviceEditorCategorizationFullURL(), lineSep, isUseExternalMonomers(), lineSep, getExternalMonomersPath(), lineSep, isUseExternalNucleotides(), lineSep, getExternalNucleotidesPath());

    return res;
  }

public String getExternalAttachmentsPath() {
	return externalAttachmentsPath;
}

public void setExternalAttachmentsPath(String externalAttachmentsPath) {
	this.externalAttachmentsPath = externalAttachmentsPath;
}

public boolean isUseExternalAttachments() {
	return isUseExternalAttachments;
}

public void setUseExternalAttachments(boolean isUseExternalAttachments) {
	this.isUseExternalAttachments = isUseExternalAttachments;
}
}
