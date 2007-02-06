/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.schema;


import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.AbstractSchemaObject;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.MutableSchemaObject;
import org.apache.directory.shared.ldap.schema.NameForm;


/**
 * A ditStructureRule bean implementation which dynamically looks up dependencies using 
 * a resgistries object.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DitStructureRuleImpl extends AbstractSchemaObject implements DITStructureRule, MutableSchemaObject
{
    private static final long serialVersionUID = 1L;
    private final String[] EMPTY_STR_ARRAY = new String[0];
    private final DITStructureRule[] EMPTY_DSR_ARRAY = new DITStructureRule[0];

    private final Registries registries;
    private String nameFormOid;
    private String[] superClassOids;
    private DITStructureRule[] superClasses;
    
    
    public DitStructureRuleImpl( String oid, Registries registries )
    {
        super( oid );
        this.registries = registries;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.DITStructureRule#getNameForm()
     */
    public NameForm getNameForm() throws NamingException
    {
        return registries.getNameFormRegistry().lookup( nameFormOid );
    }

    
    public void setNameFormOid( String nameFormOid )
    {
        this.nameFormOid = nameFormOid;
    }
    

    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.DITStructureRule#getSuperClasses()
     */
    public DITStructureRule[] getSuperClasses() throws NamingException
    {
        if ( this.superClassOids == null )
        {
            return EMPTY_DSR_ARRAY;
        }
        
        for ( int ii = 0; ii < superClassOids.length; ii++ )
        {
            superClasses[ii] = registries.getDitStructureRuleRegistry().lookup( superClassOids[ii] );
        }
        
        return superClasses;
    }
    
    
    public void setSuperClassOids( String[] superClassOids )
    {
        if ( superClassOids == null )
        {
            this.superClassOids = EMPTY_STR_ARRAY;
            this.superClasses = EMPTY_DSR_ARRAY;
        }
        else
        {
            this.superClassOids = superClassOids;
            this.superClasses = new DITStructureRule[superClassOids.length];
        }
    }
    
    
    public void setObsolete( boolean obsolete )
    {
        super.setObsolete( obsolete );
    }
    
    
    public void setNames( String[] names )
    {
        super.setNames( names );
    }
    
    
    public void setSchema( String schema )
    {
        super.setSchema( schema );
    }
    
    
    public void setDescription( String description )
    {
        super.setDescription( description );
    }
}
