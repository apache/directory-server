/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve;

import javax.naming.Name ;
import javax.naming.NamingException ;
import javax.naming.InvalidNameException ;
import javax.naming.directory.Attributes ;

import org.apache.ldap.common.name.LdapName ;
import org.apache.ldap.common.util.NamespaceTools ;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.message.LockableAttributesImpl ;

import org.apache.eve.db.Database;
import org.apache.eve.db.SearchEngine;


/**
 * A very special ContextPartition used to store system information such as
 * users, the system catalog and other administrative information.  This
 * partition is fixed at the ou=system context.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public final class SystemPartition extends AbstractContextPartition
{
    /**
     * System backend suffix constant.  Should be kept down to a single Dn name 
     * component or the default constructor will have to parse it instead of 
     * building the name.  Note that what ever the SUFFIX equals it should be 
     * both the normalized and the user provided form.
     */
    public static final String SUFFIX = "ou=system" ;
    
    /** The suffix as a name. */
    private final Name suffix ;

    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S 
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates the system partition which is used to store various peices of
     * information critical for server operation.  Things like the system
     * catalog and other operational information like system users are
     * maintained within the context of this partition.  Unlike other
     * ContextBackends which must have their suffix specified this one does
     * not since it will stay fixed at the following namingContext: ou=system.
     *
     * @param db the database used for this partition
     * @param searchEngine the search engine to conduct searches with
     * @param indexAttributes the attributeTypes of indicies to build which must
     * also contain all system index attribute types - if not the system will
     * not operate correctly.
     */
    public SystemPartition( Database db, SearchEngine searchEngine,
                            AttributeType[] indexAttributes )
        throws NamingException
    {
        super( db, searchEngine, indexAttributes );
        suffix = new LdapName() ;
        
        try
        {
            suffix.add( SUFFIX ) ;
        }
        catch ( InvalidNameException e ) 
        {
            ; // Never thrown - name will always be valid!
        }

        // add the root entry for the system root context if it does not exist
        Attributes l_attributes = db.getSuffixEntry() ;
        if ( null == l_attributes )
        {
            l_attributes = new LockableAttributesImpl() ;
            l_attributes.put( "objectClass", "top" ) ;
            l_attributes.put( "objectClass", "organizationalUnit" ) ;
            l_attributes.put( NamespaceTools.getRdnAttribute( SUFFIX ),
                NamespaceTools.getRdnValue( SUFFIX ) ) ;

            getDb().add( SUFFIX, suffix, l_attributes ) ;
            //m_logger.info( "Added suffix '" + SUFFIX
            //    + "' for system backend" ) ;
        }
    }


    // ------------------------------------------------------------------------
    // B A C K E N D   M E T H O D S 
    // ------------------------------------------------------------------------


    /**
     * @see ContextPartition#getSuffix(boolean)
     */
    public final Name getSuffix( boolean normalized )
    {
        /*
         * The suffix is presummed to be both the normalized and the user
         * provided form so we do not need to take a_normalized into account.
         */
        return ( Name ) suffix.clone() ;
    }


    /**
     * @see BackingStore#isSuffix(javax.naming.Name)
     */
    public final boolean isSuffix( Name dn )
    {
        return SUFFIX.equals( dn.toString() ) ;
    }
}
