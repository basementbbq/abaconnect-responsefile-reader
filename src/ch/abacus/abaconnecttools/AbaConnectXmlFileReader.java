/*
 * ReadAbaConnectXmlFile.java  
 *
 * Creator:
 * 06.05.11 09:31 Sippel
 *
 * Maintainer:
 * 06.05.11 09:31 Sippel
 *
 * Last Modification:
 * $Id: AbaConnectXmlFileReader.java 29791 2013-03-26 13:52:41Z sippel $
 *
 * Copyright (c) 2003 ABACUS Research AG, All Rights Reserved
 */
package ch.abacus.abaconnecttools;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class AbaConnectXmlFileReader  extends SimpleXmlSaxParser {
    private String mXmlOutputFileName = "";
    private StringBuilder mXmlFileBuffer = new StringBuilder();
    private int mCurrentTransactionIndex = -1;
    private int mCurrentTaskIndex = -1;
    private boolean mInTransaction = false;
    private StringBuilder mTransactionMessages = new StringBuilder();
    private String mAttibuteDelimeter = "\"";

    private ArrayList<TaskTransactionMessage> mTaskErrorTransactionIndexesAndMessages = new ArrayList<TaskTransactionMessage>();

    private HashMap<Integer, String> mErrorTransactionIndexesAndMessages = new HashMap<Integer, String>();
    private String mGeneralMessages = "";

    private BufferedOutputStream mBufferedOutputStream = null;
    private boolean mXmlHeaderAdded = false;
    private int mFileBufferLength = 1024;

    private boolean mRemoveTransactionElementsAttributes = false;

    @Override
    public void endElement(String name, String value) {
        appendText(convertTextToXmlCompatibleString(value));
        appendText("</");
        appendText(name);
        appendText(">");

        if ( "Transaction".equals(name) ) {
            mInTransaction = false;
        }
    }

    @Override
    public void startElement(String elementName, Attributes atts) {
        if ( "Task".equals(elementName) ) {
            if ( mCurrentTaskIndex < 0 ) {
                mCurrentTaskIndex = 0;
            } else {
                mCurrentTaskIndex++;
            }
            mCurrentTransactionIndex = -1;  // Reset the transaction count at the start of the Task block
        }
        if ( "Transaction".equals(elementName) ) {
            if ( mGeneralMessages != null && !"".equals(mGeneralMessages) ) {
                appendText("\n    <!-- General Messages -->");
                appendText("\n    <!-- ");
                appendText(mGeneralMessages);
                appendText(" -->");
                mGeneralMessages = "";
            }
            mInTransaction = true;
            mTransactionMessages.setLength(0);
            if ( mCurrentTransactionIndex < 0 ) {
                mCurrentTransactionIndex = 0;
            } else {
                mCurrentTransactionIndex++;
            }
            appendText("\n    <!-- Transaction " + mCurrentTransactionIndex + " -->");
            if ( mErrorTransactionIndexesAndMessages.containsKey(mCurrentTransactionIndex) ) {
                appendText("\n    <!-- ");
                appendText(mErrorTransactionIndexesAndMessages.get(mCurrentTransactionIndex));
                appendText(" -->");
            }
            TaskTransactionMessage dummyValue = new TaskTransactionMessage(mCurrentTaskIndex,mCurrentTransactionIndex,"");
            int foundIndex = mTaskErrorTransactionIndexesAndMessages.indexOf(dummyValue);
            if ( foundIndex >= 0 ) {
                dummyValue = mTaskErrorTransactionIndexesAndMessages.get(foundIndex);
                if ( dummyValue != null ) {
                    appendText("\n    <!-- ");
                    appendText(dummyValue.mMessage);
                    appendText(" -->");
                }
            }
        }

        appendText(convertTextToXmlCompatibleString(elementValue.toString()));
        elementValue.reset();

        appendText("<");
        appendText(elementName);
        if ( atts != null ) {
            int attCount = atts.getLength();
            // For Transaction elements, any existing attributes can be removed
            boolean removeElementAttributes = mRemoveTransactionElementsAttributes && "Transaction".equals(elementName);
            if ( attCount > 0 && !removeElementAttributes ) {
                StringBuilder attribs = new StringBuilder();
                for ( int index = 0; index < attCount; index++ ) {
                    if ( attribs.length() > 0 ) attribs.append(" ");
                    attribs.append(atts.getLocalName(index));
                    attribs.append("=");
                    attribs.append(mAttibuteDelimeter);
                    attribs.append(atts.getValue(index));
                    attribs.append(mAttibuteDelimeter);
                }
                if ( attribs.length() > 0 ) {
                    // Add the attributes to the output text string
                    appendText(" ");
                    appendText(attribs.toString());
                }
            }
        }
        appendText(">");
    }

    public void setXmlOutputFileName(String outXmlFilename) {
        mXmlOutputFileName = (outXmlFilename == null ? "" : outXmlFilename);
    }

    @Override
    public void saxParse(String filename) throws SAXException {
        mXmlFileBuffer.setLength(0);
        mCurrentTransactionIndex = -1;
        mInTransaction = false;
        mBufferedOutputStream = null;

        super.saxParse(filename);
    }

    @Override
    public void endDocument() {
        super.endDocument();

//        System.out.println("END DOCUMENT");

        writeTextToFile(mXmlFileBuffer.toString());
        mXmlFileBuffer.setLength(0);

        if ( mBufferedOutputStream != null ) {
            try {
                mBufferedOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void appendText(String text) {
        if ( !mXmlHeaderAdded && mXmlFileBuffer.length() == 0 ) {
            mXmlFileBuffer.append("<?xml version='1.0' encoding='UTF-8'?>\n");
            mXmlHeaderAdded = true;
        }

        TaskTransactionMessage dummyValue = new TaskTransactionMessage(mCurrentTaskIndex,mCurrentTransactionIndex,"");
        int foundIndex = mTaskErrorTransactionIndexesAndMessages.indexOf(dummyValue);
        if ( !mInTransaction || mErrorTransactionIndexesAndMessages.containsKey(mCurrentTransactionIndex) || foundIndex >= 0 ) {
            mXmlFileBuffer.append(text);
        }
        if ( mXmlFileBuffer.length() > mFileBufferLength ) {
            writeTextToFile(mXmlFileBuffer.toString());
            mXmlFileBuffer.setLength(0);
        }
    }

    private void writeTextToFile(String text) {
        if ( !"".equals(mXmlOutputFileName) ) {
            if ( mBufferedOutputStream == null ) {
                try {
                    mBufferedOutputStream = new BufferedOutputStream(new FileOutputStream(mXmlOutputFileName));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if ( mBufferedOutputStream != null &&  text != null ) {
                try {
                    mBufferedOutputStream.write(text.getBytes(Charset.forName("UTF-8")));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    public void setErrorTransactionIndexesAndMessages(HashMap<Integer, String> errorTransactionIndexesAndMessages) {
//        mErrorTransactionIndexesAndMessages.clear();
//        if ( errorTransactionIndexesAndMessages != null ) {
//            for ( Integer transIndex : errorTransactionIndexesAndMessages.keySet() ) {
//                mErrorTransactionIndexesAndMessages.put(transIndex, errorTransactionIndexesAndMessages.get(transIndex));
//            }
//        }
//    }

    public void setTaskErrorTransactionIndexesAndMessages(ArrayList<TaskTransactionMessage> taskErrorTransactionIndexesAndMessages) {
        mTaskErrorTransactionIndexesAndMessages.clear();
        if ( taskErrorTransactionIndexesAndMessages != null ) {
            mTaskErrorTransactionIndexesAndMessages.addAll(taskErrorTransactionIndexesAndMessages);
        }
    }

    public void setGeneralMessages(String generalMessages) {
        mGeneralMessages = generalMessages == null ? "" : generalMessages;
    }

    public String getXmlAsString() {
        return mXmlFileBuffer.toString();
    }

    public boolean isRemoveTransactionElementsAttributes() {
        return mRemoveTransactionElementsAttributes;
    }

    /**
     * Sets the flag to remove the Transaction element attributes if any exist.
     * Transaction attributes can be used to identify the Transaction with a number
     *    E.g.  <Transaction nr="0">
     *
     * The attributes will be ignored by AbaConnect but can be used to identify the Transaction if errors occur.
     *
     * @param removeTransactionElementsAttributes set to true to remove Transaction element attributes, other false (default is false)
     */
    public void setRemoveTransactionElementsAttributes(boolean removeTransactionElementsAttributes) {
        mRemoveTransactionElementsAttributes = removeTransactionElementsAttributes;
    }

    private String convertTextToXmlCompatibleString(String text) {
        if ( text == null ) return "";
        String retText = text;
        retText = retText.replaceAll("&","&amp;");
        retText = retText.replaceAll("<","&lt;");
        retText = retText.replaceAll(">","&gt;");
        retText = retText.replaceAll("\"","&quot;");
        retText = retText.replaceAll("'","&apos;");
        return retText;
    }
}
