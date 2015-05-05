/*
 * CompareTwoFilesFrame.java
 *
 * Creator:
 * 05.01.11 10:00 Sippel
 *
 * Maintainer:
 * 05.01.11 10:00 Sippel
 *
 * Last Modification:
 * $Id: CompareTwoFilesFrame.java 25459 2012-05-11 07:47:24Z sippel $
 *
 * Copyright (c) 2003 ABACUS Research AG, All Rights Reserved
 */
package ch.abacus.abaconnecttools;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

public class AcResponseFileReaderFrame extends JFrame {

    private boolean mUseTabPages = false;

    static public String FILETYPE_DEFINITION_FILE = "XML-File";

    private Object[] mFileTypeDefinitions = new Object[] {
            new String[] {FILETYPE_DEFINITION_FILE, "xml", "XML Files"},
    };


    private String mTitleBarCaption = "AbaConnect Response File Reader";
    private String mProgramVersion = "JA-2014.02";
    private String mFilename1Caption = "Response File :";

    private JTextField m_txfFileName1 = null;

    private JButton mBtnStart = null;

    private String m_StartPath = null;

    private JTextPane m_txpInfoBox = null;

    private boolean mResponseFileProcessed = false;

    private String mErroneousTransactionOutputFilename = null;
    private boolean mShowInformation = false;
    private boolean mShowWarnings = false;
    private boolean mShowErrors = false;

    private JRadioButton m_rdbInformation = null;
    private JRadioButton m_rdbWarning = null;
    private JRadioButton m_rdbError = null;

    public AcResponseFileReaderFrame() {
        initUI();
    }

    public static void main(String[] args) {
        final AcResponseFileReaderFrame caci = new AcResponseFileReaderFrame();
        caci.extractCommandlineParameters(args);

        int xc = 100;
        int yc = 100;
        int width = 900;
        int height = 800;

        caci.setLocation(xc, yc);
        caci.setSize(width, height);
        caci.setVisible(true);
    }

    public void extractCommandlineParameters(String[] args) {
        String filename1 = "";
        String filename2 = "";
        if ( args != null  &&  args.length > 0 ) {
            for (String arg : args) {
                if (arg.toLowerCase().endsWith(".xml") || arg.toLowerCase().endsWith(".zip") ) {
                    if ( "".equals(filename1) ) {
                        filename1 = arg;
                    } else if ( "".equals(filename2) ) {
                        filename2 = arg;
                    }
                }
            }
        }
        setReferenceFilename(filename1);
    }

    public void setReferenceFilename(String referenceFilename) {
        if ( referenceFilename != null && !"".equals(referenceFilename) ) {
            if ( m_txfFileName1 != null && new File(referenceFilename).exists() ) {
                m_txfFileName1.setText(referenceFilename);
                updateUiButtonStatus();
            }
            if ( m_StartPath == null || "".equals(m_StartPath) || !new File(m_StartPath).exists() ) {
                int iPos = referenceFilename.lastIndexOf(File.separator);
                if ( iPos > 0 ) {
                    m_StartPath = referenceFilename.substring(0,iPos);
                }
            }
        }
    }


    private void showMessageDialog(String text) {
        showMessageDialog(text, "AbaConnect Response File Reader", JOptionPane.OK_OPTION);
    }

    private void showMessageDialog(String text, String title, int messageType) {
        JOptionPane.showMessageDialog(this, text, title, messageType);
    }

    public void setTextInfoBox(String text) {
        if ( m_txpInfoBox != null ) {
            m_txpInfoBox.setText(text == null ? "" : text);
            if ( text != null && !"".equals(text) ) {
                // Position the Info Box caret at the top.
                m_txpInfoBox.setCaretPosition(0);
            }
        }
    }


    /**
     * Compares two Strings, and returns the portion where they differ.
     * (More precisely, return the remainder of the second String,
     * starting from where it's different from the first.)
     *
     * For example,
     * <code>difference("i am a machine", "i am a robot") -> "robot"</code>.
     *
     * <pre>
     * StringUtils.difference(null, null) = null
     * StringUtils.difference("", "") = ""
     * StringUtils.difference("", "abc") = "abc"
     * StringUtils.difference("abc", "") = ""
     * StringUtils.difference("abc", "abc") = ""
     * StringUtils.difference("ab", "abxyz") = "xyz"
     * StringUtils.difference("abcde", "abxyz") = "xyz"
     * StringUtils.difference("abcde", "xyz") = "xyz"
     * </pre>
     *
     * @param str1  the first String, may be null
     * @param str2  the second String, may be null
     * @return the portion of str2 where it differs from str1; returns the
     * empty String if they are equal
     * @since 2.0
     */
    public static String difference(String str1, String str2) {
        if (str1 == null) {
            return str2;
        }
        if (str2 == null) {
            return str1;
        }
        int at = indexOfDifference(str1, str2);
        if (at == -1) {
            return "";
        }
        return str2.substring(at);
    }

    /**
     * Compares two Strings, and returns the index at which the
     * Strings begin to differ.
     *
     * For example,
     * <code>indexOfDifference("i am a machine", "i am a robot") -> 7</code>
     *
     * <pre>
     * StringUtils.indexOfDifference(null, null) = -1
     * StringUtils.indexOfDifference("", "") = -1
     * StringUtils.indexOfDifference("", "abc") = 0
     * StringUtils.indexOfDifference("abc", "") = 0
     * StringUtils.indexOfDifference("abc", "abc") = -1
     * StringUtils.indexOfDifference("ab", "abxyz") = 2
     * StringUtils.indexOfDifference("abcde", "abxyz") = 2
     * StringUtils.indexOfDifference("abcde", "xyz") = 0
     * </pre>
     *
     * @param str1  the first String, may be null
     * @param str2  the second String, may be null
     * @return the index where str2 and str1 begin to differ; -1 if they are equal
     * @since 2.0
     */
    public static int indexOfDifference(String str1, String str2) {
        if (str1 == str2) {
            return -1;
        }
        if (str1 == null || str2 == null) {
            return 0;
        }
        int i;
        for (i = 0; i < str1.length() && i < str2.length(); ++i) {
            if (str1.charAt(i) != str2.charAt(i)) {
                break;
            }
        }
        if (i < str2.length() || i < str1.length()) {
            return i;
        }
        return -1;
    }

    /**
     * Compares two Strings, and returns the index at which the
     * Strings begin to differ.
     *
     * For example,
     * <code>indexOfDifference("i am a machine", "i am a robot") -> 7</code>
     *
     * <pre>
     * StringUtils.indexOfDifference(null, null) = -1
     * StringUtils.indexOfDifference("", "") = -1
     * StringUtils.indexOfDifference("", "abc") = 0
     * StringUtils.indexOfDifference("abc", "") = 0
     * StringUtils.indexOfDifference("abc", "abc") = -1
     * StringUtils.indexOfDifference("ab", "abxyz") = 2
     * StringUtils.indexOfDifference("abcde", "abxyz") = 2
     * StringUtils.indexOfDifference("abcde", "xyz") = 0
     * </pre>
     *
     * @param str1  the first String, may be null
     * @param str2  the second String, may be null
     * @return the index where str2 and str1 begin to differ; -1 if they are equal
     * @since 2.0
     */
    public static int lastIndexOfDifference(String str1, String str2) {
        if (str1 == str2) {
            return -1;
        }
        if (str1 == null || str2 == null) {
            return 0;
        }
        int index1 = str1.length() - 1;
        int index2 = str2.length() - 1;
        while (index1 >= 0 && index2 >= 0) {
            if (str1.charAt(index1) != str2.charAt(index2)) {
                break;
            }
            index1--;
            index2--;
        }
        if (index1 >= 0 ) {
            return index1;      // Return the last index of difference in relation to the first string
        }
        return -1;
    }

    class FileNameTransferHandler extends TransferHandler {
        JTextField m_txfFileNameEditField = null;
        FileNameTransferHandler(JTextField txfFilenameEditField) {
            m_txfFileNameEditField = txfFilenameEditField;
        }

        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
//                System.out.println("   Drop canImport");
                if ( transferFlavors == null ) return false;
                if ( transferFlavors.length != 1 ) return false;
                boolean importOk = (transferFlavors[0].isFlavorJavaFileListType() || transferFlavors[0].isFlavorTextType());
                return importOk;
            }

        @Override
        protected Transferable createTransferable(JComponent c) {
            if ( c instanceof JTextField ) {
                JTextField source = (JTextField) c;
                String data = source.getSelectedText();
                return new StringSelection(data);
            }
            return super.createTransferable(c);
        }

        @Override
        public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
            super.exportToClipboard(comp, clip, action);
            if ( action == MOVE && comp instanceof JTextField ) {
                ((JTextField)comp).replaceSelection("");
            }
        }

        @Override
        public int getSourceActions(JComponent c) {
            return (( c instanceof JTextField ) ? COPY_OR_MOVE : super.getSourceActions(c));
        }
            public boolean importData(JComponent comp, Transferable transferable) {
                String droppedFileName = "";
                Object data;
                DataFlavor[] transferFlavors = transferable.getTransferDataFlavors();
                if ( transferFlavors == null ) return false;
                if ( transferFlavors.length > 1 ) {
                    // Handle cut and pastes via the clipboard
                    for (DataFlavor transferFlavor : transferFlavors) {
                        try {
                            // Look for String objects to copy
                            if ( transferFlavor.isFlavorTextType() && transferable.isDataFlavorSupported(transferFlavor) ) {
                                data = transferable.getTransferData(transferFlavor);
                                if ( data instanceof String ) {
                                    droppedFileName = data.toString();
//                                System.out.println(" ** Plain Text : [" + droppedFileName + "]  Flavor[" + transferFlavor.getHumanPresentableName() + "]  Class[" + data.getClass().getName() + "]");
                                    if ( m_txfFileNameEditField != null ) {
                                        m_txfFileNameEditField.replaceSelection(droppedFileName);
                                        updateUiButtonStatus();
                                        return true;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                }
                if ( transferFlavors.length != 1 ) return false;
                // Handle drag and drops via mouse from other programs
                try {
                    data = transferable.getTransferData(transferFlavors[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                if ( data == null ) return false;
                if ( transferFlavors[0].isFlavorJavaFileListType() && data instanceof java.util.List) {
                    java.util.List arrData = (java.util.List) data;
                    if ( arrData.size() == 1 ) {
                        droppedFileName = arrData.get(0).toString();
                    }
                } else if ( transferFlavors[0].isFlavorTextType() ) {
                    droppedFileName = data.toString();
                }
                if ( droppedFileName == null || "".equals(droppedFileName) ) return false;
                if ( m_txfFileNameEditField != null ) {
                    m_txfFileNameEditField.setText(droppedFileName);
                    updateUiButtonStatus();
                } else {
                    System.out.println("Incompatible filename : " + droppedFileName );
                }
                return true;
            }
        }

    private void initUI() {
        Container rootPane = this.getContentPane();
        if ( rootPane == null ) return;
        rootPane.setLayout(new BorderLayout(5,5));

        JPanel pnlMain = new JPanel();
        pnlMain.setLayout(new BoxLayout(pnlMain,BoxLayout.PAGE_AXIS));
        pnlMain.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pnlMain.setMinimumSize(new Dimension(10,10));
        pnlMain.setMaximumSize(new Dimension(9999,9999));
        pnlMain.setPreferredSize(new Dimension(600,500));

        m_txfFileName1 = getTextField();

        m_txfFileName1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                updateUiButtonStatus();
            }
        });


        m_txfFileName1.setTransferHandler(new FileNameTransferHandler(m_txfFileName1));

        JButton btnFile1Select = getButton();
        btnFile1Select.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String fileExtn = "xml";
                String description = "XML Files" + " (*." + fileExtn + ")";
                action_ChooseFile(fileExtn, description, m_txfFileName1);
            }
        });


        pnlMain.add(getFileInputLine(getLabel(mFilename1Caption, 120), m_txfFileName1, btnFile1Select, null));
        pnlMain.add(Box.createVerticalStrut(3));

        int fixedCheckBoxWidth = 300;
        JPanel pnlLine = creatLinePanel();

        mBtnStart = new JButton();
        mBtnStart.setMinimumSize(new Dimension(150,20));
        mBtnStart.setMaximumSize(new Dimension(150, 20));
        mBtnStart.setPreferredSize(new Dimension(150, 20));
        mBtnStart.setText("Start");
        mBtnStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                action_Start();
            }
        });

        JLabel lblResponseMessageLevel = getLabel("Choose the Response Level of the messages that will be echoed :",-1);
        m_rdbInformation = new JRadioButton("Information");
        m_rdbWarning = new JRadioButton("Warning", true);
        m_rdbError = new JRadioButton("Error");
        ButtonGroup rdbGroup = new ButtonGroup();
        rdbGroup.add(m_rdbInformation);
        rdbGroup.add(m_rdbWarning);
        rdbGroup.add(m_rdbError);

        pnlLine.add(Box.createHorizontalStrut(10));
        pnlLine.add(lblResponseMessageLevel);
        pnlLine.add(Box.createHorizontalStrut(20));
        pnlLine.add(Box.createHorizontalGlue());
        pnlLine.add(mBtnStart);

        pnlMain.add(Box.createVerticalStrut(3));
        pnlMain.add(pnlLine);

//        pnlMain.add(Box.createVerticalStrut(3));

        // Options Panel
        int heightOptionPanel = 72;
        JPanel pnlOptionsPanel = new JPanel();
        //pnlOptionsPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE,1));
        pnlOptionsPanel.setLayout(new BoxLayout(pnlOptionsPanel,BoxLayout.LINE_AXIS));
        pnlOptionsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Could add further options in a right option panel
//        JPanel pnlOptionsRightPanel = new JPanel();
//        pnlOptionsRightPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 1));
//        pnlOptionsRightPanel.setLayout(new BoxLayout(pnlOptionsRightPanel, BoxLayout.PAGE_AXIS));
//        pnlOptionsRightPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel pnlOptionsLeftPanel = new JPanel();
        //pnlOptionsLeftPanel.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 1));
        pnlOptionsLeftPanel.setLayout(new BoxLayout(pnlOptionsLeftPanel, BoxLayout.PAGE_AXIS));
        pnlOptionsLeftPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        //pnlOptionsPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        pnlOptionsLeftPanel.setMinimumSize(new Dimension(10, heightOptionPanel));
        pnlOptionsLeftPanel.setMaximumSize(new Dimension(9999, heightOptionPanel));
        pnlOptionsLeftPanel.setPreferredSize(new Dimension(600, heightOptionPanel));

        JPanel pnlPropertyChangesLine = creatLinePanel();
        pnlPropertyChangesLine.add(Box.createHorizontalStrut(10));
        pnlPropertyChangesLine.add(m_rdbInformation);
        pnlPropertyChangesLine.add(Box.createHorizontalStrut(10));
        pnlPropertyChangesLine.add(Box.createHorizontalGlue());

        pnlOptionsLeftPanel.add(Box.createVerticalStrut(3));
        pnlOptionsLeftPanel.add(pnlPropertyChangesLine);

        // ---------- Warning Radio Option -----------
        pnlPropertyChangesLine = creatLinePanel();
        pnlPropertyChangesLine.add(Box.createHorizontalStrut(10));
        pnlPropertyChangesLine.add(m_rdbWarning);
        pnlPropertyChangesLine.add(Box.createHorizontalStrut(10));
        pnlPropertyChangesLine.add(Box.createHorizontalGlue());

        pnlOptionsLeftPanel.add(Box.createVerticalStrut(3));
        pnlOptionsLeftPanel.add(pnlPropertyChangesLine);

        // ---------- Error Radio Option -----------
        pnlPropertyChangesLine = creatLinePanel();
        pnlPropertyChangesLine.add(Box.createHorizontalStrut(10));
        pnlPropertyChangesLine.add(m_rdbError);
        pnlPropertyChangesLine.add(Box.createHorizontalStrut(10));
        pnlPropertyChangesLine.add(Box.createHorizontalGlue());

        pnlOptionsLeftPanel.add(Box.createVerticalStrut(3));
        pnlOptionsLeftPanel.add(pnlPropertyChangesLine);

        pnlOptionsPanel.add(pnlOptionsLeftPanel);
//        pnlOptionsPanel.add(pnlOptionsRightPanel);

        // - Add the options panel -
        pnlMain.add(Box.createVerticalStrut(3));
        pnlMain.add(pnlOptionsPanel);

        pnlMain.add(Box.createVerticalStrut(3));

        // Information Text Box
        m_txpInfoBox = new JTextPane();
        JScrollPane scrPane = new JScrollPane();
        scrPane.setMaximumSize(new Dimension(9999,9999));
        scrPane.setMinimumSize(new Dimension(20,20));
        scrPane.setPreferredSize(new Dimension(700,300));
        scrPane.setViewportView(m_txpInfoBox);

        m_txpInfoBox.setEditable(false);
        m_txpInfoBox.setBackground(new Color(250,250,250));
        Font font = new Font("Courier", Font.BOLD,12);
        if ( font != null ) {
            m_txpInfoBox.setFont(font);
        }

        pnlMain.add(scrPane);

        if ( mUseTabPages ) {
            JPanel pnlAllTabsPanel = new JPanel();
            pnlAllTabsPanel.setLayout(new BoxLayout(pnlAllTabsPanel,BoxLayout.LINE_AXIS));
            pnlAllTabsPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

            JTabbedPane tabPane = new JTabbedPane();
            tabPane.addTab("Main", pnlMain );
            pnlAllTabsPanel.add(tabPane);
            rootPane.add(pnlAllTabsPanel);

        } else {
            rootPane.add(pnlMain);
        }

        setTitle(mTitleBarCaption + " - Version : " + mProgramVersion);

        // Destroy the window when window closes
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        // Default position and size
        int xc = 100;
        int yc = 100;
        int width = 900;
        int height = 800;

        setLocation(xc, yc);
        setSize(width, height);

        updateUiButtonStatus();
    }

    private boolean isInformationSelected() {
        if ( m_rdbInformation == null ) return false;
        return m_rdbInformation.isSelected();
    }

    private boolean isWarningSelected() {
        if ( m_rdbWarning == null ) return false;
        return m_rdbWarning.isSelected();
    }

    private boolean isErrorSelected() {
        if ( m_rdbError == null ) return false;
        return m_rdbError.isSelected();
    }

    private JPanel creatLinePanel() {
        JPanel linePanel = new JPanel();
        linePanel.setLayout(new BoxLayout(linePanel,BoxLayout.LINE_AXIS));
        linePanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        linePanel.setMinimumSize(new Dimension(10,20));
        linePanel.setMaximumSize(new Dimension(9999,20));
        linePanel.setPreferredSize(new Dimension(600, 20));
        return linePanel;
    }

    private JCheckBox createOptionsCheckBox(String captionText, boolean intialCheckedState, int fixWidth, ActionListener actionListener) {
        JCheckBox chkBoxOption = new JCheckBox();
        int minWidth = 10;
        int maxWidth = 9999;
        int prefWidth = 600;
        if ( fixWidth > 0 ) {
            minWidth = fixWidth;
            maxWidth = fixWidth;
            prefWidth = fixWidth;
        }
        chkBoxOption.setMinimumSize(new Dimension(minWidth,20));
        chkBoxOption.setMaximumSize(new Dimension(maxWidth, 20));
        chkBoxOption.setPreferredSize(new Dimension(prefWidth, 20));
        chkBoxOption.setText(captionText);
        chkBoxOption.setSelected(intialCheckedState);
        if ( actionListener != null ) {
            chkBoxOption.addActionListener(actionListener);
        }
        return chkBoxOption;
    }

    private String getInitialInfoText() {
        StringBuilder sbInfo = new StringBuilder();
        String linefeed = "\n";

        sbInfo.append("");
        sbInfo.append("IMPORTANT : PLEASE NOTE THIS PROGRAM IS NOT AN OFFICIAL ABACUS PROGRAM AND IS NOT OFFICIALLY SUPPORTED.");
        sbInfo.append(linefeed);
        sbInfo.append(linefeed);
        sbInfo.append("This program reads an AbaConnect Response file.  The errors from the Response file will be extracted and correlated with the Transaction blocks in the ");
        sbInfo.append("AbaConnect Import file.  An new AbaConnect Import file is created with the transactions that caused errors. ");
        sbInfo.append("The error messages can be written to the new Import File as comments to assist with the correction of the errors.");
        sbInfo.append(linefeed);
        sbInfo.append(linefeed);
        sbInfo.append("The filename of new Import File will be created in the same directory as the original AbaConnect Import File, with a Prefix \"ERRORS_\" (e.g. ERRORS_*.xml).");
        sbInfo.append(linefeed);
        sbInfo.append(linefeed);
        sbInfo.append("The AbaConnect Response file can be selected using a File Chooser or simply drag and drop the file to the program edit field from the file explorer.  ");
        sbInfo.append("The Import file name will be read from the Response file, so if the files have been moved or copied from the original position the files may not be found.");
        sbInfo.append(linefeed);
        sbInfo.append(linefeed);

        sbInfo.append(linefeed);
        return sbInfo.toString();
    }

    public void updateUiButtonStatus() {
        boolean refFileExists = false;
        String refFilename = getReferenceFileName();
        if ( !AbaConnectResponseFileReader.StringIsNullOrEmpty(refFilename) ) {
            refFileExists = new File(refFilename).exists();
        }
        if ( mBtnStart != null ) {
            mBtnStart.setEnabled(refFileExists);
        }
        if ( ! mResponseFileProcessed ) {
            setTextInfoBox(getInitialInfoText());
        }
    }


    public void action_Start() {
        String refFileName = getReferenceFileName();
        System.out.println("File 1 : " + getReferenceFileName() );

        mErroneousTransactionOutputFilename = null;
        mShowInformation = false;
        mShowWarnings = false;
        mShowErrors = true;   // Errors should always be shown
        if ( isInformationSelected() ) {
            // Show Information, Warning and Errors
            mShowInformation = true;
            mShowWarnings = true;
        }
        if ( isWarningSelected() ) {
            // Show Warning and Errors
            mShowWarnings = true;
        }
        mResponseFileProcessed = true;
        ArrayList<String> resultMessages = new ArrayList<String>();
        Cursor currentCursor = this.getCursor();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        int errorCount = AbaConnectResponseFileReader.processResponseFile(refFileName, mErroneousTransactionOutputFilename, mShowInformation, mShowWarnings, mShowErrors, false, resultMessages);
        StringBuilder sb = new StringBuilder();
        for ( String val : resultMessages ) {
            sb.append(val);
            sb.append("\n");
        }
        setTextInfoBox(sb.toString());
        this.setCursor(currentCursor);
    }

    public String getReferenceFileName() {
        return ((m_txfFileName1 == null ? "" : m_txfFileName1.getText()));
    }

    private JPanel getFileInputLine(JLabel lbl, JTextField txf, JButton btn, JButton btnInspect) {
        JPanel pnlLine = new JPanel();
        pnlLine.setLayout(new BoxLayout(pnlLine,BoxLayout.LINE_AXIS));
        pnlLine.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        pnlLine.setMinimumSize(new Dimension(10,20));
        pnlLine.setMaximumSize(new Dimension(9999,20));
        pnlLine.setPreferredSize(new Dimension(600,20));

        pnlLine.add(lbl);
        pnlLine.add(txf);
        pnlLine.add(Box.createHorizontalStrut(3));
        pnlLine.add(btn);
        if ( btnInspect != null ) {
            pnlLine.add(Box.createHorizontalStrut(10));
            pnlLine.add(btnInspect);
        }
        return pnlLine;
    }

    private JLabel getLabel(String caption, int width) {
        JLabel lbl = new JLabel();
        if ( width > 0 ) {
            lbl.setMinimumSize(new Dimension(width,20));
            lbl.setMaximumSize(new Dimension(width,20));
            lbl.setPreferredSize(new Dimension(width,20));
        }
        lbl.setText(caption);
        return lbl;
    }

    private JTextField getTextField() {
        JTextField txf = new JTextField();
        txf.setMinimumSize(new Dimension(10,20));
        txf.setMaximumSize(new Dimension(9999,20));
        txf.setPreferredSize(new Dimension(600,20));
        return txf;
    }

    private JComboBox getComboBox(int width) {
        int comboBoxMinWidth = width > 0 ? width : 10;
        int comboBoxMaxWidth = width > 0 ? width : 9999;
        int comboBoxPrefWidth = width > 0 ? width : 600;
        JComboBox cmbx = new JComboBox();
        cmbx.setMinimumSize(new Dimension(comboBoxMinWidth,18));
        cmbx.setMaximumSize(new Dimension(comboBoxMaxWidth,18));
        cmbx.setPreferredSize(new Dimension(comboBoxPrefWidth,18));
        return cmbx;
    }

    private JButton getButton() {
        JButton btn = new JButton();
        btn.setMinimumSize(new Dimension(20,20));
        btn.setMaximumSize(new Dimension(20,20));
        btn.setPreferredSize(new Dimension(20,20));
        btn.setText("...");
        return btn;
    }

    private JButton getButton(String caption, int width) {
        JButton btn = new JButton();
        btn.setMinimumSize(new Dimension(width,20));
        btn.setMaximumSize(new Dimension(width,20));
        btn.setPreferredSize(new Dimension(width,20));
        btn.setText(caption == null ? "" : caption);
        return btn;
    }

    private void action_ChooseFile(String fileExtension, String fileDescription, JTextField textField) {
        String filename = m_StartPath;
        if ( textField != null ) {
            String tempFilename = textField.getText();
            int iPos = tempFilename.lastIndexOf(File.separator);
            if ( iPos > 0 ) {
                tempFilename = tempFilename.substring(0,iPos);
                if ( new File(tempFilename).exists() ) {
                    filename = tempFilename;
                }
            }
        }
        if ( filename == null || "".equals(filename) ) filename = System.getProperty("user.dir","");
        JFileChooser fc = new JFileChooser(filename);
        String[] projectExtnList = {fileExtension};
        fc.addChoosableFileFilter(new LocalOpenFileFilter(projectExtnList,fileDescription));
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int result = fc.showOpenDialog(this);
        if ( result == JFileChooser.APPROVE_OPTION ) {
            String selectedFileName = fc.getSelectedFile().getPath();
            if ( textField != null ) {
                textField.setText(selectedFileName);
            }
            int iPos = selectedFileName.lastIndexOf(File.separator);
            if ( iPos > 0 ) {
                m_StartPath = selectedFileName.substring(0,iPos);
            }
        }
    }

    class LocalOpenFileFilter extends javax.swing.filechooser.FileFilter {
        private ArrayList mFileExtensions = new ArrayList();
        private String mFileTypeDescription;

        public LocalOpenFileFilter(String[] fileExtensions, String fileTypeDescription) {
            for( int ii = 0; ii < fileExtensions.length; ii++ ) {
                mFileExtensions.add(fileExtensions[ii].toLowerCase());
            }
            mFileTypeDescription = fileTypeDescription;
        }

        public boolean accept(File ff){
            if( ff.isDirectory() ) return true;

            // Must be a file, find if it has an extension, if so then test for a couple of extensions.
            String fileName = ff.getName();
            int locDecimal = fileName.indexOf(".");
            String ext = new String();
//        System.out.println("Filename[" + fileName + "]");
            if( locDecimal > 0 ) {
                StringTokenizer tok = new StringTokenizer(fileName, ".");
                while( tok.hasMoreTokens() ){
                    ext=tok.nextToken();
                }
//            System.out.println("Extension[" + ext + "]");
                for ( Iterator it = mFileExtensions.iterator(); it.hasNext(); ) {
                    if ( ext.equalsIgnoreCase((String)it.next()) ) {
                        return true;
                    }
                }
            }
            return false;
        }

        public String getDescription() {
            return mFileTypeDescription;
        }
    }

    static  public int stringToInteger(String text) {
        return stringToInteger(text, 0);
    }

    static public int stringToInteger(String text, int defaultValue) {
        int number = defaultValue;
        if ( text == null || "".equals(text) ) return number;
        try {
            number = Integer.parseInt(text);
        } catch( NumberFormatException nfe) {
            number = defaultValue;
        }
        return number;
    }


}
