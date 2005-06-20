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
package org.apache.ldap.server.partition;


import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.server.db.Database;
import org.apache.ldap.server.db.SearchEngine;

import javax.naming.Name;
import javax.naming.NamingException;


/**
 * Creates a ContextPartition to be use for application specific contexts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApplicationPartition extends AbstractContextPartition
{
    /**
     * user provided suffix distinguished name for this backend set during
     * the Avalon configuration life-cycle phase.
     */
    private Name upSuffix;
    
    /**
     * normalized suffix distinguished name for this backend set during
     * the Avalon configuration life-cycle phase.
     */
    private Name normalizedSuffix;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     *
     * @param db the database to use for this partition
     * @param searchEngine the search engine to use for this partition
     * @param indexAttributes the index attrivutes including system attributes
     * @throws NamingException on failures while creating this partition
     */
    public ApplicationPartition(
            Database db, SearchEngine searchEngine, AttributeType[] indexAttributes )
        throws NamingException
    {
        super( db, searchEngine, indexAttributes );
    }



    // ------------------------------------------------------------------------
    // Backend Interface Method Implementations
    // ------------------------------------------------------------------------

    public void init( Name upSuffix, Name normSuffix )
    {
        this.upSuffix = upSuffix;
        this.normalizedSuffix = normSuffix;
    }

    /**
     * @see ContextPartition#getSuffix( boolean )
     */
    public Name getSuffix( boolean normalized )
    {
        if ( normalized )
        {
            return normalizedSuffix ;
        }
        
        return upSuffix ;
    }


    /**
     * @see BackingStore#isSuffix( Name )
     */
    public boolean isSuffix( Name dn ) throws NamingException
    {
        return normalizedSuffix.equals( dn ) ;
    }
}
