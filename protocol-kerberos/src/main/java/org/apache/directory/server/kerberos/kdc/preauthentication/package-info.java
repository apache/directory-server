/**
 * Provides the {@link IoHandlerChain} implementing the KDC's
 * pre-authentication processing, which is a sub-chain of the Authentication
 * Service (AS).  The pre-authentication implementation follows
 * the Chain of Responsibility pattern, using MINA's {@link IoHandlerChain}
 * support.
 */

package org.apache.directory.server.kerberos.kdc.preauthentication;
