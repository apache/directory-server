package org.apache.directory.server.core.trigger;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;


public class LoggingUtilities
{
    public static void logWarningForDeletedEntry( LdapContext ctx, Name deletedEntryName, Name operationPrincipal ) throws NamingException
    {
        System.out.println( "User \"" + operationPrincipal + "\" is about to delete entry \"" + deletedEntryName + "\"." );
    }
}
