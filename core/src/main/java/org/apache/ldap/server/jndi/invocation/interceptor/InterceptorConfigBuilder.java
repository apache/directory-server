package org.apache.ldap.server.jndi.invocation.interceptor;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * @todo doc me
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */

public class InterceptorConfigBuilder
{

    public static Map build( Map map, String prefix )
    {
        Map newMap = new HashMap();
        Iterator it = map.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry e = ( Map.Entry ) it.next();
            String key = e.getKey().toString();
            if ( key.startsWith( prefix ) && key.length() > prefix.length() )
            {
                key = key.substring( prefix.length() );
                if ( key.indexOf( '#' ) < 0 )
                {
                    continue;
                }
                if ( key.charAt( 0 ) == '.' || key.charAt( 0 ) == '#' )
                {
                    key = key.substring( 1 );
                }

                newMap.put( key, e.getValue() );
            }
        }

        return newMap;
    }


    private InterceptorConfigBuilder()
    {
    }
}
