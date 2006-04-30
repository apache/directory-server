                            Apache Directory Server
                            =======================

Documentation
-------------

All installer based distributions include a copy of the site documentation 
within the docs directory.  Point your browser to:

    ./docs/index.html

or go online here for the most up to date documentation,

     http://directory.apache.org/subprojects/apacheds/index.html


Running
-------

The server is designed to run as a Windows Service or as a UNIX Daemon (also
on MacOSX).  ApacheDS uses commons-daemon procrun and jsvc to install and run
as a service or daemon respecitively.  These are the only native components 
specific to the targeted operating system.

You can start, and stop the daemon on UNIX using the /etc/init.d/apacheds 
script.  The debug command starts the server without the daemon.  It can be
used to attach to the server using a debugger and to dump output to the
console.  Only in debug mode can the diagnostic screens be launched.  In
daemon mode the proper DISPLAY parameter must be set to launch the diagnostics
on startup.

On windows the server can be started like any other service using the services
console via Microsoft Management Console.  It can also be started, stoped and
configured using the procrun service manager installed for it: see 
Start->All Programs->apacheds->Service Setttings. A tray icon can also be 
launched for the application to monitor it and to control the service: see 
Start->All Programs->apacheds->Tray Monitor.  The server can also be started 
in a special debug mode (not for IDE remote debugging though) where it dumps
output to the command line in a cmd window rather than to the log files.  You
can launch the server in this mode by selecting Start->All Programs->
apacheds->Test Service.  The server can also be started in test mode by
running the apacheds.exe executable from the commandline.  Likewise the 
service manager can be started from the command line by invoking
apachedsw.exe.


Tool Support
------------

ApacheDS comes bundled with a apacheds-tools.jar executable jar in the bin
directory of the installation.  You can run the tools application and list the 
available commands on all platforms like so:

   java -jar apacheds-tools.jar help

The tool contains several commands.  Here's a breif listing with description:

   help             displays help message 
   notifications    listens to the server for disconnect msgs
   dump             dumps partitions in LDIF format for recovery and backup
   graceful         starts graceful shutdown with shutdown delay & timeoffline
   diagnostic       launches diagnostic UI for inspecting server partitions
                    and client sessions

Over time this tool will include clients to add, delete, compare, modify and
rename entries as well as search the directory.


Connecting
----------

See http://directory.apache.org/subprojects/apacheds/users/authentication.html
or the bundled documentation.


Building Bundled Sources
------------------------

The sources are bundled with the installers and can be found in the src
directory of the installation base.  The build system used is Maven 2.  We use
version 2.0.3 for this release.  In general we try to use the most recent 
production release of Maven.  You can build the server like so:

   cd ${install.basedir}; cd src; mvn install 

If you're interested in the latest sources you can check out ApacheDS using 
subversion at the following base URL:

   http://svn.apache.org/repos/asf/directory/trunks

We recommend issuing a build command at the top most level in case ApacheDS
has dependencies on the latest MINA version in trunks.  


Notes
-----

 o The kerberos service, NTP service, and Changepw service are present
   however they are inactive by default.  SSL support likewise is present
   yet inactive.  For more information visit the website or contact us at
   users@directory.apache.org.

Issues
------

Please direct all issues to users@directory.apache.org or file a JIRA here:

   http://issues.apache.org/jira/DIRSERVER


Thanks and enjoy,
Apache Directory Team

