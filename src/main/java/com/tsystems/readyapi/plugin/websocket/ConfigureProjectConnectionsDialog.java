package com.tsystems.readyapi.plugin.websocket;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.eviware.soapui.impl.wsdl.actions.project.SimpleDialog;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JXToolBar;

public class ConfigureProjectConnectionsDialog extends SimpleDialog {

    /** serialVersionUID description. */
    private static final long serialVersionUID = 1166476207933608609L;
    private JTable grid;
    private Project connectionsTargetItem;
    private ConnectionsTableModel tableModel;
    private Action editAction;
    private Action removeAction;

    protected ConfigureProjectConnectionsDialog(ModelItem modelItem) {
        super("Configure Connections to Websocket Servers",
                "Add, remove or edit connections to websocket servers needed for the project", null, true);
        if (modelItem instanceof Project)
            connectionsTargetItem = (Project) modelItem;
        else
            connectionsTargetItem = ModelSupport.getModelItemProject(modelItem);
    }

    public static boolean showDialog(ModelItem modelItem) {
        ConfigureProjectConnectionsDialog dialog = new ConfigureProjectConnectionsDialog(modelItem);
        try {
            dialog.setModal(true);
            UISupport.centerDialog(dialog);
            UISupport.centerDialog(dialog);
            dialog.setVisible(true);
            return true;
        } finally {
            dialog.dispose();
        }
    }

    @Override
    protected Component buildContent() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buildToolbar(), BorderLayout.NORTH);
        mainPanel.add(buildGrid(), BorderLayout.CENTER);
        return mainPanel;
    }

    private JComponent buildGrid() {
        tableModel = new ConnectionsTableModel();
        grid = new JTable(tableModel);
        for (int i = 0; i < ConnectionsTableModel.Column.values().length; ++i)
            grid.getColumnModel().getColumn(i).setIdentifier(ConnectionsTableModel.Column.values()[i]);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        grid.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                editAction.setEnabled(grid.getSelectionModel().getLeadSelectionIndex() >= 0);
                removeAction.setEnabled(grid.getSelectedRowCount() > 0);
            }
        });
        tableModel.setData(ConnectionsManager.getAvailableConnections(connectionsTargetItem));
        tableModel.setUsageData(formUsageData());
        grid.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JPasswordField pswEdit = new JPasswordField();
        grid.getColumn(ConnectionsTableModel.Column.Password).setCellRenderer(
                new PasswordRenderer(pswEdit.getEchoChar()));
        grid.getColumn(ConnectionsTableModel.Column.Password).setCellEditor(new DefaultCellEditor(pswEdit));
        return new JScrollPane(grid);
    }

    private JComponent buildToolbar() {
        JXToolBar toolBar = UISupport.createToolbar();
        Action addAction = new AddConnectionAction();
        toolBar.add(UISupport.createActionButton(addAction, addAction.isEnabled()));
        editAction = new EditAction();
        JButton editButton = UISupport.createActionButton(editAction, editAction.isEnabled());
        toolBar.add(editButton);
        removeAction = new RemoveConnectionAction();
        JButton removeButton = UISupport.createActionButton(removeAction, removeAction.isEnabled());
        toolBar.add(removeButton);
        return toolBar;
    }

    private HashMap<Connection, List<TestStep>> formUsageData() {
        HashMap<Connection, List<TestStep>> usageData = new HashMap<>();
        List<? extends TestSuite> testSuites = connectionsTargetItem.getTestSuiteList();
        if (testSuites != null)
            for (TestSuite testSuite : testSuites) {
                List<? extends TestCase> testCases = testSuite.getTestCaseList();
                if (testCases == null)
                    continue;
                for (TestCase testCase : testCases) {
                    List<TestStep> testSteps = testCase.getTestStepList();
                    if (testSteps == null)
                        continue;
                    for (TestStep testStep : testSteps)
                        if (testStep instanceof ConnectedTestStep) {
                            Connection testStepConnection = ((ConnectedTestStep) testStep).getConnection();
                            if (testStepConnection != null) {
                                List<TestStep> usingItems = usageData.get(testStepConnection);
                                if (usingItems == null) {
                                    usingItems = new ArrayList<>();
                                    usageData.put(testStepConnection, usingItems);
                                }
                                usingItems.add(testStep);
                            }
                        }
                }
            }
        return usageData;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 350);
    }

    @Override
    protected boolean handleOk() {
        for (int i = 0; i < tableModel.getRowCount(); ++i) {
            ConnectionRecord checkedRecord = tableModel.getItem(i);
            if (StringUtils.isNullOrEmpty(checkedRecord.name)) {
                grid.getSelectionModel().clearSelection();
                grid.editCellAt(i, ConnectionsTableModel.Column.Name.ordinal());
                UISupport
                        .showErrorMessage("Please specify a name for the connection to have a possibility to identify it later.");
                return false;
            }
            for (int j = 0; j < i; ++j)
                if (Utils.areStringsEqual(tableModel.getItem(j).name, checkedRecord.name,
                        Connection.ARE_NAMES_CASE_INSENSITIVE)) {
                    grid.getSelectionModel().clearSelection();
                    grid.editCellAt(i, ConnectionsTableModel.Column.Name.ordinal());
                    UISupport
                            .showErrorMessage("There are other connections with the same name. Please make it unique.");
                    return false;

                }
            if (StringUtils.isNullOrEmpty(checkedRecord.params.serverUri)) {
                grid.clearSelection();
                grid.editCellAt(i, ConnectionsTableModel.Column.ServerUri.ordinal());
                UISupport.showErrorMessage("Please specify URI of websocket server");
                return false;
            }
        }
        if (tableModel.getRemovedConnections() != null)
            for (Connection connection : tableModel.getRemovedConnections()) {
                List<TestStep> usingTestSteps = tableModel.getUsageData().get(connection);
                if (usingTestSteps != null && !usingTestSteps.isEmpty())
                    for (TestStep testStep : usingTestSteps)
                        ((ConnectedTestStep) testStep).setConnection(null);
                ConnectionsManager.removeConnection(connectionsTargetItem, connection);
            }
        for (int i = 0; i < tableModel.getRowCount(); ++i) {
            ConnectionRecord record = tableModel.getItem(i);
            if (record.originalConnection == null) {
                Connection connection = new Connection(record.name, record.params);
                ConnectionsManager.addConnection(connectionsTargetItem, connection);
            } else {
                record.originalConnection.setName(record.name);
                record.originalConnection.setParams(record.params);
            }
        }
        return true;
    }

    private class AddConnectionAction extends AbstractAction {
        /** serialVersionUID description. */
        private static final long serialVersionUID = 7313669609017703632L;

        public AddConnectionAction() {
            putValue(Action.SHORT_DESCRIPTION, "Add Connection");
            putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/eviware/soapui/resources/images/add.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            EditConnectionDialog.Result result = EditConnectionDialog.showDialog("Create Connection",
                    connectionsTargetItem, null, null, null);
            if (result != null)
                tableModel.addItem(result.connectionName, result.connectionParams);

        }
    }

    private static class ConnectionRecord {
        public String name;
        public ConnectionParams params;
        public Connection originalConnection;
    }

    private static class ConnectionsTableModel extends AbstractTableModel {
        /** serialVersionUID description. */
        private static final long serialVersionUID = -3787980610508008745L;

        private ArrayList<ConnectionRecord> data;

        private ArrayList<Connection> removedConnections;
        private HashMap<Connection, List<TestStep>> usageData;

        public int addItem(String name, ConnectionParams params) {
            ConnectionRecord record = new ConnectionRecord();
            record.name = name;
            record.params = params;
            data.add(record);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
            return data.size() - 1;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (Column.values()[columnIndex] == Column.Used)
                return Boolean.class;
            else
                return super.getColumnClass(columnIndex);
        }

        @Override
        public int getColumnCount() {
            return Column.values().length;
        }

        @Override
        public String getColumnName(int column) {
            return Column.values()[column].getCaption();
        }

        public ConnectionRecord getItem(int row) {
            return data.get(row);
        }

        public List<Connection> getRemovedConnections() {
            return removedConnections;
        }

        @Override
        public int getRowCount() {
            return data == null ? 0 : data.size();
        }

        public HashMap<Connection, List<TestStep>> getUsageData() {
            return usageData;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (Column.values()[columnIndex]) {
            case Name:
                return data.get(rowIndex).name;
            case ServerUri:
                return data.get(rowIndex).params.serverUri;
            case Subprotocols:
                return data.get(rowIndex).params.subprotocols;
            case Login:
                return data.get(rowIndex).params.login;
            case Password:
                return data.get(rowIndex).params.password;
            case Used:
                Connection connection = data.get(rowIndex).originalConnection;
                if (connection == null)
                    return false;
                List<TestStep> involvingTestSteps = usageData.get(connection);
                return involvingTestSteps != null && !involvingTestSteps.isEmpty();
            default:
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            Column column = Column.values()[columnIndex];
            return column == Column.Name || column == Column.ServerUri || column == Column.Subprotocols
                    || column == Column.Login || column == Column.Password;
        }

        public void removeItem(int row) {
            if (data.get(row).originalConnection != null)
                removedConnections.add(data.get(row).originalConnection);
            data.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public void setData(List<Connection> data) {
            this.data = new ArrayList<>(data == null ? 5 : data.size() + 5);
            removedConnections = new ArrayList<>();
            if (data != null)
                for (Connection connection : data) {
                    ConnectionRecord record = new ConnectionRecord();
                    record.name = connection.getName();
                    record.originalConnection = connection;
                    record.params = connection.getParams();
                    this.data.add(record);
                }
            fireTableDataChanged();
        }

        public void setUsageData(HashMap<Connection, List<TestStep>> usageData) {
            this.usageData = usageData;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            switch (Column.values()[columnIndex]) {
            case Name:
                data.get(rowIndex).name = (String) aValue;
                break;
            case ServerUri:
                data.get(rowIndex).params.serverUri = (String) aValue;
                break;
            case Subprotocols:
                data.get(rowIndex).params.subprotocols = (String) aValue;
                break;
            case Login:
                data.get(rowIndex).params.login = (String) aValue;
                break;
            case Password:
                data.get(rowIndex).params.password = (String) aValue;
                break;
            }
        }

        public void updateItem(int row, String name, ConnectionParams params) {
            data.get(row).name = name;
            data.get(row).params = params;
            fireTableRowsUpdated(row, row);
        }

        public enum Column {
            Name("Name"), ServerUri("Websocket Server URI"), Subprotocols("Subprotocols"), Login("Login"), Password(
                    "Password"), Used("Used by Test Steps");
            private final String caption;

            Column(String caption) {
                this.caption = caption;
            }

            public String getCaption() {
                return caption;
            }
        }
    }

    private class EditAction extends AbstractAction {
        /** serialVersionUID description. */
        private static final long serialVersionUID = 3951707484497149469L;

        public EditAction() {
            putValue(Action.SHORT_DESCRIPTION, "Configure Selected Connection");
            putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/eviware/soapui/resources/images/options.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int rowNo = grid.getSelectionModel().getLeadSelectionIndex();
            if (rowNo < 0)
                return;
            ConnectionRecord focusedRecord = tableModel.getItem(rowNo);
            EditConnectionDialog.Result result = EditConnectionDialog.showDialog(
                    String.format("Edit %s Connection", focusedRecord.name), connectionsTargetItem, focusedRecord.name,
                    focusedRecord.params, null);
            if (result != null)
                tableModel.updateItem(rowNo, result.connectionName, result.connectionParams);

        }
    }

    private static class PasswordRenderer extends DefaultTableCellRenderer {
        /** serialVersionUID description. */
        private static final long serialVersionUID = -8129935536446652833L;
        private char passwordChar;

        public PasswordRenderer(char passwordChar) {
            super();
            this.passwordChar = passwordChar;
        }

        @Override
        public void setValue(Object value) {
            if (value == null || value.toString() == null)
                setText("");
            else {
                char[] arr = new char[value.toString().length()];
                Arrays.fill(arr, passwordChar);
                setText(new String(arr));
            }
        }
    }

    private class RemoveConnectionAction extends AbstractAction {
        /** serialVersionUID description. */
        private static final long serialVersionUID = -8080128635589936389L;

        public RemoveConnectionAction() {
            putValue(Action.SHORT_DESCRIPTION, "Remove Selected Connections");
            putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/eviware/soapui/resources/images/delete.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final int maxCount = 4;
            int[] rows = grid.getSelectedRows();
            if (rows == null || rows.length == 0)
                return;
            int connectionNo = 0;
            StringBuffer msg = new StringBuffer();
            if (rows.length == 1) {
                String connectionName = tableModel.getItem(rows[0]).name;
                if (StringUtils.isNullOrEmpty(connectionName))
                    msg.append("Do you really want to delete this connection?");
                else
                    msg.append("Do you really want to delete \"" + connectionName + "\" connection?");
            } else {
                msg.append("Do you really want to delete these connections?");
                for (int row : rows) {
                    ConnectionRecord record = tableModel.getItem(row);
                    if (connectionNo != maxCount) {
                        msg.append('\n');
                        if (StringUtils.hasContent(record.name))
                            msg.append(record.name);
                        else
                            msg.append("<untitled>");
                        connectionNo++;
                    } else
                        msg.append("\n...");
                }
            }
            List<String> affectedModelItems = getAffectedModelItems(rows, maxCount);
            if (affectedModelItems != null && !affectedModelItems.isEmpty()) {
                msg.append("\nNote, that the following test step(s) will be deprived of a connection and have to be customized later:");
                for (int i = 0; i < affectedModelItems.size(); ++i) {
                    msg.append('\n');
                    msg.append(affectedModelItems.get(i));
                }

            }
            if (UISupport.getDialogs().confirm(msg.toString(), "Confirm deletion")) {
                Arrays.sort(rows);
                for (int i = rows.length - 1; i >= 0; --i)
                    tableModel.removeItem(rows[i]);
            }
        }

        private List<String> getAffectedModelItems(int[] rows, int maxRowCount) {
            ArrayList<String> result = new ArrayList<>();
            List<? extends TestSuite> testSuites = connectionsTargetItem.getTestSuiteList();
            if (testSuites != null)
                for (TestSuite testSuite : testSuites) {
                    List<? extends TestCase> testCases = testSuite.getTestCaseList();
                    if (testCases == null)
                        continue;
                    for (TestCase testCase : testCases) {
                        List<TestStep> testSteps = testCase.getTestStepList();
                        if (testSteps == null)
                            continue;
                        for (TestStep testStep : testSteps)
                            if (testStep instanceof ConnectedTestStep) {
                                Connection testStepConnection = ((ConnectedTestStep) testStep).getConnection();
                                if (testStepConnection != null)
                                    for (int row : rows) {
                                        ConnectionRecord record = tableModel.getItem(row);
                                        if (record.originalConnection == testStepConnection)
                                            if (result.size() == maxRowCount) {
                                                result.add("...");
                                                return result;
                                            } else
                                                result.add(String.format("\"%s\" of \"%s\" test case",
                                                        testStep.getName(), testCase.getName()));
                                    }
                            }
                    }
                }
            return result;
        }
    }

}
