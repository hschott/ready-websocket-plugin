package com.tsystems.readyapi.plugin.websocket;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import com.eviware.soapui.impl.wsdl.panels.teststeps.AssertionsPanel;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionsListener;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.support.DateUtil;
import com.eviware.soapui.support.JsonUtil;
import com.eviware.soapui.support.ListDataChangeListener;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.support.log.JLogList;
import com.eviware.soapui.support.xml.SyntaxEditorUtil;
import com.eviware.soapui.support.xml.XmlUtils;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;

public class ReceiveTestStepPanel extends ConnectedTestStepPanel<ReceiveTestStep> implements AssertionsListener,
        ExecutionListener {

    /** serialVersionUID description. */
    private static final long serialVersionUID = 398715768048748118L;
    private final static String LOG_TAB_TITLE = "Test Step Log (%d)";
    private JComponentInspector<JComponent> assertionInspector;
    private JInspectorPanel inspectorPanel;
    private AssertionsPanel assertionsPanel;
    private JTextArea recMessageMemo;
    private JTabbedPane jsonEditor;
    private JComponent jsonTreeEditor;
    private JTabbedPane xmlEditor;

    private JComponent xmlTreeEditor;
    private JComponentInspector<JComponent> logInspector;

    private JLogList logArea;
    private RunTestStepAction startAction;

    public ReceiveTestStepPanel(ReceiveTestStep modelItem) {
        super(modelItem);
        buildUI();
        modelItem.addAssertionsListener(this);
        modelItem.addExecutionListener(this);
    }

    @Override
    public void afterExecution(ExecutableTestStep testStep, ExecutableTestStepResult executionResult) {
        logArea.addLine(DateUtil.formatFull(new Date(executionResult.getTimeStamp())) + " - "
                + executionResult.getOutcome());
    }

    @Override
    public void assertionAdded(TestAssertion assertion) {
        assertionListChanged();
    }

    private void assertionListChanged() {
        assertionInspector.setTitle(String.format("Assertions (%d)", getModelItem().getAssertionCount()));
    }

    @Override
    public void assertionMoved(TestAssertion assertion, int ix, int offset) {
        assertionListChanged();
    }

    @Override
    public void assertionRemoved(TestAssertion assertion) {
        assertionListChanged();
    }

    private AssertionsPanel buildAssertionsPanel() {
        return new AssertionsPanel(getModelItem());
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
        PresentationModel<ReceiveTestStep> pm = new PresentationModel<ReceiveTestStep>(getModelItem());
        SimpleBindingForm form = new SimpleBindingForm(pm);
        buildConnectionSection(form, pm);
        form.appendSeparator();
        form.appendHeading("Receiver settings");
        form.appendComboBox("expectedMessageType", "Expected message type", ReceiveTestStep.MessageType.values(),
                "Expected type of a received message");
        buildTimeoutSpinEdit(form, pm, "Timeout");
        form.appendSeparator();
        form.appendHeading("Received message");

        recMessageMemo = form.appendTextArea("receivedMessage", "Message", "The payload of the received message");
        recMessageMemo.setEditable(false);
        recMessageMemo.getCaret().setVisible(true);
        recMessageMemo.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                recMessageMemo.getCaret().setVisible(true);

            }

            @Override
            public void focusLost(FocusEvent e) {
                //
            }
        });
        recMessageMemo.setRows(8);

        jsonEditor = new JTabbedPane();

        RSyntaxTextArea syntaxTextArea = SyntaxEditorUtil.createDefaultJavaScriptSyntaxTextArea();
        syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        Bindings.bind(syntaxTextArea, pm.getModel("receivedMessage"), true);
        syntaxTextArea.setEditable(false);
        jsonEditor.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

        jsonTreeEditor = Utils.createJsonTreeEditor(false, getModelItem());
        if (jsonTreeEditor == null)
            jsonEditor.addTab("Tree View", new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER));
        else {
            JScrollPane scrollPane = new JScrollPane(jsonTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(jsonTreeEditor, "text", pm.getModel("receivedMessage"));
            jsonEditor.addTab("Tree View", scrollPane);
        }

        jsonEditor.setPreferredSize(new Dimension(450, 350));
        form.append("Message", jsonEditor);

        xmlEditor = new JTabbedPane();

        syntaxTextArea = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
        syntaxTextArea.setEditable(false);
        Bindings.bind(syntaxTextArea, pm.getModel("receivedMessage"), true);
        xmlEditor.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

        xmlTreeEditor = Utils.createXmlTreeEditor(false, getModelItem());
        if (xmlTreeEditor == null)
            xmlEditor.addTab("Tree View", new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER));
        else {
            JScrollPane scrollPane = new JScrollPane(xmlTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(xmlTreeEditor, "text", pm.getModel("receivedMessage"));
            xmlEditor.addTab("Tree View", scrollPane);
        }

        xmlEditor.setPreferredSize(new Dimension(450, 350));
        form.append("Message", xmlEditor);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.add(buildToolbar(), BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(form.getPanel(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        return mainPanel;
    }

    private JComponent buildToolbar() {
        JXToolBar toolBar = UISupport.createToolbar();
        startAction = new RunTestStepAction(getModelItem());
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

        assertionsPanel = buildAssertionsPanel();

        assertionInspector = new JComponentInspector<JComponent>(assertionsPanel, "Assertions ("
                + getModelItem().getAssertionCount() + ")", "Assertions for this Message", true);

        inspectorPanel.addInspector(assertionInspector);

        logInspector = new JComponentInspector<JComponent>(buildLogPanel(), String.format(LOG_TAB_TITLE, 0),
                "Log of the test step executions", true);
        inspectorPanel.addInspector(logInspector);

        inspectorPanel.setDefaultDividerLocation(0.6F);
        inspectorPanel.setCurrentInspector("Assertions");

        updateStatusIcon();

        add(inspectorPanel.getComponent());

        propertyChange(new PropertyChangeEvent(getModelItem(), "receivedMessage", null, getModelItem()
                .getReceivedMessage()));

    }

    @Override
    public boolean release() {
        startAction.cancel();
        getModelItem().removeExecutionListener(this);
        getModelItem().removeAssertionsListener(this);
        assertionsPanel.release();
        inspectorPanel.release();
        if (jsonTreeEditor != null)
            Utils.releaseTreeEditor(jsonTreeEditor);
        if (xmlTreeEditor != null)
            Utils.releaseTreeEditor(xmlTreeEditor);

        return super.release();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getPropertyName().equals("assertionStatus"))
            updateStatusIcon();
        else if (event.getPropertyName().equals("receivedMessage")) {
            String msg = (String) event.getNewValue();
            if (StringUtils.isNullOrEmpty(msg)) {
                Utils.showMemo(recMessageMemo, true);
                jsonEditor.setVisible(false);
                xmlEditor.setVisible(false);
            } else if (JsonUtil.seemsToBeJson(msg)) {
                Utils.showMemo(recMessageMemo, false);
                jsonEditor.setVisible(true);
                xmlEditor.setVisible(false);
            } else if (XmlUtils.seemsToBeXml(msg)) {
                Utils.showMemo(recMessageMemo, false);
                jsonEditor.setVisible(false);
                xmlEditor.setVisible(true);
            } else {
                Utils.showMemo(recMessageMemo, true);
                jsonEditor.setVisible(false);
                xmlEditor.setVisible(false);
            }
        }
    }

    private void updateStatusIcon() {
        Assertable.AssertionStatus status = getModelItem().getAssertionStatus();
        switch (status) {
        case FAILED: {
            assertionInspector.setIcon(UISupport.createImageIcon("com/smartbear/assets/failed_assertion.png"));
            inspectorPanel.activate(assertionInspector);
            break;
        }
        case UNKNOWN: {
            assertionInspector.setIcon(UISupport.createImageIcon("com/smartbear/assets/unknown_assertion.png"));
            break;
        }
        case VALID: {
            assertionInspector.setIcon(UISupport.createImageIcon("com/smartbear/assets/valid_assertion.png"));
            inspectorPanel.deactivate();
            break;
        }
        }
    }
}
