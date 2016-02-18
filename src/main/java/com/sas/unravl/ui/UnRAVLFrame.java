package com.sas.unravl.ui;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.sas.unravl.ApiCall;
import com.sas.unravl.Main;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.UnRAVLRuntime;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.springframework.http.HttpStatus;
import org.apache.http.Header;

/**
 * A basic user interface for running an UnRAVL script.
 *
 * @author David.Biesack@sas.com
 */
public class UnRAVLFrame extends JFrame {

    private static final String NONE = "<none>"; // NOI18N
    private static final String SCRIPT_SOURCE_PREFERENCE_KEY = "unravl.script.source"; // NOI18N
    private static final long serialVersionUID = 1L;
    private final UndoManager undoManager;
    private static final int SOURCE_TAB = 0;
    private static final int OUTPUT_TAB = 1;
    private int callIndex = 0;
    ResourceBundle resources = ResourceBundle
            .getBundle("com/sas/unravl/ui/Resources");
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());

    /**
     * Creates new form UnRAVLFrame
     */
    public UnRAVLFrame() {
        undoManager = new UndoManager();
        initComponents();
        postInitComponents();
    }

    // Additional initialization done after initComponents();
    private void postInitComponents() {
        tabs.setSelectedIndex(SOURCE_TAB);
        varName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateVarTab();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateVarTab();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateVarTab();
            }
        });
        jsonSourceTextArea.getDocument().addUndoableEditListener(
                new MyUndoableEditListener());
        reset();
    }

    private void setHeaders(List<Header> headers, JTextArea textArea) {
        textArea.setText(""); // NOI18N
        if (headers != null) {
            for (Header header : headers) {
                String name = header.getName();
                String value = header.getValue();
                textArea.append(name);
                textArea.append(": "); // NOI18N
                textArea.append(value);
                textArea.append("\n"); // NOI18N
            }
            textArea.setCaretPosition(0);
        }
    }

    private void zoom(int increment) {
        Font font = jsonSourceTextArea.getFont();
        if (font.getSize() + increment > 4) {
            Font resizedFont = new Font(font.getName(), font.getStyle(),
                    font.getSize() + increment);
            for (JTextArea ta : textAreas)
                ta.setFont(resizedFont);
        }
    }

    private class MyUndoableEditListener implements UndoableEditListener {

        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            undoManager.addEdit(e.getEdit());
            // undoAction.updateUndoState();
            // redoAction.updateRedoState();
        }
    }

    private UnRAVLRuntime runtime;

    private void reset() {
        runtime = new UnRAVLRuntime();
        changedVars = new LinkedHashMap<String, Object>();
        runtime.addPropertyChangeListener(new RuntimePropertyChangeListener());
        updateVarTab();
        updateCallsTab();
    }

    public String scriptTemplate() {
        String script = prefs.get(SCRIPT_SOURCE_PREFERENCE_KEY, null);
        if (script != null) {
            return script;
        }
        try (InputStream is = this.getClass().getResourceAsStream(
                "newScript.json")) { // NOI18N
            StringBuilder b = new StringBuilder();
            Reader r = new InputStreamReader(is);
            for (int c = r.read(); c >= 0; c = r.read()) {
                b.append((char) c);
            }
            return b.toString();
        } catch (IOException | NullPointerException e) {
            return "{\n}"; // NOI18N
        }
    }

    private ApiCall call;

    void addHandlers() {
        jsonSourceTextArea.getDocument().addDocumentListener(
                new DocumentListener() {
                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        onSourceChange();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        onSourceChange();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        onSourceChange();
                    }
                });

        addUndoRedoToSourceTextArea();

        try {
            jsonSourceTextArea.getDocument().insertString(0, scriptTemplate(),
                    null);
            jsonSourceTextArea.setCaretPosition(0);
        } catch (BadLocationException ex) {
            Logger.getLogger(UnRAVLFrame.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

        addFileDragAndDropToSourceTextArea();
        addZoomMouseListeners();
        clearJsonError();
    }

    public void addUndoRedoToSourceTextArea() {
        InputMap im = jsonSourceTextArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = jsonSourceTextArea.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()),
                resources.getString("UNDO.txt"));
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()),
                resources.getString("REDO.txt"));

        am.put(resources.getString("UNDO.txt"),
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {

                        try {
                            if (undoManager.canUndo()) {
                                undoManager.undo();
                            }
                        } catch (CannotUndoException exp) {
                            exp.printStackTrace(System.err);
                        }
                    }
                });
        am.put(resources.getString("REDO.txt"),
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            if (undoManager.canRedo()) {
                                undoManager.redo();
                            }
                        } catch (CannotUndoException exp) {
                            exp.printStackTrace(System.err);
                        }
                    }
                });
    }

    public void addFileDragAndDropToSourceTextArea() {
        jsonSourceTextArea.setTransferHandler(new TransferHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean canImport(TransferHandler.TransferSupport ts) {
                for (DataFlavor df : ts.getDataFlavors()) {
                    if (df.isFlavorTextType() || df.isFlavorJavaFileListType()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport ts) {
                try {
                    for (DataFlavor df : ts.getDataFlavors()) {
                        if (df.isFlavorTextType()) {
                            String s = (String) ts.getTransferable()
                                    .getTransferData(DataFlavor.stringFlavor);
                            jsonSourceTextArea.setText(s);
                        } else if (df.isFlavorJavaFileListType()) {
                            @SuppressWarnings("unchecked")
                            List<File> droppedFiles = (List<File>) ts
                                    .getTransferable().getTransferData(
                                            DataFlavor.javaFileListFlavor);
                            if (droppedFiles != null
                                    && droppedFiles.size() == 1) {
                                File f = droppedFiles.get(0);
                                try {
                                    String s = Files.toString(f,
                                            Charset.defaultCharset());
                                    jsonSourceTextArea.setText(s);
                                } catch (IOException ex) {
                                    status.setText(ex.getMessage());
                                    Logger.getLogger(
                                            UnRAVLFrame.class.getName()).log(
                                            Level.SEVERE, null, ex);
                                }
                            }
                            return true;
                        }
                    }
                } catch (UnsupportedFlavorException | IOException ex) {
                    status.setText(ex.getMessage());
                    Logger.getLogger(UnRAVLFrame.class.getName()).log(
                            Level.SEVERE, null, ex);
                    return false;
                }
                return false;
            }

        });
    }

    public void addZoomMouseListeners() {
        MouseWheelListener zoomer = new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                onZoom(evt);
            }
        };
        textAreas = new JTextArea[] { jsonSourceTextArea, outputTextArea,
                requestHeaders, responseHeaders, responseBody, variableBinding };

        for (JTextArea ta : textAreas) {
            // Note: We cannot add a MouseWheelListener to the JTextArea; the
            // JScrollPane
            // will disable it's scrolling handler of the child has a mouse
            // listener.
            // So walk from the JtextArea to JViewPort to JScrollPane and add
            // the listeners there.
            if (ta.getParent().getParent() instanceof JScrollPane) {
                JScrollPane parent = (JScrollPane) ta.getParent().getParent();
                parent.addMouseWheelListener(zoomer);
            }
        }
    }

    JTextArea textAreas[];

    private boolean onSourceChange() {
        if (highlightTag != null) {
            jsonSourceTextArea.getHighlighter().removeHighlight(highlightTag);
        }
        try {
            setStatusText(""); // NOI18N
            Document d = jsonSourceTextArea.getDocument();
            String s = d.getText(0, d.getEndPosition().getOffset());
            prefs.put(SCRIPT_SOURCE_PREFERENCE_KEY, s);
            return validateJson(s);

        } catch (BadLocationException ex) {
            Logger.getLogger(UnRAVLFrame.class.getName()).log(Level.SEVERE,
                    null, ex);
            return false;
        }
    }

    /**
     * @param args
     *            the command line arguments
     * @return the JFrame that is created
     */
    public static JFrame main(String args[]) {

        final UnRAVLFrame f = new UnRAVLFrame();
        redirectStdoOutStdErr(f.outputTextArea);
        /* Create and display the form */
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                f.clearJsonError();
                f.addHandlers();
                f.setVisible(true);
            }
        });
        return f;
    }

    private static void redirectStdoOutStdErr(JTextArea textArea) {
        /**
         * An OutputStream that writes to a JTextArea
         */
        final class TextAreaOutputStream extends OutputStream {

            private final JTextArea textArea;

            public TextAreaOutputStream(JTextArea textArea) {
                this.textArea = textArea;
            }

            // This won't handle Unicode
            @Override
            public void write(int byt) throws IOException {
                textArea.append(String.valueOf((char) byt));
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }
        }
        TextAreaOutputStream out = new TextAreaOutputStream(textArea);
        PrintStream ps = new PrintStream(out);
        Main.setOut(ps);
        Main.setErr(ps);
    }

    private JsonNode unravlScript = null;
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean validateJson(String json) {
        try {
            unravlScript = null;
            unravlScript = mapper.readTree(json);
            clearJsonError();
            return true;
        } catch (JsonProcessingException e) {
            JsonLocation l = e.getLocation();
            Integer line = null, col = null;
            if (l != null) {
                line = l.getLineNr();
                col = l.getColumnNr();
            }
            jsonError(line, col, e.getMessage());
        } catch (IOException e) {
            jsonError(null, null, e.getMessage());
        }
        return false;
    }

    void setStatusText(final String message) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                status.setText(message);
            }
        });
    }

    Highlighter.HighlightPainter painter;
    private Object highlightTag = null;

    public void clearJsonError() {
        run.setEnabled(true);
        prettyPrintSource.setEnabled(true);
        jumpToError.setEnabled(false);
    }

    int errLine = 0;
    int errCol = 0;

    void jsonError(Integer line, Integer col, String message) {
        String prefix = ""; // NOI18N
        run.setEnabled(false);
        prettyPrintSource.setEnabled(false);
        if (line != null && col != null) {
            try {

                jumpToError.setEnabled(true);
                prefix = "[" + line + "," + col + "] "; // NOI18N //NOI18N
                errLine = line > 0 ? line - 1 : line;
                errLine = Math.max(0, Math.min(errLine,
                        jsonSourceTextArea.getLineCount() - 1));
                errCol = col - 1;
                int startIndex = jsonSourceTextArea.getLineStartOffset(errLine);
                int endIndex = jsonSourceTextArea.getLineEndOffset(errLine);

                painter = new DefaultHighlighter.DefaultHighlightPainter(
                        Color.ORANGE);
                highlightTag = jsonSourceTextArea.getHighlighter()
                        .addHighlight(startIndex, endIndex, painter);

            } catch (BadLocationException ex) {
                Logger.getLogger(UnRAVLFrame.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        setStatusText(prefix + message);
    }

    LinkedHashMap<String, Object> changedVars = new LinkedHashMap<String, Object>();

    class RuntimePropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Object source = evt.getSource();
            if (source == runtime) {
                String name = evt.getPropertyName();
                if (name.startsWith(UnRAVLRuntime.ENV_PROPERTY_CHANGE_PREFIX)) {
                    changedVars.put(name
                            .substring(UnRAVLRuntime.ENV_PROPERTY_CHANGE_PREFIX
                                    .length()), evt.getNewValue());
                } else {
                    switch (name) {
                    case ("calls"): // NOI18N
                        if (callIndex > runtime.size()) {
                            callIndex = 0;
                        }
                        updateCallsTab();
                    default:
                        ;
                    }
                }
            }
        }
    }

    private void updateCallsTab() {
        try {
            if (runtime == null || runtime.size() == 0) {
                // no calls in the runtime: reset the view to empty
                // create an empty ApiCall. Use a different UnRAVLRuntime so
                // we don't skew this app's UnRAVLRuntime
                callIndex = 0;
                call = new ApiCall(new UnRAVL(new UnRAVLRuntime()));
            } else {
                callIndex = Math.min(callIndex, runtime.size() - 1);
                call = runtime.getApiCalls().get(callIndex);
            }
            next.setEnabled(callIndex < runtime.size() - 1);
            previous.setEnabled(callIndex > 0);
            updateCall();
        } catch (UnRAVLException ex) {
            Logger.getLogger(UnRAVLFrame.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UnRAVLFrame.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }

    void updateCall() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // local vars make debugging easier...
                UnRAVLFrame f = UnRAVLFrame.this;
                ApiCall call = f.call;
                f.method.setText(call.getMethod() == null ? NONE : call
                        .getMethod().name());
                f.testName.setText((call.getScript() == null
                        || call.getScript().getName() == null || call
                        .getScript().getName().trim().length() == 0) ? NONE
                        : call.getScript().getName());
                f.url.setText(call.getURI() == null ? NONE : call.getURI());
                f.responseCode.setText(statusLine(call.getHttpStatus()));
                setHeaders(call.getScript().getRequestHeaders(),
                        f.requestHeaders);
                setHeaders(
                        call.getResponseHeaders() == null ? null : Arrays
                                .asList(call.getResponseHeaders()),
                        f.responseHeaders);
                String body = call.getResponseBody() == null ? "" : call // NOI18N
                        .getResponseBody().toString();
                if (call.getException() != null) {
                    status.setText(call.getException().getMessage());
                } else {
                    int passed = call.getPassedAssertions().size();
                    int failed = call.getFailedAssertions().size();
                    int skipped = call.getSkippedAssertions().size();
                    String summary = String.format(resources.getString("SUMMARY.txt"), passed, failed, skipped);
                    if (call.wasCancelled()) {
                        summary += resources.getString("CANCELLED.txt");
                    }
                    if (call.wasSkipped()) {
                        summary += resources.getString("SKIPPED.txt");
                    }
                    status.setText(summary);
                }
                if (f.prettyPrintResponseBody.isSelected()) {
                    body = prettyPrint(body);
                }
                f.responseBody.setText(body);
                f.responseBody.setCaretPosition(0);
            }
        });

    }

    protected String statusLine(int httpStatus) {
        String statusLine = Integer.toString(httpStatus);
        try {
            return statusLine + " "
                    + HttpStatus.valueOf(httpStatus).getReasonPhrase(); // NOI18N
                                                                        // //NOI18N
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    private void updateVarTab() {
        if (runtime == null) {
            return;
        }
        String match = varName.getText();
        if (match.trim().length() == 0) {
            match = null;
        }
        Map<String, Object> varMap;
        if (showAll.isSelected()) {
            varMap = runtime.getBindings();
        } else {
            varMap = changedVars;
        }
        Object[] listData = varMap.keySet().toArray();
        ArrayList<String> names = new ArrayList<String>(listData.length);
        for (int i = 0; i < listData.length; i++) {
            String name = listData[i].toString();
            if (match == null || name.contains(match)) {
                names.add(name);
            }
        }
        final String[] namesArray = names.toArray(new String[names.size()]);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                varNames.setListData(namesArray);

            }
        });
    }

    private String prettyPrint(String text) {
        try {
            JsonNode node = mapper.readTree(text);
            return prettyPrint(node);
        } catch (IOException e) {
            return text;
        }
    }

    private String prettyPrint(JsonNode node) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    node);
        } catch (JsonProcessingException ex) {
            return node.toString();
        }
    }

    void onErrorPosition(java.awt.event.ActionEvent evt) {
        try {
            int pos = jsonSourceTextArea.getLineStartOffset(errLine);
            if (pos + errCol < jsonSourceTextArea.getDocument().getLength()) {
                pos += errCol;
            }
            jsonSourceTextArea.setCaretPosition(pos);
        } catch (BadLocationException ex) {
            Logger.getLogger(UnRAVLFrame.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }

    // =============== NetBeans IDE Generated code below ================== //
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed"
    // <editor-fold defaultstate="collapsed"
    // <editor-fold defaultstate="collapsed"
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        title = new javax.swing.JLabel();
        run = new javax.swing.JButton();
        status = new javax.swing.JLabel();
        reset = new javax.swing.JButton();
        jumpToError = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        tabs = new javax.swing.JTabbedPane();
        sourcePanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jsonSourceTextArea = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        outputTextArea = new javax.swing.JTextArea();
        callsPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        method = new javax.swing.JLabel();
        url = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        requestHeaders = new javax.swing.JTextArea();
        jScrollPane6 = new javax.swing.JScrollPane();
        responseHeaders = new javax.swing.JTextArea();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        responseBody = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();
        prettyPrintResponseBody = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        responseCode = new javax.swing.JLabel();
        testName = new javax.swing.JLabel();
        previous = new javax.swing.JButton();
        next = new javax.swing.JButton();
        varPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        varNames = new javax.swing.JList<>();
        varName = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        showAll = new javax.swing.JCheckBox();
        jScrollPane4 = new javax.swing.JScrollPane();
        variableBinding = new javax.swing.JTextArea();
        position = new javax.swing.JLabel();
        prettyPrintSource = new javax.swing.JButton();

        setTitle("UnRAVL"); // NOI18N
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setName("null");

        title.setFont(new java.awt.Font("Arial Unicode MS", 3, 20)); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/sas/unravl/ui/Resources"); // NOI18N
        title.setText(bundle.getString("UNRAVL_RUNNER.txt")); // NOI18N

        run.setText(bundle.getString("RUN.txt")); // NOI18N
        run.setToolTipText(bundle.getString("RUN_TOOLTIP.txt")); // NOI18N
        run.setEnabled(false);
        run.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onRun(evt);
            }
        });

        status.setToolTipText(bundle.getString("STATUS_TOOLTIP.txt")); // NOI18N

        reset.setText(bundle.getString("RESET.txt")); // NOI18N
        reset.setToolTipText(bundle.getString("RESET_TOOLTIP.txt")); // NOI18N
        reset.setEnabled(false);
        reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onReset(evt);
            }
        });

        jumpToError.setFont(new java.awt.Font("Arial Unicode MS", 1, 24)); // NOI18N
        jumpToError.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/sas/unravl/ui/error-up-orange.png"))); // NOI18N
        jumpToError.setToolTipText(bundle.getString("MOVE_CURSOR_TOOLTIP.txt")); // NOI18N
        jumpToError.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/com/sas/unravl/ui/error-up-grey.png"))); // NOI18N
        jumpToError.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onErrorPosition(evt);
            }
        });

        cancel.setText(bundle.getString("CANCEL.txt")); // NOI18N
        cancel.setToolTipText(bundle.getString("CANCEL_TOOLTIP.txt")); // NOI18N
        cancel.setEnabled(false);
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onCancel(evt);
            }
        });

        tabs.setToolTipText(bundle.getString("SOURCE_TAB.txt")); // NOI18N

        jsonSourceTextArea.setColumns(20);
        jsonSourceTextArea.setFont(new java.awt.Font("Lucida Console", 0, 13)); // NOI18N
        jsonSourceTextArea.setRows(5);
        jsonSourceTextArea.setToolTipText(bundle.getString("SOURCE_TOOLTIP.txt")); // NOI18N
        jsonSourceTextArea.setInputVerifier(new InputVerifier() {public boolean verify(JComponent input) { return onSourceChange(); }});
        jsonSourceTextArea.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                onPositionChange(evt);
            }
        });
        jScrollPane1.setViewportView(jsonSourceTextArea);

        javax.swing.GroupLayout sourcePanelLayout = new javax.swing.GroupLayout(sourcePanel);
        sourcePanel.setLayout(sourcePanelLayout);
        sourcePanelLayout.setHorizontalGroup(
            sourcePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1120, Short.MAX_VALUE)
        );
        sourcePanelLayout.setVerticalGroup(
            sourcePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
        );

        tabs.addTab(bundle.getString("SOURCE.txt"), sourcePanel); // NOI18N

        outputTextArea.setEditable(false);
        outputTextArea.setColumns(20);
        outputTextArea.setFont(new java.awt.Font("Lucida Console", 0, 13)); // NOI18N
        outputTextArea.setRows(5);
        outputTextArea.setToolTipText(bundle.getString("OUTPUT_TOOLTIP.txt")); // NOI18N
        jScrollPane2.setViewportView(outputTextArea);

        tabs.addTab(bundle.getString("OUTPUT.txt"), jScrollPane2); // NOI18N

        jLabel2.setText(bundle.getString("TEST_NAME.txt")); // NOI18N
        jLabel2.setToolTipText(bundle.getString("TEST_NAME_TOOLTIP.txt")); // NOI18N

        method.setText(bundle.getString("METHOD.txt")); // NOI18N
        method.setToolTipText(bundle.getString("METHOD_TOOLTIP.txt")); // NOI18N

        url.setToolTipText(bundle.getString("URL_TOOLTIP.txt")); // NOI18N

        requestHeaders.setEditable(false);
        requestHeaders.setColumns(20);
        requestHeaders.setFont(new java.awt.Font("Lucida Console", 0, 13)); // NOI18N
        requestHeaders.setRows(5);
        requestHeaders.setToolTipText(bundle.getString("REQUEST_HEADERS_TOOLTIP.txt")); // NOI18N
        jScrollPane5.setViewportView(requestHeaders);

        responseHeaders.setEditable(false);
        responseHeaders.setColumns(20);
        responseHeaders.setFont(new java.awt.Font("Lucida Console", 0, 13)); // NOI18N
        responseHeaders.setRows(5);
        responseHeaders.setToolTipText(bundle.getString("RESPONSE_HEADERS_TOOLTIP.txt")); // NOI18N
        jScrollPane6.setViewportView(responseHeaders);

        jLabel4.setText(bundle.getString("REQUEST_HEADERS.txt")); // NOI18N

        jLabel5.setLabelFor(responseHeaders);
        jLabel5.setText(bundle.getString("RESPONSE_HEADERS.txt")); // NOI18N

        responseBody.setEditable(false);
        responseBody.setColumns(20);
        responseBody.setFont(new java.awt.Font("Lucida Console", 0, 13)); // NOI18N
        responseBody.setRows(5);
        responseBody.setToolTipText(bundle.getString("RESPONSE_BODY_TOOLTIP.txt")); // NOI18N
        jScrollPane7.setViewportView(responseBody);

        jLabel6.setLabelFor(responseBody);
        jLabel6.setText(bundle.getString("RESPONSE_BODY.txt")); // NOI18N

        prettyPrintResponseBody.setSelected(true);
        prettyPrintResponseBody.setText(bundle.getString("PRETTY_PRINT.txt")); // NOI18N
        prettyPrintResponseBody.setToolTipText(bundle.getString("PRETTY_PRINT_TOOLTIP.txt")); // NOI18N
        prettyPrintResponseBody.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prettyPrintResponseBodyActionPerformed(evt);
            }
        });

        jLabel7.setText(bundle.getString("RESPONSE_CODE.txt")); // NOI18N

        responseCode.setText("null");
        responseCode.setToolTipText(bundle.getString("RESPONSE_CODE.txt")); // NOI18N

        previous.setFont(new java.awt.Font("Arial Unicode MS", 1, 24)); // NOI18N
        previous.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/sas/unravl/ui/left-triangle-black.png"))); // NOI18N
        previous.setToolTipText(bundle.getString("PREVIOUS_CALL_TOOLTIP.txt")); // NOI18N
        previous.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/com/sas/unravl/ui/left-triangle-grey.png"))); // NOI18N
        previous.setEnabled(false);
        previous.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previous(evt);
            }
        });

        next.setFont(new java.awt.Font("Arial Unicode MS", 0, 24)); // NOI18N
        next.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/sas/unravl/ui/right-triangle-black.png"))); // NOI18N
        next.setToolTipText(bundle.getString("NEXT_CALL_TOOLTIP.txt")); // NOI18N
        next.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/com/sas/unravl/ui/right-triangle-grey.png"))); // NOI18N
        next.setEnabled(false);
        next.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                next(evt);
            }
        });

        javax.swing.GroupLayout callsPanelLayout = new javax.swing.GroupLayout(callsPanel);
        callsPanel.setLayout(callsPanelLayout);
        callsPanelLayout.setHorizontalGroup(
            callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(callsPanelLayout.createSequentialGroup()
                .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(callsPanelLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(method, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, callsPanelLayout.createSequentialGroup()
                                .addComponent(testName, javax.swing.GroupLayout.DEFAULT_SIZE, 946, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(previous, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(next, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(url, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 1020, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, callsPanelLayout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jScrollPane7)
                            .addGroup(callsPanelLayout.createSequentialGroup()
                                .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 517, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 407, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 517, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)))
                            .addGroup(callsPanelLayout.createSequentialGroup()
                                .addGap(535, 535, 535)
                                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(responseCode, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 49, Short.MAX_VALUE)
                                .addComponent(prettyPrintResponseBody)))))
                .addContainerGap())
        );
        callsPanelLayout.setVerticalGroup(
            callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(callsPanelLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(testName)
                    .addComponent(previous, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(next, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(url, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(method, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE))
                .addGap(12, 12, 12)
                .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addGap(3, 3, 3)
                .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(callsPanelLayout.createSequentialGroup()
                        .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(responseCode))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(callsPanelLayout.createSequentialGroup()
                        .addGroup(callsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(prettyPrintResponseBody))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                        .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        tabs.addTab("Calls", callsPanel);

        varNames.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "name", "jsonResponse" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        varNames.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        varNames.setToolTipText(bundle.getString("SELECT_VAR.txt")); // NOI18N
        varNames.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                onVarNamesValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(varNames);

        varName.setToolTipText(bundle.getString("MATCH_TOOLTIP.txt")); // NOI18N
        varName.setActionCommand("null");
        varName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onVarNameChange(evt);
            }
        });

        jLabel3.setLabelFor(varName);
        jLabel3.setText(bundle.getString("SEARCH.txt")); // NOI18N

        showAll.setText(bundle.getString("SHOW_ALL.txt")); // NOI18N
        showAll.setToolTipText(bundle.getString("SHOW_ALL_TOOLTIP.txt")); // NOI18N
        showAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onToggleShowAll(evt);
            }
        });

        variableBinding.setEditable(false);
        variableBinding.setColumns(20);
        variableBinding.setFont(new java.awt.Font("Lucida Console", 0, 13)); // NOI18N
        variableBinding.setRows(5);
        jScrollPane4.setViewportView(variableBinding);

        javax.swing.GroupLayout varPanelLayout = new javax.swing.GroupLayout(varPanel);
        varPanel.setLayout(varPanelLayout);
        varPanelLayout.setHorizontalGroup(
            varPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, varPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(varPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, varPanelLayout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(varName, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(showAll)
                        .addGap(0, 598, Short.MAX_VALUE)))
                .addContainerGap())
        );
        varPanelLayout.setVerticalGroup(
            varPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, varPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(varPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(varName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(showAll))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 312, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabs.addTab(bundle.getString("VARIABLES.txt"), varPanel); // NOI18N

        position.setForeground(new java.awt.Color(64, 64, 64));
        position.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        position.setToolTipText(bundle.getString("POSITION_TOOLTIP.txt")); // NOI18N

        prettyPrintSource.setFont(new java.awt.Font("Lucida Console", 1, 14)); // NOI18N
        prettyPrintSource.setText("{ }");
        prettyPrintSource.setToolTipText(bundle.getString("PETTY_PRINT_SOURCE.txt")); // NOI18N
        prettyPrintSource.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prettyPrintSource(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jumpToError, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(status, javax.swing.GroupLayout.PREFERRED_SIZE, 959, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(prettyPrintSource, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(title, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(run)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(reset)
                        .addContainerGap())))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabs)
                .addContainerGap())
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(1075, Short.MAX_VALUE)
                    .addComponent(position, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(17, 17, 17)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(title)
                    .addComponent(run)
                    .addComponent(cancel)
                    .addComponent(reset))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(status, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jumpToError, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(prettyPrintSource)))
                .addContainerGap(15, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(546, Short.MAX_VALUE)
                    .addComponent(position, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(14, 14, 14)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void prettyPrintSource(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_prettyPrintSource
        String src = jsonSourceTextArea.getText();
        src = prettyPrint(src);
        jsonSourceTextArea.setText(src);
    }// GEN-LAST:event_prettyPrintSource

    private void onZoom(java.awt.event.MouseWheelEvent evt) {// GEN-FIRST:event_onZoom
        if (evt.isControlDown()) {
            if (evt.getWheelRotation() < 0) {
                zoom(1);
            } else if (evt.getWheelRotation() > 0) {
                zoom(-1);
            }
        }
    }// GEN-LAST:event_onZoom

    private void next(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_next
        if (callIndex < runtime.size() - 1) {
            callIndex++;
            updateCallsTab();
        }
    }// GEN-LAST:event_next

    private void previous(java.awt.event.ActionEvent evt) {
        if (callIndex > 0) {
            callIndex--;
            updateCallsTab();
        }
    }

    private void onRun(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_onRun
        outputTextArea.setText(""); // NOI18N
        tabs.setSelectedIndex(OUTPUT_TAB);
        new Thread(new Runnable() {
            @Override
            public void run() {
                runScript();
            }
        }).start();
    }

    /**
     * Enable/disable buttons and other controls based on whether a script is
     * running or not
     *
     * @param running
     *            true if a script is running
     */
    public void enableControlsForRunState(final boolean running) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                cancel.setEnabled(running);
                run.setEnabled(!running);
                reset.setEnabled(!running);
                varNames.setEnabled(!running);
                showAll.setEnabled(!running);
                prettyPrintSource.setEnabled(!running);
            }
        });
    }

    public void runScript() {
        setStatusText(resources.getString("RUNNING_TOOLTIP.txt"));
        enableControlsForRunState(true);
        runtime.resetFailedAssertionCount();
        changedVars = new LinkedHashMap<String, Object>();
        String text = null;
        try {
            if (runtime == null) {
                reset();
            }
            runtime.execute(unravlScript);
        } catch (Throwable t) {
            text = t.getMessage();
            System.err.println(t.getMessage());
        } finally {

            runtime.report();
            if (text != null) {
                text = resources.getString("RUNNING_DONE_TOOLTIP.txt");
            }
            enableControlsForRunState(false);
            final String statusText = text;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    status.setText(statusText);
                    outputTextArea.setCaretPosition(0);
                    callIndex = runtime.size() - 1;
                    updateCallsTab();
                }
            });
            updateVarTab();
        }

    }// GEN-LAST:event_onRun

    private void onVarNameChange(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_onVarNameChange
        updateVarTab();
    }// GEN-LAST:event_onVarNameChange

    // update the cursr position indicator
    private void onPositionChange(javax.swing.event.CaretEvent evt) {// GEN-FIRST:event_onPositionChange
        try {
            JTextArea editArea = (JTextArea) evt.getSource();
            int caretpos = editArea.getCaretPosition();
            int l = editArea.getLineOfOffset(caretpos);
            int c = caretpos - editArea.getLineStartOffset(l);
            String pos = (l + 1) + "," + (c + 1); // NOI18N //NOI18N //NOI18N
                                                  // //NOI18N
            // //NOI18N //NOI18N
            position.setText(pos);

        } catch (BadLocationException ex) {
            Logger.getLogger(UnRAVLFrame.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }// GEN-LAST:event_onPositionChange

    private void onCancel(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_onCancel
        if (runtime != null) {
            runtime.cancel();
        }
        cancel.setEnabled(false);

    }// GEN-LAST:event_onCancel

    private void onReset(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_onReset
        reset();
    }// GEN-LAST:event_onReset

    private void onToggleShowAll(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_onToggleShowAll
        updateVarTab();
    }// GEN-LAST:event_onToggleShowAll

    private void onVarNamesValueChanged(javax.swing.event.ListSelectionEvent evt) {// GEN-FIRST:event_onVarNamesValueChanged
        int index = varNames.getSelectedIndex();
        if (index > -1) {
            String varName = varNames.getModel().getElementAt(index);
            Object varValue = runtime.binding(varName);
            if (varValue instanceof JsonNode) {
                varValue = prettyPrint((JsonNode) varValue);
            }
            variableBinding.setText(varValue == null ? null : varValue
                    .toString());
        }
    }// GEN-LAST:event_onVarNamesValueChanged

    private void prettyPrintResponseBodyActionPerformed(
            java.awt.event.ActionEvent evt) {
        if (prettyPrintResponseBody.isSelected()) {
            responseBody.setText(prettyPrint(responseBody.getText()));
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel callsPanel;
    private javax.swing.JButton cancel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTextArea jsonSourceTextArea;
    private javax.swing.JButton jumpToError;
    private javax.swing.JLabel method;
    private javax.swing.JButton next;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JLabel position;
    private javax.swing.JCheckBox prettyPrintResponseBody;
    private javax.swing.JButton prettyPrintSource;
    private javax.swing.JButton previous;
    private javax.swing.JTextArea requestHeaders;
    private javax.swing.JButton reset;
    private javax.swing.JTextArea responseBody;
    private javax.swing.JLabel responseCode;
    private javax.swing.JTextArea responseHeaders;
    private javax.swing.JButton run;
    private javax.swing.JCheckBox showAll;
    private javax.swing.JPanel sourcePanel;
    private javax.swing.JLabel status;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JLabel testName;
    private javax.swing.JLabel title;
    private javax.swing.JLabel url;
    private javax.swing.JTextField varName;
    private javax.swing.JList<String> varNames;
    private javax.swing.JPanel varPanel;
    private javax.swing.JTextArea variableBinding;
    // End of variables declaration//GEN-END:variables
}
