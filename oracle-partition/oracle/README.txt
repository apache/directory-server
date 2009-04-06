To install the oracle partition follow this steps:

1) sqlplus / as sysdba @createuser.sql
2) sqlplus dsorapart/dsorapart @schema.sql
3) build the sources 'mvn compile' (this will ask you to download and install the oracle driver: follow the instructions)
4) mvn package
5) put the apacheds-oracle*.jar into your server classpath
6) customize the provided server.xml file 
7) start the server

