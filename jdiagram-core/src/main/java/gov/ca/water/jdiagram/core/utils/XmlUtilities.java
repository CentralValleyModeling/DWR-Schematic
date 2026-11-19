package gov.ca.water.jdiagram.core.utils;

import java.io.FileWriter;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.mindfusion.diagramming.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Utility class for XML operations.
 */
public final class XmlUtilities {
	private XmlUtilities() {
		throw new IllegalStateException("Utility class, cannot be instantiated.");
	}

	/**
	 * Creates a new XML document.
	 * @return A new XML document.
	 * @throws XmlException If an error occurs while creating the document.
	 */
	public static Document newDocument() throws XmlException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			return factory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException ex) {
			throw new XmlException(ex);
		}
	}

	/**
	 * Save a provided XML document to the specified file.
	 * @param document The XML document to save.
	 * @param filename The filename to use when saving the document.
	 * @throws IOException If an error occurs while saving the document.
	 * @throws TransformerException If an error occurs while transforming the XML document into a savable format.
	 */
	public static void saveTo(Node document, String filename) throws IOException, TransformerException {
		try (FileWriter writer = new FileWriter(filename)) {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();

			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(writer);
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16");
			transformer.transform(source, result);
		}
	}
}
