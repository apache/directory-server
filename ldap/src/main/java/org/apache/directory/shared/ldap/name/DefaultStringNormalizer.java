/**
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.name;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A normalizer which transforms an escape sequence into an internal 
 * canonical form: into UTF-8 characters presuming the escape sequence
 * fits that range.  This is used explicity for non-binary attribute
 * types only.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultStringNormalizer implements Normalizer
{
    private static final DefaultStringNormalizer NORMALIZER = new DefaultStringNormalizer();
    
    public Object normalize( Object value ) throws NamingException
    {
        String str = ( String ) value;
        
        if ( str == null || str.length() == 0 )
        {
            return str;
        }
        
        if ( str.charAt( 0 ) == '#' )
        {
            return StringTools.decodeHexString( str );
        }
        
        if ( str.indexOf( '\\' ) != -1 )
        {
            return StringTools.decodeEscapedHex( str );
        }
        
        return str;
    }

    public static Object normalizeString( String string ) throws NamingException
    {
        return NORMALIZER.normalize( string );
    }
}
