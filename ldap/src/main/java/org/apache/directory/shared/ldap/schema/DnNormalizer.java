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
package org.apache.directory.shared.ldap.schema;


import javax.naming.Name ;
import javax.naming.NameParser ;
import javax.naming.NamingException ;

import org.apache.directory.shared.ldap.name.DnParser;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.name.SimpleNameComponentNormalizer;


/**
 * A distinguished name normalizer that works with a schema or without.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DnNormalizer implements Normalizer
{
    /** name parser used by this normalizer */
    private NameParser parser = null ;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    

    /**
     * Creates a Dn Normalizer which normalizes distinguished names by
     * performing a deep trim to lower case normalization on assertion values
     * regardless of the attributeType of the name component.
     */
    public DnNormalizer() throws NamingException
    {
        this ( new SimpleNameComponentNormalizer( new DeepTrimToLowerNormalizer() ) ) ;
    }


    /**
     * Creates a Dn Normalizer which normalizes distinguished names by
     * using the same Normalizer to to normalize assertion values regardless
     * of the attributeType of the name component.
     *
     * @param normalizer Normalizer to use for values regardless of attributeType
     */
    public DnNormalizer( Normalizer normalizer ) throws NamingException
    {
        this ( new SimpleNameComponentNormalizer( normalizer ) ) ;
    }


    /**
     * Creates a Dn Normalizer which uses a name component normalizer to
     * dynamically normalize all name component assertion values based on
     * attributeType.
     *
     * @param normalizer the name component normalizer to use
     */
    public DnNormalizer( NameComponentNormalizer normalizer ) throws NamingException
    {
        parser = new DnParser( normalizer ) ;
    }


    /**
     * Normalizes the value if it is a Name or a String returning the String 
     * representation always.  If the value is not a String or a Name the object
     * is returned as is.
     *
     * @see org.apache.directory.shared.ldap.schema.Normalizer#normalize(java.lang.Object)
     */
    public Object normalize( Object value ) throws NamingException
    {
        if ( value == null )
        {
            return null;
        }
        
        String str = null ;

        if ( value instanceof Name )
        {
            str = value.toString() ;
        }
        else if ( value instanceof String )
        {
            str = ( String ) value ;
        }

        return parser.parse( str ).toString() ;
    }
}
