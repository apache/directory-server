package org.apache.directory.server.operations.modifydn;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.integ.ServerIntegrationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Check that moving an entry properly set the nbChildren and nbSubordinates values.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(name = "ModifyRdnIT-class", enableChangeLog = false,
    partitions = 
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif = "dn: dc=example,dc=com\n" +
                        "objectClass: domain\n" +
                        "objectClass: top\n" +
                        "dc: example\n\n"
                ),
                indexes = {
                    @CreateIndex( attribute = "uid" )
                }
            )
    })
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    })
public class MoveWithDescendantIT extends AbstractLdapTestUnit
{
    private static final String BASE = "dc=example,dc=com";


    /**
     * Move an entry, check it's parents (old and new) nbChildren and nbSubordinates values 
     */
    @Test
    public void testMoveDescendant() throws Exception
    {
        LdapConnection connection = ServerIntegrationUtils.getAdminConnection( getLdapServer() );
        connection.loadSchema();

        // Create a users root
        Entry entry = new DefaultEntry( "cn=users," + BASE,
            "objectClass: top",
            "objectClass: person",
            "cn", "users",
            "sn: users",
            "description", "The users root" );

        connection.add( entry );
        
        // Create a person root
        entry = new DefaultEntry( "cn=persons," + BASE,
            "objectClass: top",
            "objectClass: person",
            "cn", "persons",
            "sn: persons",
            "description", "The persons root" );

        connection.add( entry );
        
        // Create a person, cn value is rdn
        String cn = "Myra Ellen Amos";
        String rdn = "cn=" + cn;
        String dn = rdn + ", " + "cn=users," + BASE;

        entry = new DefaultEntry( dn,
            "objectClass: top",
            "objectClass: person",
            "cn", cn,
            "sn: Amos",
            "description", cn + " is a person." );

        connection.add( entry );

        // Get the top entry
        Entry tori = connection.lookup( dn, "+", "*" );

        assertNotNull( tori );
        assertTrue( tori.contains( "cn", "Myra Ellen Amos" ) );

        // Get the users root
        Entry users = connection.lookup( "cn=users," + BASE, "+", "*" );
        
        assertTrue( users.contains( "nbSubordinates", "1" ) );
        assertTrue( users.contains( "nbChildren", "1" ) );

        // Get the persons root
        Entry persons = connection.lookup( "cn=persons," + BASE, "+", "*" );
        
        assertTrue( persons.contains( "nbSubordinates", "0" ) );
        assertTrue( persons.contains( "nbChildren", "0" ) );

        // move the top entry
        connection.move( dn, "cn=persons," + BASE );
        

        // Get the users root
        users = connection.lookup( "cn=users," + BASE, "+", "*" );
        
        assertTrue( users.contains( "nbSubordinates", "0" ) );
        assertTrue( users.contains( "nbChildren", "0" ) );

        // Get the persons root
        persons = connection.lookup( "cn=persons," + BASE, "+", "*" );
        
        assertTrue( persons.contains( "nbSubordinates", "1" ) );
        assertTrue( persons.contains( "nbChildren", "1" ) );
    }
}
