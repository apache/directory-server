/*
 * $Id: BackendModule.java,v 1.10 2003/08/22 21:15:54 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import org.apache.eve.schema.Schema ;
import org.apache.eve.AbstractModule ;
import org.apache.eve.client.ClientManager ;
import org.apache.eve.schema.SchemaManager ;

import java.io.File ;
import java.util.Iterator ;
import java.util.Collection ;
import java.util.NoSuchElementException ;

import javax.naming.Name ;
import javax.naming.NamingException ;
import javax.naming.InvalidNameException ;
import javax.naming.NameNotFoundException ;

import org.apache.commons.collections.LRUMap ;
import org.apache.commons.collections.MultiMap ;
import org.apache.commons.collections.MultiHashMap ;

import org.apache.avalon.phoenix.BlockContext ;

import org.apache.avalon.framework.context.Context ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.context.ContextException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;


public abstract class BackendModule
    extends AbstractModule
    implements AtomicBackend
{
    //
    // Configuration Variables
	//

    protected boolean m_isReadOnly = false ;
    protected Name m_suffix = null ;
	protected String m_wkdirPath = null ;
    protected Name m_adminUser = null ;
    protected String m_adminPassword = null ;
    protected BlockContext m_context ;

    protected UnifiedBackend m_nexus = null ;
    protected ClientManager m_clientMan = null ;
    protected SchemaManager m_schemaManager = null ;
	protected Schema m_schema = null ;
    protected LRUMap m_cache = new LRUMap(1000) ;
	protected MultiMap m_suffixEntry = new MultiHashMap() ;


    /**
     * Gets the normalized suffix name of this BackendModule.  The name is the
     * normalized name found in the configuration for the backend within the
     * suffix tag.
     *
     * @return the normalized suffix name for this BackendModule
     */
    public Name getSuffix()
    {
        return m_suffix ;
    }


    public Schema getSchema()
    {
        return m_schema ;
    }


    public boolean isReadOnly()
    {
        return this.m_isReadOnly ;
    }


    public void setReadOnly(boolean a_isReadOnlyMode)
    {
        this.m_isReadOnly = a_isReadOnlyMode ;
    }


    public void setEntryCacheSize(int a_numMaxEntries)
        throws BackendException
    {
        this.m_cache.setMaximumSize(a_numMaxEntries) ;
    }


    public String getWorkingDirPath()
    {
        return this.m_wkdirPath ;
    }


    /**
     * Sets the working directory path for implementation specific files or logs
     * if any.
     *
     * @param a_dirPath the absolute path to the working directory for the
     * configurable backend
     * @throws BackendException if the configurable module has started and is
     * not Reconfigurable.
     */
    public void setWorkingDirPath(String a_dirPath)
        throws BackendException
    {
        if (hasStarted()) {
            throw new BackendException("Cannot change working directory path "
                + "after backend has started!") ;
        }

        File l_dir = new File(a_dirPath) ;

        if (!l_dir.exists()) {
            throw new BackendException(a_dirPath + " does not exist") ;
        } else if (!l_dir.canWrite()) {
            throw new BackendException("Cannot read " + a_dirPath) ;
        } else if (!l_dir.canWrite()) {
            throw new BackendException("Cannot write to " + a_dirPath) ;
        } else if (!l_dir.isDirectory()) {
            throw new BackendException(a_dirPath + " is not a directory.") ;
        }

        this.m_wkdirPath = l_dir.getAbsolutePath() ;
    }


    public void setAdminUserDN(Name an_adminDN)
        throws BackendException
    {
        this.m_adminUser = an_adminDN ;
    }


    public Name getAdminUserDN()
    {
        return m_adminUser ;
    }


    public void setAdminUserPassword(String an_adminPassword)
        throws BackendException
    {
        this.m_adminPassword = an_adminPassword ;
    }


    public String getAdminUserPassword()
    {
        return this.m_adminPassword ;
    }


    public int getEntryCacheSize()
    {
        return this.m_cache.getMaximumSize() ;
    }


    public void initialize()
        throws Exception
    {
        if(hasEntry(getSuffix())) {
            getLogger().info("Suffix entry exists skipping initial creation!") ;
            return ;
        }

		getLogger().info("Suffix entry DOES NOT EXIST! " +
            "Creating Suffix entry for the first time.") ;

        // Extract User Provided Distinguished Name and create the new suffix
        // root entry.
        Collection l_col = (Collection) m_suffixEntry.get(Schema.DN_ATTR) ;
        if(l_col == null) {
            l_col = (Collection) m_suffixEntry.get("dn") ;
        }
        if(l_col == null) {
			throw new BackendException("Cannot find a 'distinguishedName' or "
                + "'dn' attribute in the suffix entry!") ;
        }
        String l_updn = (String) l_col.iterator().next() ;

        LdapEntry l_entry = newEntry(l_updn) ;
        Iterator l_keys = m_suffixEntry.keySet().iterator() ;
        while(l_keys.hasNext()) {
			String l_key = (String) l_keys.next() ;
            if(l_key.toLowerCase().trim().equals(Schema.DN_ATTR) ||
               l_key.toLowerCase().trim().equals("dn"))
            {
                continue ;
            }

            Iterator l_values = ((Collection)
                m_suffixEntry.get(l_key)).iterator() ;
            while(l_values.hasNext()) {
                l_entry.addValue(l_key, l_values.next()) ;
            }
        }

        create(l_entry) ;
    }


    /**
     * All derived modules should call super.service first.
     */
    public void service(ServiceManager a_manager)
        throws ServiceException
    {
        m_schemaManager
            = (SchemaManager) a_manager.lookup(SchemaManager.ROLE) ;
        m_nexus = (UnifiedBackend) a_manager.lookup(UnifiedBackend.ROLE) ;
    }


    public void start()
        throws Exception 
    {
        super.start() ;
        m_nexus.register( this ) ;
    }


    public void stop()
        throws Exception 
    {
        m_nexus.unregister( this ) ;
        super.stop() ;
    }


    public void registerClientManager( ClientManager a_manager )
    {
        m_clientMan = a_manager ;
    }


    ////////////////////////////////////////////
    // Configuration Interface Implementation //
    ////////////////////////////////////////////


    /** DN of backend suffix property tag. */
	public static final String SUFFIX_NODE = "suffix" ;
    /** Admin user dn property tag. */
	public static final String ADMINDN_NODE = "adminUserDN" ;
    /** Admin user password property tag. */
	public static final String ADMINPW_NODE = "adminUserPassword" ;
    /** Working directory path property tag. */
	public static final String WDIR_NODE = "workingDirPath" ;
    /** Entry cache size property tag. */
	public static final String CACHESZ_NODE = "entryCacheSize" ;


    /**
     * Avalon Configurable interface implementation. Here's an example
     * configuration:<br>
     *   <config>
     *		<backend>
     * 			<!-- mandatory -->
     *			<suffix>dc=domain,dc=com</suffix>
     * 
     * 			<!-- mandatory -->
     *			<adminUserDN>cn=admin,dc=domain,dc=com</adminUserDN>
     * 
     * 			<!-- mandatory -->
     *			<adminUserPassword>secret</adminUserPassword>
     * 
     * 			<!-- mandatory -->
     *			<workingDirPath>var/backend0</workingDirPath>
     * 
     * 			<!-- optional -->
     *			<entryCacheSize>100</entryCacheSize>
     *		</backend>
     *   </config>
     *
     * @param a_config an avalon configuration object for this backend block.
     * @throws ConfigurationException if the configuration is not correct.
     */
    public void configure( Configuration a_config )
        throws ConfigurationException
    {
        Configuration l_suffixConfig = a_config.getChild( SUFFIX_NODE, false ) ;
        if( l_suffixConfig == null )
        {
            throw new ConfigurationException( "Encountered module config with "
                + "missing <" + SUFFIX_NODE+ "> tag! Cannot process "
                + "config without unique backend suffix name." ) ;
        }

        // Got through all the attributes and add it to the suffix multimap
        Configuration [] l_attributes = l_suffixConfig.getChildren() ;
        for( int ii = 0 ; ii < l_attributes.length ; ii++ )
        {
            String l_name = l_attributes[ii].getAttribute( "name", null ) ;
            String l_value = l_attributes[ii].getAttribute( "value", null ) ;

            if( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Adding suffix attribute " + l_name
                    + " with value " + l_value ) ;
            }

            m_suffixEntry.put( l_name, l_value ) ;
        }

		if( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Dumping suffix entry:\n" + m_suffixEntry ) ;
        }

        // Check multimap for the long and short names of 'dn' and complain if
        // this or 'distinguishedName' keys do not exist.
        Collection l_dnVals = ( Collection )
            m_suffixEntry.get( Schema.DN_ATTR ) ;
		if( l_dnVals == null )
        {
            l_dnVals = ( Collection ) m_suffixEntry.get( "dn" ) ;
        }
        if( l_dnVals == null )
        {
            throw new ConfigurationException( "Encountered module config with a"
                + " missing attribute for the dn! Cannot process config without"
                + " unique backend suffix distinguished name." ) ;
        }

        String l_suffix = null ;
        try
        {
        	l_suffix = ( String ) l_dnVals.iterator().next() ;
        }
        catch( NoSuchElementException e )
        {
            throw new ConfigurationException( "Encountered module config with a"
                + " missing value for the dn! Cannot process config without"
                + " unique backend suffix distinguished name.") ;
        }
        if( l_suffix == null || l_suffix.trim().equals( "" ) )
        {
            throw new ConfigurationException("Encountered module config with a "
                + "missing value for the dn! Cannot process config without "
                + "unique backend suffix distinguished name.") ;
        }


        try
        {
            m_schema = m_schemaManager.getSchema( l_suffix ) ;

            if( m_schema == null )
            {
                throw new ConfigurationException( "Got a null schema from the "
                    + "schema manager using the suffix " + l_suffix ) ;
            }
            else if( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Got schema for suffix " + l_suffix ) ;
            }

            m_suffix = m_schema.getNormalizingParser().parse( l_suffix ) ;
        }
        catch( InvalidNameException e )
        {
            String l_msg = "The module suffix " + l_suffix
                + " is not syntacticly correct: " ;
            getLogger().error( l_msg, e ) ;
            throw new ConfigurationException( l_msg, e ) ;
        }
        catch( NameNotFoundException e )
        {
            String l_msg = "Schema manager does not recognize this suffix: "
                + l_suffix ;
            getLogger().error( l_msg, e ) ;
            throw new ConfigurationException( l_msg, e ) ;
        }
        catch( NamingException e )
        {
            String l_msg = "Schema manager does not recognize this suffix: "
                + l_suffix ;
            getLogger().error( l_msg, e ) ;
            throw new ConfigurationException( l_msg, e ) ;
        }


        String l_adminUserDn =
            a_config.getChild( ADMINDN_NODE ).getValue( null ) ;
        if( l_adminUserDn == null )
        {
            throw new ConfigurationException( "Encountered "
                + getImplementationName()
                + " module config for suffix " + m_suffix + " with missing <"
                + ADMINDN_NODE
                + "> tag! Cannot process config without this mandatory tag." ) ;
        }

        try
        {
            m_adminUser =
                m_schema.getNormalizingParser().parse( l_adminUserDn ) ;
        }
        catch( NamingException e )
        {
            throw new ConfigurationException( "Encountered "
                + getImplementationName()
                + " module config for suffix " + m_suffix + " with bad <"
                + this.ADMINDN_NODE + "> tag! Cannot process config without "
                + "this mandatory tag being present with a valid DN" ) ;
        }

		m_adminPassword = a_config.getChild( ADMINPW_NODE ).getValue( null ) ;
        if( m_adminPassword == null )
        {
            throw new ConfigurationException( "Encountered "
                + getImplementationName()
                + " module config for suffix " + m_suffix + " with missing <"
                + ADMINPW_NODE
                + "> tag! Cannot process config without this mandatory tag." ) ;
        }

		m_wkdirPath = m_context.getBaseDirectory().getAbsolutePath()
            + File.separator + a_config.getChild( WDIR_NODE ).getValue( null ) ;
        File l_dir = new File( m_wkdirPath ) ;
		if( ! l_dir.exists() )
        {
			l_dir.mkdirs() ;
		}

        if( m_wkdirPath == null )
        {
            throw new ConfigurationException( "Encountered "
                + getImplementationName()
                + " module config for suffix " + m_suffix + " with missing <"
                + WDIR_NODE
                + "> tag! Cannot process config without this mandatory tag." ) ;
        }

        m_cache.setMaximumSize( a_config.getChild( CACHESZ_NODE ).
            getValueAsInteger( DEFAULT_ENTRY_CACHESZ ) ) ;
    }


    /**
     * Looks up the BlockContext to get the base directory for the application.
     */
    public void contextualize( Context a_context )
        throws ContextException
    {
        super.contextualize( a_context ) ;

        try
        {
        	m_context = ( BlockContext ) a_context ;
            if( getLogger().isDebugEnabled() )
            {
        		getLogger().debug( "Got handle on block context with base "
                    + "directory "
                    + m_context.getBaseDirectory().getAbsolutePath() ) ;
            }
        }
        catch( ClassCastException e )
        {
			getLogger().debug( "Context is not an instance of BlockContext!" ) ;
            throw new ContextException(
                "Context is not an instance of BlockContext!", e ) ;
        }
    }
}
