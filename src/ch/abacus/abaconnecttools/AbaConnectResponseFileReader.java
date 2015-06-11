/*
 * AbaConnectResponseFileReader.java  
 *
 * Creator:
 * 05.05.11 11:40 Sippel
 *
 * Maintainer:
 * 05.05.11 11:40 Sippel
 *
 * Last Modification:
 * $Id: AbaConnectResponseFileReader.java 29791 2013-03-26 13:52:41Z sippel $
 *
 * Copyright (c) 2003 ABACUS Research AG, All Rights Reserved
 */
package ch.abacus.abaconnecttools;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;


public class AbaConnectResponseFileReader extends SimpleXmlSaxParser {

    private String mXmlFileName = "";
    private boolean mResponseFileRootTagDetected = false;
    private boolean mInInformationBlock = false;
    private boolean mInStatisticBlock = false;
    private boolean mInTaskStatisticBlock = false;
    private boolean mInTransactionStatisticBlock = false;
    private boolean mInMessagesBlock = false;
    private boolean mInTaskBlock = false;
    private boolean mInTransactionBlock = false;

    private String mTransactionResponseState = "";
    private int mTransactionIndex = -1;
    private int mTaskBlockIndex = -1;

    private String mLastMessageStatus = "";
    private boolean mShowInformationMessages = false;
    private boolean mShowWarningMessages = true;
    private boolean mShowErrorMessages = true;

    private int mTaskInformationMessageCount = -1;
    private int mTaskWarningMessageCount = -1;
    private int mTaskErrorMessageCount = -1;

    private int mTransactionInformationMessageCount = -1;
    private int mTransactionWarningMessageCount = -1;
    private int mTransactionErrorMessageCount = -1;

    private ArrayList<TaskTransactionMessage> mTaskErrorTransactionIndexesAndMessages = new ArrayList<TaskTransactionMessage>();
    private ArrayList<TaskTransactionMessage> mTaskGeneralMessages = new ArrayList<TaskTransactionMessage>();

    private HashMap<Integer, String> mErrorTransactionIndexesAndMessages = new HashMap<Integer, String>();
    private StringBuffer mTransactionMessages = new StringBuffer();
    private StringBuffer mGeneralMessages = new StringBuffer();
    private StringBuffer mEchoedMessages = new StringBuffer();

    private ArrayList<String> mXmlResponseFileName = new ArrayList<String>();   // Name of AbaConnect Response file (e.g. from Import)
    private String mXmlGeneratedErrorImportFileName = "";                       // Name of generated AbaConnect Input File with Error Transactions
    private boolean mShowInformation = false;
    private boolean mShowWarnings = false;
    private boolean mShowErrors = true;

    static String ECHO_MESSAGE_LEVEL_INFO = "Information";
    static String ECHO_MESSAGE_LEVEL_WARNING = "Warning";
    static String ECHO_MESSAGE_LEVEL_ERROR = "Error";

    @Override
    public void endElement(String name, String value) {
        if ( ECHO_MESSAGE_LEVEL_INFO.equals(name) ) {
            mInInformationBlock = false;
        }
        if ( "Statistic".equals(name) ) {
            mInStatisticBlock = false;
            mInTaskStatisticBlock = false;
            mInTransactionStatisticBlock = false;
        }
        if ( "Messages".equals(name) ) {
            mInMessagesBlock = false;
        }
        if ( "Task".equals(name) ) {
            mInTaskBlock = false;
            if ( mGeneralMessages.length() > 0 ) {
                mTaskGeneralMessages.add(new TaskTransactionMessage(mTaskBlockIndex, -1, mGeneralMessages.toString()));
                mGeneralMessages.setLength(0);
            }
        }
        if ( "Transaction".equals(name) ) {
            mInTransactionBlock = false;
            if ( mTransactionIndex >= 0 && ECHO_MESSAGE_LEVEL_ERROR.equalsIgnoreCase(mTransactionResponseState) ) {
//                System.out.println("             Transaction Index[" + mTransactionIndex + "] : State[" + mTransactionResponseState + "]");
                mErrorTransactionIndexesAndMessages.put(mTransactionIndex,mTransactionMessages.toString());

                mTaskErrorTransactionIndexesAndMessages.add(new TaskTransactionMessage(mTaskBlockIndex,mTransactionIndex,mTransactionMessages.toString()));
            }
        }
        if ( mInStatisticBlock ) {
            if ( mInTaskStatisticBlock ) {
                if ( ECHO_MESSAGE_LEVEL_INFO.equals(name) ) {
                    mTaskInformationMessageCount = (int)convertTextToLong(value,-1);
                } else if ( ECHO_MESSAGE_LEVEL_WARNING.equals(name) ) {
                    mTaskWarningMessageCount = (int)convertTextToLong(value,-1);
                } else if ( ECHO_MESSAGE_LEVEL_ERROR.equals(name) ) {
                    mTaskErrorMessageCount = (int)convertTextToLong(value,-1);
                }
            } else if ( mInTransactionStatisticBlock ) {
                if ( ECHO_MESSAGE_LEVEL_INFO.equals(name) ) {
                    mTransactionInformationMessageCount = (int)convertTextToLong(value,-1);
                } else if ( ECHO_MESSAGE_LEVEL_WARNING.equals(name) ) {
                    mTransactionWarningMessageCount = (int)convertTextToLong(value,-1);
                } else if ( ECHO_MESSAGE_LEVEL_ERROR.equals(name) ) {
                    mTransactionErrorMessageCount = (int)convertTextToLong(value,-1);
                }
            }
        }
        if ( mInInformationBlock ) {
            if ( "FileName".equals(name) && StringIsNullOrEmpty(mXmlFileName) ) {
                mXmlFileName = value;
            }
        }
        if ( mInTaskBlock && mInTransactionBlock ) {
            if ( "ResponseState".equals(name) && StringIsNullOrEmpty(mTransactionResponseState) ) {
                mTransactionResponseState = value;
            }
        }

        if ( mInMessagesBlock ) {
            if ( "Message".equals(name) ) {
                if ( mInTransactionBlock ) {
                    if ( mTransactionMessages.length() > 0 ) mTransactionMessages.append("\n");
                    mTransactionMessages.append(value);
                } else {
                    if ( mGeneralMessages.length() > 0 ) mGeneralMessages.append("\n");
                    mGeneralMessages.append(value);
                }
                if ( isMessageStatusLogging() ) {
                    if ( mEchoedMessages.length() > 0 ) mEchoedMessages.append("\n");
                    mEchoedMessages.append(mLastMessageStatus);
                    mEchoedMessages.append("\n");
                    mEchoedMessages.append(value);
                }
            }
        }
    }

    @Override
    public void startElement(String elementName, Attributes atts) {
        if ( "AbaResponse".equals(elementName) ) {
            mResponseFileRootTagDetected = true;
        }
        elementValue.reset();
        if ( ECHO_MESSAGE_LEVEL_INFO.equals(elementName) ) {
            mInInformationBlock = true;
        }
        if ( "Statistic".equals(elementName) ) {
            mInStatisticBlock = true;
            if ( atts != null ) {
                int attIndex = atts.getIndex("mode");
                if ( attIndex >= 0 ) {
                    String attValue = atts.getValue(attIndex);
                    if ( "Task".equals(attValue) ) {
                        mInTaskStatisticBlock = true;
                    } else if ( "Transaction".equals(attValue) ) {
                        mInTransactionStatisticBlock = true;
                    }
                }
            }

        }
        if ( "Messages".equals(elementName) ) {
            mInMessagesBlock = true;
        }
        if ( "Task".equals(elementName) ) {
            mInTaskBlock = true;
            mTaskBlockIndex++;
        }
        if ( "Transaction".equals(elementName) ) {
            mTransactionIndex = -1;
            mTransactionResponseState = "";
            mTransactionMessages.setLength(0);
            if ( atts != null ) {
                int attIndex = atts.getIndex("name");
                if ( attIndex >= 0 ) {
                    String attValue = atts.getValue(attIndex);
                    mTransactionIndex = (int)convertTextToLong(attValue, -1);
                }
            }
            mInTransactionBlock = true;
        }
        if ( mInMessagesBlock ) {
            if ( mInTransactionBlock ) {
                if ( "Message".equals(elementName) ) {
                    mLastMessageStatus = "";
                    if ( mTransactionMessages.length() > 0 ) mLastMessageStatus += ("\n");
                    mLastMessageStatus += getTransactionStatusMessagePrefix();
                    if ( atts != null ) {
                        int attCount = atts.getLength();
                        for ( int attIndex = 0; attIndex < attCount; attIndex++ ) {
                            if ( attIndex > 0 ) mLastMessageStatus += (" ");
                            String attValue = atts.getValue(attIndex);
                            mLastMessageStatus += (atts.getLocalName(attIndex));
                            mLastMessageStatus += ("[");
                            mLastMessageStatus += (attValue);
                            mLastMessageStatus += ("]");
                        }
                        mTransactionMessages.append(mLastMessageStatus);
                    }
                }
            } else {
                if ( "Message".equals(elementName) ) {
                    mLastMessageStatus = "";
                    if ( mGeneralMessages.length() > 0 ) mLastMessageStatus += ("\n");
                    if ( atts != null ) {
                        int attCount = atts.getLength();
                        for ( int attIndex = 0; attIndex < attCount; attIndex++ ) {
                            if ( attIndex > 0 ) mLastMessageStatus += (" ");
                            String attValue = atts.getValue(attIndex);
                            mLastMessageStatus += (atts.getLocalName(attIndex));
                            mLastMessageStatus += ("[");
                            mLastMessageStatus += (attValue);
                            mLastMessageStatus += ("]");
                        }
                        mGeneralMessages.append(mLastMessageStatus);
                    }
                }
            }
        }

    }

    @Override
    public void saxParse(String filename) throws SAXException {
        mXmlFileName = "";
        mInInformationBlock = false;
        mInStatisticBlock = false;
        mInTaskStatisticBlock = false;
        mInTransactionStatisticBlock = false;
        mInMessagesBlock = false;
        mInTaskBlock = false;
        mInTransactionBlock = false;

        mTransactionResponseState = "";
        mTransactionIndex = -1;
        mTaskBlockIndex = -1;

        mErrorTransactionIndexesAndMessages.clear();
        mTaskErrorTransactionIndexesAndMessages.clear();

        super.saxParse(filename);
    }

    public boolean isShowErrors() {
        return mShowErrors;
    }

    public void setShowErrors(boolean showErrors) {
        mShowErrors = showErrors;
    }

    public boolean isShowInformation() {
        return mShowInformation;
    }

    public void setShowInformation(boolean showInformation) {
        mShowInformation = showInformation;
    }

    public String getXmlGeneratedErrorImportFileName() {
        return mXmlGeneratedErrorImportFileName;
    }

    public void setXmlGeneratedErrorImportFileName(String xmlGeneratedErrorImportFileName) {
        mXmlGeneratedErrorImportFileName = xmlGeneratedErrorImportFileName;
    }

    public ArrayList<String> getXmlResponseFileName() {
        return mXmlResponseFileName;
    }

    public void addXmlResponseFileName(String xmlResponseFileName) {
        mXmlResponseFileName.add(xmlResponseFileName);
    }

    public boolean isValidResponseFileDetected() {
        return mResponseFileRootTagDetected;
    }

    public boolean isShowWarnings() {
        return mShowWarnings;
    }

    public void setShowWarnings(boolean showWarnings) {
        mShowWarnings = showWarnings;
    }
    
    public void extractCommandlineParameters(String[] args) {
        if ( args != null  &&  args.length > 0 ) {
            for (String arg : args) {
                if ( arg.toLowerCase().startsWith("-outputfile") || arg.toLowerCase().startsWith("/outputfile") ) {
                    mXmlGeneratedErrorImportFileName = arg.substring(11);
                } else if ( arg.toLowerCase().endsWith(".xml")) {
                    mXmlResponseFileName.add(arg);
                }
                if ( (arg.toLowerCase().startsWith("-echo") || arg.toLowerCase().startsWith("/echo")) ) {
                    String echoParam = arg.substring(5);
                    if ( !StringIsNullOrEmpty(echoParam) ) {
                        setShowErrors(true);
                        if ( echoParam.toLowerCase().contains("info") ) {
                            setShowInformation(true);
                            setShowWarnings(true);
                        } else if ( echoParam.toLowerCase().contains("warn") ) {
                            setShowInformation(false);
                            setShowWarnings(true);
                        } else {
                            setShowInformation(false);
                            setShowWarnings(false);
                        }
                    }
                }
            }
        }

    }

    
    private String getTransactionStatusMessagePrefix() {
        String transactionText;
        if ( mTransactionIndex >= 0 ) {
            transactionText = "Transaction[" + mTransactionIndex + "] : ";
        } else {
            transactionText = "General Message : ";
        }
        return transactionText;
    }

    private boolean isMessageStatusLogging() {
        if ( StringIsNullOrEmpty(mLastMessageStatus) ) return true;
        if (  mLastMessageStatus.toLowerCase().contains("info") && mShowInformationMessages ) return true;
        if (  mLastMessageStatus.toLowerCase().contains("warn") && mShowWarningMessages ) return true;
        return ( mLastMessageStatus.toLowerCase().contains("error") && mShowErrorMessages);
    }
    
    public int getTaskBlockCount() {
        return mTaskBlockIndex < 0 ? 0 : (mTaskBlockIndex + 1);
    }

    public int getTaskInformationMessageCount() {
        return mTaskInformationMessageCount;
    }

    public int getTaskWarningMessageCount() {
        return mTaskWarningMessageCount;
    }

    public int getTaskErrorMessageCount() {
        return mTaskErrorMessageCount;
    }

    public int getTransactionInformationMessageCount() {
        return mTransactionInformationMessageCount;
    }

    public int getTransactionWarningMessageCount() {
        return mTransactionWarningMessageCount;
    }

    public int getTransactionErrorMessageCount() {
        return mTransactionErrorMessageCount;
    }

    public boolean hasErrorTransactions(int taskIndex) {
        for ( TaskTransactionMessage val : mTaskErrorTransactionIndexesAndMessages ) {
            if ( val.mTaskIndex == taskIndex && val.mTransactionIndex >= 0 ) {
                return true;
            }
        }
        return false;
    }

//    public boolean hasErrorTransactions() {
//        return (mErrorTransactionIndexesAndMessages.size() > 0);
//    }

    public void setShowInformationMessages(boolean showInformationMessages) {
        mShowInformationMessages = showInformationMessages;
    }

    public void setShowWarningMessages(boolean showWarningMessages) {
        mShowWarningMessages = showWarningMessages;
    }

    public void setShowErrorMessages(boolean showErrorMessages) {
        mShowErrorMessages = showErrorMessages;
    }

    
//    public int[] getErrorTransactionIndexes() {
//        int[] errorTransactionIndexes = new int[mErrorTransactionIndexesAndMessages.size()];
//        int index = 0;
//        for ( Integer val : mErrorTransactionIndexesAndMessages.keySet() ) {
//            errorTransactionIndexes[index] = val;
//            index++;
//
//        }
//        return errorTransactionIndexes;
//    }

    public Integer[] getErrorTransactionIndexes(int taskIndex) {
        ArrayList<Integer> errorTransactionIndexes = new ArrayList<Integer>();
        for ( TaskTransactionMessage val : mTaskErrorTransactionIndexesAndMessages ) {
            if ( val.mTaskIndex == taskIndex ) {
                errorTransactionIndexes.add(val.mTransactionIndex);
            }
        }
        return errorTransactionIndexes.toArray(new Integer[errorTransactionIndexes.size()]);
    }

    public String getGeneralMessages() {
        return mGeneralMessages.toString();
    }

    public String getGeneralMessages(int taskIndex) {
        for ( TaskTransactionMessage val : mTaskGeneralMessages ) {
            if ( val.mTaskIndex == taskIndex ) {
                return val.mMessage;
            }
        }
        return "";
    }

    public String getEchoedMessages() {
        return mEchoedMessages.toString();
    }

//    public HashMap<Integer, String> getErrorTransactionIndexesAndMessages() {
//        return  mErrorTransactionIndexesAndMessages;
//    }

    public HashMap<Integer, String> getErrorTransactionIndexesAndMessages(int taskIndex) {
        HashMap<Integer, String> errorTransactions = new HashMap<Integer, String>();
        for ( TaskTransactionMessage val : mTaskErrorTransactionIndexesAndMessages ) {
            if ( val.mTaskIndex == taskIndex ) {
                errorTransactions.put(val.mTransactionIndex, val.mMessage);
            }
        }
        return  errorTransactions;
    }

    public ArrayList<TaskTransactionMessage> getAllTaskErrorTransactionIndexesAndMessages() {
        return  mTaskErrorTransactionIndexesAndMessages;
    }

    public String getXmlFileName() {
        return mXmlFileName;
    }

    public void setXmlFileName(String xmlFileName) {
        mXmlFileName = xmlFileName;
    }

    static public long convertTextToLong(String text, long defaultValue)
    {
        long val;
        try
        {
            val = Long.parseLong(text);
        }
        catch (NumberFormatException nfe)
        {
            val = defaultValue;
        }
        return val;
    }

    static public boolean StringIsNullOrEmpty(String text) {
        return( text == null || "".equals(text) );
    }

    public static void main(String[] args) {
        String responseFileName = "";
        String echoParams = "";

        AbaConnectResponseFileReader acResponseReader = new AbaConnectResponseFileReader();

        acResponseReader.extractCommandlineParameters(args);

        ArrayList<String> responseFileList = acResponseReader.getXmlResponseFileName();
        if ( responseFileList != null ) {
            if ( responseFileList.size() > 0 ) {
                responseFileName = responseFileList.get(responseFileList.size()-1);
            }
        }

        String erroneousTransactionOutputFilename = acResponseReader.getXmlGeneratedErrorImportFileName();

        if ( args != null  &&  args.length > 0 ) {
            for (String arg : args) {
                if ( StringIsNullOrEmpty(responseFileName) && arg.toLowerCase().endsWith(".xml")) {
                    responseFileName = arg;
                }
                if ( StringIsNullOrEmpty(erroneousTransactionOutputFilename) && (arg.toLowerCase().startsWith("-outputfile") || arg.toLowerCase().startsWith("/outputfile")) ) {
                    erroneousTransactionOutputFilename = arg.substring(11);
                }
                if ( StringIsNullOrEmpty(echoParams) && (arg.toLowerCase().startsWith("-echo") || arg.toLowerCase().startsWith("/echo")) ) {
                    echoParams = arg;
                }
            }
        }

        if ( !StringIsNullOrEmpty(responseFileName) ) {
            if ( ! new File(responseFileName).exists() ) {
                System.out.println("");
                System.out.println("Error : Specified response file name cannot be found.");
                System.out.println("       " + responseFileName);
            }
        }
        if ( responseFileList.size() < 1 ) {
            System.out.println(acResponseReader.getCommandlineUsage());

//        java -cp ac_utilities.jar -Xms128m -Xmx384m ch.abacus.abaconnecttools.AbaConnectResponseFileReader d:\test\debi\BatchFiles\DEBI_Import_Customer_m7777_Result.xml
            return;
        }

        boolean showInformation = acResponseReader.isShowInformation();
        boolean showWarnings = acResponseReader.isShowWarnings();
        boolean showErrors = acResponseReader.isShowErrors();
        
         String outputPath = "";
        // Normally there is only one file being processed when started from commandline
        // For testing purposes there can be more files to process.
        int totalErrorCount = 0;
        boolean processingMultipleFiles = responseFileList.size() > 1;
        for ( String xmlResponseFilename : responseFileList ) {
            if ( StringIsNullOrEmpty(outputPath) ) {
                outputPath = xmlResponseFilename;
                int iLastSlashPos = outputPath.lastIndexOf(File.separator);
                if ( iLastSlashPos > 0 ) {
                    outputPath = outputPath.substring(0,iLastSlashPos+1);
                } else {
                    outputPath = "";
                }
            }
            int errorCount = processResponseFile(xmlResponseFilename, erroneousTransactionOutputFilename, showInformation, showWarnings, showErrors );
            if ( processingMultipleFiles ) {
                if ( errorCount > 0 ) {
                    totalErrorCount += errorCount;
                }
            } else {
                // For single file processing return the error count returned from the processing
                totalErrorCount = errorCount;
            }
        }
        // Write the error count in an "ERROR_COUNT.log" file.  This could be read by another program for further processing
        try {
            FileOutputStream foutError = new FileOutputStream(outputPath + "ERROR_COUNT.log");
            foutError.write(String.valueOf(totalErrorCount).getBytes());
            foutError.write("\n".getBytes());
            foutError.flush();
            foutError.close();
        } catch (Exception allex) {
            allex.printStackTrace();
        }
   
    }

    static public String getCommandlineUsage() {
        String lineFeed = "\n";
        StringBuilder sb = new StringBuilder();

        sb.append("");
        sb.append(lineFeed);
        sb.append("Usage : ");
        sb.append(lineFeed);
        sb.append("   java -cp ac_utilities.jar -Xms128m -Xmx384m ch.abacus.abaconnecttools.AbaConnectResponseFileReader [-echoINFO-WARN-ERROR] <response-filename> <error-result-filename>");
        sb.append(lineFeed);
        sb.append("Where : ");
        sb.append(lineFeed);
        sb.append("   <response-filename>     - is AbaConnect response file<error-result-filename>");
        sb.append(lineFeed);
        sb.append("   <error-result-filename> - is result file with erroneous transactions");
        sb.append(lineFeed);
        sb.append("   -echo                   - an optional parameter. Echoes Message types INFO-WARN-ERROR to DOS Window.  ERROR messages will always be shown.");
//        sb.append(lineFeed);
        return sb.toString();

    }

    static public int processResponseFile(String xmlResponseFilename, String erroneousTransactionOutputFilename, boolean showInformation, boolean showWarnings, boolean showErrors ) {
        return processResponseFile(xmlResponseFilename, erroneousTransactionOutputFilename, showInformation, showWarnings, showErrors, true, null );
    }

    static public int processResponseFile(String xmlResponseFilename, String erroneousTransactionOutputFilename, boolean showInformation, boolean showWarnings, boolean showErrors, boolean echoToSystemOut, ArrayList<String> resultMessages ) {
        int errorCount = -1;
        if ( StringIsNullOrEmpty(xmlResponseFilename) ) return errorCount;

        AbaConnectResponseFileReader acrfr = new AbaConnectResponseFileReader();
        AbaConnectXmlFileReader acxfr = new AbaConnectXmlFileReader();

        boolean erroneousTransactionFileProcessed = false;
        String xmlFileName = "";
        if ( new File(xmlResponseFilename).exists() ) {
                String echoedMessages = "";
                String lineIndent = "";
                boolean errorsDetected = false;
                try {
                    acrfr.setShowInformationMessages(showInformation);
                    acrfr.setShowWarningMessages(showWarnings);
                    acrfr.setShowErrorMessages(showErrors);

                    acrfr.saxParse(xmlResponseFilename);

                    xmlFileName = acrfr.getXmlFileName();

                    int taskInformationMessageCount = acrfr.getTaskInformationMessageCount();
                    int taskWarningMessageCount = acrfr.getTaskWarningMessageCount();
                    int taskErrorMessageCount = acrfr.getTaskErrorMessageCount();

                    int transactionInformationMessageCount = acrfr.getTransactionInformationMessageCount();
                    int transactionWarningMessageCount = acrfr.getTransactionWarningMessageCount();
                    int transactionErrorMessageCount = acrfr.getTransactionErrorMessageCount();

                    ArrayList<String> headerText = new ArrayList<String>();
                    headerText.add("");
                    headerText.add("====================================================");
                    headerText.add(lineIndent + "      Response File : " + xmlResponseFilename);
                    headerText.add(lineIndent + "           Xml File : " + (StringIsNullOrEmpty(xmlFileName) ? "NO XML FILENAME" : xmlFileName ));
                    if ( StringIsNullOrEmpty(xmlFileName) ) {
                        headerText.add(lineIndent + "           WARNING : The Xml File is not defined in the Response File");
                        headerText.add(lineIndent + "                     In this case, a transaction file cannot be created");
                    } else if ( !new File(xmlFileName).exists() ) {
                        headerText.add(lineIndent + "           WARNING : The Xml File cannot be located.  The files may have been relocated.");
                        headerText.add(lineIndent + "                     In this case, a transaction file cannot be created");
                    }
                    headerText.add(lineIndent + "Task Info - ");
                    headerText.add(lineIndent + "=========================");
                    headerText.add(lineIndent + "        Information : " + taskInformationMessageCount);
                    headerText.add(lineIndent + "           Warnings : " + taskWarningMessageCount);
                    headerText.add(lineIndent + "             Errors : " + taskErrorMessageCount);
                    headerText.add(lineIndent + "Transaction Statistics - ");
                    headerText.add(lineIndent + "=========================");
                    headerText.add(lineIndent + "        Information : " + transactionInformationMessageCount);
                    headerText.add(lineIndent + "           Warnings : " + transactionWarningMessageCount);
                    headerText.add(lineIndent + "             Errors : " + transactionErrorMessageCount);

                    errorCount = 0;
                    String generalMessages = "";
                    int taskBlockCount = acrfr.getTaskBlockCount();
                    headerText.add(lineIndent + "        Task Count  : " + taskBlockCount );
                    for( int taskIndex = 0; taskIndex < taskBlockCount; taskIndex++ ) {
                        int taskErrorCount = 0;
                        Integer[] errorTransactionIndexes = acrfr.getErrorTransactionIndexes(taskIndex);
                        if (errorTransactionIndexes.length > 0) {
                            taskErrorCount = errorTransactionIndexes.length;
                            if (transactionErrorMessageCount != taskErrorCount) {
                                headerText.add(lineIndent + "     Task " + String.format("%2d", taskIndex) + " Errors : " + taskErrorCount);
                            }
                            errorsDetected = true;
                        } else {
                            generalMessages = acrfr.getGeneralMessages(taskIndex);
                            if (StringIsNullOrEmpty(generalMessages)) {
                                headerText.add(lineIndent + "     Task " + String.format("%2d", taskIndex) + " Errors : NO ERRORS DETECTED");
                            } else {
                                errorsDetected = true;
                                headerText.add(lineIndent + "     Task " + String.format("%2d", taskIndex) + " Errors : GENERAL ERRORS DETECTED");
                            }
                        }
                        errorCount += taskErrorCount;
                    }
                    // Messages to be echoed to DOS Window via System.out
                    echoedMessages = acrfr.getEchoedMessages();

                    if ( !StringIsNullOrEmpty(xmlFileName) ) {
                        File acXmlFile = new File(xmlFileName);
                        if ( errorsDetected && acXmlFile.exists() ) {
                            if ( acXmlFile.getName().toLowerCase().endsWith(".zip") ) {
                                headerText.add("Transactions cannot be extracted from a ZIP file format : " + acXmlFile.getName());
                            } else {
                                String xmlOutputFileName = erroneousTransactionOutputFilename;
                                if ( StringIsNullOrEmpty(xmlOutputFileName) ) {
                                    xmlOutputFileName = acXmlFile.getPath();
                                    int ipos = xmlOutputFileName.indexOf(acXmlFile.getName());
                                    if ( ipos >= 0 ) {
                                        xmlOutputFileName = xmlOutputFileName.substring(0,ipos);
                                        xmlOutputFileName += "ERRORS_";
                                        xmlOutputFileName += acXmlFile.getName();
                                    }
                                }
                                headerText.add(lineIndent + " Error Transactions : " + xmlOutputFileName);

                                //acxfr.setRemoveTransactionElementsAttributes();
                                acxfr.setXmlOutputFileName(xmlOutputFileName);
                                acxfr.setGeneralMessages(generalMessages);
                                // acxfr.setErrorTransactionIndexesAndMessages(acrfr.getErrorTransactionIndexesAndMessages());
                                acxfr.setTaskErrorTransactionIndexesAndMessages(acrfr.getAllTaskErrorTransactionIndexesAndMessages());
                                acxfr.saxParse(xmlFileName);
                                erroneousTransactionFileProcessed = true;
                            }
                        }

                        if ( echoToSystemOut ) {
                            for( String line : headerText ) {
                                System.out.println(line);
                            }
                        }
                        if ( resultMessages != null ) {
                            for( String line : headerText ) {
                                resultMessages.add(line);
                            }
                        }
                    }
                } catch (SAXException e) {
                    e.printStackTrace();
                }

                // Messages to be echoed to DOS Window via System.out
                if ( !StringIsNullOrEmpty(echoedMessages) ) {
                    String message = lineIndent + "    Echoed Messages :";
                    message += (showInformation ? " [Information]" : "");
                    message += (showWarnings ? " [Warning]" : "");
                    message += (showErrors ? " [Error]" : "");

                    if ( echoToSystemOut ) {
                        System.out.print(message);

                        System.out.println("");
                        System.out.println("Messages from Response File : ");
                        System.out.println("-----------------------------------------");
                        outputTextToCodedDosWindow(echoedMessages);
                    }
                    if ( resultMessages != null ) {
                        resultMessages.add(message);

                        resultMessages.add("");
                        resultMessages.add("Messages from Response File : ");
                        resultMessages.add("-----------------------------------------");
                        resultMessages.add(echoedMessages);
                    }
                }
        }
        if ( erroneousTransactionFileProcessed ) {

        }
        return errorCount;
    }


    /**
     * In DOS Window the umlaut characters cannot be correctly displayed unless the coded page is used.
     * This method uses a coded Charset OutputStream to display umlaut characters in the DOS Window Console
     *
     * @param text original text
     */
    public static void outputTextToCodedDosWindow(String text) {

        PrintWriter out = null;
        String codepage = "Cp850";
        try
        {
            out = new PrintWriter(new OutputStreamWriter(System.out, codepage));

            /* Output the text to the DOS Window Console */
//            out.println("Test mit deutschen Spezialzeichen:  äöü ÄÖÜ ß");
            out.println(text);

            /* Use flush to show the stream contents in the DOS Window Console */
            out.flush();
//            out.close();  // Do not close the stream after - otherwise all subsequent output to System.out will also be suppressed
        }
        catch (UnsupportedEncodingException e)
        {
            System.out.println("The OutputStream for the coded character set " + codepage + " could not be created:\n" + e.getMessage());
        }
    }
}
