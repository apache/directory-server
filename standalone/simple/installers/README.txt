                               Apache Directory Server
                               =======================

Documentation
-------------

To run, build or just to read about how to use the server please refer to the
documentation.  For binary distributions you can just refer to the local copy
here:

./docs/index.html

or online here,

http://directory.apache.org/subprojects/apacheds/index.html


Running
-------

To run with defaults,

java -jar apacheds-main-<version>.jar 

or with custom settings,

java -jar apacheds-main-<version>.jar your.properties

where <version> is the current version of ApacheDS 
(for instance, apache-main-0.9.1.jar)

Connecting
----------

see http://directory.apache.org/subprojects/apacheds/users/authentication.html


Building 
--------

maven multiproject:install 


Notes
-----

 o The kerberos service has been added to this distribution but is off by 
   default.  Please check documentation on kerberos section of site for
   configuring the kerberos protocol provider plugin. 

