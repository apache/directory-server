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
package org.apache.eve.decoder ;


/**
 * A configuration for decoders.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface DecoderConfig
{
    /**
     * Gets this Decoder's decoded object size limit for in memory objects.  If
     * this limit in bytes is reached then the data is streamed to a temporary
     * data store on disk for use later.  In such situations rather than 
     * returning the object itself a reference to access the object is returned.
     * 
     * @return the in memory size limit to decoded data before it is streamed 
     * to disk
     */
    int getDecodeLimit() ;
}
