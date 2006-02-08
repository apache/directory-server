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


import java.util.Comparator;

import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.DnParser;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;


/**
 * A DnComparator that uses a parser to parse Dn strings. The parser may or may
 * not be Schema enabled.
 * 
 * @todo start using some kinda name cache here; it is way too expensive to be
 *       doing this over and over again
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DnComparator implements Comparator
{
    /** The parser used to parse DN Strings */
    private NameParser parser;

    private static final Object parserMutex = new Object();


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a default schema-less DN Comparator whose parser does not attempt
     * to normalize assertion valeus while comparing DN components.
     */
    public DnComparator() throws NamingException
    {
        synchronized ( parserMutex )
        {
            parser = new DnParser();
        }
    }


    /**
     * Creates a DN Comparator using a name component normalizer which should
     * use schema normalizers for attribute equality matching rules to normalize
     * assertion values.
     */
    public DnComparator(NameComponentNormalizer normalizer) throws NamingException
    {
        synchronized ( parserMutex )
        {
            parser = new DnParser( normalizer );
        }
    }


    // ------------------------------------------------------------------------
    // Comparator Methods
    // ------------------------------------------------------------------------

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( Object obj1, Object obj2 )
    {
        Name dn1 = null;
        Name dn2 = null;

        // Figure out how to compose the Name for the first object
        if ( obj1 instanceof Name )
        {
            dn1 = ( Name ) obj1;
        }
        else if ( obj1 instanceof String )
        {
            // Speedup the comparison
            if ( ( ( String ) obj1 ).compareTo( ( String ) obj2 ) == 0 )
            {
                return 0;
            }
            else
            {
                try
                {
                    synchronized ( parserMutex )
                    {
                        dn1 = parser.parse( ( String ) obj1 );
                    }
                }
                catch ( NamingException ne )
                {
                    throw new IllegalArgumentException( "first argument (" + obj1 + ") was not a distinguished name" );
                }
            }
        }
        else
        {
            throw new IllegalArgumentException( "first argument (" + obj1 + ") was not a Name or a String" );
        }

        // Figure out how to compose the Name for the second object
        if ( obj2 instanceof Name )
        {
            dn2 = ( Name ) obj2;
        }
        else if ( obj2 instanceof String )
        {
            try
            {
                synchronized ( parserMutex )
                {
                    dn2 = parser.parse( ( String ) obj2 );
                }
            }
            catch ( NamingException ne )
            {
                throw new IllegalArgumentException( "second argument was not a distinguished name" );
            }
        }
        else
        {
            throw new IllegalArgumentException( "second argument was not a distinguished name" );
        }

        return dn1.compareTo( dn2 );
    }
}
