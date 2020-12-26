package org.openrefine.extension.database;

import java.sql.SQLException;

import org.openrefine.extension.database.mariadb.MariaDBDatabaseService;
import org.openrefine.extension.database.mysql.MySQLDatabaseService;
import org.openrefine.extension.database.pgsql.PgSQLDatabaseService;
import org.testng.annotations.*;

@Test(groups = { "requiresMySQL", "requiresPgSQL", "requiresMariaDB", "requiresSQLite" })
public class DatabaseTestConfig extends DBExtensionTests {
    
    private DatabaseConfiguration mysqlDbConfig;
    private DatabaseConfiguration pgsqlDbConfig;
    private DatabaseConfiguration mariadbDbConfig;
    private DatabaseConfiguration sqliteDbConfig;

    @BeforeSuite
    @Parameters({ "mySqlDbName", "mySqlDbHost", "mySqlDbPort", "mySqlDbUser", "mySqlDbPassword", "mySqlTestTable",
                  "pgSqlDbName", "pgSqlDbHost", "pgSqlDbPort", "pgSqlDbUser", "pgSqlDbPassword", "pgSqlTestTable",
                  "mariadbDbName", "mariadbDbHost", "mariadbDbPort", "mariadbDbUser", "mariadbDbPassword", "mariadbTestTable",
                  "sqliteDbName", "sqliteTestTable"})
    public void beforeSuite(
            @Optional(DEFAULT_MYSQL_DB_NAME)   String mySqlDbName,     @Optional(DEFAULT_MYSQL_HOST)  String mySqlDbHost, 
            @Optional(DEFAULT_MYSQL_PORT)      String mySqlDbPort,     @Optional(DEFAULT_MYSQL_USER)  String mySqlDbUser,
            @Optional(DEFAULT_MYSQL_PASSWORD)  String mySqlDbPassword, @Optional(DEFAULT_TEST_TABLE)  String mySqlTestTable,
            
            @Optional(DEFAULT_PGSQL_DB_NAME)   String pgSqlDbName,     @Optional(DEFAULT_PGSQL_HOST)  String pgSqlDbHost, 
            @Optional(DEFAULT_PGSQL_PORT)      String pgSqlDbPort,     @Optional(DEFAULT_PGSQL_USER)  String pgSqlDbUser,
            @Optional(DEFAULT_PGSQL_PASSWORD)  String pgSqlDbPassword, @Optional(DEFAULT_TEST_TABLE)  String pgSqlTestTable,
            
            @Optional(DEFAULT_MARIADB_NAME)      String mariadbDbName,     @Optional(DEFAULT_MARIADB_HOST)  String mariadbDbHost, 
            @Optional(DEFAULT_MARIADB_PORT)      String mariadbDbPort,     @Optional(DEFAULT_MARIADB_USER)  String mariadbDbUser,
            @Optional(DEFAULT_MARIADB_PASSWORD)  String mariadbDbPassword, @Optional(DEFAULT_TEST_TABLE)    String mariadbTestTable,

            @Optional(DEFAULT_SQLITE_DB_NAME) String sqliteDbName, @Optional(DEFAULT_TEST_TABLE) String sqliteTestTable)
                    throws DatabaseServiceException, SQLException {
        
        //System.out.println("@BeforeSuite\n");
         mysqlDbConfig = new DatabaseConfiguration();
        mysqlDbConfig.setDatabaseHost(mySqlDbHost);
        mysqlDbConfig.setDatabaseName(mySqlDbName);
        mysqlDbConfig.setDatabasePassword(mySqlDbPassword);
        mysqlDbConfig.setDatabasePort(Integer.parseInt(mySqlDbPort));
        mysqlDbConfig.setDatabaseType(MySQLDatabaseService.DB_NAME);
        mysqlDbConfig.setDatabaseUser(mySqlDbUser);
        mysqlDbConfig.setUseSSL(false);
        
         pgsqlDbConfig = new DatabaseConfiguration();
        pgsqlDbConfig.setDatabaseHost(pgSqlDbHost);
        pgsqlDbConfig.setDatabaseName(pgSqlDbName);
        pgsqlDbConfig.setDatabasePassword(pgSqlDbPassword);
        pgsqlDbConfig.setDatabasePort(Integer.parseInt(pgSqlDbPort));
        pgsqlDbConfig.setDatabaseType(PgSQLDatabaseService.DB_NAME);
        pgsqlDbConfig.setDatabaseUser(pgSqlDbUser);
        pgsqlDbConfig.setUseSSL(false);
        
         mariadbDbConfig = new DatabaseConfiguration();
        mariadbDbConfig.setDatabaseHost(mariadbDbHost);
        mariadbDbConfig.setDatabaseName(mariadbDbName);
        mariadbDbConfig.setDatabasePassword(mariadbDbPassword);
        mariadbDbConfig.setDatabasePort(Integer.parseInt(mariadbDbPort));
        mariadbDbConfig.setDatabaseType(MariaDBDatabaseService.DB_NAME);
        mariadbDbConfig.setDatabaseUser(mariadbDbUser);
        mariadbDbConfig.setUseSSL(false);

        sqliteDbConfig = new DatabaseConfiguration();
        sqliteDbConfig.setDatabaseName(sqliteDbName);
    
        DBExtensionTestUtils.initTestData(mysqlDbConfig);
        DBExtensionTestUtils.initTestData(pgsqlDbConfig);
        DBExtensionTestUtils.initTestData(mariadbDbConfig);
        DBExtensionTestUtils.initTestData(sqliteDbConfig);
    }
  
    @AfterSuite
    public void afterSuite() {
       // System.out.println("@AfterSuite");
       
        DBExtensionTestUtils.cleanUpTestData(mysqlDbConfig);
        DBExtensionTestUtils.cleanUpTestData(pgsqlDbConfig);
        DBExtensionTestUtils.cleanUpTestData(mariadbDbConfig);
        DBExtensionTestUtils.cleanUpTestData(sqliteDbConfig);
    }

}

