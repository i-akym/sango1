@echo off
setlocal enabledelayedexpansion

if "%1"=="" (
  call :print_usage
  goto ret
)

if defined SANGO_JAVA_BIN (
  set JAVAC=%SANGO_JAVA_BIN%\javac -Xlint:unchecked
  set JAVA=%SANGO_JAVA_BIN%\java
) else (
  set JAVAC=javac -Xlint:unchecked
  set JAVA=java
)
echo -- JAVA platform information  --
echo SANGO_JAVA_BIN=%SANGO_JAVA_BIN%
%JAVAC% -version
%JAVA% -version
echo.

set SANGOC=%JAVA% -cp src org.sango_lang.Compiler

if "%1"=="-all" (
  call :compile_all
) else if "%1"=="-sys" (
  call :compile_sys
) else if "%1"=="-lib" (
  call :compile_lib
) else if "%1"=="-sample" (
  call :compile_sample
) else if "%1"=="-misc" (
  call :compile_misc
) else (
  call :compile_targets %*
)
goto exit

rem -- subroutines --

:compile_all
call :compile_sys
if errorlevel 1 exit /b 1
call :compile_lib
if errorlevel 1 exit /b 1
call :compile_sample
if errorlevel 1 exit /b 1
call :compile_misc
if errorlevel 1 exit /b 1
exit /b 0

:compile_sys
echo Compiling language system programs...
for /r src\org\sango_lang %%F in (*.class) do (
  del /q %%F
)
%JAVAC% -cp src src\org\sango_lang\Compiler.java src\org\sango_lang\RuntimeEngine.java
if errorlevel 1 exit /b 1
exit /b 0

:compile_lib
echo Compiling library programs...
for /r src %%F in (*.sgm) do (
  del /q %%F
)
for /r src\sango %%F in (*.sg) do (
  if not exist %%~dpF%%~nF.sgm (
    %SANGOC% -m "src" -quiet all %%F
    if errorlevel 1 exit /b 1
  )
)

for /r src\sni_sango %%F in (*.class) do (
  del /q %%F
)
%JAVAC% -cp src src\sni_sango\*.java
if errorlevel 1 exit /b 1
%JAVAC% -cp src src\sni_sango\sni_arith\*.java
if errorlevel 1 exit /b 1
%JAVAC% -cp src src\sni_sango\sni_char\sni_encoding\*.java
if errorlevel 1 exit /b 1
%JAVAC% -cp src src\sni_sango\sni_io\*.java
if errorlevel 1 exit /b 1
%JAVAC% -cp src src\sni_sango\sni_io\sni_filesys\*.java
if errorlevel 1 exit /b 1
%JAVAC% -cp src src\sni_sango\sni_lang\*.java
if errorlevel 1 exit /b 1
%JAVAC% -cp src src\sni_sango\sni_net\*.java
if errorlevel 1 exit /b 1
%JAVAC% -cp src src\sni_sango\sni_num\*.java
if errorlevel 1 exit /b 1
%JAVAC% -cp src src\sni_sango\sni_system\*.java
if errorlevel 1 exit /b 1
%JAVAC% -cp src src\sni_sango\sni_util\*.java
if errorlevel 1 exit /b 1
exit /b 0

:compile_sample
echo Compiling sample programs...
for /r src\sample %%F in (*.sgm) do (
  del /q %%F
)
for /r src\sample %%F in (*.sg) do (
  if not exist %%~dpF%%~nF.sgm (
    %SANGOC% -L "src" -m "src\sample" -quiet all %%F
    if errorlevel 1 exit /b 1
  )
)
exit /b 0

:compile_misc
echo Compiling miscellaneous programs...
for /r tool %%F in (*.class) do (
  del /q %%F
)
%JAVAC% -cp src tool\ConfigTool.java
if errorlevel 1 exit /b 1
exit /b 0

:compile_targets
set LL=%*
set NN=
:_target
if not "%1"=="" (
  call :set_native_impl %1
  if errorlevel 1 exit /b 1
  if not "!N!"=="" (
    set NN=%NN% !N!
  )
  shift
  goto _target
)
:_compile_target_end
%SANGOC% -m "src" -quiet all %LL%
if errorlevel 1 exit /b 1
if not "%NN%"=="" (
  %JAVAC% -cp src %NN%
  if errorlevel 1 exit /b 1
)
exit /b 0

:set_native_impl
set _T=%1
if not "%_T:~0,4%"=="src\" (
  echo Invalid target. - %1
  exit /b 1
)
set _TT=%_T:~4%
set _SS=
set _S=
:convert
if "%_TT%"=="" (
  set _SS=%_SS%\SNI%_S:.sg=.java%
  goto convert_end
) else if "%_TT:~0,1%"=="\" (
  set _SS=%_SS%\sni_%_S%
  set _S=
  set _TT=%_TT:~1%
  goto convert
) else (
  set _S=%_S%%_TT:~0,1%
  set _TT=%_TT:~1%
  goto convert
)
:convert_end
set _NI=src%_SS%
if exist %_NI% (
  set N=%_NI%
) else (
  set N=
)
exit /b

:print_usage
echo.Usage:
echo.  .\build.bat -all  -- all programs
echo.  .\build.bat -sys  -- compiler and runtime engine
echo.  .\build.bat -lib  -- all library programs
echo.  .\build.bat -sample  -- all sample programs
echo.  .\build.bat -misc  -- miscellaneous programs
echo.  .\build.bat src\sango\util\map.sg ...  -- library programs
exit /b 1

:exit
if errorlevel 1 (
  echo ** ERROR.
)

:ret
exit /b
