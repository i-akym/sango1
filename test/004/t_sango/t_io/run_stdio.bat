@echo off
setlocal
set TEST_HOME=%CD%\..\..
set TEST_ROOT=%TEST_HOME%\..
set TEST_SANGO=%TEST_ROOT%\inst\bin\sango.bat
echo Hi, Sango! | call %TEST_SANGO% -m ".;%TEST_HOME%" test_stdio
