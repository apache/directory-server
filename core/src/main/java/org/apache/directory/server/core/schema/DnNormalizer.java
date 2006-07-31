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
package org.apache.directory.server.core.schema;


import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DnNormalizer implements Normalizer
{
    // @TODO use this later for seting up normalization
    private final AttributeTypeRegistry attrRegistry;
    
    
    public DnNormalizer( AttributeTypeRegistry attrRegistry )
    {
        this.attrRegistry = attrRegistry;
    }
    

    public Object normalize( Object value ) throws NamingException
    {
        LdapDN dn = null;
        if ( value instanceof LdapDN )
        {
            dn = ( LdapDN ) ( ( LdapDN ) value ).clone();
        }
        else if ( value instanceof Name )
        {
            dn = new LdapDN( ( Name ) value );
        }
        else if ( value instanceof String )
        {
            dn = new LdapDN( ( String ) value );
        }
        
        dn.normalize( attrRegistry.getNormalizerMapping() );
        return dn;
    }
}
