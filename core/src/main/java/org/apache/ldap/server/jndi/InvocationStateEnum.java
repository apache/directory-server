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
package org.apache.ldap.server.jndi;


import org.apache.ldap.common.util.ValuedEnum;


/**
 * Enumeration type for the states an Invocation object goes through. 
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class InvocationStateEnum extends ValuedEnum
{
    private static final long serialVersionUID = 3256720693239558450L;

    /** The enumeration constant value for the preinvocation state */
    public static final int PREINVOCATION_VAL = 0;
    /** The enumeration constant value for the postinvocation state */
    public static final int POSTINVOCATION_VAL = 1;
    /** The enumeration constant value for the failurehandling state */
    public static final int FAILUREHANDLING_VAL = 2;

    /**
     * Invocations within the pre-invocation state have begun to be intercepted
     * within the before InterceptorPipeline.
     */
    public static final InvocationStateEnum PREINVOCATION = 
        new InvocationStateEnum( "PREINVOCATION", PREINVOCATION_VAL );
    
    /**
     * Invocations within the post-invocation state have been invoked on the 
     * target proxied object and have begun to be intercepted within the after 
     * InterceptorPipeline.
     */
    public static final InvocationStateEnum POSTINVOCATION =
        new InvocationStateEnum( "POSTINVOCATION", POSTINVOCATION_VAL );

    /**
     * Invocations within the failure handling state are being handled within 
     * the after failure InterceptorPipeline.  These Invocations raised 
     * exceptions either within the before pipeline, on the actual invocation, 
     * or within the after pipeline.  API's on the Invocation can be used to
     * determine exactly where the failure occured.
     */
    public static final InvocationStateEnum FAILUREHANDLING =
        new InvocationStateEnum( "FAILUREHANDLING", FAILUREHANDLING_VAL );


    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param name a string name for the enumeration value.
     * @param value the integer value of the enumeration.
     */
    private InvocationStateEnum( final String name, final int value )
    {
        super( name, value );
    }
}
