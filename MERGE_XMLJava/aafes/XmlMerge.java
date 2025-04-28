package aafes;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.atteo.xmlcombiner.XmlCombiner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbBLOB;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;

public class XmlMerge extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		MbOutputTerminal alt = getOutputTerminal("failure");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below
			MbElement msgxml   = null;
			msgxml = inAssembly.getGlobalEnvironment().getRootElement().getFirstElementByPath("/VAR").getFirstChild();
			
			// create combiner
			XmlCombiner xmlCombiner = new XmlCombiner();
			String finalRes = "";
			while (msgxml != null) {
				String message = msgxml.getFirstChild().getValue().toString();
				finalRes = finalRes.concat(message);
				msgxml = msgxml.getNextSibling();
			}
			finalRes = "<merge>" + finalRes +"</merge>";
			xmlCombiner.combine(this.StringToDoc(finalRes));
			Document finalDocument = xmlCombiner.buildDocument();
			//finalDocument.appendChild(finalDocument);
			String finalXml = this.DocToString(finalDocument);
			
			MbElement Result = outMessage.getRootElement().createElementAsLastChild(MbBLOB.PARSER_NAME);
			Result.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "BLOB", finalXml.getBytes()); 
			// End of user code
			// ----------------------------------------------------------
		} catch (MbException e) {
			// Re-throw to allow Broker handling of MbException
			throw e;
		} catch (RuntimeException e) {
			// Re-throw to allow Broker handling of RuntimeException
			throw e;
		} catch (Exception e) {
			// Consider replacing Exception with type(s) thrown by user code
			// Example handling ensures all exceptions are re-thrown to be handled in the flow
			throw new MbUserException(this, "evaluate()", "", "", e.toString(),
					null);
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly);

	}
	private Document StringToDoc(String xml)
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try{
		builder= factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(xml)));
		return doc;
		}catch(Exception e)
		{
			
		}
		return null;
	}
	
	private String DocToString(Document document) throws TransformerException
	{
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
		
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
		
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
			return output;
		}catch (TransformerConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return null;
	}
	/*public class XmlCombiner {
	    private final Document combinedDoc;
	    private final Element root;

	    public XmlCombiner() throws ParserConfigurationException {
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        combinedDoc = builder.newDocument();
	        root = combinedDoc.createElement("Combined"); // Root element for merged XML
	        combinedDoc.appendChild(root);
	    }

	    public void combine(Document doc) {
	        Node importedNode = (Node) combinedDoc.importNode(doc.getDocumentElement(), true);
	        root.appendChild((org.w3c.dom.Node) importedNode);
	    }

	    public Document buildDocument() {
	        return combinedDoc; // Return pre-built document, never null
	    }
	}*/

}
