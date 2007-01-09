package org.apache.directory.server.core.trigger;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BackupUtilities
{
    private static final Logger log = LoggerFactory.getLogger( BackupUtilities.class );

    public static void backupDeleted( LdapContext ctx, Name deletedEntryName, Name operationPrincipal, Attributes deletedEntry ) throws NamingException
    {
        log.info( "User \"" + operationPrincipal + "\" has deleted entry \"" + deletedEntryName + "\"" );
        log.info( "Entry content was: " + deletedEntry );
        LdapContext backupCtx = ( LdapContext ) ctx.lookup( "ou=backupContext,ou=system" );
        String deletedEntryRdn = deletedEntryName.get( deletedEntryName.size() - 1 );
        backupCtx.createSubcontext( deletedEntryRdn, deletedEntry );
        log.info( "Backed up deleted entry to \"" + ( ( LdapContext ) backupCtx.lookup( deletedEntryRdn ) ).getNameInNamespace() + "\"" );
    }
}
