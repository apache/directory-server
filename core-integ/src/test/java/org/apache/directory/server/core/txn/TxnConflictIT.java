    
package org.apache.directory.server.core.txn;

import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.directory.server.core.integ.IntegrationUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


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
        MODIFY_MODIFY_CONLICT
    }

    
    @Test
    public void testModifyModifyConflict() throws Exception
    {
        try
        {
            ConflictingThread cThread = new ConflictingThread( ConflictType.MODIFY_MODIFY_CONLICT );
            
            cThread.start();
            
            LdapContext sysRoot = getSystemContext( getService() );
            
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
        
        public void run()
        {
            System.out.println("here1");
            try
            {
                if (type == ConflictType.MODIFY_MODIFY_CONLICT )
                {
                    this.doConflictingModify();
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

