@echo off
setlocal
set TEST_HOME=%CD%\..
set TEST_ROOT=%TEST_HOME%\..
set TEST_SANGO=%TEST_ROOT%\inst\bin\sango.bat
call %TEST_SANGO% -m ".;%TEST_HOME%" test_io . data
