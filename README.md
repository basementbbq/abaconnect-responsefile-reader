# abaconnect-responsefile-reader
This is a simple JAVA project to read an ABACUS AbaConnect Response File and decipher the Messages and correlate them to AbaConnect XML-Import File



The program reads an ABACUS AbaConnect Response File and extracts the messages from the response file and correlates them to the AbaConnect XML import file.
A new import file containing the erroneous data transactions can be created.

Please note that the path of the Import XML file is automatically read from the Response file.  If the files have been moved to another directory for processing, the path may have to be corrected.


The program can also be used from the commandline with the following options :
   java -classpath ac_response_file_reader.jar -Xms128m -Xmx384m ch.abacus.abaconnecttools.AbaConnectResponseFileReader "-echoERROR-WARNING" "%RESPONSE_FILE_NAME%" "<new_xml_import_file_containing_errors>.xml"
  
   


