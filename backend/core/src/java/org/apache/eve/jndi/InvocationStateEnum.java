package org.apache.eve.jndi ;


import org.apache.ldap.common.util.ValuedEnum;


/**
 * Enumeration type for the states an Invocation object goes through. 
 *
 */
public class InvocationStateEnum extends ValuedEnum
{
    /** The enumeration constant value for the preinvocation state */
    public static final int PREINVOCATION_VAL = 0 ;
    /** The enumeration constant value for the postinvocation state */
    public static final int POSTINVOCATION_VAL = 1 ;
    /** The enumeration constant value for the failurehandling state */
    public static final int FAILUREHANDLING_VAL = 2 ;

    /**
     * Invocations within the pre-invocation state have begun to be intercepted
     * within the before InterceptorPipeline.
     */
    public static final InvocationStateEnum PREINVOCATION = 
        new InvocationStateEnum( "PREINVOCATION", PREINVOCATION_VAL ) ;
    
    /**
     * Invocations within the post-invocation state have been invoked on the 
     * target proxied object and have begun to be intercepted within the after 
     * InterceptorPipeline.
     */
    public static final InvocationStateEnum POSTINVOCATION =
        new InvocationStateEnum( "POSTINVOCATION", POSTINVOCATION_VAL ) ;

    /**
     * Invocations within the failure handling state are being handled within 
     * the after failure InterceptorPipeline.  These Invocations raised 
     * exceptions either within the before pipeline, on the actual invocation, 
     * or within the after pipeline.  API's on the Invocation can be used to
     * determine exactly where the failure occured.
     */
    public static final InvocationStateEnum FAILUREHANDLING =
        new InvocationStateEnum( "FAILUREHANDLING", FAILUREHANDLING_VAL ) ;


    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param a_name a string name for the enumeration value.
     * @param a_value the integer value of the enumeration.
     */
    private InvocationStateEnum( final String a_name, final int a_value )
    {
        super( a_name, a_value ) ;
    }
}
