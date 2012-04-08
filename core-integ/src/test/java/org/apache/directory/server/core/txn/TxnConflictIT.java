    
package org.apache.directory.server.core.txn;

import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;

import javax.naming.NameNotFoundException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.name.Dn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;


@RunWith ( FrameworkRunner.class )
@CreateDS(name = "TxnConflictIT")
@ApplyLdifs(
    {
        "dn: cn=test1, ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: test1",
        "sn: Jim",
        "description: useless guy"
    }
)


public class TxnConflictIT extends AbstractLdapTestUnit
{
    enum ConflictType
    {
        MODIFY_MODIFY_CONLICT,
        DELETE_MODIFY_CONFLICT,
        ADD_ADD_CONFLICT,
        RENAME_MODIFY_CONFLICT
    }

    
    @Test
    public void testModifyModifyConflict() throws Exception
    {
        try
        {
            ConflictingThread cThread = new ConflictingThread( ConflictType.MODIFY_MODIFY_CONLICT );
            
            LdapContext sysRoot = getSystemContext( getService() );
            
            cThread.start();
            
            // The added value
            Attributes attrs = new BasicAttributes( "telephoneNumber", "1 650 300 6089", true );
    
            // Add the Ava
            sysRoot.modifyAttributes( "cn=test1", DirContext.ADD_ATTRIBUTE, attrs );
            
            cThread.join();
            
            // Entry should contain two telephone numbers now
            attrs = sysRoot.getAttributes( "cn=test1" );
            Attribute attr = attrs.get( "telephoneNumber" );
            assertNotNull( attr );
            assertEquals( 2, attr.size() );
            assertTrue( attr.contains( "1 650 300 6089" ) );
            assertTrue( attr.contains( "1 650 300 6088" ) );
            
        }
        catch( Exception e )
        {
            e.printStackTrace();
            
            fail();
        }
        
        
    }
    
    
    @Test
    public void testDeleteModifyConflict() throws Exception
    {
        try
        {
            Dn dn = new Dn("cn=test1, ou=system");
            ConflictingThread cThread = new ConflictingThread( ConflictType.DELETE_MODIFY_CONFLICT );
            
            LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
            
            cThread.start();
 
            // Delete the entry, this should succeed
            connection.delete( dn );
            
            cThread.join();
            
            Entry entry = connection.lookup( dn );
            assertTrue( entry == null );
            
        }
        catch( Exception e )
        {
            e.printStackTrace();
            
            fail();
        }
        
        
    }
    
    
    @Test
    public void testAddAddConflict() 
    {
        try
        {
            Dn dn = new Dn( "cn=test2,ou=system" );
            Entry entry = new DefaultEntry( dn,
                "ObjectClass: top",
                "ObjectClass: person",
                "sn: TEST",
                "cn: test2" );
            
            LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
            
            ConflictingThread cThread = new ConflictingThread( ConflictType.ADD_ADD_CONFLICT );
            
            cThread.start();
            
            try
            {
                connection.add( entry );
            }
            catch ( LdapException e )
            {
                //e.printStackTrace();
            }
            
            cThread.join();
            
            // Entry should be there
            entry = connection.lookup( dn );
            assertTrue( entry != null );
            
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }
    }
    
    
    @Test
    public void testRenameModifyConflict() 
    {
        try
        {
            Dn dn = new Dn( "cn=test3,ou=users,ou=system" );
            Entry entry = new DefaultEntry( dn,
                "ObjectClass: top",
                "ObjectClass: person",
                "sn: TEST",
                "cn: test3" );
            
            LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
            
            connection.add( entry );
            
            ConflictingThread cThread = new ConflictingThread( ConflictType.RENAME_MODIFY_CONFLICT );
            
            cThread.start();
            
            connection.rename( "ou=users,ou=system", "ou=people" );
            
            cThread.join();
            
            // test3 should be there with the new dn
            entry = connection.lookup( new Dn("cn=test3,ou=people,ou=system") );
            assertTrue( entry != null );
            
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }
    }
    
    
    class ConflictingThread extends Thread
    {
        ConflictType type;
        
        public ConflictingThread( ConflictType type )
        {
            this.type = type;
        }
        
        
        private void doConflictingModify() throws Exception
        {
            LdapContext sysRoot = getSystemContext( getService() );
            
            // The added value
            Attributes attrs = new BasicAttributes( "telephoneNumber", "1 650 300 6088", true );

            // Add the Ava
            sysRoot.modifyAttributes( "cn=test1", DirContext.ADD_ATTRIBUTE, attrs );
        }
        
        
        private void doConflictingAdd() throws Exception
        {
            Dn dn = new Dn( "cn=test2,ou=system" );
            Entry entry = new DefaultEntry( dn,
                "ObjectClass: top",
                "ObjectClass: person",
                "sn: TES2",
                "cn: test2" );
            
            LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
            
            connection.add( entry );
        }
        
        
        private void doModifyConfclictingWithRename() throws Exception
        {
            LdapContext sysRoot = getSystemContext( getService() );
            
            // The added value
            Attributes attrs = new BasicAttributes( "telephoneNumber", "1 650 300 6088", true );

            // Add the Ava
            sysRoot.modifyAttributes( "cn=test3,ou=users", DirContext.ADD_ATTRIBUTE, attrs );
        }
        
        
        public void run()
        {
            try
            {
                if (type == ConflictType.MODIFY_MODIFY_CONLICT )
                {
                    doConflictingModify();
                }
                else if ( type == ConflictType.DELETE_MODIFY_CONFLICT )
                {
                    try
                    {
                        doConflictingModify();
                    }
                    catch ( NameNotFoundException e )
                    {
                        // Entry might have been already deleted, 
                        // hence this is OK.
                        //e.printStackTrace();
                    }
                }
                else if ( type == ConflictType.ADD_ADD_CONFLICT )
                {
                    try
                    {
                        doConflictingAdd();
                    }
                    catch ( LdapException e )
                    {
                        //Entry with the same dn might have been alrady added
                        // so this is ok.
                        //e.printStackTrace();
                    }
                }
                else if ( type == ConflictType.RENAME_MODIFY_CONFLICT )
                {
                    try
                    {
                        doModifyConfclictingWithRename();
                    }
                    catch ( NameNotFoundException e )
                    {
                        // Entry might have been moved. 
                        //e.printStackTrace();
                    }
                }
            }
            catch( Exception e )
            {
                e.printStackTrace();
                
                fail();
            }
        }
    }
}

