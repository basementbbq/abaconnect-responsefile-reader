package ch.abacus.abaconnecttools;

import java.io.CharArrayWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * This is an abstract class, that can be used in a derived class, to read and parse
 * XML Files using the interfaces from the SAX components included in the Java API library.
 * It it very quick to parse XML Files and can be extended in a derived class to read
 * the XML elements from the XML File in a derived class using the 2 abstract functions
 * {@link #startElement(String, Attributes) startElement} and
 * {@link #endElement(String, String) endElement}.<br><br>
 *
 * <b>Example 1 :</b> A derived class to remember the element node level and the element
 * attributes.  The function <code>currentelement</code> would receive notification
 * for every element with the full information about the element.
 * <pre>
 *    public class XmlFileParser extends SimpleXmlSaxParser {
 *        // Internal member for remembering attributes of the current element
 *        private AttributesImpl currentAttributes = new AttributesImpl();
 *
 *        // Internal member for tracking element node level
 *        int mElementLevel = 0;
 *
 *        // This function is called when an element node is started.
 *        public void startElement(String elementName, Attributes atts) {
 *            if ( atts != null ) {
 *                currentAttributes.setAttributes(atts);
 *            }
 *            mElementLevel++;
 *        }
 *
 *        // This function is called when an element node is completed.
 *        public void endElement(String name, String value) {
 *            currentElement( mElementLevel, name, value, currentAttributes );
 *            // Clear the Attributes
 *            currentAttributes.clear();
 *            mElementLevel--;
 *        }
 *
 *        // This function receives the information for an element node every time it is
 *        // parsed by this class.  It is called from the endElement function.
 *        public void currentElement(int elementLevel, String elementName, String elementValue, Attributes atts) {
 *             // Receives notification about the element nodes
 *        }
 *   }
 * </pre>
 *
 * <b>Example 2 :</b> How to access Element Attributes in a derived class.
 * <pre>
 *        // See the Example above to see how this function could be implemented
 *        // in a derived class from SimpleXmlSaxParser.
 *        public void currentElement(int elementLevel, String elementName, String elementValue, Attributes atts) {
 *             // Write Element info to standard output
 *             System.out.print("Element [ < " + elementName );
 *             if ( atts != null ) {
 *                 int attLength = atts.getLength();
 *                 for( int ii = 0; ii < attLength; ii++ ) {
 *                     System.out.print( " " + atts.getQName(ii) + "='" + atts.getValue(ii) + "'" );
 *                 }
 *             }
 *             // Trim element value to eliminate any whitespace characters.
 *             System.out.print(" >" + elementValue.trim() + "< /"+ elementName + " >]\n");
 *        }
 *
 * </pre>
 */
public abstract class SimpleXmlSaxParser implements ContentHandler, ErrorHandler {

   XMLReader m_XMLReader;
  /**
   * This is an character array member variable that contains the value of the each
   * element as it is parsed.  The <code>elementValue</code> will be sent, as a string,
   * to the {@link #endElement(String,String) endElement(name,value} function.<br><br>
   *
   * <b>Note :</b> The <code>elementValue</code> is set during the calls
   * to the {@link #characters(char[],int,int) characters} method and cleared again
   * in {@link #endElement(String,String,String) endElement(namespaceURI,localName,qName)}
   * after the call to the {@link #endElement(String,String) endElement(name,value)}
   *
   * @see #endElement(String,String,String) endElement(namespaceURI,localName,qName)
   * @see #startElement(String,String,String,Attributes) startElement(namespaceURI,localName,qName,attributes)
   */
  public CharArrayWriter elementValue = new CharArrayWriter();

  /**
   * Receives notification of character data for an element (i.e. the element value).
   * It can be that this function delivers the data in several chunks, although for
   * small amounts of element data, the data is delivered in 1 call.  The data may contain
   * whitespace characters which includes spaces, carriage returns and linefeeds
   * (CR/LF).<br><br>
   *
   * <b>Note :</b> The {@link #elementValue elementValue} is set during the calls
   * to the <code>characters</code> method and cleared again
   * in {@link #endElement(String,String,String) endElement(namespaceURI,localName,qName)}
   * after the call to the {@link #endElement(String,String) endElement(name,value)}
   *
   * @param ch The characters from the XML document.
   * @param start The start position in the array.
   * @param length The number of characters to read from the array
   *
   * @see #startElement(String,String,String,Attributes) startElement(namespaceURI,localName,qName,attributes)
   * @see #startElement(String,Attributes) startElement(name,attributes)
   * @see #endElement(String,String,String) endElement(namespaceURI,localName,qName)
   * @see #endElement(String,String) endElement(name,value)
   * @see ContentHandler#characters
   * @see ContentHandler
   */
  public void characters(char ch[], int start, int length) {
    elementValue.write(ch, start, length);
  }

  /**
   * Receives notification of the end of an element. Each time the <code>endElement</code>
   * is called, an Element closing node has been read from the XML file.  The element
   * value is available from the stored {@link #elementValue elementValue}
   * member variable.<br><br>
   *
   * <b>Note :</b> During this method the abstract
   * {@link #endElement(String,String) endElement(name,value} member method is called
   * with the element name and element value contained in the <code>elementValue</code>.
   * After the call, the contents of the {@link #elementValue elementValue} are cleared with
   * <code>elementValue.reset()</code>.
   *
   * @param namespaceURI The Namespace URI, or the empty string if the
   *        element has no Namespace URI or if Namespace
   *        processing is not being performed.
   * @param localName The local name (without prefix), or the
   *        empty string if Namespace processing is not being
   *        performed.
   * @param qName The qualified XML 1.0 name (with prefix), or the
   *        empty string if qualified names are not available.
   *
   * @see #startElement(String,String,String,Attributes) startElement(namespaceURI,localName,qName,attributes)
   * @see #startElement(String,Attributes) startElement(name,attributes)
   * @see #endElement(String,String) endElement(name,value)
   * @see ContentHandler#endElement
   * @see ContentHandler
   */
  public void endElement(String namespaceURI, String localName, String qName) {
    endElement(qName, elementValue.toString());
    elementValue.reset();
  }

  /**
   * The <code>endElement</code> method indicates the end of an element and must be
   * implemented in a derived class.  Each time <code>endElement</code> is called,
   * an element closing node has been read from the XML file.  The <code>value</code>
   * parameter is the element value supplied from the {@link #elementValue elementValue},
   * which can contain addition whitespace characters that are read from the XML File.
   * <br><br>
   *
   * <b>Hint :</b> Call the <code>String.trim()</code> method to eliminate unwanted
   * whitespace characters in the <code>value</code> parameter.
   * (i.e. <code>value.trim();</code>)<br><br>
   *
   * <b>Note :</b> The {@link #elementValue elementValue} is set during the calls
   * to the {@link #characters(char[], int, int) characters} method and cleared again
   * in {@link #endElement(String, String, String) endElement(namespaceURI, localName, qName)}
   * after the call to the {@link #endElement(String, String) endElement(name, value)}
   *
   * @param name the name of the element
   * @param value the value of the element as String
   *
   * @see #startElement(String,String,String,Attributes) startElement(namespaceURI,localName,qName,attributes)
   * @see #startElement(String,Attributes) startElement(name,attributes)
   * @see #endElement(String,String,String) endElement(namespaceURI,localName,qName)
   */
  public abstract void endElement(String name, String value);

  /**
   * Parses an specified XML File defined by the full path name of the file, with the
   * SAX components.  The XML file elements can be accessed via the
   * abstract {@link #startElement(String, Attributes) startElement} and
   * {@link #endElement(String, String) endElement} methods defined in this class.
   *
   * @param filename java.lang.String the full path name of the XML file
   * @throws SAXException this Exception is thrown when a fatal error occures during
   *                     the xml parsing. It depends on the actual code what to do with
   *                     this Exception. The Exception is thrown when an illegal
   *                     character should be parsed etc.
   *
   * @see #startElement(String,Attributes) startElement(name,attributes)
   * @see #endElement(String,String) endElement(name,value)
   */
  public void saxParse(String filename) throws SAXException {
    try {
      FileInputStream fis = new FileInputStream(filename);
      saxParse(fis);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("XML-File could not be found. The File is: " + filename, e);
    }
  }

  /**
   * Parses an XML File specified by the input stream, with the
   * SAX components.  The XML file elements can be accessed via the
   * abstract {@link #startElement(String, Attributes) startElement} and
   * {@link #endElement(String, String) endElement} methods defined in this class.
   *
   * @param xmlStream the xml file as input stream
   * @throws SAXException this Exception is thrown when a fatal error occurs during
   *                     the xml parsing. It depends on the actual code what to do with
   *                     this Exception. The Exception is thrown when an illegal
   *                     character should be parsed etc.
   *
   * @see #saxParse(String) saxParse(fileName);
   * @see #startElement(String,Attributes) startElement(name,attributes)
   * @see #endElement(String,String) endElement(name,value)
   */
  public void saxParse(InputStream xmlStream) throws SAXException {
    InputSource is = new InputSource(xmlStream);
    saxParse(is);
  }

  /**
   * Parses an XML File specified by the reader, with the
   * SAX components.  The XML file elements can be accessed via the
   * abstract {@link #startElement(String, Attributes) startElement} and
   * {@link #endElement(String, String) endElement} methods defined in this class.
   *
   * @param xmlReader the xml file as a Reader
   * @throws SAXException this Exception is thrown when a fatal error occurs during
   *                     the xml parsing. It depends on the actual code what to do with
   *                     this Exception. The Exception is thrown when an illegal
   *                     character should be parsed etc.
   *
   * @see #saxParse(String) saxParse(fileName);
   * @see #startElement(String,Attributes) startElement(name,attributes)
   * @see #endElement(String,String) endElement(name,value)
   */
  public void saxParse(Reader xmlReader) throws SAXException {
    InputSource is = new InputSource(xmlReader);
    saxParse(is);
  }

  /**
   * Parses an XML File specified by the input source, with the
   * SAX components.  The XML file elements can be accessed via the
   * abstract {@link #startElement(String, Attributes) startElement} and
   * {@link #endElement(String, String) endElement} methods defined in this class.
   *
   * @param is the xml file as org.xml.sax.InputSource
   * @throws SAXException this Exception is thrown when a fatal error occures during the xml parsing. It depends on the
   * actual code what to do with this Exception. The Exception is thrown when an illegal character should be parsed etc.
   *
   * @see #saxParse(String) saxParse(fileName);
   * @see #startElement(String,Attributes) startElement(name,attributes)
   * @see #endElement(String,String) endElement(name,value)
   */
  public void saxParse(InputSource is) throws SAXException {

    if(is == null) {
      throw new NullPointerException("InputSource for SimpleXmlSaxParser is NULL. Check if the specified XML file exists!");
    }

    try {

      // if the SimpleXmlSaxParser has problems caused by xerxes, set this property
//      System.setProperty("javax.xml.parsers.SAXParserFactory", "org.apache.crimson.jaxp.SAXParserFactoryImpl");


      XMLReader reader = getReader ();
        // allows to overwrite the error methods
      //      parser.setErrorHandler(new SaxErrorHandler());
      reader.parse(is);
      // up to the class that extends from this one
      //    } catch (SAXException e) {
      //      System.out.println("XXX: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
      //      e.printStackTrace();
      //      throw new AbaRuntimeException(this.getClass(), e, "saxParse(FileInputStream) - SAXException");
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("saxParse(FileInputStream) - ParserConfigurationException - the parser is configured in a wrong way", e);
    } catch (IOException e) {
      throw new RuntimeException("saxParse(FileInputStream) - IOException",e);
    }
  }

    private XMLReader getReader () throws ParserConfigurationException, SAXException {
        if ( m_XMLReader != null ) return m_XMLReader;

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(false);
        SAXParser sp = spf.newSAXParser();
        m_XMLReader = sp.getXMLReader();
        m_XMLReader.setContentHandler(this);
        m_XMLReader.setErrorHandler(this);

        return m_XMLReader;
    }

    /**
     * The <code>startElement</b> defines the beginning of a new element and must be
     * implemented in a derived class.  Each time <code>startElement</code> is called,
     * a new Element has been read from the XML file.<br><br>
     *
     * <b>Note :</b> See the example in the class description to see how the class
     * can be extended and the Attributes for each node temporarily cached for later
     * use.
     *
     * @param elementName the name of the element.
     * @param atts the attributes attached to this element, if any.
     *
     * @see #startElement(String,String,String,Attributes) startElement(namespaceURI,localName,qName,attributes)
     * @see #endElement(String,String,String) endElement(namespaceURI,localName,qName)
     * @see #endElement(String,String) endElement(name,value)
     */
    public abstract void startElement(String elementName, Attributes atts);

  /**
   * This method will be called for every new element node read from the XML File.
   * Within this method the abstract member function {@link #startElement(String, Attributes) startElement}
   * will be called to notify any derived classes of the element start event.
   *
   * @param namespaceURI
   * @param localName
   * @param qName the element qualified name
   * @param atts the attributes attached to this element, if any
   *
   * @see #startElement(String,Attributes) startElement(name,attributes)
   * @see #endElement(String,String,String) endElement(namespaceURI,localName,qName)
   * @see #endElement(String,String) endElement(name,value)
   * @see ContentHandler#startElement
   * @see ContentHandler
   */
  public void startElement(
    String namespaceURI,
    String localName,
    String qName,
    Attributes atts) {
    startElement(qName, atts);
  }

  /**
   * Receives an object for locating the origin of SAX document events (see the original
   * {@link ContentHandler#setDocumentLocator setDocumentLocator} interface method for
   * more information).
   *
   * @see ContentHandler#setDocumentLocator
   * @see ContentHandler
   */
  public void setDocumentLocator(Locator locator) {
  }

  /**
   * Receive notification of the beginning of a document(see the original
   * {@link ContentHandler#startDocument startDocument} interface method for
   * more information). This notification event is called only once as the first
   * call before parsing.
   *
   * @see ContentHandler#startDocument
   * @see ContentHandler
   */
  public void startDocument() {
  }

  /**
   * Receive notification of the end of a document (see the original
   * {@link ContentHandler#endDocument endDocument} interface method for
   * more information).  This notification event is called only once as the last
   * call after parsing.
   *
   * @see ContentHandler#endDocument
   * @see ContentHandler
   */
  public void endDocument() {
  }

  /**
   * Receive notification of a skipped entity (see the original
   * {@link ContentHandler#skippedEntity skippedEntity} interface method for
   * more information).
   *
   * @param entity the name of the skipped entity
   *
   * @see ContentHandler#skippedEntity
   * @see ContentHandler
   */
  public void skippedEntity(String entity) {
  }

  /**
   * Begin the scope of a prefix-URI Namespace mapping (see the original
   * {@link ContentHandler#startPrefixMapping startPrefixMapping} interface method for
   * more information).
   *
   * @param prefix the Namespace prefix being declared.
   * @param uri the Namespace URI the prefix is mapped to.
   *
   * @see ContentHandler#startPrefixMapping
   * @see ContentHandler
   */
  public void startPrefixMapping(String prefix, String uri) {
  }

  /**
   * End the scope of a prefix-URI mapping (see the original
   * {@link ContentHandler#endPrefixMapping endPrefixMapping} interface method for
   * more information).
   *
   * @param prefix the prefix that was being mapping.
   *
   * @see ContentHandler#endPrefixMapping
   * @see ContentHandler
   */
  public void endPrefixMapping(String prefix) {
  }

  /**
   * Receive notification of ignorable whitespace in element content(see the original
   * {@link ContentHandler#ignorableWhitespace ignorableWhitespace} interface method for
   * more information).
   *
   * @param ch the characters from the XML document.
   * @param start the start position in the array.
   * @param length the number of characters to read from the array.
   *
   * @see ContentHandler#ignorableWhitespace
   * @see ContentHandler
   */
  public void ignorableWhitespace(char[] ch, int start, int length) {
  }

  /**
   * Receive notification of a processing instruction (see the original
   * {@link ContentHandler#processingInstruction processingInstruction} interface method for
   * more information).
   *
   * @param target the processing instruction target.
   * @param data  the processing instruction data, or null if none was supplied.
   *
   * @see ContentHandler#processingInstruction
   * @see ContentHandler
   */
  public void processingInstruction(String target, String data) {
  }

  /**
   * Receives notification of XML parsing warnings and calls the {@link #logError logError}
   * member function.  No exception is thrown for warnings.
   *
   * @param ex the {@link org.xml.sax.SAXParseException SAXParseException} warning exception
   *
   * @see ErrorHandler#warning
   * @see ErrorHandler
   * @see org.xml.sax.SAXParseException
   */
  public void warning(SAXParseException ex) {
    logError(ex);
  }

  /**
   * Receives notification of XML parsing errors, calls the {@link #logError logError}
   * member function and throws an AbaRuntimeException.
   *
   * @param ex the {@link org.xml.sax.SAXParseException SAXParseException} error exception
   *
   * @see ErrorHandler#error
   * @see ErrorHandler
   * @see org.xml.sax.SAXParseException
   */
  public void error(SAXParseException ex) {
    logError(ex);
    throw new RuntimeException("Error in SimpleXmlSaxParser", ex);
  }

  /**
   * Receives notification of fatal XML parsing errors, calls the {@link #logError logError}
   * member function and throws an AbaRuntimeException.
   *
   * @param ex the {@link org.xml.sax.SAXParseException SAXParseException} error exception
   *
   * @see ErrorHandler#fatalError
   * @see ErrorHandler
   * @see org.xml.sax.SAXParseException
   */
  public void fatalError(SAXParseException ex) throws SAXException {
    logError(ex);
    throw new RuntimeException("Fatal Error in SimpleXmlSaxParser", ex);
  }

  /**
   * Receives notification of XML parsing errors and writes the standard output.
   *
   * @param ex the SAX parsing exception error
   *
   * @see org.xml.sax.SAXParseException
   */
  protected void logError(SAXParseException ex) {
    System.out.println(
      ex + " in " + this.getClass() + " SAX parsing error: " + ex.getMessage());
    System.out.println("Exception at location: " + getLocationString(ex));
    ex.printStackTrace();
  }

  /**
   * Get the location (i.e. XML file line number and sub-string) from the
   * {@link SAXParseException SAXParseException} error during the parse
   *
   * @param ex the org.xml.sax.SAXParseException SAX exception
   * @return returns a string with a human readable form of the error (XML file line number and sub-string)
   *
   * @see org.xml.sax.SAXParseException
   */
  private String getLocationString(SAXParseException ex) {
    StringBuffer str = new StringBuffer();
    String systemId = ex.getSystemId();
    if (systemId != null) {
      int index = systemId.lastIndexOf('/');
      if (index != -1)
        systemId = systemId.substring(index + 1);
      str.append(systemId);
    }
    str.append(':');
    str.append(ex.getLineNumber());
    str.append(':');
    str.append(ex.getColumnNumber());
    return str.toString();
  }
}
