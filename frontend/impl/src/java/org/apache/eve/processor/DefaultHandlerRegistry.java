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
package org.apache.eve.processor ;


import java.util.Map ;
import java.util.HashMap ;

import org.apache.eve.processor.RequestHandler ;
import org.apache.eve.processor.HandlerRegistry ;

import org.apache.ldap.common.message.MessageTypeEnum ;


/**
 * A registry of handlers.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultHandlerRegistry implements HandlerRegistry
{
    /** a map of handler request types to the handler */ 
    private final Map map = new HashMap() ; 
    
    
    /**
     * Creates a default handler with all the handlers set.
     */
    public DefaultHandlerRegistry()
    {
        RequestHandler handler = new AbandonHandler() ;
        map.put( handler.getRequestType(), handler ) ;
        
        handler = new AddHandler() ;
        map.put( handler.getRequestType(), handler ) ;
        
        handler = new BindHandler() ;
        map.put( handler.getRequestType(), handler ) ;
        
        handler = new CompareHandler() ;
        map.put( handler.getRequestType(), handler ) ;
        
        handler = new DeleteHandler() ;
        map.put( handler.getRequestType(), handler ) ;
        
        handler = new ExtendedHandler() ;
        map.put( handler.getRequestType(), handler ) ;
        
        handler = new ModifyDnHandler() ;
        map.put( handler.getRequestType(), handler ) ;
        
        handler = new ModifyHandler() ;
        map.put( handler.getRequestType(), handler ) ;
        
        handler = new SearchHandler() ;
        map.put( handler.getRequestType(), handler ) ;
        
        handler = new UnbindHandler() ;
        map.put( handler.getRequestType(), handler ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.processor.HandlerRegistry#lookup(
     * org.apache.ldap.common.message.MessageTypeEnum)
     */
    public RequestHandler lookup( MessageTypeEnum messageType )
    {
        return ( RequestHandler ) map.get( messageType ) ;
    }
}
