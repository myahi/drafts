package calypso.lbp.qa.easy.tnr.step.file;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lbp.qa.easy.tnr.bean.Step;
import lbp.qa.easy.tnr.execution.exception.StepExecutionException;
import lbp.qa.easy.tnr.execution.log.Log;
import lbp.qa.easy.tnr.util.StepAttributeConstants;
import lbp.qa.easy.tnr.util.Util;
import org.apache.commons.io.IOUtils;

public class SqlToCSVStep extends Step {
  private static final long serialVersionUID = 4655233964123650891L;
  
  public void setAttributes() {
    addAttribute("DB_URL", "DataBase URL", null, true);
    addAttribute("DB_USER_NAME", "DataBase User", null, true);
    addAttribute("DB_PWD", "DataBase Password", null, true);
    addAttribute("DB_SQL_STATMENT", "DataBase SQL Statment", null, true);
    addAttribute("DB_SQL_COLUMN_LABEL", "The column label to export, use ; to seperate columns name", null, false);
    addAttribute("DB_CSV_TARGET_NAME", "The CSV target file", null, true);
    addAttribute("DB_COLUMN_DELIMITER", "The delimiter between column in the CSV target file", null, true);
    addAttribute("DB_COLUMN_DELIMITER", "The delimiter between column in the CSV target file", null, true);
    addAttribute("DB_CSV_SHOULD_PRINT_HEADER", "Should the CSV result file contain headers ?", StepAttributeConstants.YES_NO, false);
  }
  
  public boolean doSteplet(List<String> messages, Map<String, Object> collector) throws StepExecutionException {
    Properties props = new Properties();
    String url = getDynamicAttributeValue("DB_URL");
    props.setProperty("user", getDynamicAttributeValue("DB_USER_NAME"));
    props.setProperty("password", getDynamicAttributeValue("DB_PWD"));
    String sql = getDynamicAttributeValue("DB_SQL_STATMENT");
    String targetColumnLabels = getDynamicAttributeValue("DB_SQL_COLUMN_LABEL");
    String csvFileName = getDynamicAttributeValue("DB_CSV_TARGET_NAME");
    String columnDelimiter = getDynamicAttributeValue("DB_COLUMN_DELIMITER");
    boolean shouldPrintHeaders = Util.isTrue(
        getDynamicAttributeValue("DB_CSV_SHOULD_PRINT_HEADER"), false);
    FileWriter fw = null;
    Connection conn = null;
    try {
      conn = DriverManager.getConnection(url, props);
      Statement st = conn.createStatement();
      ResultSet resSet = st.executeQuery(sql);
      List<String> columnsNameList = Arrays.asList(targetColumnLabels.split(";"));
      if (!checkIfEntriesAreValide(columnsNameList, getColumnsLabel(resSet))) {
        Log.error(String.format("unable to map the target column labels: %s with the result set value: %s", new Object[] { columnsNameList, 
                getColumnsLabel(resSet) }));
        return false;
      } 
      fw = new FileWriter(csvFileName);
      if (shouldPrintHeaders) {
        for (String targetColumnName : columnsNameList) {
          fw.append(targetColumnName);
          if (columnsNameList.size() > 1)
            fw.append(columnDelimiter); 
        } 
        fw.append(System.getProperty("line.separator"));
      } 
      while (resSet.next()) {
        for (String columName : columnsNameList) {
          Object data = resSet.getObject(columName);
          String stringValue = (data != null) ? convertSQLObjToString(data, messages) : "";
          fw.append(stringValue);
          if (columnsNameList.size() > 1)
            fw.append(columnDelimiter); 
        } 
        fw.append(System.getProperty("line.separator"));
      } 
      fw.flush();
      fw.close();
    } catch (IOException e) {
      Log.error(this, e);
      messages.add(String.format("Unable de read/write CSV file %s", new Object[] { csvFileName }));
      return false;
    } catch (SQLException ex) {
      Log.error(this, ex);
      messages.add(String.format("Could execute SQL query: %s on Database : %s", new Object[] { sql, url }));
      return false;
    } finally {
      closeDBConnection(conn, messages);
    } 
    return true;
  }
  
  public String getUsage() {
    return "Execute SQL query and save the result set in a CSV file";
  }
  
  private List<String> getColumnsLabel(ResultSet res) throws SQLException {
    List<String> columnLabels = new ArrayList<>();
    for (int i = 1; i < res.getMetaData().getColumnCount() + 1; i++) {
      Log.info("Column name = " + res.getMetaData().getColumnLabel(i));
      columnLabels.add(res.getMetaData().getColumnLabel(i));
    } 
    return columnLabels;
  }
  
  private boolean checkIfEntriesAreValide(List<String> columnsNameList, List<String> resultSetColumnLabels) throws SQLException {
    if (columnsNameList.size() > resultSetColumnLabels.size())
      return false; 
    if (!resultSetColumnLabels.containsAll(columnsNameList))
      return false; 
    return true;
  }
  
  private void closeDBConnection(Connection connection, List<String> messages) {
    try {
      if (connection != null)
        connection.close(); 
    } catch (SQLException e) {
      messages.add(String.format("Could not close connection to the Database", new Object[0]));
    } 
  }
  
  private String convertSQLObjToString(Object data, List<String> messages) {
    if (data instanceof Clob)
      try {
        InputStream in = ((Clob)data).getAsciiStream();
        StringWriter w = new StringWriter();
        IOUtils.copy(in, w);
        return w.toString();
      } catch (IOException e) {
        messages.add(String.format("Unable to convert Clob to String", new Object[0]));
      } catch (SQLException e) {
        messages.add(String.format("Error while getting Clob data", new Object[0]));
      }  
    return data.toString();
  }
}
