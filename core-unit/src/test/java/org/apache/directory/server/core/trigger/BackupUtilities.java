package org.apache.directory.server.core.trigger;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.jndi.ServerLdapContext;


public class BackupUtilities
{
    public static void backupDeleted( ServerLdapContext ctx, Name deletedEntryName, Attributes deletedEntry ) throws NamingException
    {
        System.out.println( "Backing up deleted entry: " + deletedEntryName );
        System.out.println( "Entry content is: " + deletedEntry );
        ServerLdapContext backupCtx = ( ServerLdapContext ) ctx.lookup( "ou=backupContext,ou=system" );
        backupCtx.createSubcontext( deletedEntryName.get( 1 ), deletedEntry );
    }
}
