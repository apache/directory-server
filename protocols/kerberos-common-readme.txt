The dependency on protocol-common is keeping this module here.  This dep should
be removed somehow and kerberos-common should be moved to directory/common.

One reason for this out of place module in the dependency tree can be due to
the fact that we are trying to share classes between the pp for kerberos and 
changepw.  In this case we can separate these common class from common classes
meant for a client and a server to share.
