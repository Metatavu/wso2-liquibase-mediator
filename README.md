# wso2-liquibase-mediator


## Example configuration

    <class name="fi.metatavu.wso2.liquibase.LiquibaseMediator">
      <property name="user" value="dbuser"/>
      <property name="password" value="dbpass"/>
      <property name="url" value="jdbc:mysql://localhost:3306/dbname"/>
      <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
      <property name="changeLog" expression="get-property('changeLog')"/>
    </class>