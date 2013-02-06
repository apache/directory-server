
package org.apache.directory.server.annotations;

public @interface CreateChngPwdServer
{
    /** The default kdc service principal */
    String srvPrincipal() default "kadmin/changepw@EXAMPLE.COM";

    /** The transports to use, default none */
    CreateTransport[] transports() default {};
}
