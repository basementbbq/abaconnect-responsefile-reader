/*
 * CheckXmlFileSyntax.java  
 *
 * Creator:
 * 20/08/12 11:14 AM Sippel
 *
 * Maintainer:
 * 20/08/12 11:14 AM Sippel
 *
 * Last Modification:
 * $Id: $
 *
 * Copyright (c) 2003 ABACUS Research AG, All Rights Reserved
 */
package ch.abacus.abaconnecttools;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.*;

public class CheckXmlFileSyntax {

    private long mTransactionCount = 0;

    public static void main(String[] args) {
        long splitSize = -1;
        String filename = "";

        if ( args != null  &&  args.length > 0 ) {
            for (String arg : args) {
                if ( StringIsNullOrEmpty(filename) && arg.toLowerCase().endsWith(".xml")) {
                    if ( new File(arg).exists() ) {
                        filename = arg;
                    }
                }
                if ( (arg.toLowerCase().startsWith("-split") || arg.toLowerCase().startsWith("/split")) ) {
                    splitSize = convertTextToLong(arg.substring(6), -1);
                }
            }
        }
        if ( splitSize < 50 ) {
            splitSize = -1;
        }
        if ( StringIsNullOrEmpty(filename) ) {
            System.out.println("");
            System.out.println("Usage : ");
            System.out.println("   java -cp ac_utilities.jar -Xms128m -Xmx384m ch.abacus.abaconnecttools.CheckXmlFileSyntax [-split<split-number>] <xml-filename>");
            System.out.println("Where : ");
            System.out.println("   <xml-filename>    - is the original large import file to be split");
            System.out.println("  OPTIONAL :");
            System.out.println("   <split-number>    - is the max number of Transactions in each split file (minimum is 50))");
            System.out.println("");
            System.out.println("If the \"-split\" parameter is not defined. the XML file will only be check for validity.");
            System.out.println("");
            System.out.println("This utility can be used to divide a large AbaConnect Import file into smaller files.  The");
            System.out.println("result files have the same name as the import file with an additional index number appended.");
            System.out.println("");
            System.out.println("Please Note : This utility is provided with no guarantees. The full source code is available,");
            System.out.println("and problems can be corrected.");

//        java -cp ac_utilities.jar -Xms128m -Xmx384m ch.abacus.abaconnecttools.AbaConnectResponseFileReader d:\test\debi\BatchFiles\DEBI_Import_Customer_m7777_Result.xml
            return;
        }

        //String filename = "d:\\data\\abaconnect\\test\\kred\\import_ascii_antinio_chiafala\\KRED6295_2283.xml";
//        String filename = "d:\\data\\abaconnect\\test\\scmm\\2012_12_19_serviceobject_SVM-5738\\jira-save\\edited_serviceobjektestruktur.xml";
//        String filename = "d:\\data\\abaconnect\\test\\kred\\import_ascii_antinio_chiafala\\KRED6295_2279.xml";

        CheckXmlFileSyntax chkXml = new CheckXmlFileSyntax();

        System.out.println("Checking the Syntax of file :");
        System.out.println("     " + filename );
        System.out.println("");
        System.out.println("Depending on the file size this could take a few seconds.  Please wait.");
        boolean fileSyntaxOk = chkXml.parseXmlFile(filename);
        if ( fileSyntaxOk ) {
            System.out.println("The file Syntax was OK");
        } else {
            System.out.println("ERROR : The file could not be parsed successfully.");
            System.out.println("        The XML file structure contains errors and cannot be read.");
        }
        long transactionCount = chkXml.getTransactionCount();
        System.out.println("Transaction Count = " + transactionCount);

        if ( splitSize > 0 && fileSyntaxOk ) {
            System.out.println("");
            System.out.println("Splitting file into smaller files of maximum " + splitSize + " Transactions.  Please wait.");
            int numberOfSplitFiles = chkXml.splitXmlFile(filename, splitSize);
            System.out.println("The input file : ");
            System.out.println("     " + filename );
            System.out.println("was split into " + numberOfSplitFiles + " separate files.");
        }
    }

    public boolean parseXmlFile(String filename) {

        boolean fileOK = false;
        SimpleXmlSaxParser xmlParser = new SimpleXmlSaxParser() {
            @Override
            public void endElement(String name, String value) {
            }

            @Override
            public void startElement(String elementName, Attributes atts) {
                if ( "Transaction".equals(elementName) ) {
                    mTransactionCount++;
                }
            }

            @Override
            public void startDocument() {
                super.startDocument();    //To change body of overridden methods use File | Settings | File Templates.
            }
        };

        resetTransactionCount();
        try {
            xmlParser.saxParse(filename);
            fileOK = true;
        } catch (SAXParseException e) {
            System.out.println("Message from SAXParseException : " + e.getMessage() );
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            if ( e.getCause() != null ) {
                System.out.println("Error Message from SAX Parser : " + e.getCause().getMessage() );
            }
            e.printStackTrace();
        }
        return fileOK;
    }

    public long getTransactionCount() {
        return mTransactionCount;
    }

    public void resetTransactionCount() {
        mTransactionCount = 0;
    }

    public int splitXmlFile(String filename, long splitSize) {
        int fileSplitCount = 0;
        if ( filename == null || "".equals(filename) ) {
            return fileSplitCount;
        }

        boolean lastabaConnectContainerWritten = false;
        long transactionCount = 0;
        long transactionSplitCount = 0;

        String fileExtension = ".xml";
        String baseOutputFileBase = filename;
        int ipos = baseOutputFileBase.lastIndexOf(".");
        if ( ipos > 0 ) {
            fileExtension = baseOutputFileBase.substring(ipos);
            baseOutputFileBase = baseOutputFileBase.substring(0,ipos);
        }

        String outputPutFilename;
        if ( new File(filename).exists() ) {
            try {
                BufferedOutputStream outs = null;

                StringBuilder sbHeader = new StringBuilder();

                String lineFeed = "\r\n";

                FileInputStream inStream = new FileInputStream(filename);
                BufferedReader inputFile = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
                String lineText = inputFile.readLine();
                while (lineText != null ) {
                    if ( lineText.contains("<Transaction>") ) {
                        transactionCount++;
                        transactionSplitCount++;
                    }
                    if ( transactionCount == 0 ) {
                        sbHeader.append(lineText);
                        sbHeader.append(lineFeed);
                    }
                    if ( transactionSplitCount >= splitSize ) {
                        // Close the file when the number of transactions is reached
                        if ( outs != null ) {
                            outs.write("  </Task>".getBytes("UTF-8"));
                            outs.write(lineFeed.toString().getBytes("UTF-8"));
                            outs.write("</AbaConnectContainer>".getBytes("UTF-8"));
                            outs.write(lineFeed.toString().getBytes("UTF-8"));

                            outs.close();
                            outs = null;
                            transactionSplitCount = 0;
                        }
                    }
                    if ( outs == null ) {
                        outputPutFilename = baseOutputFileBase + "_" + String.format("%04d",(fileSplitCount+1)) + fileExtension;
                        fileSplitCount++;
                        outs = new BufferedOutputStream(new FileOutputStream(outputPutFilename,false));
                        if ( transactionCount > 0 ) {
                            outs.write(sbHeader.toString().getBytes("UTF-8"));
                            outs.write(lineFeed.toString().getBytes("UTF-8"));
                        }
                    }
                    if ( outs != null ) {
                        outs.write(lineText.toString().getBytes("UTF-8"));
                        outs.write(lineFeed.toString().getBytes("UTF-8"));
                        if ( lineText.contains("</AbaConnectContainer>") ) {
                            lastabaConnectContainerWritten = true;
                        }
                    }
                    lineText = inputFile.readLine();
                }
                inputFile.close();

                if ( outs != null ) {
                    // Write End if not already written - generally it will be written already
                    if ( ! lastabaConnectContainerWritten ) {
                        outs.write("  </Task>".getBytes("UTF-8"));
                        outs.write(lineFeed.toString().getBytes("UTF-8"));
                        outs.write("</AbaConnectContainer>".getBytes("UTF-8"));
                        outs.write(lineFeed.toString().getBytes("UTF-8"));
                    }
                    outs.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return fileSplitCount;
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

}