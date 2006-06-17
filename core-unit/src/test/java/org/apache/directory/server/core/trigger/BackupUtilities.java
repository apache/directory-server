package org.apache.directory.server.core.trigger;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.jndi.ServerLdapContext;


public class BackupUtilities
{
    public static void backupDeleted( LdapContext ctx, Name deletedEntryName, Name operationPrincipal, Attributes deletedEntry ) throws NamingException
    {
        System.out.println( "User \"" + operationPrincipal + "\" has deleted entry \"" + deletedEntryName + "\"" );
        System.out.println( "Entry content was: " + deletedEntry );
        
        ServerLdapContext backupCtx = ( ServerLdapContext ) ctx.lookup( "ou=backupContext,ou=system" );
        backupCtx.createSubcontext( deletedEntryName.get( 1 ), deletedEntry );
        System.out.println( "Backed up deleted entry to \"" + ( ( LdapContext ) backupCtx.lookup( deletedEntryName.get( 1 ) ) ).getNameInNamespace() + "\"" );
    }
}
