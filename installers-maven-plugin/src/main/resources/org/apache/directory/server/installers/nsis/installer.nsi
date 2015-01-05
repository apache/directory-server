#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#

#
# Constants and variables
#
    !define Application "ApacheDS"
    !define Version "${version}"
    !define InstallerIcon "installer.ico"
    !define UninstallerIcon "uninstaller.ico"
    !define WelcomeImage "welcome.bmp"
    !define HeaderImage "header.bmp"
    !define OutFile "${finalName}"
    !define InstallationFiles "${installationFiles}"
    !define InstancesFiles "${instancesFiles}"
    !define JREVersion "1.5.0"
    !define INSTDIR_REG_ROOT "HKLM"
    !define INSTDIR_REG_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${Application}"
    
    # Variables needed for JRE detection
    Var JREPath

#
# Modules inclusions
#
    # Modern UI module
    !include "MUI.nsh"
    
    # Sections module
    !include "Sections.nsh"

#
# Macros (which need to be defined before they're used)
#
    # Creates an Internet shortcut
    !macro CreateInternetShortcut FILENAME URL ;ICONFILE ICONINDEX
        WriteINIStr "${FILENAME}.url" "InternetShortcut" "URL" "${URL}"
    !macroend

#
# Configuration
#
    # Name of the application
    Name "${Application}"
    
    # Output installer file
    OutFile "../${OutFile}"
    
    # Default install directory
    InstallDir "$PROGRAMFILES\${Application}"
    
    # Branding text
    BrandingText "${Application} - ${Version}"

    # Activating XPStyle
    XPStyle on

    # Installer icon
    !define MUI_ICON "${InstallerIcon}"
    
    # Uninstaller icon
    !define MUI_UNICON "${UninstallerIcon}"
    
    # Welcome image
    !define MUI_WELCOMEFINISHPAGE_BITMAP "${WelcomeImage}"
    
    # Activating header image
    !define MUI_HEADERIMAGE
    !define MUI_HEADERIMAGE_BITMAP "${HeaderImage}"

    # Activating small description for the components page
    !define MUI_COMPONENTSPAGE_SMALLDESC
    
    # Activating a confirmation when aborting the installation
    !define MUI_ABORTWARNING

#
# Pages
#
    #
    # Installer pages
    #
    
    # Welcome page
    !insertmacro MUI_PAGE_WELCOME
    
    # License page
    !insertmacro MUI_PAGE_LICENSE "${InstallationFiles}\LICENSE"

    # Installation directory page
    Var SERVER_HOME_DIR
    !define MUI_DIRECTORYPAGE_VARIABLE $SERVER_HOME_DIR
    !define MUI_DIRECTORYPAGE_TEXT_DESTINATION "Server Home Directory"
    !define MUI_DIRECTORYPAGE_TEXT_TOP "Select the directory where you would like to install ${Application}"
    !insertmacro MUI_PAGE_DIRECTORY

    # Instances directory page
    Var INSTANCES_HOME_DIR
    !define MUI_PAGE_CUSTOMFUNCTION_PRE PreInstancesDir
    !define MUI_DIRECTORYPAGE_VARIABLE $INSTANCES_HOME_DIR
    !define MUI_DIRECTORYPAGE_TEXT_DESTINATION "Server Instances Home Directory"
    !define MUI_DIRECTORYPAGE_TEXT_TOP "Select the directory where you would like instances data to be stored.$\n$\nThis directory will be the home location for new instances."
    !insertmacro MUI_PAGE_DIRECTORY

    # JRE directory page
    Var JAVA_HOME_DIR
    !define MUI_DIRECTORYPAGE_VARIABLE $JAVA_HOME_DIR
    !define MUI_DIRECTORYPAGE_TEXT_DESTINATION "Java Home Directory"
    !define MUI_DIRECTORYPAGE_TEXT_TOP "Select the Java home directory that you would like to use for running the installed applications."
    !insertmacro MUI_PAGE_DIRECTORY
    
    # Installation page
    !insertmacro MUI_PAGE_INSTFILES
    
    # Finish page
    !insertmacro MUI_PAGE_FINISH
    
    
    #
    # Uninstaller pages
    #
    
    # Confirmation page
    !insertmacro MUI_UNPAGE_CONFIRM
    
    # Uninstallation page
    !insertmacro MUI_UNPAGE_INSTFILES

#
# Languages (the first one is the default one)
#
    !insertmacro MUI_LANGUAGE "English"
    
#
# Sections
#
    # Installer section
    Section
    
    	# Writing installation files
        SetOutPath "$SERVER_HOME_DIR"
        
        # Adding installation source files
        File /r "${InstallationFiles}\*"
        
        # Converting files line encoding
        Push "$SERVER_HOME_DIR"
        Push "*.txt"
        Call ConvertFiles
        
        # Converting files line encoding
        Push "$SERVER_HOME_DIR\conf"
        Push "*.*"
        Call ConvertFiles
        
        # Replacing java home directory in config file
        GetFunctionAddress $R0 ReplaceJavaHome # handle to callback fn
        Push $R0
        Push "$SERVER_HOME_DIR\conf\wrapper.conf" # file to replace in
        Call ReplaceInFile
        
        # Configuring registries for the uninstaller
        WriteRegStr "${INSTDIR_REG_ROOT}" "SOFTWARE\${Application}" "SERVER_HOME_DIR" $SERVER_HOME_DIR
        WriteRegStr "${INSTDIR_REG_ROOT}" "SOFTWARE\${Application}" "INSTANCES_HOME_DIR" $INSTANCES_HOME_DIR
        WriteRegStr "${INSTDIR_REG_ROOT}" "${INSTDIR_REG_KEY}" "DisplayName" "${Application} - (remove only)"
        WriteRegStr "${INSTDIR_REG_ROOT}" "${INSTDIR_REG_KEY}" "DisplayIcon" "$SERVER_HOME_DIR\uninstall.exe"
        WriteRegStr "${INSTDIR_REG_ROOT}" "${INSTDIR_REG_KEY}" "UninstallString" '"$SERVER_HOME_DIR\uninstall.exe"'
        WriteRegDWORD "${INSTDIR_REG_ROOT}" "${INSTDIR_REG_KEY}" "NoModify" "1"
        WriteRegDWORD "${INSTDIR_REG_ROOT}" "${INSTDIR_REG_KEY}" "NoRepair" "1"

        # Creating the uninstaller
        WriteUninstaller "$INSTDIR\Uninstall.exe"
    
        # Creating directory in start menu
        CreateDirectory "$SMPROGRAMS\${Application}"
        
        # Creating links in start menu
        !insertmacro CreateInternetShortcut "$SMPROGRAMS\${Application}\Basic Users Guide" "http://directory.apache.org/apacheds/1.5/apacheds-v15-basic-users-guide.html"
        !insertmacro CreateInternetShortcut "$SMPROGRAMS\${Application}\Advanced Users Guide" "http://directory.apache.org/apacheds/1.5/apacheds-v15-advanced-users-guide.html"
        !insertmacro CreateInternetShortcut "$SMPROGRAMS\${Application}\Developers Guide" "http://directory.apache.org/apacheds/1.5/apacheds-v15-developers-guide.html"
        
        # Creating a shortcut to the 'Manage ApacheDS' utility
        CreateShortCut "$SMPROGRAMS\${Application}\Manage ApacheDS.lnk" "$SERVER_HOME_DIR\Manage ApacheDS.exe" "" "$SERVER_HOME_DIR\Manage ApacheDS.exe" 0
        
        # Creating a shortcut to the uninstaller
        CreateShortCut "$SMPROGRAMS\${Application}\Uninstall ApacheDS.lnk" "$SERVER_HOME_DIR\Uninstall.exe" "" "$SERVER_HOME_DIR\Uninstall.exe" 0
        
    	# Writing instances files
        SetOutPath "$INSTANCES_HOME_DIR"
        
        # Adding instances source files
        File /r "${InstancesFiles}\*"
        
        # Converting files line encoding
        Push "$INSTANCES_HOME_DIR\conf"
        Push "*.*"
        Call ConvertFiles
        
        # Replacing installation directory in config file
        GetFunctionAddress $R0 ReplaceInstallationDirectory # handle to callback fn
        Push $R0
        Push "$INSTANCES_HOME_DIR\default\conf\wrapper-instance.conf" # file to replace in
        Call ReplaceInFile
        
        # Registering the server instance
        Call RegisterInstance
    SectionEnd

    # Uninstaller section
    Section Uninstall
    	# Getting install locations
        ReadRegStr $R1 "${INSTDIR_REG_ROOT}" "SOFTWARE\${Application}" "SERVER_HOME_DIR"
        StrCpy $SERVER_HOME_DIR $R1
        ReadRegStr $R1 "${INSTDIR_REG_ROOT}" "SOFTWARE\${Application}" "INSTANCES_HOME_DIR"
        StrCpy $INSTANCES_HOME_DIR $R1
    
        #Need to parse a list of instances or directories somehow
        Call un.RegisterInstance
        
        # Remove shortcuts and folders in the start menu
        RMDir /r "$SMPROGRAMS\${Application}"
        
        # Removing registry keys
        DeleteRegKey "${INSTDIR_REG_ROOT}" "${INSTDIR_REG_KEY}"
        DeleteRegKey "${INSTDIR_REG_ROOT}" "SOFTWARE\${Application}"
        
        # Removing files in root, then all dirs created by the installer (leave user added or instance dirs)
        Delete "$INSTDIR\*"
        RMDir /r "$INSTDIR\bin"
        RMDir /r "$INSTDIR\conf"
        RMDir /r "$INSTDIR\lib"

    SectionEnd

#
# Functions
#
    #
    # <ConvertFiles>
    #
    
    # Converts Unix to CR/LF for Windows
    Function ConvertFiles
        Pop $R0
        Pop $R1

        FindFirst $R2 $R3 "$R1\$R0"
        loop:
          StrCmp $R3 "" done
          Push "$R1\$R3"
          Call ConvertUnixNewLines
          FindNext $R2 $R3
          Goto loop
        done:
          FindClose $R2

        Pop $R3
        Pop $R2
        Pop $R1
        Pop $R0
    FunctionEnd
    
    #
    # </ConvertFiles>
    #
    
    #
    # <RegisterInstance>
    #

    Function RegisterInstance
        nsExec::ExecToLog '"$SERVER_HOME_DIR\bin\wrapper.exe" -i "$INSTANCES_HOME_DIR\default\conf\wrapper-instance.conf" set.INSTANCE_DIRECTORY="$INSTANCES_HOME_DIR\default" set.INSTANCE="default"'
    FunctionEnd
    
    #
    # </RegisterInstance>
    #
    
    #
    # <un.RegisterInstance>
    #

    Function un.RegisterInstance
        nsExec::ExecToLog '"$SERVER_HOME_DIR\bin\wrapper.exe" -r "$INSTANCES_HOME_DIR\default\conf\wrapper-instance.conf" set.INSTANCE_DIRECTORY="$INSTANCES_HOME_DIR\default" set.INSTANCE="default"'
    FunctionEnd
    
    #
    # </un.RegisterInstance>
    #
    
    #
    # <.onInstSuccess>
    #

    Function .onInstSuccess
        # Start the server
        MessageBox MB_YESNO|MB_ICONQUESTION "Do you want to start the default server instance?" IDYES startService IDNO End
        startService:  
            nsExec::ExecToLog '"$SERVER_HOME_DIR\bin\wrapper.exe" -t "$INSTANCES_HOME_DIR\default\conf\wrapper-instance.conf" set.INSTANCE_DIRECTORY="$INSTANCES_HOME_DIR\default" set.INSTANCE="default"'
  
        End:
    FunctionEnd
    
    #
    # </.onInstSuccess>
    #
    
    #
    # <.onInit>
    #

    Function .onInit
        SetCurInstType 0
        SetAutoClose false
        StrCpy $SERVER_HOME_DIR "$PROGRAMFILES\ApacheDS"
        Call CheckInstalledJRE
        StrCpy $JAVA_HOME_DIR "$JREPath"
    FunctionEnd
    
    #
    # </.onInit>
    #
    
    #
    # <PreInstancesDir>
    #

    Function PreInstancesDir
        StrCpy $INSTANCES_HOME_DIR $SERVER_HOME_DIR\instances
    FunctionEnd
    
    #
    # </PreInstancesDir>
    #
    
    #
    # <CheckInstalledJRE>
    #

    Function CheckInstalledJRE
        Push "${JREVersion}"
        Call DetectJRE
        Exch $0	; Get return value from stack
        StrCmp $0 "0" End
        StrCmp $0 "-1" End
        Goto JREAlreadyInstalled
    
        JREAlreadyInstalled:
            StrCpy $JREPath "$0"
            Pop $0 # Restore $0
            Return
      
        End:
    FunctionEnd
    
    #
    # </CheckInstalledJRE>
    #
    
    #
    # <DetectJRE>
    #

    # DetectJRE. Version requested is on the stack.
    # Returns (on stack): 0 - JRE not found. -1 - JRE found but too old. Otherwise - Path to JAVA EXE
    # Stack value will be overwritten!
    Function DetectJRE
      Exch $0	; Get version requested
            ; Now the previous value of $0 is on the stack, and the asked for version of JDK is in $0
      Push $1	; $1 = Java version string (ie 1.5.0)
      Push $2	; $2 = Javahome
      Push $3	; $3 and $4 are used for checking the major/minor version of java
      Push $4
      ;MessageBox MB_OK "Detecting JRE"
      ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
      ;MessageBox MB_OK "Read : $1"
      StrCmp $1 "" DetectTry2
      ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$1" "JavaHome"
      ;MessageBox MB_OK "Read 3: $2"
      StrCmp $2 "" DetectTry2
      Goto GetJRE
    
    DetectTry2:
      ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
      ;MessageBox MB_OK "Detect Read : $1"
      StrCmp $1 "" NoFound
      ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$1" "JavaHome"
      ;MessageBox MB_OK "Detect Read 3: $2"
      StrCmp $2 "" NoFound
    
    GetJRE:
    ; $0 = version requested. $1 = version found. $2 = javaHome
      ;MessageBox MB_OK "Getting JRE"
      IfFileExists "$2\bin\java.exe" 0 NoFound
      StrCpy $3 $0 1			; Get major version. Example: $1 = 1.5.0, now $3 = 1
      StrCpy $4 $1 1			; $3 = major version requested, $4 = major version found
      ;MessageBox MB_OK "Want $3 , found $4"
      IntCmp $4 $3 0 FoundOld FoundNew
      StrCpy $3 $0 1 2
      StrCpy $4 $1 1 2			; Same as above. $3 is minor version requested, $4 is minor version installed
      ;MessageBox MB_OK "Want $3 , found $4"
      IntCmp $4 $3 FoundNew FoundOld FoundNew
    
    NoFound:
      ;MessageBox MB_OK "JRE not found"
      Push "0"
      Goto DetectJREEnd
    
    FoundOld:
      MessageBox MB_OK "JRE too old: $3 is older than $4"
    ;  Push ${TEMP2}
      Push "-1"
      Goto DetectJREEnd
    FoundNew:
      ;MessageBox MB_OK "JRE is new: $3 is newer than $4"
    
      Push "$2"
    ;  Push "OK"
    ;  Return
       Goto DetectJREEnd
    DetectJREEnd:
        ; Top of stack is return value, then r4,r3,r2,r1
        Exch	; => r4,rv,r3,r2,r1,r0
        Pop $4	; => rv,r3,r2,r1r,r0
        Exch	; => r3,rv,r2,r1,r0
        Pop $3	; => rv,r2,r1,r0
        Exch 	; => r2,rv,r1,r0
        Pop $2	; => rv,r1,r0
        Exch	; => r1,rv,r0
        Pop $1	; => rv,r0
        Exch	; => r0,rv
        Pop $0	; => rv
    FunctionEnd
    
    #
    # </DetectJRE>
    #
    
    #
    # <ConvertUnixNewLines>
    #
    
    Function ConvertUnixNewLines
        ; Usage:
        ;Push "path\to\text_file.txt"
        ;Call ConvertUnixNewLines
    
        Exch $R0 ;file #1 path
        Push $R1 ;file #1 handle
        Push $R2 ;file #2 path
        Push $R3 ;file #2 handle
        Push $R4 ;data
        Push $R5
    
         FileOpen $R1 $R0 r
         GetTempFileName $R2
         FileOpen $R3 $R2 w
    
         loopRead:
          ClearErrors
          FileRead $R1 $R4
          IfErrors doneRead
    
           StrCpy $R5 $R4 1 -1
           StrCmp $R5 $\n 0 +4
           StrCpy $R5 $R4 1 -2
           StrCmp $R5 $\r +3
           StrCpy $R4 $R4 -1
           StrCpy $R4 "$R4$\r$\n"
    
          FileWrite $R3 $R4
    
         Goto loopRead
         doneRead:
    
         FileClose $R3
         FileClose $R1
    
         SetDetailsPrint none
         Delete $R0
         Rename $R2 $R0
         SetDetailsPrint both
    
        Pop $R5
        Pop $R4
        Pop $R3
        Pop $R2
        Pop $R1
        Pop $R0
    FunctionEnd
    
    #
    # </ConvertUnixNewLines>
    #

    #
    # <ReplaceInFile>
    #
    Function ReplaceInFile
        ;
        Exch $R0 ;file name to search in
        Exch
        Exch $R4 ;callback function handle
        Push $R1 ;file handle
        Push $R2 ;temp file name
        Push $R3 ;temp file handle
        Push $R5 ;line read
    
        GetTempFileName $R2
          FileOpen $R1 $R0 r ;file to search in
          FileOpen $R3 $R2 w ;temp file
    
    loop_read:
         ClearErrors
         FileRead $R1 $R5 ;read line
         Push $R5 ; put line on stack
         Call $R4
         Pop $R5 ; read line from stack
         IfErrors exit
         FileWrite $R3 $R5 ;write modified line
        Goto loop_read
    exit:
          FileClose $R1
          FileClose $R3
    
           SetDetailsPrint none
          Delete $R0
          Rename $R2 $R0
          Delete $R2
           SetDetailsPrint both
    
        ; pop in reverse order
        Pop $R5
        Pop $R3
        Pop $R2
        Pop $R1
        Pop $R4
        Pop $R0
    FunctionEnd
    
    #
    # </ReplaceInFile>
    #

    #
    # <ReplaceJavaHome>
    #
    
    # Replaces the '@java.home@' placeholder
    Function ReplaceJavaHome
	    Push $R1
	    Exch
	    
        Push "@java.home@" # String to find
        Push "$JAVA_HOME_DIR" # Replacement string
        Call StrReplace
    
        ; restore stack
        Exch
        Pop $R1
    FunctionEnd
    
    #
    # </ReplaceJavaHome>
    #

    #
    # <ReplaceInstallationDirectory>
    #
    
    # Replaces the '@installation.directory@' placeholder
    Function ReplaceInstallationDirectory
	    Push $R1
	    Exch
	    
        Push "@installation.directory@" # String to find
        Push "$SERVER_HOME_DIR" # Replacement string
        Call StrReplace
    
        ; restore stack
        Exch
        Pop $R1
    FunctionEnd
    
    #
    # </ReplaceInstallationDirectory>
    #

    #
    # <StrReplace>
    #
    
    Var STR_REPLACE_VAR_0
    Var STR_REPLACE_VAR_1
    Var STR_REPLACE_VAR_2
    Var STR_REPLACE_VAR_3
    Var STR_REPLACE_VAR_4
    Var STR_REPLACE_VAR_5
    Var STR_REPLACE_VAR_6
    Var STR_REPLACE_VAR_7
    Var STR_REPLACE_VAR_8
    
    Function StrReplace
      Exch $STR_REPLACE_VAR_2
      Exch 1
      Exch $STR_REPLACE_VAR_1
      Exch 2
      Exch $STR_REPLACE_VAR_0
        StrCpy $STR_REPLACE_VAR_3 -1
        StrLen $STR_REPLACE_VAR_4 $STR_REPLACE_VAR_1
        StrLen $STR_REPLACE_VAR_6 $STR_REPLACE_VAR_0
        loop:
          IntOp $STR_REPLACE_VAR_3 $STR_REPLACE_VAR_3 + 1
          StrCpy $STR_REPLACE_VAR_5 $STR_REPLACE_VAR_0 $STR_REPLACE_VAR_4 $STR_REPLACE_VAR_3
          StrCmp $STR_REPLACE_VAR_5 $STR_REPLACE_VAR_1 found
          StrCmp $STR_REPLACE_VAR_3 $STR_REPLACE_VAR_6 done
          Goto loop
        found:
          StrCpy $STR_REPLACE_VAR_5 $STR_REPLACE_VAR_0 $STR_REPLACE_VAR_3
          IntOp $STR_REPLACE_VAR_8 $STR_REPLACE_VAR_3 + $STR_REPLACE_VAR_4
          StrCpy $STR_REPLACE_VAR_7 $STR_REPLACE_VAR_0 "" $STR_REPLACE_VAR_8
          StrCpy $STR_REPLACE_VAR_0 $STR_REPLACE_VAR_5$STR_REPLACE_VAR_2$STR_REPLACE_VAR_7
          StrLen $STR_REPLACE_VAR_6 $STR_REPLACE_VAR_0
          Goto loop
        done:
      Pop $STR_REPLACE_VAR_1 ; Prevent "invalid opcode" errors and keep the
      Pop $STR_REPLACE_VAR_1 ; stack as it was before the function was called
      Exch $STR_REPLACE_VAR_0
    FunctionEnd
    
    !macro _strReplaceConstructor OUT NEEDLE NEEDLE2 HAYSTACK
      Push "${HAYSTACK}"
      Push "${NEEDLE}"
      Push "${NEEDLE2}"
      Call StrReplace
      Pop "${OUT}"
    !macroend
    
    !define StrReplace '!insertmacro "_strReplaceConstructor"'
    
    #
    # </StrReplace>
    #
