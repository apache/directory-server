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
package org.apache.eve.processor.impl ;


import java.util.EventObject ;

import org.apache.eve.event.AbstractSubscriber ;
import org.apache.eve.event.EventRouter ;
import org.apache.eve.event.RequestEvent ;
import org.apache.eve.event.RequestSubscriber ;
import org.apache.eve.processor.RequestProcessor ;
import org.apache.eve.processor.RequestProcessorMonitor;
import org.apache.eve.processor.RequestProcessorMonitorAdapter;
import org.apache.eve.seda.DefaultStage ;
import org.apache.eve.seda.StageConfig ;
import org.apache.ldap.common.message.Request;


/**
 * Default RequestProcessor service implemented as a POJO.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultRequestProcessor extends DefaultStage
    implements RequestProcessor, RequestSubscriber
{
    private final EventRouter router ;
    private RequestProcessorMonitor monitor = null ;
    
    
    public DefaultRequestProcessor( EventRouter router, StageConfig config )
    {
        super( config ) ;
        
        this.router = router ;
        this.router.subscribe( RequestEvent.class, this ) ;
        this.monitor = new RequestProcessorMonitorAdapter() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.RequestSubscriber#inform(
     * org.apache.eve.event.RequestEvent)
     */
    public void inform( RequestEvent event )
    {
        Request request = event.getRequest() ;
        
        
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     */
    public void inform( EventObject event )
    {
        try
        {
            AbstractSubscriber.inform( this, event ) ;
        }
        catch ( Throwable t )
        {
            monitor.failedOnInform( this, event, t ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.processor.RequestProcessor#dummy()
     */
    public void dummy()
    {
    }
}
