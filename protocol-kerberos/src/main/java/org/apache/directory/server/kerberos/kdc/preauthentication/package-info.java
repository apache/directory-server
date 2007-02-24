/**
 * Provides the {@link IoHandlerChain} implementing the KDC's
 * pre-authentication processing, which is a sub-chain of the Authentication
 * Service (AS).  The pre-authentication implementation follows
 * the Chain of Responsibility pattern, using MINA's {@link IoHandlerChain}
 * support.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */

package org.apache.directory.server.kerberos.kdc.preauthentication;
