@echo off

setlocal
set RET=0
set TEST_ROOT=%CD%

rmdir /s /q release
if errorlevel 1 goto err02

rmdir /s /q inst
if errorlevel 1 goto err02

cd ..

call .\make_release.bat %TEST_ROOT%
if errorlevel 1 goto err02

cd %TEST_ROOT%

cd release

call .\win_configure -install-to %TEST_ROOT%\inst
if errorlevel 1 goto err04

call .\install
if errorlevel 1 goto err04

goto ret

:err02
echo ** Failed to make release.
set RET=1
goto ret

:err04
echo ** Failed to install.
set RET=1
goto ret

:ret
cd %TEST_ROOT%
exit /b %RET%
