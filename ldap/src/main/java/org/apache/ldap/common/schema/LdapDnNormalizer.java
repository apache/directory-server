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
package org.apache.ldap.common.schema;


import javax.naming.InvalidNameException;
import javax.naming.Name ;
import javax.naming.NamingException ;

import org.apache.commons.lang.StringUtils;
import org.apache.ldap.common.name.LdapDnParser;
import org.apache.ldap.common.name.DnOidContainer;


/**
 * A distinguished name normalizer that works with a schema or without.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 359114 $
 */
public class LdapDnNormalizer implements Normalizer
{
	private static DnOidContainer oidContainer = DnOidContainer.getInstance();
	
	public static void setOidContainer( DnOidContainer oidContainer )
	{
		LdapDnNormalizer.oidContainer = oidContainer;
	}
	
    /**
     * @see org.apache.ldap.common.schema.Normalizer#normalize(java.lang.Object)
     */
    public Object normalize( Object value ) throws NamingException
    {
    	if ( value instanceof String )
    	{
    		return LdapDnNormalizer.normalize( (String)value );
    	}
    	else
    	{
    		return LdapDnNormalizer.normalize( (Name)value );
    	}
    }

    private static Name internalNormalize( Name name ) throws InvalidNameException, NamingException
	{
    	// First, check that the mapping table is filled with 
    	// values, else we can return the name as is.
    	if ( oidContainer == null )
    	{
    		return name;
    	}
    	
        // Loop on every NameComponent
        if ( name.size() != 0 )
        {
        	//name = LdapDN.toOidName( name, DnOidContainer.getOids() );
        }
        
        return name;
	}
	
    /**
     * Normalizes the value if it is a Name or a String returning the String 
     * representation always.  If the value is not a String or a Name the object
     * is returned as is.
     *
     * @see org.apache.ldap.common.schema.Normalizer#normalize(java.lang.Object)
     */
    public static Object normalize( Name value ) throws NamingException
    {
        if ( value == null )
        {
            return null;
        }
        
        return internalNormalize( (Name)value );
    }

    /**
     * Normalizes the value if it is a Name or a String returning the String 
     * representation always.  If the value is not a String or a Name the object
     * is returned as is.
     *
     * @see org.apache.ldap.common.schema.Normalizer#normalize(java.lang.Object)
     */
    public static Object normalize( String value ) throws NamingException
    {
        if ( StringUtils.isEmpty( value ) )
        {
            return null;
        }
        
        return internalNormalize( LdapDnParser.getNameParser().parse( value ) );
    }
    
    /**
     * A String representation of this normalizer
     */
    public String toString()
    {
    	return "DNNormalizer";
    }
}
