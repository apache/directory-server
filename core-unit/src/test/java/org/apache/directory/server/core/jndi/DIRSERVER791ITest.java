
package org.apache.directory.server.core.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.SchemaViolationException;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;

/**
 * A test case which demonstrates the three defects described in DIRSERVER-791.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DIRSERVER791ITest extends AbstractAdminTestCase
{
    /**
     * Returns the attributes as depicted as test data in DIRSERVER-791
     */
    protected Attributes getTestEntryAttributes() {

        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl("objectClass");
        ocls.add("top");
        ocls.add("person");
        ocls.add("organizationalPerson");
        ocls.add("inetOrgPerson");
        attrs.put(ocls);
        
        Attribute cn = new AttributeImpl("cn");
        cn.add("test");
        cn.add("aaa");
        attrs.put(cn);
        
        attrs.put("sn", "test");

        return attrs;
    }

    protected void setUp() throws Exception 
    {
        super.setUp();

        Attributes entry = this.getTestEntryAttributes();
        sysRoot.createSubcontext("cn=test", entry);
    }

    /**
     * Demonstrates that removal of a value from RDN attribute which is not part
     * of the RDN is not possible.
     */
    public void testDefect1a() throws NamingException 
    {

        Hashtable<String,Object> env = configuration.toJndiEnvironment();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );

        DirContext ctx = new InitialDirContext( env );
        Attribute attr = new AttributeImpl("cn", "aaa");
        ModificationItemImpl modification = new ModificationItemImpl( DirContext.REMOVE_ATTRIBUTE, attr );
        ctx.modifyAttributes( "cn=test", new ModificationItemImpl[] { modification } );

        Attributes attrs = ctx.getAttributes("cn=test", new String[] { "cn" });
        Attribute cn = attrs.get("cn");

        assertEquals("number of cn values", 1, cn.size());
        assertTrue( cn.contains("test") );
        assertFalse( cn.contains("aaa") );
    }

    /**
     * Checks whether it is possible to replace the cn attribute with a single
     * value. The JIRA issue states that this one works.
     */
    public void testDefect1b() throws NamingException 
    {
        Hashtable<String,Object> env = configuration.toJndiEnvironment();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );

        DirContext ctx = new InitialDirContext( env );

        Attribute attr = new AttributeImpl("cn", "test");
        ModificationItemImpl modification = new ModificationItemImpl(DirContext.REPLACE_ATTRIBUTE, attr);
        ctx.modifyAttributes("cn=test", new ModificationItemImpl[] { modification });

        Attributes attrs = ctx.getAttributes("cn=test", new String[] { "cn" });
        Attribute cn = attrs.get("cn");

        assertEquals("number of cn values", 1, cn.size());
        assertTrue(cn.contains("test"));
        assertFalse(cn.contains("aaa"));
    }

    /**
     * It is possible to add an value to objectclass, which isn't a valid
     * objectclass. The server returns an error, but nevertheless the invalid
     * value is stored. I think this should be rejected from server.
     */
    public void testDefect2() throws NamingException 
    {
        Hashtable<String,Object> env = configuration.toJndiEnvironment();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );

        DirContext ctx = new InitialDirContext( env );


        Attribute attr = new AttributeImpl( "objectclass", "test" );
        ModificationItemImpl modification = new ModificationItemImpl(DirContext.ADD_ATTRIBUTE, attr);
        
        try 
        {
            ctx.modifyAttributes("cn=test", new ModificationItemImpl[] { modification });
            fail("Exception expected");
        } 
        catch ( SchemaViolationException sve ) 
        {
            // Valid behavior
        } 
        catch ( InvalidAttributeValueException iave ) 
        {
            // Valid behavior
        }
        catch ( NamingException ne )
        {
            // Valid behavior
        }

        Attributes attrs = ctx.getAttributes("cn=test", new String[] { "objectClass" });
        Attribute ocls = attrs.get("objectClass");

        assertEquals("number of objectClasses", 4, ocls.size());
        assertTrue(ocls.contains("top"));
        assertTrue(ocls.contains("person"));
        assertTrue(ocls.contains("organizationalPerson"));
        assertTrue(ocls.contains("inetOrgPerson"));
        assertFalse(ocls.contains("test"));
    }

    /**
     * It is possible to add an attribute to the entry that is not allowed
     * according the objectclasses. The server should reject this.
     */
    public void testDefect3() throws NamingException 
    {
        Hashtable<String,Object> env = configuration.toJndiEnvironment();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );

        DirContext ctx = new InitialDirContext( env );


        Attribute attr = new AttributeImpl("bootParameter", "test");
        ModificationItemImpl modification = new ModificationItemImpl(DirContext.ADD_ATTRIBUTE, attr);
    
        try 
        {
            ctx.modifyAttributes("cn=test", new ModificationItemImpl[] { modification });
            fail("Exception expected");
        } 
        catch (SchemaViolationException sve) 
        {
            // Valid behavior
        } 
        catch (InvalidAttributeIdentifierException iaie) 
        {
            // Valid behavior
        }
    }
}
