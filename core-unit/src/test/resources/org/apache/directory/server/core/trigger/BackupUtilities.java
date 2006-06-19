package org.apache.directory.server.core.trigger;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;


public class BackupUtilities
{
    public static void backupDeleted( LdapContext ctx, Name deletedEntryName, Name operationPrincipal, Attributes deletedEntry ) throws NamingException
    {
        System.out.println( "User \"" + operationPrincipal + "\" has deleted entry \"" + deletedEntryName + "\"" );
        System.out.println( "Entry content was: " + deletedEntry );
        LdapContext backupCtx = ( LdapContext ) ctx.lookup( "ou=backupContext,ou=system" );
        String deletedEntryRdn = deletedEntryName.get( deletedEntryName.size() - 1 );
        backupCtx.createSubcontext( deletedEntryRdn, deletedEntry );
        System.out.println( "Backed up deleted entry to \"" + ( ( LdapContext ) backupCtx.lookup( deletedEntryRdn ) ).getNameInNamespace() + "\"" );
    }
}
