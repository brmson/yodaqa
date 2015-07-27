package cz.brmlab.yodaqa.provider.sqlite;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.JDBC;

import java.sql.*;
import java.util.List;

/**
 * Created by honza on 14.7.15.
 */
public class SqliteConnector {
	private final Logger logger = LoggerFactory.getLogger(SqliteConnector.class);
	private Connection connection;

	public SqliteConnector() {
		connection = null;
	}

	public void connect() {
		Statement stmt;
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.home") + "/bingresults-master.db");
			stmt = connection.createStatement();
			String sql = "CREATE TABLE IF NOT EXISTS Answer " +
					"(ID			 INTEGER	  PRIMARY KEY AUTOINCREMENT," +
					" QUERY 		 TEXT 	  NOT NULL," +
					" TITLE          TEXT     NOT NULL, " +
					" DESCRIPTION    TEXT, " +
					" URL    		 TEXT, " +
					" RANK			 INTEGER)";
			stmt.executeUpdate(sql);
			stmt.close();
		} catch ( Exception e ) {
			logger.error(e.getMessage());
		}
	}

	public void insert(String table, String[] columns, Object[] values) {
		Statement stmt;
		String colNames = StringUtils.join(columns, ", ");
		String vals = StringUtils.join(values, ", ");
		String sql = "";
		try {
			stmt = connection.createStatement();
			sql = "INSERT INTO " + table + " (" + colNames + ")"
					+ " VALUES (" + vals + ");";
//			logger.info("SQL " + sql);
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			logger.error("Unable to save results to db: " + e.getMessage() + "\n"
				+ "SQL query: " + sql);
		}
	}

	public ResultSet select(String table, String where) throws SQLException {
		Statement stmt = connection.createStatement();
		String sql = String.format("SELECT * from %s WHERE %s", table, where);
		ResultSet res = stmt.executeQuery(sql);
		return res;
	}

	public void close() throws SQLException {
		connection.close();
	}
}
