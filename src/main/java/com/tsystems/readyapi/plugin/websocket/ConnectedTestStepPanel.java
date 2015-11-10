package com.tsystems.readyapi.plugin.websocket;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;

import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.list.SelectionInList;
import com.tsystems.readyapi.plugin.websocket.ConnectedTestStep.TimeMeasure;

public abstract class ConnectedTestStepPanel<T extends ConnectedTestStep> extends ModelItemDesktopPanel<T> {

    /** serialVersionUID description. */
    private static final long serialVersionUID = 7510275395689660990L;
    private final static String NEW_CONNECTION_ITEM = "<New Connection...>";
    private JButton configureConnectionButton;
    private ConnectionsComboBoxModel connectionsModel;

    public ConnectedTestStepPanel(T modelItem) {
        super(modelItem);
    }

    protected void addConnectionActionsToToolbar(JXToolBar toolBar) {
        Action configureConnectionsAction = new ConfigureConnectionsAction();
        JButton button = UISupport.createActionButton(configureConnectionsAction,
                configureConnectionsAction.isEnabled());
        toolBar.add(button);
    }

    protected void buildConnectionSection(SimpleBindingForm form, PresentationModel<T> pm) {
        connectionsModel = new ConnectionsComboBoxModel();
        ConnectionsManager.addConnectionsListener(getModelItem(), connectionsModel);

        form.appendHeading("Connection to Websocket Server");
        form.appendComboBox("Connection", connectionsModel, "Choose one of pre-configured connections");
        configureConnectionButton = form.appendButtonWithoutLabel("Configure...", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Connection connection = getModelItem().getConnection();
                EditConnectionDialog.Result dialogResult = EditConnectionDialog.editConnection(connection,
                        getModelItem());
                if (dialogResult != null) {
                    connection.setName(dialogResult.connectionName);
                    connection.setParams(dialogResult.connectionParams);
                }
            }
        });
        configureConnectionButton.setIcon(UISupport.createImageIcon("com/eviware/soapui/resources/images/options.png"));
        configureConnectionButton.setEnabled(getModelItem().getConnection() != null);
    }

    protected void buildRadioButtonsFromEnum(SimpleBindingForm form, PresentationModel<T> pm, String label,
            String propertyName, Class<?> propertyType) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        for (Object option : propertyType.getEnumConstants()) {
            UIOption uiOption = (UIOption) option;
            JRadioButton radioButton = new JRadioButton(uiOption.getTitle());
            Bindings.bind(radioButton, pm.getModel(propertyName), option);
            panel.add(radioButton);
        }
        form.append(label, panel);
    }

    protected void buildTimeoutSpinEdit(SimpleBindingForm form, PresentationModel<T> pm, String label) {
        JPanel timeoutPanel = new JPanel();
        timeoutPanel.setLayout(new BoxLayout(timeoutPanel, BoxLayout.X_AXIS));
        JSpinner spinEdit = Utils.createBoundSpinEdit(pm, "shownTimeout", 0, Integer.MAX_VALUE, 1);
        spinEdit.setPreferredSize(new Dimension(80, spinEdit.getHeight()));
        timeoutPanel.add(spinEdit);
        JComboBox<TimeMeasure> measureCombo = new JComboBox<TimeMeasure>(TimeMeasure.values());
        Bindings.bind(measureCombo, new SelectionInList<Object>(TimeMeasure.values(), pm.getModel("timeoutMeasure")));
        timeoutPanel.add(measureCombo);
        timeoutPanel.add(new JLabel(" (0 - forever)"));
        form.append(label, timeoutPanel);

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        if (Utils.areStringsEqual(evt.getPropertyName(), "connection")) {
            if (connectionsModel != null)
                connectionsModel.setSelectedItem(new ConnectionComboItem((Connection) evt.getNewValue()));
            configureConnectionButton.setEnabled(evt.getNewValue() != null);
        }
    }

    @Override
    protected boolean release() {
        if (connectionsModel != null)
            ConnectionsManager.removeConnectionsListener(getModelItem(), connectionsModel);
        return super.release();
    }

    class ConfigureConnectionsAction extends AbstractAction {
        /** serialVersionUID description. */
        private static final long serialVersionUID = -3402287439060333406L;

        public ConfigureConnectionsAction() {
            putValue(Action.SHORT_DESCRIPTION, "Configure Websocket Connections of the Project");
            putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/eviware/soapui/resources/images/options.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ConfigureProjectConnectionsDialog.showDialog(getModelItem());
        }
    }

    static class ConnectionComboItem {
        private Connection obj;

        public ConnectionComboItem(Connection connection) {
            obj = connection;
        }

        @Override
        public boolean equals(Object op) {
            if (op instanceof ConnectionComboItem)
                return ((ConnectionComboItem) op).obj == obj;
            else
                return false;
        }

        public Connection getObject() {
            return obj;
        }

        @Override
        public String toString() {
            return obj.getName();
        }
    }

    class ConnectionsComboBoxModel extends AbstractListModel<ConnectionComboItem> implements
            ComboBoxModel<ConnectionComboItem>, ConnectionsListener {

        /** serialVersionUID description. */
        private static final long serialVersionUID = -7769726049010958997L;
        private ArrayList<ConnectionComboItem> items = new ArrayList<>();
        private boolean connectionCreationInProgress = false;

        public ConnectionsComboBoxModel() {
            updateItems();
        }

        @Override
        public void connectionChanged(Connection connection, String propertyName, Object oldPropertyValue,
                Object newPropertyValue) {
            if (Utils.areStringsEqual(propertyName, "name"))
                for (int i = 0; i < items.size(); ++i)
                    if (items.get(i).getObject() == connection) {
                        fireContentsChanged(connection, i, i);
                        break;
                    }
        }

        @Override
        public void connectionListChanged() {
            updateItems();
        }

        @Override
        public ConnectionComboItem getElementAt(int index) {
            return items.get(index);
        }

        @Override
        public Object getSelectedItem() {
            if (connectionCreationInProgress)
                return NewConnectionComboItem.getInstance();
            if (getModelItem().getConnection() == null)
                return null;
            return new ConnectionComboItem(getModelItem().getConnection());
        }

        @Override
        public int getSize() {
            return items.size();
        }

        @Override
        public void setSelectedItem(Object anItem) {
            if (anItem == null)
                getModelItem().setConnection(null);
            else if (anItem instanceof NewConnectionComboItem) {
                connectionCreationInProgress = true;
                UISupport.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EditConnectionDialog.Result dialogResult = EditConnectionDialog
                                .createConnection(getModelItem());
                        connectionCreationInProgress = false;
                        if (dialogResult == null)
                            fireContentsChanged(ConnectionsComboBoxModel.this, -1, -1);
                        else {
                            Connection newConnection = new Connection(dialogResult.connectionName,
                                    dialogResult.connectionParams);
                            ConnectionsManager.addConnection(getModelItem(), newConnection);
                            getModelItem().setConnection(newConnection);
                        }

                    }
                });
            } else {
                Connection newParams = ((ConnectionComboItem) anItem).getObject();
                getModelItem().setConnection(newParams);
                fireContentsChanged(this, -1, -1);
            }
        }

        private void updateItems() {
            items.clear();
            List<Connection> list = ConnectionsManager.getAvailableConnections(getModelItem());
            if (list != null)
                for (Connection curParams : list)
                    items.add(new ConnectionComboItem(curParams));
            items.add(NewConnectionComboItem.getInstance());
            fireContentsChanged(this, -1, -1);
        }

    }

    static class NewConnectionComboItem extends ConnectionComboItem {
        private final static NewConnectionComboItem instance = new NewConnectionComboItem();

        private NewConnectionComboItem() {
            super(null);
        }

        public static NewConnectionComboItem getInstance() {
            return instance;
        }

        @Override
        public String toString() {
            return NEW_CONNECTION_ITEM;
        }
    }

    public interface UIOption {
        String getTitle();
    }

}
