
package org.apache.directory.server.core.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;

/**
 * Tries to demonstrate DIRSERVER-783 ("Adding another value to an attribute
 * results in the value to be added twice").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */

public class DIRSERVER783ITest extends AbstractAdminTestCase
{
    protected void setUp() throws Exception 
    {
        super.setUp();
    }


    /**
     * Try to add entry with required attribute missing.
     */
    /*
    public void testAddAnotherValueToAnAttribute() throws NamingException
    {
        // create a person without sn
        Attributes attrs = new BasicAttributes();
        Attribute ocls = new BasicAttribute("objectClass");

        ocls.add("top");
        ocls.add("person");
        attrs.put(ocls);
        attrs.put("cn", "Fiona Apple");
        attrs.put("sn", "Apple");

        String rdn = "cn=Fiona Apple";

        Hashtable env = configuration.toJndiEnvironment();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );

        DirContext ctx = new InitialDirContext( env );

        ctx.createSubcontext( rdn, attrs );

        // Add the first value for description
        String description1 = "an American singer-songwriter";
        Attribute firstDescr = new BasicAttribute("description", description1);
        ModificationItem modification = new ModificationItem(DirContext.ADD_ATTRIBUTE, firstDescr);
        ctx.modifyAttributes(rdn, new ModificationItem[] { modification });

        // Add a second value to description
        String description2 = "Grammy award winning";
        Attribute otherDescr = new BasicAttribute("description", description2 );

        modification = new ModificationItem(DirContext.ADD_ATTRIBUTE, otherDescr );
        ctx.modifyAttributes(rdn, new ModificationItem[] { modification } );
      
        // Add a third value to description
        String description3 = "MTV Music Award winning";
        Attribute thirdDescr = new BasicAttribute("description", description3 );

        modification = new ModificationItem(DirContext.ADD_ATTRIBUTE, thirdDescr );
        ctx.modifyAttributes(rdn, new ModificationItem[] { modification });

        // Search Entry
        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        String filter = rdn;
        String base = "";

        // Check entry
        NamingEnumeration enm = ctx.search(base, filter, sctls);
        assertTrue(enm.hasMore());

        while (enm.hasMore()) 
        {
            SearchResult sr = (SearchResult) enm.next();

            attrs = sr.getAttributes();
            Attribute desc = sr.getAttributes().get("description");

            assertNotNull(desc);
            assertTrue(desc.contains(description1));
            assertTrue(desc.contains(description2));
            assertTrue(desc.contains(description3));
            assertEquals(3, desc.size());
        }

        // Remove the person entry
        ctx.destroySubcontext(rdn);
    }
    */
}

