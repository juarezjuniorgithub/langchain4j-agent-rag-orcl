package com.oracle.dev.jdbc.langchain4j;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.Properties;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

public class OracleDbUtils {

  // JDBC connection details
  private final static String URL = "jdbc:oracle:thin:@localhost:1521/FREEPDB1";
  private final static String USERNAME = System.getenv("DB_USERNAME");
  private final static String PASSWORD = System.getenv("DB_PASSWORD");

  public static Connection getConnectionFromPooledDataSource() throws SQLException {
    // Create pool-enabled data source instance
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    // set connection properties on the data source
    pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
    pds.setURL(URL);
    pds.setUser(USERNAME);
    pds.setPassword(PASSWORD);
    // Configure pool properties with a Properties instance
    Properties prop = new Properties();
    prop.setProperty("oracle.jdbc.vectorDefaultGetObjectType", "String");
    pds.setConnectionProperties(prop);
    // Override any pool properties directly
    pds.setInitialPoolSize(10);
    // Get a database connection from the pool-enabled data source
    Connection conn = pds.getConnection();
    return conn;
  }

  public static DataSource getPooledDataSource() throws SQLException {
    // Create pool-enabled data source instance
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    // set connection properties on the data source
    pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
    pds.setURL(URL);
    pds.setUser(USERNAME);
    pds.setPassword(PASSWORD);
    // Configure pool properties with a Properties instance
    Properties prop = new Properties();
    prop.setProperty("oracle.jdbc.vectorDefaultGetObjectType", "String");
    pds.setConnectionProperties(prop);
    // Override any pool properties directly
    pds.setInitialPoolSize(10);
    return pds;
  }

}
