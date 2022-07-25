package org.apache.directory.server.ldap.handlers.extended;

import org.apache.directory.api.ldap.extras.extended.startTls.StartTlsResponse;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.ssl.SslFilter;

public class StartTlsFilter extends IoFilterAdapter 
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void filterWrite( NextFilter nextFilter, IoSession session, WriteRequest writeRequest ) throws Exception 
    {
        if ( writeRequest.getOriginalMessage() instanceof StartTlsResponse )
        {
            // We need to bypass the SslFilter
            IoFilterChain chain = session.getFilterChain();
            
            for ( IoFilterChain.Entry entry : chain.getAll() )
            {
                IoFilter filter = entry.getFilter();
                
                if ( filter instanceof SslFilter )
                {
                    entry.getNextFilter().filterWrite( session, writeRequest );
                }
            }
        }
        else
        {
            nextFilter.filterWrite( session, writeRequest );
        }
    }
}
