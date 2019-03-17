@echo off
rem ###########################################################################
rem # MIT License                                                             #
rem # Copyright (c) 2018 Isao Akiyama                                         #
rem #                                                                         #
rem # Permission is hereby granted, free of charge, to any person obtaining   #
rem # a copy of this software and associated documentation files (the         #
rem # "Software"), to deal in the Software without restriction, including     #
rem # without limitation the rights to use, copy, modify, merge, publish,     #
rem # distribute, sublicense, and/or sell copies of the Software, and to      #
rem # permit persons to whom the Software is furnished to do so, subject to   #
rem # the following conditions:                                               #
rem #                                                                         #
rem # The above copyright notice and this permission notice shall be          #
rem # included in all copies or substantial portions of the Software.         #
rem #                                                                         #
rem # THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,         #
rem # EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF      #
rem # MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  #
rem # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY    #
rem # CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,    #
rem # TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE       #
rem # SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                  #
rem ###########################################################################

rem Perform test.
rem Usage:
rem   .\test.bat -all
rem   .\test.bat -children <module_name_prefix>  ex) sango.util
rem   .\test.bat -under <module_name_prefix>
rem   .\test.bat <module_name>  ex) sango.util.map

setlocal enabledelayedexpansion
set TEST_HOME=%CD%

if "%1"=="" goto print_usage

if "%1"=="-all" (
  call :test_all
) else if "%1"=="-children" (
  call :test_children %2
) else if "%1"=="-under" (
  call :test_under %2
) else if not "%1"=="" (
  call :test_target %1
)
if errorlevel 1 goto test_error
cd %TEST_HOME%
goto ret

:test_all
call :test_under "sango"
exit /b

:test_children
set M=%1
set D=t_%M:.=\t_%
for %%F in (%D%\test_*.sg) do (
  call %TEST_HOME%\run_driver.bat %%F
  if errorlevel 1 exit /b 1
)
exit /b

:test_under
set M=%1
set D=t_%M:.=\t_%
for /r %D% %%F in (test_*.sg) do (
  call %TEST_HOME%\run_driver.bat %%F
  if errorlevel 1 exit /b 1
)
exit /b 0

:test_target
set _T=%1
set _SS=%TEST_HOME%
set _S=
:convert
if "%_T%"=="" (
  set _SS=%_SS%\test_%_S%.sg
  goto convert_end
) else if "%_T:~0,1%"=="." (
  set _SS=%_SS%\t_%_S%
  set _S=
  set _T=%_T:~1%
  goto convert
) else (
  set _S=%_S%%_T:~0,1%
  set _T=%_T:~1%
  goto convert
)
:convert_end
call %TEST_HOME%\run_driver.bat %_SS%
exit /b

:print_usage
echo.Usage:
echo.  .\test.bat -all
echo.  .\test.bat -children module_name_prefix  ex) sango.util
echo.  .\test.bat -under module_name_prefix
echo.  .\test.bat module_name  ex) sango.util.map
exit /b 1

:test_error
cd %TEST_HOME%
exit /b 1

:ret
exit /b 0
