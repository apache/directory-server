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
package org.apache.directory.server.core.schema.bootstrap;


import javax.naming.NamingException;

import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.server.core.schema.Registries;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A name or numeric id normalizer.  Needs an OID registry to operate properly.
 * The OID registry is injected into this class after instantiation if a 
 * setRegistries(Registries) method is exposed.
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameOrNumericIdNormalizer implements Normalizer
{
    private static final long serialVersionUID = 1L;

    private transient OidRegistry registry;
    
    
    public NameOrNumericIdNormalizer( OidRegistry registry )
    {
        this.registry = registry;
    }
    
    
    public NameOrNumericIdNormalizer()
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.Normalizer#normalize(java.lang.Object)
     */
    public Object normalize( Object value ) throws NamingException
    {
        String strValue;

        if ( value == null )
        {
            return null;
        }
        
        if ( value instanceof String )
        {
            strValue = ( String ) value;
        }
        else if ( value instanceof byte[] )
        {
            strValue = StringTools.utf8ToString( ( byte[] ) value ); 
        }
        else
        {
            strValue = value.toString();
        }

        if ( strValue.length() == 0 )
        {
            return "";
        }
        
        if ( registry.hasOid( strValue ) )
        {
            return registry.getOid( strValue );
        }
        
        throw new LdapNamingException( ResultCodeEnum.OTHER );
    }
    
    
    public void setRegistries( Registries registries )
    {
        this.registry = registries.getOidRegistry();
    }
}
