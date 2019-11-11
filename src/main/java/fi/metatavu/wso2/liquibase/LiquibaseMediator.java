package fi.metatavu.wso2.liquibase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.datasource.DataSourceFinder;
import org.apache.synapse.mediators.AbstractMediator;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;

/**
 * WSO2 mediator for running Liquibase migrations
 * 
 * @author Antti Leppä
 */
public class LiquibaseMediator extends AbstractMediator {
  
  private String dsName;
  private String user;
  private String password;
  private String url;
  private String changeLog;
  private String driver;
  
  /**
   * Data source JNDI lookup name
   * 
   * @return Data source JNDI lookup name
   */
  public String getDsName() {
    return dsName;
  }
  
  /**
   * Sets data source JNDI lookup name
   * 
   * @param dsName data source JNDI lookup name
   */
  public void setDsName(String dsName) {
    this.dsName = dsName;
  }
  
  /**
   * Returns "user" setting value
   * 
   * @return "user" setting value
   */
  public String getUser() {
    return user;
  }

  /**
   * Setter for "user" setting
   * 
   * @param user setting value
   */
  public void setUser(String user) {
    this.user = user;
  }

  /**
   * Returns "password" setting value
   * 
   * @return "password" setting value
   */
  public String getPassword() {
    return password;
  }

  /**
   * Setter for "password" setting
   * 
   * @param password setting value
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Returns "url" setting value
   * 
   * @return "url" setting value
   */
  public String getUrl() {
    return url;
  }
  
  /**
   * Returns JDBC driver name
   * 
   * @return JDBC driver name
   */
  public String getDriver() {
    return driver;
  }
  
  /**
   * Sets JDBC driver name
   * 
   * @param driver JDBC driver name
   */
  public void setDriver(String driver) {
    this.driver = driver;
  }

  /**
   * Setter for "url" setting
   * 
   * @param url setting value
   */
  public void setUrl(String url) {
    this.url = url;
  }
  
  /**
   * Returns change log contents
   * 
   * @return change log contents
   */
  public String getChangeLog() {
    return changeLog;
  }
  
  /**
   * Sets change log contents
   * 
   * @param changeLog change log contents   
   */
  public void setChangeLog(String changeLog) {
    this.changeLog = changeLog;
  }
  
  /**
  * Mediate method
  * 
  * @param context context
  * @return true
  */
  public boolean mediate(MessageContext context) {
    if (getChangeLog() == null) {
      log.error("ChangeLog is required ");
      return false;
    }
    
    if (getDsName() == null) {
      if (getUser() == null) {
        log.error("User is required when DSName is not provided");
        return false;
      }
      
      if (getPassword() == null) {
        log.error("Password is required when DSName is not provided");
        return false;
      }
  
      if (getUrl() == null) {
        log.error("URL is required when DSName is not provided");
        return false;
      }
  
      if (getDriver() == null) {
        log.error("Driver is required when DSName is not provided");
        return false;
      }
    }
    
    File parentDirectory;
    try {
      parentDirectory = Files.createTempDirectory("changelog").toFile();
    } catch (IOException e) {
      log.error("Failed to create changelog directory");
      return false;
    }

    File changeLogFile = new File(parentDirectory, "changelog.xml");
    if (!changeLogFile.exists()) {
      try {
        changeLogFile.createNewFile();
      } catch (IOException e) {
        log.error("Failed to create changelog file");
        return false;
      }
    }
    
    try {
      Liquibase liquibase = null;
      
      try (Connection connection = getConnection(); FileOutputStream fileStream = new FileOutputStream(changeLogFile)) {
        fileStream.write(getChangeLog().getBytes(StandardCharsets.UTF_8));
        
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        
        liquibase = new Liquibase(changeLogFile.getName(), new FileSystemResourceAccessor(parentDirectory.getAbsolutePath()), database);
        liquibase.update("main");
      } catch (IOException e) {
        log.error("Failed to read changelog", e);
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
        log.error("Failed to initialize driver", e);
      } catch (LiquibaseException | SQLException e) {
        log.error("Failed to run migrations", e);
      }
    } finally {
      try {
        Files.delete(changeLogFile.toPath());
      } catch (IOException e) {
      }
    }

    return true;
  }

  /**
   * Opens database connection 
   * 
   * @return database connection
   * @throws SQLException SQL thrown when connection opening fails
   * @throws InstantiationException thrown when database driver initialization fails
   * @throws IllegalAccessException thrown when database driver initialization fails
   * @throws ClassNotFoundException thrown when database driver initialization fails
   */
  private Connection getConnection() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    if (getDsName() != null) {
      DataSource dataSource = DataSourceFinder.find(getDsName(), new Properties());
      return dataSource.getConnection();
    }
   
    Class.forName(getDriver()).newInstance();
    
    return DriverManager.getConnection(getUrl(), getUser(), getPassword());
  }
  
}
