/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.server.protocol.shared;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Load strategy for configuration properties coming from a properties file.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PropsLoader implements LoadStrategy
{
    public Map load( String prefix, Map properties )
    {
        Map configuration = new HashMap( properties.size() );

        Iterator it = properties.keySet().iterator();

        while ( it.hasNext() )
        {
            String key = (String) it.next();

            if ( properties.get( key ) instanceof String )
            {
                String value = (String) properties.get( key );

                if ( key.startsWith( prefix ) )
                {
                    key = key.substring( key.indexOf( "." ) + 1 );
                }

                configuration.put( key, value );
            }
        }

        return configuration;
    }
}
