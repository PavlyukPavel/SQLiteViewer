package viewer;

import org.sqlite.SQLiteDataSource;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.*;

public class SQLiteViewer extends JFrame {

    protected String dbName;
    protected JComboBox<String> tablesComboBox;
    protected SQLiteDataSource dataSource;
    protected JPanel pSelectDB;
    protected JTextField fileNameTextField;
    protected JButton openFileButton;
    protected JPanel pQuery;
    protected JTextArea queryTextArea;
    protected JButton executeQueryButton;
    protected JTable tableResults;

    // Конструктор
    public SQLiteViewer() {
        super("SQLite Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 400);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setResizable(false);
        setLocationRelativeTo(null);

        initComponents();

        setVisible(true);
    }

    // Заблокировать/разблокировать элементы
    private void enableComponents(boolean bEnabled) {
        pSelectDB.setEnabled(bEnabled);
        tablesComboBox.setEnabled(bEnabled);
        pQuery.setEnabled(bEnabled);
        tableResults.setEnabled(bEnabled);
        queryTextArea.setEnabled(bEnabled);
        executeQueryButton.setEnabled(bEnabled);
    }

    // Инициалиализация всех элементов формы
    private void initComponents() {
        pSelectDB = new JPanel();
        pSelectDB.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));

        fileNameTextField = new JTextField();
        fileNameTextField.setName("FileNameTextField");
        fileNameTextField.setColumns(40);
        pSelectDB.add(fileNameTextField);

        openFileButton = new JButton("Open");
        openFileButton.setName("OpenFileButton");
        pSelectDB.add(openFileButton);

        add(pSelectDB);

        tablesComboBox = new JComboBox<>();
        tablesComboBox.setName("TablesComboBox");
        tablesComboBox.setMaximumSize(new Dimension(500, 22));

        add(tablesComboBox);

        pQuery = new JPanel();
        pQuery.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));

        queryTextArea = new JTextArea(10,35);
        queryTextArea.setName("QueryTextArea");
        pQuery.add(queryTextArea);

        executeQueryButton = new JButton("Execute");
        executeQueryButton.setName("ExecuteQueryButton");
        pQuery.add(executeQueryButton);

        add(pQuery);

        tableResults = new JTable();
        tableResults.setName("Table");
        JScrollPane sp = new JScrollPane(tableResults);
        tableResults.setBackground(Color.white);
        add(sp);

        pSelectDB.setEnabled(false);
        pQuery.setEnabled(false);
        queryTextArea.setEnabled(false);
        executeQueryButton.setEnabled(false);

        openFileButton.addActionListener(e -> {
            dbName = fileNameTextField.getText().trim();
            File file = new File(dbName);
            if ("".equals(dbName)) {
                tablesComboBox.removeAllItems();
                JOptionPane.showMessageDialog(new Frame(), "Empty DB name!");
                enableComponents(false);
            } else if (!file.exists()) {
                tablesComboBox.removeAllItems();
                JOptionPane.showMessageDialog(new Frame(), "File doesn't exist!");
                enableComponents(false);
            } else {
                try {
                    if (fillTables()) {
                        enableComponents(true);
                    } else {
                        enableComponents(false);
                    }
                } catch (SQLException exception) {
                    JOptionPane.showMessageDialog(new Frame(), exception.getMessage());
                }
            }
        });

        tablesComboBox.addActionListener(e -> {
            String selectedTab = (String)tablesComboBox.getSelectedItem();
            if ((selectedTab == null) || ("".equals(selectedTab))) {
                queryTextArea.setText("");
            } else {
                queryTextArea.setText("SELECT * FROM "+ selectedTab + ";");
            }
        });

        executeQueryButton.addActionListener(e -> {
            String selectedTab = (String)tablesComboBox.getSelectedItem();
            if ("".equals(selectedTab)) {
                JOptionPane.showMessageDialog(new Frame(), "Please, select table!");
            } else {
                try (Connection con = dataSource.getConnection()) {
                    if (con.isValid(5)) {
                        Statement statement = con.createStatement();
                        ResultSet rs = statement.executeQuery(queryTextArea.getText().trim());
                        // Заполняем столбцы
                        ResultSetMetaData metaData = rs.getMetaData();
                        int iCols = metaData.getColumnCount();
                        String[] columns = new String[iCols];
                        for (int i = 1; i <= iCols; i++) {
                            columns[i-1] = metaData.getColumnName(i);
                        }
                        DefaultTableModel tableModel = new DefaultTableModel();
                        tableModel.setColumnIdentifiers(columns);
                        // Заполняем строки
                        while (rs.next()) {
                            String[] row = new String[iCols];
                            for (int i = 1; i <= iCols; i++){
                                row[i-1] = rs.getString(i);
                            }
                            tableModel.addRow(row);
                        }
                        // Выводим в табличку
                        tableResults.setModel(tableModel);
                    } else {
                        JOptionPane.showMessageDialog(new Frame(), "Error accessing database: timeout!");
                    }
                } catch (SQLException exception) {
                    JOptionPane.showMessageDialog(new Frame(), exception.getMessage());
                }
            }
        });
    }

    // Заполнить список таблиц
    private boolean fillTables() throws SQLException {
        tablesComboBox.removeAllItems();
        dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbName);
        try (Connection con = dataSource.getConnection()) {
            if (con.isValid(5)) {
                Statement statement = con.createStatement();
                ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name NOT LIKE 'sqlite_%'");
                String tabName;
                while (rs.next()) {
                    tabName = rs.getString("name");
                    tablesComboBox.addItem(tabName);
                }
            }
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(new Frame(), exception.getMessage());
            return false;
        }
        return true;
    }
}