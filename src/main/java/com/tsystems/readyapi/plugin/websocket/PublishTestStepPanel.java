package com.tsystems.readyapi.plugin.websocket;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import com.eviware.soapui.support.DateUtil;
import com.eviware.soapui.support.ListDataChangeListener;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.support.log.JLogList;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
import com.eviware.soapui.support.xml.SyntaxEditorUtil;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;

public class PublishTestStepPanel extends ConnectedTestStepPanel<PublishTestStep> implements ExecutionListener {

    /** serialVersionUID description. */
    private static final long serialVersionUID = 7242999356880461009L;
    private final static String LOG_TAB_TITLE = "Test Step Log (%d)";
    private JTextField numberEdit;
    private JTextArea textMemo;
    private JTextField fileNameEdit;
    private JButton chooseFileButton;
    private JTabbedPane jsonEditor;
    private JComponent jsonTreeEditor;
    private JTabbedPane xmlEditor;

    private JComponent xmlTreeEditor;
    private JInspectorPanel inspectorPanel;
    private JComponentInspector<JComponent> logInspector;
    private JLogList logArea;

    public PublishTestStepPanel(PublishTestStep modelItem) {
        super(modelItem);
        buildUI();
        modelItem.addExecutionListener(this);
    }

    @Override
    public void afterExecution(ExecutableTestStep testStep, ExecutableTestStepResult executionResult) {
        logArea.addLine(DateUtil.formatFull(new Date(executionResult.getTimeStamp())) + " - "
                + executionResult.getOutcome());
    }

    protected JComponent buildLogPanel() {
        logArea = new JLogList("Test Step Log");

        logArea.getLogList().getModel().addListDataListener(new ListDataChangeListener() {

            @Override
            public void dataChanged(ListModel model) {
                logInspector.setTitle(String.format(LOG_TAB_TITLE, model.getSize()));
            }
        });

        return logArea;
    }

    private JComponent buildMainPanel() {

        PresentationModel<PublishTestStep> pm = new PresentationModel<PublishTestStep>(getModelItem());
        SimpleBindingForm form = new SimpleBindingForm(pm);
        buildConnectionSection(form, pm);

        form.appendSeparator();
        form.appendHeading("Published Message");
        form.appendComboBox("messageKind", "Message type", PublishedMessageType.values(), "");
        numberEdit = form.appendTextField("message", "Message", "The number which will be published.");
        textMemo = form.appendTextArea("message", "Message", "The text which will be published.");
        PropertyExpansionPopupListener.enable(textMemo, getModelItem());
        fileNameEdit = form.appendTextField("message", "File name", "The file which content will be used as payload");
        PropertyExpansionPopupListener.enable(fileNameEdit, getModelItem());
        chooseFileButton = form.addRightButton(new SelectFileAction());

        JScrollPane scrollPane;

        jsonEditor = new JTabbedPane();
        RSyntaxTextArea syntaxTextArea = SyntaxEditorUtil.createDefaultJavaScriptSyntaxTextArea();
        syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        Bindings.bind(syntaxTextArea, pm.getModel("message"), true);
        PropertyExpansionPopupListener.enable(syntaxTextArea, getModelItem());
        jsonEditor.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

        jsonTreeEditor = Utils.createJsonTreeEditor(true, getModelItem());
        if (jsonTreeEditor != null) {
            scrollPane = new JScrollPane(jsonTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(jsonTreeEditor, "text", pm.getModel("message"));
            jsonEditor.addTab("Tree View", scrollPane);
        } else
            jsonEditor.addTab("Tree View", new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER));

        jsonEditor.setPreferredSize(new Dimension(450, 350));
        form.append("Message", jsonEditor);

        xmlEditor = new JTabbedPane();

        syntaxTextArea = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
        Bindings.bind(syntaxTextArea, pm.getModel("message"), true);
        PropertyExpansionPopupListener.enable(syntaxTextArea, getModelItem());
        xmlEditor.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

        xmlTreeEditor = Utils.createXmlTreeEditor(true, getModelItem());
        if (xmlTreeEditor != null) {
            scrollPane = new JScrollPane(xmlTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(xmlTreeEditor, "text", pm.getModel("message"));
            xmlEditor.addTab("Tree View", scrollPane);
        } else
            xmlEditor.addTab("Tree View", new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER));

        xmlEditor.setPreferredSize(new Dimension(450, 350));
        form.append("Message", xmlEditor);

        form.appendSeparator();
        form.appendHeading("Message Delivering Settings");
        buildTimeoutSpinEdit(form, pm, "Timeout");

        JPanel result = new JPanel(new BorderLayout(0, 0));
        result.add(new JScrollPane(form.getPanel(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        result.add(buildToolbar(), BorderLayout.NORTH);

        propertyChange(new PropertyChangeEvent(getModelItem(), "messageKind", null, getModelItem().getMessageKind()));

        return result;
    }

    private JComponent buildToolbar() {
        JXToolBar toolBar = UISupport.createToolbar();
        RunTestStepAction startAction = new RunTestStepAction(getModelItem());
        JButton submitButton = UISupport.createActionButton(startAction, startAction.isEnabled());
        toolBar.add(submitButton);
        submitButton.setMnemonic(KeyEvent.VK_ENTER);
        toolBar.add(UISupport.createActionButton(startAction.getCorrespondingStopAction(), startAction
                .getCorrespondingStopAction().isEnabled()));
        addConnectionActionsToToolbar(toolBar);
        return toolBar;
    }

    private void buildUI() {
        JComponent mainPanel = buildMainPanel();
        inspectorPanel = JInspectorPanelFactory.build(mainPanel);

        logInspector = new JComponentInspector<JComponent>(buildLogPanel(), String.format(LOG_TAB_TITLE, 0),
                "Log of the test step executions", true);
        inspectorPanel.addInspector(logInspector);

        inspectorPanel.setDefaultDividerLocation(0.6F);

        add(inspectorPanel.getComponent());
        setPreferredSize(new Dimension(500, 300));

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);

        if (evt.getPropertyName().equals("messageKind")) {
            PublishedMessageType newMessageType = (PublishedMessageType) evt.getNewValue();
            boolean isNumber = newMessageType == PublishedMessageType.DoubleValue
                    || newMessageType == PublishedMessageType.FloatValue
                    || newMessageType == PublishedMessageType.IntegerValue
                    || newMessageType == PublishedMessageType.LongValue;
            boolean isFile = newMessageType == PublishedMessageType.BinaryFile;
            boolean isText = newMessageType == PublishedMessageType.Text;
            numberEdit.setVisible(isNumber);
            textMemo.setVisible(isText);
            if (textMemo.getParent() instanceof JScrollPane)
                textMemo.getParent().setVisible(isText);
            else if (textMemo.getParent().getParent() instanceof JScrollPane)
                textMemo.getParent().getParent().setVisible(isText);
            fileNameEdit.setVisible(isFile);
            chooseFileButton.setVisible(isFile);
            jsonEditor.setVisible(newMessageType == PublishedMessageType.Json);
            xmlEditor.setVisible(newMessageType == PublishedMessageType.Xml);
        }
    }

    @Override
    protected boolean release() {
        getModelItem().removeExecutionListener(this);
        inspectorPanel.release();
        if (jsonTreeEditor != null)
            Utils.releaseTreeEditor(jsonTreeEditor);
        if (xmlTreeEditor != null)
            Utils.releaseTreeEditor(xmlTreeEditor);
        return super.release();
    }

    public class SelectFileAction extends AbstractAction {
        /** serialVersionUID description. */
        private static final long serialVersionUID = 3499349831088040594L;
        private JFileChooser fileChooser;

        public SelectFileAction() {
            super("Browse...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (fileChooser == null)
                fileChooser = new JFileChooser();

            int returnVal = fileChooser.showOpenDialog(UISupport.getMainFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION)
                fileNameEdit.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

}
