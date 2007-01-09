package org.apache.directory.server.core.trigger;

import javax.naming.Name;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoggingUtilities
{
    private static final Logger log = LoggerFactory.getLogger( LoggingUtilities.class ); 
    
    public static void logWarningForDeletedEntry( Name deletedEntryName, Name operationPrincipal ) throws NamingException
    {
        log.info( "User \"" + operationPrincipal + "\" is about to delete entry \"" + deletedEntryName + "\"." );
    }
}
