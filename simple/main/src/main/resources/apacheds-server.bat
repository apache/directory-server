echo off

rem The following VARS are parsed and replaced by ant via the maven goal 'standalone'.
rem
rem @..@ indicates that ant will parse this file and set the correct value.


set MAIN_JAR=@APACHE_DS_MAIN_JAR@

set ARGS=-Xms128m -Xmx256m -jar %MAIN_JAR% apacheds-server.xml

rem still needs to be constructed
rem if someone uses windows and wants to make a duplicate of apacheds-server.sh, go for it.

set CLASSPATH=%CLASSPATH%;.

rem Create the logs dir, if it does not already exist
if exist logs goto logsExists
mkdir logs
:logsExists

java %ARGS%

:end