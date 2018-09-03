/*******************************************************************************
 * Copyright C 2012, The Pistoia Alliance
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
 ******************************************************************************/
package org.helm.notation2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.helm.chemtoolkit.CTKException;
import org.helm.notation2.exception.AttachmentException;
import org.helm.notation2.exception.ChemistryException;
import org.helm.notation2.exception.EncoderException;
import org.helm.notation2.exception.MonomerException;
import org.helm.notation2.exception.MonomerLoadingException;
import org.helm.notation2.tools.MonomerParser;
import org.helm.notation2.wsadapter.MonomerStoreConfiguration;
import org.helm.notation2.wsadapter.MonomerWSLoader;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.LoggerFactory;

/**
 * This is a factory class to build monomer database from
 * MonomerDBGZEnconded.xml document
 *
 * @author zhangtianhong
 */
public class MonomerFactory {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MonomerFactory.class);

	public static final String NOTATION_DIRECTORY = NotationConstant.NOTATION_DIRECTORY;

	public static final String MONOMER_CACHE_FILE_NAME = "MonomerCache.ser";

	public static final String MONOMER_CACHE_FILE_PATH = NOTATION_DIRECTORY + System.getProperty("file.separator")
			+ MONOMER_CACHE_FILE_NAME;

	public static final String MONOMER_DB_FILE_NAME = "MonomerDBGZEncoded.xml";

	public static final String MONOMER_DB_FILE_PATH = NOTATION_DIRECTORY + System.getProperty("file.separator")
			+ MONOMER_DB_FILE_NAME;

	public static final String MONOMER_DB_XML_RESOURCE = "resources/MonomerDBGZEncoded.xml";

	public static final String MONOMER_DB_SCHEMA_RESOURCE = "resources/MonomerDBSchema.xsd";

	public static final String ATTACHMENTS_RESOURCE = "resources/Attachments.json";

	public static final String XML_SCHEMA_VALIDATION_FEATURE = "http://apache.org/xml/features/validation/schema";

	public static final String EXTERNAL_SCHEMA_LOCATION_KEY = "http://apache.org/xml/properties/schema/external-schemaLocation";

	public static final String DEFAULT_NAME_SPACE = "lmr";

	public static final String POLYMER_LIST_ELEMENT = "PolymerList";

	public static final String POLYMER_ELEMENT = "Polymer";

	public static final String POLYMER_TYPE_ATTRIBUTE = "polymerType";

	public static final String ATTACHMENT_LIST_ELEMENT = "AttachmentList";

	private static MonomerFactory instance;

	/**
	 * First key is polymer Type, such as "RNA" Second key is monomer ID, such
	 * as "A"
	 */
	private static Map<String, Map<String, Monomer>> monomerDB;
	// key is
	// monomer
	// SMILES, value
	// is Monomer

	private static Map<String, Monomer> smilesMonomerDB; // key is
	// AttachementID,
	// value is
	// Attachment

	private static Map<String, Attachment> attachmentDB;

	// private static Map<String, Map<String, Monomer>> externalMonomerDB;
	private static SAXBuilder builder;

	private static Logger logger = Logger.getLogger(MonomerFactory.class.toString());

	private static boolean dbChanged = true;

	/**
	 * retruns the monomer database
	 *
	 * @return Map as {@code Map<String, Map<String, Monomer>>}
	 */
	public synchronized Map<String, Map<String, Monomer>> getMonomerDB() {
		return getMonomerDB(true);
	}

	/**
	 * returns the monomer database including monomers that where temporary
	 * marked as new, else without those monomers
	 *
	 * @param includeNewMonomers
	 *            if true, then the new monomers will be added to the monomer db
	 * @return Map as {@code Map<String, Map<String, Monomer>>}
	 */
	public synchronized Map<String, Map<String, Monomer>> getMonomerDB(boolean includeNewMonomers) {
		if (includeNewMonomers) {
			return monomerDB;
		} else {
			Map<String, Map<String, Monomer>> reducedMonomerDB = new TreeMap<String, Map<String, Monomer>>(
					String.CASE_INSENSITIVE_ORDER);
			for (String polymerType : monomerDB.keySet()) {
				Map<String, Monomer> monomerMap = monomerDB.get(polymerType);
				reducedMonomerDB.put(polymerType, excludeNewMonomers(monomerMap));
			}
			return reducedMonomerDB;
		}
	}

	protected MonomerStore monomerStore;

	/**
	 * create a MonomerStore instance based on MonomerFactory's monomerDB and
	 * smilesMonomerDB
	 *
	 * @return MonomerStore
	 */
	public synchronized MonomerStore getMonomerStore() {
		if (monomerStore == null) {
			monomerStore = new MonomerStore(monomerDB, smilesMonomerDB);
		}
		return monomerStore;
	}

	public synchronized Map<String, Attachment> getAttachmentDB() {
		return attachmentDB;
	}

	public synchronized Map<String, Monomer> getSmilesMonomerDB() {
		return getSmilesMonomerDB(true);
	}

	public synchronized Map<String, Monomer> getSmilesMonomerDB(boolean includeNewMonomers) {
		if (includeNewMonomers) {
			return smilesMonomerDB;
		} else {
			return excludeNewMonomersSmiles(smilesMonomerDB);
		}
	}

	private synchronized Map<String, Monomer> excludeNewMonomersSmiles(Map<String, Monomer> monomerMap) {
		Map<String, Monomer> reducedMonomerMap = new HashMap<String, Monomer>();
		for (String identifier : monomerMap.keySet()) {
			Monomer monomer = monomerMap.get(identifier);
			if (!monomer.isNewMonomer()) {
				reducedMonomerMap.put(identifier, monomer);
			}
		}
		return reducedMonomerMap;
	}

	private synchronized Map<String, Monomer> excludeNewMonomers(Map<String, Monomer> monomerMap) {
		Map<String, Monomer> reducedMonomerMap = new TreeMap<String, Monomer>(String.CASE_INSENSITIVE_ORDER);
		for (String identifier : monomerMap.keySet()) {
			Monomer monomer = monomerMap.get(identifier);
			if (!monomer.isNewMonomer()) {
				reducedMonomerMap.put(identifier, monomer);
			}
		}
		return reducedMonomerMap;
	}

	public synchronized List<String> getPolymerTypes() {
		List<String> l = new ArrayList<String>();
		l.addAll(monomerDB.keySet());
		Collections.sort(l);
		return l;
	}

	public synchronized List<String> getMonomerTypes() {
		List<String> monomerTypeList = new ArrayList<String>();
		Object[] col = monomerDB.values().toArray();
		for (int i = 0; i < col.length; i++) {
			Map<String, Monomer> map = (Map<String, Monomer>) col[i];
			Monomer[] monomers = map.values().toArray(new Monomer[0]);
			for (int j = 0; j < monomers.length; j++) {
				if (!monomerTypeList.contains(monomers[j].getMonomerType())) {
					monomerTypeList.add(monomers[j].getMonomerType());
				}
			}
		}
		Collections.sort(monomerTypeList);
		return monomerTypeList;
	}

	public synchronized Map<String, List<String>> getAttachmentLabelIDs() {
		Map<String, List<String>> labelMap = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);

		// group attachments based on R value (label)
		Set<String> idSet = attachmentDB.keySet();
		for (String id : idSet) {
			String label = attachmentDB.get(id).getLabel();
			List<String> ids = labelMap.get(label);
			if (null == ids || ids.isEmpty()) {
				ids = new ArrayList<String>();
				ids.add(id);
				labelMap.put(label, ids);
			} else {
				ids.add(id);
			}
		}

		// Sort ids in each R group
		Set<String> labelSet = labelMap.keySet();
		for (String label : labelSet) {
			List<String> ids = labelMap.get(label);
			Collections.sort(ids);
		}

		return labelMap;
	}

	public static void setupBuilder() {
		URL schema = MonomerFactory.class.getResource(MONOMER_DB_SCHEMA_RESOURCE);
		builder = new SAXBuilder(false); // checks both well-formedness and
		// validity
		builder.setFeature(XML_SCHEMA_VALIDATION_FEATURE, true);
		builder.setProperty(EXTERNAL_SCHEMA_LOCATION_KEY, DEFAULT_NAME_SPACE + " " + schema.toString());
	}

	private MonomerFactory() {
	}

	/**
	 * Initialize MonomerCache and returns the singleton Factory class
	 *
	 * @return MonomerFactory current monomerfactory
	 * @throws MonomerLoadingException
	 *             if the monomer could not be loaded from the source
	 * @throws ChemistryException
	 *             if the chemistry could not be initialized
	 */
	public static MonomerFactory getInstance() throws MonomerLoadingException, ChemistryException {
		if (null == instance) {
			refreshMonomerCache();
		}

		else if (MonomerStoreConfiguration.getInstance().isUseWebservice()
				&& MonomerStoreConfiguration.getInstance().isUpdateAutomatic()) {
			refreshMonomerCache();
		}
		return instance;
	}

	public static void refreshMonomerCache() throws MonomerLoadingException, ChemistryException {
		initializeMonomerCache();
		instance = new MonomerFactory();
	}

	public static void setDBChanged(boolean isChanged) {
		dbChanged = isChanged;
	}

	/**
	 * Returns whether one of the stored databases has changed, for example by
	 * adding or removing monomers.
	 *
	 * @return true when database has changed else false
	 */
	public static boolean hasDBChanged() {
		return dbChanged;
	}

	public static void resetDBChanged() {
		dbChanged = false;
	}

	private static void serializeMonomerCache(MonomerCache monomerCache, String fileName) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(monomerCache);
		oos.close();
		fos.close();
	}

	private static MonomerCache deserializeMonomerCache(String fileName) throws IOException, MonomerException {
		FileInputStream fis = new FileInputStream(fileName);
		ObjectInputStream ois = new ObjectInputStream(fis);
		MonomerCache cache = null;
		try {
			cache = (MonomerCache) ois.readObject();
			// ensure monomers have their canonical SMILES set to what we store
			for (Map.Entry<String, Monomer> e : cache.getSmilesMonomerDB().entrySet()) {
				e.getValue().setCanSMILES(e.getKey());
			}

			cache.setAttachmentDB(buildAttachmentDB());
		} catch (ClassNotFoundException cnfe) {
			throw new MonomerException("Unable to deserialize monomer cache from file");
		} finally {
			ois.close();
		}
		fis.close();
		return cache;
	}

	/**
	 * To add new monomer into monomerCache
	 *
	 * @param monomer
	 *            given Monomer
	 * @throws IOException
	 *             if monomer store can not be read
	 * @throws MonomerException
	 *             if monomer is not valid
	 */
	public synchronized void addNewMonomer(Monomer monomer) throws IOException, MonomerException {
		monomer.setNewMonomer(true);
		addMonomer(monomerDB, smilesMonomerDB, monomer);

		dbChanged = true;
	}

	private void addMonomer(Map<String, Map<String, Monomer>> monomerDB, Map<String, Monomer> smilesMonomerDB,
			Monomer monomer) throws IOException, MonomerException {
		Map<String, Monomer> monomerMap = monomerDB.get(monomer.getPolymerType());
		if (null == monomerMap) {
			Map<String, Monomer> map = new TreeMap<String, Monomer>(String.CASE_INSENSITIVE_ORDER);
			Monomer copyMonomer = DeepCopy.copy(monomer);
			map.put(monomer.getAlternateId(), copyMonomer);
			monomerDB.put(monomer.getPolymerType(), map);
		} else {
			if (!monomerMap.containsKey(monomer.getAlternateId())) {
				monomerMap.put(monomer.getAlternateId(), monomer);
			}
		}

		if (monomer.getCanSMILES() != null && monomer.getCanSMILES().length() > 0) {
			if (!smilesMonomerDB.containsKey(monomer.getCanSMILES())) {
				smilesMonomerDB.put(monomer.getCanSMILES(), monomer);
			}
		}

		dbChanged = true;
	}

	/**
	 * Build an MonomerCache object with monomerDBXML String
	 *
	 * @param monomerDBXML
	 *            monomer db in xml format as string
	 * @return MonomerCache
	 * @throws org.helm.notation2.exception.MonomerException
	 *             if monomer is not valid
	 * @throws java.io.IOException
	 *             if string can not be read
	 * @throws org.jdom2.JDOMException
	 *             if xml file is not valid
	 * @throws ChemistryException
	 *             if chemistry could not be initialized
	 * @throws CTKException
	 *             general ChemToolKit exception passed to HELMToolKit
	 */
	public MonomerCache buildMonomerCacheFromXML(String monomerDBXML)
			throws MonomerException, IOException, JDOMException, ChemistryException, CTKException {
		ByteArrayInputStream bais = new ByteArrayInputStream(monomerDBXML.getBytes());
		return buildMonomerCacheFromXML(bais);
	}

	/**
	 * merge remote monomerCache with local monomerCache, will throw exception
	 * if conflicts found. Client needs to resolve conflicts prior to calling
	 * merge
	 *
	 * @param remoteMonomerCache
	 *            remote monomer cache
	 * @throws java.io.IOException
	 *             if monomer store can not be read
	 * @throws org.helm.notation2.exception.MonomerException
	 *             if monomer is not valid
	 */
	public synchronized void merge(MonomerCache remoteMonomerCache) throws IOException, MonomerException {
		Map<Monomer, Monomer> conflicts = getConflictedMonomerMap(remoteMonomerCache);
		if (conflicts.size() > 0) {
			throw new MonomerException("Local new monomer and remote monomer database conflict found");
		} else {
			Map<String, Map<String, Monomer>> monoDB = remoteMonomerCache.getMonomerDB();

			Set<String> polymerTypeSet = monoDB.keySet();
			for (Iterator i = polymerTypeSet.iterator(); i.hasNext();) {
				String polymerType = (String) i.next();
				Map<String, Monomer> map = monoDB.get(polymerType);
				Set<String> monomerSet = map.keySet();
				for (Iterator it = monomerSet.iterator(); it.hasNext();) {
					String id = (String) it.next();
					Monomer m = map.get(id);
					addMonomer(monomerDB, smilesMonomerDB, m);
				}
			}
		}

		dbChanged = true;
	}

	/**
	 * replace local cache with remote one completely, may cause loss of data
	 *
	 * @param remoteMonomerCache
	 *            remote monomer cache
	 * @throws java.io.IOException
	 *             if monomer store can not be read
	 * @throws org.helm.notation2.exception.MonomerException
	 *             if monomer is not valid
	 */
	public synchronized void setMonomerCache(MonomerCache remoteMonomerCache) throws IOException, MonomerException {
		monomerDB = remoteMonomerCache.getMonomerDB();
		attachmentDB = remoteMonomerCache.getAttachmentDB();
		smilesMonomerDB = remoteMonomerCache.getSmilesMonomerDB();

		dbChanged = true;
	}

	/**
	 *
	 * @param remoteMonomerCache
	 *            remoteMonomerCache
	 * @return localMonomer and remoteMonomer mismatch, key is local, value is
	 *         remote
	 * @throws java.io.IOException
	 *             if monomer store can not be read
	 * @throws org.helm.notation2.exception.MonomerException
	 *             if monomer is not valid
	 */
	public synchronized Map<Monomer, Monomer> getConflictedMonomerMap(MonomerCache remoteMonomerCache)
			throws IOException, MonomerException {
		Map<String, Map<String, Monomer>> remoteMonomerDB = remoteMonomerCache.getMonomerDB();
		Map<String, Monomer> remoteSmilesDB = remoteMonomerCache.getSmilesMonomerDB();

		Map<Monomer, Monomer> map = new HashMap<Monomer, Monomer>();
		List<Monomer> newMonomers = getNewMonomers(monomerDB);
		if (newMonomers.size() > 0) {

			for (int i = 0; i < newMonomers.size(); i++) {
				Monomer local = newMonomers.get(i);
				if (remoteMonomerDB.containsKey(local.getPolymerType())) {
					Map<String, Monomer> monomers = remoteMonomerDB.get(local.getPolymerType());

					if (monomers.containsKey(local.getAlternateId())) {
						Monomer remote = monomers.get(local.getAlternateId());
						if (local.getCanSMILES().equals(remote.getCanSMILES())) {
							logger.log(Level.INFO, "Perfect Match");
						} else {
							map.put(local, remote);
						}
					} else {
						if (remoteSmilesDB.containsKey(local.getCanSMILES())) {
							Monomer remote = remoteSmilesDB.get(local.getCanSMILES());
							map.put(local, remote);
						} else {
							logger.log(Level.INFO, "Really New");
						}
					}
				} else {
					logger.log(Level.INFO, "New Polymer Type");
				}
			}
		}
		return map;
	}

	private List<Monomer> getNewMonomers(Map<String, Map<String, Monomer>> monomerDB) {
		List<Monomer> l = new ArrayList<Monomer>();
		Set<String> typeSet = monomerDB.keySet();
		for (Iterator<String> i = typeSet.iterator(); i.hasNext();) {
			String polymerType = i.next();
			Map<String, Monomer> map = monomerDB.get(polymerType);
			Object[] monomers = map.values().toArray();
			for (int j = 0; j < monomers.length; j++) {
				Monomer m = (Monomer) monomers[j];
				if (m.isNewMonomer()) {
					l.add(m);
				}
			}
		}
		return l;
	}

	private static MonomerCache buildMonomerCacheFromWebService() throws MonomerException, IOException, JDOMException {

		Map<String, Attachment> newAttachmentDB = buildAttachmentDB();
		Map<String, Map<String, Monomer>> newMonomerDB;
		try {
			newMonomerDB = fetchMonomerDBFromWebService(newAttachmentDB);
		} catch (URISyntaxException | EncoderException e) {
			e.printStackTrace();
			throw new IOException("URISyntaxException prevents fetching monomers from webservice.");
		}
		// Map<String, Map<String, Monomer>> newMonomerDB =
		// buildMonomerDB(polymerList);
		Map<String, Monomer> newSmilesMonomerDB = buildSmilesMonomerDB(newMonomerDB);

		MonomerCache cache = new MonomerCache();
		cache.setMonomerDB(newMonomerDB);
		cache.setAttachmentDB(newAttachmentDB);
		cache.setSmilesMonomerDB(newSmilesMonomerDB);

		return cache;
	}

	private static Map<String, Map<String, Monomer>> fetchMonomerDBFromWebService(Map<String, Attachment> attachments)
			throws IOException, URISyntaxException, EncoderException {
		Map<String, Map<String, Monomer>> monomerDB = new TreeMap<String, Map<String, Monomer>>(
				String.CASE_INSENSITIVE_ORDER);

		monomerDB.put("PEPTIDE", new MonomerWSLoader("PEPTIDE").loadMonomerStore(attachments));
		monomerDB.put("RNA", new MonomerWSLoader("RNA").loadMonomerStore(attachments));
		monomerDB.put("CHEM", new MonomerWSLoader("CHEM").loadMonomerStore(attachments));

		return monomerDB;
	}

	private static MonomerCache buildMonomerCacheFromWS() throws MonomerException, IOException, JDOMException {
		return buildMonomerCacheFromWebService();
	}

	private static MonomerCache buildMonomerCacheFromXML(InputStream monomerDBInputStream)
			throws MonomerException, IOException, JDOMException, ChemistryException, CTKException {

		if (null == builder) {
			setupBuilder();
		}
		Document doc = builder.build(monomerDBInputStream);
		Element root = doc.getRootElement();

		Element polymerList = root.getChild(POLYMER_LIST_ELEMENT, root.getNamespace());
		// Element attachmentList = root.getChild(ATTACHMENT_LIST_ELEMENT,
		// root.getNamespace());

		Map<String, Attachment> newAttachmentDB = buildAttachmentDB();
		Map<String, Map<String, Monomer>> newMonomerDB = buildMonomerDB(polymerList);
		Map<String, Monomer> newSmilesMonomerDB = buildSmilesMonomerDB(newMonomerDB);

		MonomerCache cache = new MonomerCache();
		cache.setMonomerDB(newMonomerDB);
		cache.setAttachmentDB(newAttachmentDB);
		cache.setSmilesMonomerDB(newSmilesMonomerDB);

		return cache;
	}

	/**
	 * builds attachment db from default file or external file
	 * @return attachment db
	 * @throws IOException if the given attachments can not be read or the file doesn't exist or the attachment is not valid
	 */
	private static Map<String, Attachment> buildAttachmentDB() throws  IOException {
		Map<String, Attachment> map = new TreeMap<String, Attachment>(String.CASE_INSENSITIVE_ORDER);

		if (MonomerStoreConfiguration.getInstance().isUseExternalAttachments()) {

			map = AttachmentLoader.loadAttachments(
					new FileInputStream(MonomerStoreConfiguration.getInstance().getExternalAttachmentsPath()));
		} else {

			InputStream in = MonomerFactory.class.getResourceAsStream(ATTACHMENTS_RESOURCE);
			map = AttachmentLoader.loadAttachments(in);
		}

		return map;

	}

	private static String buildMonomerDbXMLFromCache(MonomerCache cache) throws MonomerException {
		XMLOutputter outputer = new XMLOutputter(Format.getPrettyFormat());

		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator")
				+ "<MonomerDB xmlns=\"lmr\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
				+ System.getProperty("line.separator"));

		Map<String, Map<String, Monomer>> mDB = cache.getMonomerDB();
		Element polymerListElement = new Element(POLYMER_LIST_ELEMENT);
		Set<String> polymerTypeSet = mDB.keySet();
		for (Iterator i = polymerTypeSet.iterator(); i.hasNext();) {
			String polymerType = (String) i.next();
			Element polymerElement = new Element(POLYMER_ELEMENT);
			Attribute att = new Attribute(POLYMER_TYPE_ATTRIBUTE, polymerType);
			polymerElement.setAttribute(att);
			polymerListElement.getChildren().add(polymerElement);

			Map<String, Monomer> monomerMap = mDB.get(polymerType);
			Set<String> monomerSet = monomerMap.keySet();

			for (Iterator it = monomerSet.iterator(); it.hasNext();) {
				String monomerID = (String) it.next();
				Monomer m = monomerMap.get(monomerID);
				Element monomerElement = MonomerParser.getMonomerElement(m);
				polymerElement.getChildren().add(monomerElement);
			}
		}
		String polymerListString = outputer.outputString(polymerListElement);
		sb.append(polymerListString + System.getProperty("line.separator"));

		Map<String, Attachment> aDB = cache.getAttachmentDB();
		Element attachmentListElement = new Element(ATTACHMENT_LIST_ELEMENT);
		Set<String> attachmentSet = aDB.keySet();
		for (Iterator itr = attachmentSet.iterator(); itr.hasNext();) {
			String attachmentID = (String) itr.next();
			Attachment attachment = aDB.get(attachmentID);
			Element attachmentElement = MonomerParser.getAttachementElement(attachment);
			attachmentListElement.getChildren().add(attachmentElement);
		}
		String attachmentListString = outputer.outputString(attachmentListElement);
		sb.append(attachmentListString);

		sb.append(System.getProperty("line.separator") + "</MonomerDB>" + System.getProperty("line.separator"));

		return sb.toString();
	}

	/**
	 * This method is called during startup, use serialized version if exists,
	 * otherwise use XML version (First from local, then from jar)
	 *
	 * @throws ChemistryException
	 * @throws CTKException
	 *
	 * @throws org.helm.notation2.exception.MonomerException
	 * @throws java.io.IOException
	 * @throws org.jdom.JDOMException
	 */
	private static void initializeMonomerCache() throws MonomerLoadingException, ChemistryException {
		MonomerCache cache = null;
		InputStream in = null;

		// check for webservice properties file

		if (MonomerStoreConfiguration.getInstance().isUseWebservice()) {
			try {
				cache = buildMonomerCacheFromWS();
				validate(cache.getMonomerDB());
			} catch (MonomerException | IOException | JDOMException | CTKException e) {
				throw new MonomerLoadingException(
						"Initializing MonomerStore failed because of " + e.getClass().getSimpleName(), e);
			}

			logger.log(Level.INFO, "WebService '' is used for monomer cache initialization");
		} else if (MonomerStoreConfiguration.getInstance().isUseExternalMonomers()) {
			try {
				in = new FileInputStream(MonomerStoreConfiguration.getInstance().getExternalMonomersPath());
				cache = buildMonomerCacheFromXML(in);
				validate(cache.getMonomerDB());
				logger.log(Level.INFO, MonomerStoreConfiguration.getInstance().getExternalMonomersPath()
						+ " is used for monomer cache initialization");
			} catch (Exception e) {
				logger.log(Level.INFO, "Unable to use local monomer DB file: "
						+ MonomerStoreConfiguration.getInstance().getExternalMonomersPath());
			}

		} else {
			File cacheFile = new File(MONOMER_CACHE_FILE_PATH);
			if (cacheFile.exists()) {
				try {
					cache = deserializeMonomerCache(MONOMER_CACHE_FILE_PATH);
					validate(cache.getMonomerDB());
					logger.log(Level.INFO, MONOMER_CACHE_FILE_PATH + " is used for monomer cache initialization");
				} catch (Exception e) {
					logger.log(Level.INFO, "Unable to use local monomer cache file: " + MONOMER_CACHE_FILE_NAME);
					cacheFile.delete();
					logger.log(Level.INFO, "Deleted local monomer cache file: " + MONOMER_CACHE_FILE_NAME);
				}
			}

			File localMonomerDBFile = new File(MONOMER_DB_FILE_PATH);
			if (null == cache && localMonomerDBFile.exists()) {
				try {
					in = new FileInputStream(MONOMER_DB_FILE_PATH);
					cache = buildMonomerCacheFromXML(in);
					validate(cache.getMonomerDB());
					logger.log(Level.INFO, MONOMER_DB_FILE_PATH + " is used for monomer cache initialization");
				} catch (Exception e) {
					logger.log(Level.INFO, "Unable to use local monomer DB file: " + MONOMER_DB_FILE_NAME);
					localMonomerDBFile.delete();
					logger.log(Level.INFO, "Deleted local monomer DB file: " + MONOMER_DB_FILE_NAME);
				}
			}

			if (null == cache) {
				in = MonomerFactory.class.getResourceAsStream(MONOMER_DB_XML_RESOURCE);
				try {
					LOG.info("BuildMonomerCacheFromXML");
					cache = buildMonomerCacheFromXML(in);
					validate(cache.getMonomerDB());
				} catch (MonomerException | IOException | JDOMException | CTKException e) {
					throw new MonomerLoadingException(
							"Initializing MonomerStore failed because of " + e.getClass().getSimpleName(), e);
				}

				logger.log(Level.INFO, MONOMER_DB_XML_RESOURCE + " is used for monomer cache initialization");
			}

		}

		monomerDB = cache.getMonomerDB();
		attachmentDB = cache.getAttachmentDB();
		smilesMonomerDB = cache.getSmilesMonomerDB();

		dbChanged = true;

	}

	/**
	 * save monomerCache to disk file
	 *
	 * @throws java.io.IOException
	 *             if the monomer can not be saved to disk file
	 * @throws MonomerException
	 *             if monomer is not valid
	 */
	public void saveMonomerCache() throws IOException, MonomerException {
		File f = new File(NOTATION_DIRECTORY);
		if (!f.exists()) {
			f.mkdir();
		}
		MonomerCache cache = new MonomerCache();
		cache.setMonomerDB(getMonomerDB(false));
		cache.setAttachmentDB(getAttachmentDB());
		cache.setSmilesMonomerDB(getSmilesMonomerDB(false));
		serializeMonomerCache(cache, MONOMER_CACHE_FILE_PATH);

		String monomerDbXML = buildMonomerDbXMLFromCache(cache);

		FileOutputStream fos = new FileOutputStream(MONOMER_DB_FILE_PATH);
		fos.write(monomerDbXML.getBytes());
		fos.close();
	}

	private static Map<String, Map<String, Monomer>> buildMonomerDB(Element polymerList)
			throws MonomerException, IOException, JDOMException, CTKException, ChemistryException {
		Map<String, Map<String, Monomer>> map = new TreeMap<String, Map<String, Monomer>>(
				String.CASE_INSENSITIVE_ORDER);
		List poplymers = polymerList.getChildren();

		Iterator i = poplymers.iterator();
		while (i.hasNext()) {
			Element polymer = (Element) i.next();
			Attribute polymerType = polymer.getAttribute(POLYMER_TYPE_ATTRIBUTE);

			Map idMonomerMap = new TreeMap<String, Monomer>(String.CASE_INSENSITIVE_ORDER);

			List monomers = polymer.getChildren();
			Iterator it = monomers.iterator();
			while (it.hasNext()) {
				Element monomer = (Element) it.next();
				
				Monomer m = MonomerParser.getMonomer(monomer);

				if (MonomerParser.validateMonomer(m)) {
					idMonomerMap.put(m.getAlternateId(), m);
				}
			}
			map.put(polymerType.getValue(), idMonomerMap);
		}

		return map;
	}

	private static Map<String, Attachment> buildAttachmentDB(Element attachmentList)
			throws MonomerException, IOException, JDOMException, ChemistryException {
		Map<String, Attachment> map = new TreeMap<String, Attachment>(String.CASE_INSENSITIVE_ORDER);

		List attachments = attachmentList.getChildren();
		Iterator i = attachments.iterator();
		while (i.hasNext()) {
			Element attachment = (Element) i.next();
			Attachment att = MonomerParser.getAttachment(attachment);

			if (MonomerParser.validateAttachement(att)) {
				map.put(att.getAlternateId(), att);
			}

		}

		return map;
	}

	private static Map<String, Monomer> buildSmilesMonomerDB(Map<String, Map<String, Monomer>> monomerDB) {
		Map<String, Monomer> map = new HashMap<String, Monomer>();
		Set<String> polymerSet = monomerDB.keySet();
		for (Iterator i = polymerSet.iterator(); i.hasNext();) {
			String polymer = (String) i.next();
			Map<String, Monomer> monomerMap = monomerDB.get(polymer);
			Set<String> monomerSet = monomerMap.keySet();
			for (Iterator it = monomerSet.iterator(); it.hasNext();) {
				String monomerID = (String) it.next();
				Monomer monomer = monomerMap.get(monomerID);
				String smiles = monomer.getCanSMILES();

				try {
					Chemistry.getInstance().getManipulator().canonicalize(smiles);
				} catch (Exception e) {
					smiles = monomer.getCanSMILES();

				}

				monomer.setCanSMILES(smiles);
				map.put(smiles, monomer);

			}
		}
		return map;
	}

	private static boolean validate(Map<String, Map<String, Monomer>> monomerDB)
			throws MonomerException, IOException, CTKException, ChemistryException {
		Set<String> polymers = monomerDB.keySet();
		for (String polymer : polymers) {
			Map<String, Monomer> monomerMap = monomerDB.get(polymer);
			Set<String> monomers = monomerMap.keySet();
			for (String monomer : monomers) {
				Monomer m = monomerMap.get(monomer);
				logger.info(m.getAlternateId());
				try {
				MonomerParser.validateMonomer(m);
				} catch (Exception e) {
					LOG.error("Monomer is invalide");
					monomerDB.remove(monomer);
				}
			}
		}
		return true;
	}

	public static void finalizeMonomerCache() {

		monomerDB = null;
		attachmentDB = null;
		smilesMonomerDB = null;
		dbChanged = true;
		instance = null;
	}

}
