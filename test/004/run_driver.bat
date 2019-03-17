@echo off
setlocal
set EXIT_CODE=0
set TEST_HOME=%CD%
set TEST_ROOT=%CD%\..
set TEST_INST=%TEST_ROOT%\inst
set TEST_LIB=%TEST_HOME%
set TEST_BIN=%TEST_INST%\bin
set TEST_SANGOC=%TEST_BIN%\sangoc.bat
set TEST_SANGO=%TEST_BIN%\sango.bat

set D=%~dp1
set N=%~n1
set B=%N:test_=%

cd %D%
echo Running %1 ...

call %TEST_SANGOC% -m ".;%TEST_LIB%" test_%B%.sg > result\%B%.txt 2>&1
if errorlevel 1 goto err01
if exist run_%B%.bat (
  call .\run_%B%.bat >> result\%B%.txt 2>&1
) else (
  call %TEST_SANGO% -m ".;%TEST_LIB%" test_%B% >> result\%B%.txt 2>&1
)
if errorlevel 1 goto err01
goto ret

:err01
echo ** ERROR.
set EXIT_CODE=1

:ret
cd %TEST_HOME%
exit /b %EXIT_CODE%

