package com.gvenzl.awr;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Exposes various Automatic Workload Repository (AWR) interfaces
 * @author gvenzl
 *
 */
public class AWR {
	
	private Connection conn = null;
	private static AWR instance = null;
	private int beginSnapshot = -1;
	private int endSnapshot = -1;
	private long dbid = -1;
	
	protected AWR() {
		// Exists only to defeat instantiation.
	}
	   
	/**
	 * Returns an instance of the AWR class
	 * @return An instance of the AWR class
	 */
	public static AWR getInstance() {
		if(instance == null) {
			instance = new AWR();
		}
		return instance;
	}

	public void setConnection(Connection conn) {
		this.conn = conn;
	}
	
	/**
	 * Creates a new snapshot
	 * @throws SQLException	Any SQL error that occurs during the operation
	 */
	public void createSnapshot() throws SQLException {
		
		if (null == conn) {
			throw new SQLException("No connection to the database");
		}
		
		PreparedStatement stmt = conn.prepareStatement(
				"SELECT DBMS_WORKLOAD_REPOSITORY.CREATE_SNAPSHOT() FROM dual");
		ResultSet rslt = stmt.executeQuery();
		rslt.next();
		int id = rslt.getInt(1);
		stmt.close();
		
		if (beginSnapshot == -1) {
			beginSnapshot = id;
			endSnapshot = -1;
		}
		else {
			endSnapshot = id;
		}
	}
	
	/**
	 * Sets the DBID for AWR generation
	 * @param conn	A connection to the database
	 * @throws SQLException Any SQL error that occurs during the operation
	 */
	private void setDBID() throws SQLException {
		// DBID is already set
		if (dbid != -1) {
			return;
		}
		
		if (null == conn) {
			throw new SQLException("No connection to the database");
		}
		
		PreparedStatement stmt = conn.prepareStatement("SELECT dbid FROM v$database");
		ResultSet rslt = stmt.executeQuery();
		rslt.next();
		dbid = rslt.getLong(1);
		rslt.close();
		stmt.close();
	}
	
	/**
	 * Returns an AWR report in plain text
	 * @param mode The {@link} AWR_MODE format of the report
	 * @return The AWR report
	 * @throws SQLException Any SQL error that occurs during the operation
	 */
	public String getAWRReport(AWR_MODE mode) throws SQLException {
		if (null == conn) {
			throw new SQLException("No connection to the database");
		}
		
		if (beginSnapshot == -1) {
			throw new SQLException("No begin snapshot availabe, create begin and end snapshots first!");
		}
		
		if (endSnapshot == -1) {
			throw new SQLException("No end snapshot available, create begin snapshot first!");
		}
		
		setDBID();

		PreparedStatement stmt;
		switch (mode) {
			case HTML: {
				stmt = conn.prepareStatement(
						"SELECT * FROM TABLE(DBMS_WORKLOAD_REPOSITORY.AWR_REPORT_HTML(?, 1, ?, ?))");
				break;
			}
			case TEXT:
			default: {
				stmt = conn.prepareStatement(
						"SELECT * FROM TABLE(DBMS_WORKLOAD_REPOSITORY.AWR_REPORT_TEXT(?, 1, ?, ?))");
			}
		}
		
		stmt.setLong(1, dbid);
		stmt.setInt(2, beginSnapshot);
		stmt.setInt(3, endSnapshot);
		ResultSet rslt = stmt.executeQuery();
		
		String retAWRReport = "";
		while (rslt.next()) {
			retAWRReport += rslt.getString(1) + '\n';
		}
		stmt.close();
		return retAWRReport;
	}
}
