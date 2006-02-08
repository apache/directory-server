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
package org.apache.directory.shared.asn1.codec.stateful ;




/**
 * A do nothing decoder monitor adapter.  At a bare minimum warning, error and
 * fatal exceptions are reported to the console when using this adapter to 
 * prevent exceptions from being completely ignored.
 *
 * @author Apache Software Foundation
 * @version $Rev$
 */
public class DecoderMonitorAdapter implements DecoderMonitor
{
    /* (non-Javadoc)
     * @see org.apache.asn1.codec.stateful.DecoderMonitor#error(
     * org.apache.asn1.codec.stateful.StatefulDecoder, java.lang.Exception)
     */
    public void error( StatefulDecoder decoder, Exception exception )
    {
        System.err.println( "ERROR: " + exception.getMessage() ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.asn1.codec.stateful.DecoderMonitor#fatalError(
     * org.apache.asn1.codec.stateful.StatefulDecoder, java.lang.Exception)
     */
    public void fatalError( StatefulDecoder decoder, Exception exception )
    {
        System.err.println( "FATAL: " + exception.getMessage() ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.asn1.codec.stateful.DecoderMonitor#warning(
     * org.apache.asn1.codec.stateful.StatefulDecoder, java.lang.Exception)
     */
    public void warning( StatefulDecoder decoder, Exception exception )
    {
        System.err.println( "WARN: " + exception.getMessage() ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.asn1.codec.stateful.DecoderMonitor#callbackOccured(
     * org.apache.asn1.codec.stateful.StatefulDecoder,
     * org.apache.asn1.codec.stateful.DecoderCallback, java.lang.Object)
     */
    public void callbackOccured( StatefulDecoder decoder, DecoderCallback cb,
								 Object decoded )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.asn1.codec.stateful.DecoderMonitor#callbackSet(
     * org.apache.asn1.codec.stateful.StatefulDecoder,
     * org.apache.asn1.codec.stateful.DecoderCallback,
     * org.apache.asn1.codec.stateful.DecoderCallback)
     */
    public void callbackSet( StatefulDecoder decoder, DecoderCallback oldcb,
							 DecoderCallback newcb )
    {
    }
}
