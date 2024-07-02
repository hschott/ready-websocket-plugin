package org.hschott.readyapi.plugin.websocket;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;

import com.eviware.soapui.support.DateUtil;
import com.eviware.soapui.support.ListDataChangeListener;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.support.log.JLogList;
import com.jgoodies.binding.PresentationModel;

public class RiseConnectionTestStepPanel extends ConnectedTestStepPanel<RiseConnectionTestStep>
        implements ExecutionListener {

    /** serialVersionUID description. */
    private static final long serialVersionUID = -8477527042685376996L;

    private final static String LOG_TAB_TITLE = "Test Step Log (%d)";

    private JInspectorPanel inspectorPanel;
    private JComponentInspector<JComponent> logInspector;
    private JLogList logArea;

    public RiseConnectionTestStepPanel(RiseConnectionTestStep modelItem) {
        super(modelItem);
        buildUI();
        modelItem.addExecutionListener(this);
    }

    private JComponent buildMainPanel() {
        PresentationModel<RiseConnectionTestStep> pm = new PresentationModel<RiseConnectionTestStep>(getModelItem());
        SimpleBindingForm form = new SimpleBindingForm(pm);
        buildConnectionSection(form, pm);
        form.appendSeparator();
        form.appendHeading("Settings");
        buildTimeoutSpinEdit(form, pm, "Timeout");

        JPanel result = new JPanel(new BorderLayout(0, 0));
        result.add(new JScrollPane(form.getPanel(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        result.add(buildToolbar(), BorderLayout.NORTH);

        return result;
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

    @Override
    public void afterExecution(ExecutableTestStep testStep, ExecutableTestStepResult executionResult) {
        logArea.addLine(
                DateUtil.formatFull(new Date(executionResult.getTimeStamp())) + " - " + executionResult.getOutcome());
    }

    @Override
    protected boolean release() {
        getModelItem().removeExecutionListener(this);
        inspectorPanel.release();
        return super.release();
    }

    private JComponent buildToolbar() {
        JXToolBar toolBar = UISupport.createToolbar();
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
}
