#!/bin/sh
#
# The Kerberos command script
#
# Environment Variables
#
#   KERBEROS_JAVA_HOME The java implementation to use.  Default is
#                      assumed to be located at /usr/bin/java.
#
#   KERBEROS_HOME      The path that the KERBEROS server is to be run
#                      within. Default is the parent directory.
#
#   KERBEROS_LIB       The path containing the Java archives.
#

THIS="$0"

while [ -h "$THIS" ]; do
  ls=`ls -ld "$THIS"`
  LINK=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$LINK" : '.*/.*' > /dev/null; then
    THIS="$LINK"
  else
    THIS=`dirname "$THIS"`/"$LINK"
  fi
done

if [ "$KERBEROS_HOME" = '' ] ; then
   THIS_DIR=`dirname "$THIS"`
   KERBEROS_HOME=`cd "$THIS_DIR/.." ; pwd`
fi

cd $KERBEROS_HOME

if [ "$KERBEROS_LIB" = '' ] ; then
   KERBEROS_LIB="$KERBEROS_HOME/lib"
fi

for JAR in `ls $KERBEROS_LIB/*.jar`; do
   if [ "$CLASS_PATH" != '' ] ; then
      CLASS_PATH="$CLASS_PATH:$JAR"
   elif [ "$JAR" != '' ] ; then
      CLASS_PATH="$KERBEROS_LIB:$JAR"
   fi
done

if [ "$KERBEROS_JAVA_HOME" = '' ] ; then
   if [ "$JAVA_HOME" = '' ] ; then
      KERBEROS_JAVA_HOME='/usr'
   else
      KERBEROS_JAVA_HOME="$JAVA_HOME"
   fi
fi

case "$1" in
   start)
      if [ -f "$KERBEROS_JAVA_HOME/bin/java" ] ; then
         $KERBEROS_JAVA_HOME/bin/java -server -classpath $CLASS_PATH -Dserver.properties=server.properties org.apache.kerberos.kdc.server.udp.Main > /dev/null 2>&1 &
      fi
   ;;
   stop)
      if [ -f /usr/bin/pkill ] ; then
         /usr/bin/pkill -f KERBEROS
      fi 
   ;;
   *)
   echo "Usage: $0 { start | stop }"
esac
exit 0

