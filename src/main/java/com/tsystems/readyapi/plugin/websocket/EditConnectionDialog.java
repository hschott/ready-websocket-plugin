package com.tsystems.readyapi.plugin.websocket;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import com.eviware.soapui.impl.wsdl.actions.project.SimpleDialog;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JUndoableTextField;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.binding.value.AbstractValueModel;

public class EditConnectionDialog extends SimpleDialog {

    /** serialVersionUID description. */
    private static final long serialVersionUID = -5308487928062110765L;

    private final static Insets defaultInsets = new Insets(4, 4, 4, 4);

    private final static Insets defaultInsetsWithIndent = new Insets(defaultInsets.top, defaultInsets.left + 12,
            defaultInsets.bottom, defaultInsets.right);
    private JTextField nameEdit;
    private JUndoableTextField serverUriEdit;
    private JUndoableTextField subprotocolEdit;
    private JCheckBox authRequiredCheckBox;

    private JUndoableTextField loginEdit;
    private JPasswordField passwordEdit;
    private JCheckBox hidePasswordCheckBox;
    private char passwordChar;

    private ModelItem modelItemOfConnection;

    private String initialName;
    private Connection connection;
    private List<String> presentNames;
    private Result result = null;

    protected EditConnectionDialog(String title, ModelItem modelItemOfConnection, String initialConnectionName,
            ConnectionParams initialConnectionParams, List<String> alreadyPresentNames) {
        super(title, "Please specify the parameters for the connection", null, true);
        this.modelItemOfConnection = modelItemOfConnection;
        presentNames = alreadyPresentNames;
        initialName = initialConnectionName;
        if (initialConnectionParams == null) {
            connection = new Connection();
            connection.setName(initialConnectionName);
        } else
            connection = new Connection(initialConnectionName, initialConnectionParams);
    }

    public static Result createConnection(ModelItem modelItemOfConnection) {
        Project project = ModelSupport.getModelItemProject(modelItemOfConnection);
        ArrayList<String> existingNames = new ArrayList<>();
        List<Connection> connections = ConnectionsManager.getAvailableConnections(project);
        if (connections != null)
            for (Connection curConnection : connections)
                existingNames.add(curConnection.getName());
        return showDialog("Create New Connection", project, null, null, existingNames);

    }

    public static Result editConnection(Connection connection, ModelItem modelItemOfConnection) {
        Project project = ModelSupport.getModelItemProject(modelItemOfConnection);
        ArrayList<String> existingNames = new ArrayList<>();
        List<Connection> connections = ConnectionsManager.getAvailableConnections(project);
        if (connections != null)
            for (Connection curConnection : connections)
                if (curConnection != connection)
                    existingNames.add(curConnection.getName());
        String title = String.format("Configure %s Connection", connection.getName());
        return showDialog(title, project, connection.getName(), connection.getParams(), existingNames);

    }

    public static Result showDialog(String title, ModelItem modelItemOfConnection, String initialConnectionName,
            ConnectionParams initialConnectionParams, List<String> alreadyPresentNames) {
        EditConnectionDialog dialog = new EditConnectionDialog(title, modelItemOfConnection, initialConnectionName,
                initialConnectionParams, alreadyPresentNames);
        try {
            dialog.setModal(true);
            UISupport.centerDialog(dialog);
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
        return dialog.result;

    }

    @Override
    protected void beforeShow() {
        if (StringUtils.isNullOrEmpty(connection.getLogin())) {
            loginEdit.setEnabled(false);
            passwordEdit.setEnabled(false);
            hidePasswordCheckBox.setEnabled(false);
            authRequiredCheckBox.setSelected(false);
        } else
            authRequiredCheckBox.setSelected(true);
    }

    @Override
    protected Component buildContent() {
        final int defEditCharCount = 30;
        new Dimension(50, 180);
        final int indentSize = 20;

        int row = 0;

        PresentationModel<Connection> pm = new PresentationModel<Connection>(connection);
        JPanel mainPanel = new JPanel(new GridBagLayout());

        nameEdit = new JUndoableTextField(defEditCharCount);
        nameEdit.setToolTipText("The unique connection name to identify it.");
        Bindings.bind(nameEdit, pm.getModel("name"));
        mainPanel.add(nameEdit, componentPlace(row));
        mainPanel.add(createLabel("Name:", nameEdit, 0), labelPlace(row));
        ++row;

        serverUriEdit = new JUndoableTextField(defEditCharCount);
        serverUriEdit.setToolTipText("The websocket server URI");
        Bindings.bind(serverUriEdit, pm.getModel("serverUri"));
        mainPanel.add(serverUriEdit, componentPlace(row));
        mainPanel.add(createLabel("Server URI:", serverUriEdit, 0), labelPlace(row));
        ++row;

        subprotocolEdit = new JUndoableTextField(defEditCharCount);
        subprotocolEdit.setToolTipText("Fill this field if you want to connect with subprotocol or leave it empty");
        Bindings.bind(subprotocolEdit, pm.getModel(Connection.SUB_PROTOCOLS_BEAN_PROP));
        mainPanel.add(subprotocolEdit, componentPlace(row));
        mainPanel.add(createLabel("Subprotocols (optional):", subprotocolEdit, 0), labelPlace(row));
        ++row;

        JPanel indent = new JPanel();
        indent.setPreferredSize(new Dimension(1, indentSize));
        mainPanel.add(indent, componentPlace(row));
        ++row;

        authRequiredCheckBox = new JCheckBox("The server requires authentication");
        authRequiredCheckBox.setToolTipText("Check if the websocket server requires authentication to connect");
        authRequiredCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginEdit.setEnabled(authRequiredCheckBox.isSelected());
                passwordEdit.setEnabled(authRequiredCheckBox.isSelected());
                hidePasswordCheckBox.setEnabled(authRequiredCheckBox.isSelected());
                if (authRequiredCheckBox.isSelected())
                    loginEdit.grabFocus();
            }
        });
        mainPanel.add(authRequiredCheckBox, largePlace(row));
        mainPanel.add(createLabel("Authentication:", authRequiredCheckBox, 0), labelPlace(row));
        ++row;

        loginEdit = new JUndoableTextField(defEditCharCount);
        loginEdit.setToolTipText("Login for websocket server");
        Bindings.bind(loginEdit, pm.getModel(Connection.LOGIN_BEAN_PROP));
        mainPanel.add(loginEdit, componentPlace(row));
        mainPanel.add(createLabel("Login:", loginEdit, 0), labelPlaceWithIndent(row));
        ++row;

        passwordEdit = new JPasswordField(defEditCharCount);
        passwordEdit.setToolTipText("Password for websocket server");
        Bindings.bind(passwordEdit, pm.getModel(Connection.PASSWORD_BEAN_PROP));
        mainPanel.add(passwordEdit, componentPlace(row));
        mainPanel.add(createLabel("Password:", passwordEdit, 0), labelPlaceWithIndent(row));
        hidePasswordCheckBox = new JCheckBox("Hide", true);
        hidePasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hidePasswordCheckBox.isSelected())
                    passwordEdit.setEchoChar(passwordChar);
                else {
                    passwordChar = passwordEdit.getEchoChar();
                    passwordEdit.setEchoChar('\0');
                }
            }
        });
        mainPanel.add(hidePasswordCheckBox, extraComponentPlace(row));
        ++row;

        PropertyExpansionPopupListener.enable(serverUriEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(loginEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(passwordEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(subprotocolEdit, modelItemOfConnection);

        return new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    private GridBagConstraints componentPlace(int row) {
        return new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE,
                defaultInsets, 0, 0);
    }

    private JLabel createLabel(String text, final JComponent targetComponent, int hitCharNo) {
        JLabel label = new JLabel(text);
        label.setLabelFor(targetComponent);
        if (targetComponent != null) {
            Bindings.bind(label, "visible", new IsVisibleValueModel(targetComponent));
            Bindings.bind(label, "enabled", new PropertyAdapter<JComponent>(targetComponent, "enabled", true));
        }
        if (hitCharNo >= 0) {
            label.setDisplayedMnemonic(text.charAt(hitCharNo));
            label.setDisplayedMnemonicIndex(hitCharNo);
        }
        return label;
    }

    private GridBagConstraints extraComponentPlace(int row) {
        return new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE,
                defaultInsets, 0, 0);
    }

    @Override
    protected boolean handleOk() {
        if (StringUtils.isNullOrEmpty(nameEdit.getText())) {
            nameEdit.grabFocus();
            UISupport.showErrorMessage("Please specify a name for the connection.");
            return false;
        }
        if (presentNames != null
                && !Utils.areStringsEqual(initialName, nameEdit.getText(), Connection.ARE_NAMES_CASE_INSENSITIVE)) {
            boolean alreadyExists = false;
            for (String name : presentNames)
                if (Utils.areStringsEqual(nameEdit.getText(), name, Connection.ARE_NAMES_CASE_INSENSITIVE)) {
                    alreadyExists = true;
                    break;
                }
            if (alreadyExists) {
                nameEdit.grabFocus();
                UISupport.showErrorMessage(String.format(
                        "The connection with \"%s\" name already exists. Please specify another name.",
                        nameEdit.getText()));
                return false;
            }
        }

        if (StringUtils.isNullOrEmpty(serverUriEdit.getText())) {
            serverUriEdit.grabFocus();
            UISupport.showErrorMessage("Please specify URI of websocket server.");
            return false;
        }
        if (authRequiredCheckBox.isSelected() && StringUtils.isNullOrEmpty(loginEdit.getText())) {
            loginEdit.grabFocus();
            UISupport
                    .showErrorMessage("Please specify a login or uncheck \"Authentication required\" check-box if the authentication is not required for this websocket server.");
            return false;
        }

        result = new Result();
        if (nameEdit != null)
            result.connectionName = nameEdit.getText();

        result.connectionParams.serverUri = serverUriEdit.getText();
        result.connectionParams.subprotocols = subprotocolEdit.getText();

        if (authRequiredCheckBox.isSelected())
            result.connectionParams.setCredentials(loginEdit.getText(), new String(passwordEdit.getPassword()));
        else
            result.connectionParams.setCredentials(null, null);
        return true;
    }

    private GridBagConstraints labelPlace(int row) {
        return new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE,
                defaultInsets, 0, 0);
    }

    private GridBagConstraints labelPlaceWithIndent(int row) {
        return new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE,
                defaultInsetsWithIndent, 0, 0);
    }

    private GridBagConstraints largePlace(int row) {
        return new GridBagConstraints(1, row, 2, 1, 0, 0, GridBagConstraints.BASELINE_LEADING,
                GridBagConstraints.HORIZONTAL, defaultInsets, 0, 0);
    }

    public static class IsCheckedValueModel extends AbstractValueModel {
        private static final long serialVersionUID = -7698588095167209408L;

        private JCheckBox component;

        public IsCheckedValueModel(final JCheckBox component) {
            this.component = component;
            this.component.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    fireValueChange(!component.isSelected(), component.isSelected());
                }
            });
        }

        @Override
        public Object getValue() {
            return component.isSelected();
        }

        @Override
        public void setValue(Object newValue) {
            //
        }

    }

    public static class IsVisibleValueModel extends AbstractValueModel {
        private static final long serialVersionUID = -3278995837998406344L;

        private JComponent component;

        public IsVisibleValueModel(JComponent component) {
            this.component = component;
            this.component.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    fireValueChange(true, false);
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    fireValueChange(false, true);
                }
            });
        }

        @Override
        public Object getValue() {
            return component.isVisible();
        }

        @Override
        public void setValue(Object newValue) {
            //
        }
    }

    public class Result {
        public String connectionName;
        public ConnectionParams connectionParams;

        public Result() {
            connectionParams = new ConnectionParams();
        }
    }

    public class SelectFileAction extends AbstractAction {
        /** serialVersionUID description. */
        private static final long serialVersionUID = -8407757799130481919L;
        private JFileChooser fileChooser;
        private JTextField fileNameEdit;

        public SelectFileAction(JTextField fileNameEdit) {
            super("Browse...");
            this.fileNameEdit = fileNameEdit;
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
