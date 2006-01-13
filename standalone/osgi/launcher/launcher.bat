@echo off
rem find VTMP_HOME
if "%VTMP_HOME%"=="" goto noHome

set VTMPPROFILE=%1
if "%VTMPPROFILE%"=="" set VTMPPROFILE=vtmp

"%JAVA_HOME%\bin\java" -Xmx400m -Dvtmp.home="%VTMP_HOME%" -jar "%VTMP_HOME%\lib\launch.jar" vtmp %VTMPPROFILE%

goto :end

:noHome
echo VTMP_HOME is not set. Please set VTMP_HOME.
goto end

:end

