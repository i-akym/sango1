#!/bin/sh
###########################################################################
# MIT License                                                             #
# Copyright (c) 2018 Isao Akiyama                                         #
#                                                                         #
# Permission is hereby granted, free of charge, to any person obtaining   #
# a copy of this software and associated documentation files (the         #
# "Software"), to deal in the Software without restriction, including     #
# without limitation the rights to use, copy, modify, merge, publish,     #
# distribute, sublicense, and/or sell copies of the Software, and to      #
# permit persons to whom the Software is furnished to do so, subject to   #
# the following conditions:                                               #
#                                                                         #
# The above copyright notice and this permission notice shall be          #
# included in all copies or substantial portions of the Software.         #
#                                                                         #
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,         #
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF      #
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  #
# IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY    #
# CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,    #
# TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE       #
# SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                  #
###########################################################################

# Build Sango system.
# Usage:
#   (enable execution if needed)
#   ./build.sh -all  -> all programs
#   ./build.sh -sys  -> compiler and runtime engine
#   ./build.sh -lib  -> all library programs
#   ./build.sh -misc  -> miscellaneous programs, eg. installation tool
#   ./build.sh src/sango/util/map.sg ...  -> library programs

compile_all() {
  compile_sys
  compile_lib
  compile_sample
  compile_misc
}

compile_sys() {
  echo "Compiling compiler and runtime engine..."
  find src/org/sango_lang -name '*.class' -delete || build_error
  $JAVAC -cp src \
    src/org/sango_lang/Compiler.java \
    src/org/sango_lang/RuntimeEngine.java || build_error
}

compile_lib() {
  echo "Compiling library programs..."
  find src/sango -name '*.sgm' -delete || build_error
  find src/sango -name '*.sg' | xargs $SANGOC -m src -quiet all || build_error
  find src/sni_sango -name '*.class' -delete || build_error
  find src/sni_sango -name '*.java' | xargs $JAVAC -cp src || build_error
}

compile_sample() {
  echo "Compiling sample programs..."
  find src/sample -name '*.sgm' -delete || build_error
  find src/sample -name '*.sg' | xargs $SANGOC -L src -m src/sample -quiet all || build_error
}

compile_misc() {
  echo "Compiling miscellaneous programs..."
  find tool -name '*.class' -delete || build_error
  $JAVAC -cp tool:src tool/*.java || build_error
}

compile_targets() {
  $SANGOC -m src -quiet all $* || build_error
  IMPLS=`tool/nativeimpl.sh $*`
  if [ "$IMPLS" != "" ]; then
    $JAVAC -cp src $IMPLS || build_error
  fi
}

print_usage() {
  echo "Usage:"
  echo "  ./build.sh -all  -- all programs"
  echo "  ./build.sh -sys  -- compiler and runtime engine"
  echo "  ./build.sh -lib  -- all library programs"
  echo "  ./build.sh -sample  -- all sample programs"
  echo "  ./build.sh -misc  -- miscellaneous programs"
  echo "  ./build.sh src/sango/util/map.sg ...  -- library programs"
  exit 1
}

build_error() {
  echo "** ERROR."
  exit 1
}

# -- start --

test "$1" != "" || print_usage

if [ "$SANGO_JAVA_BIN" != "" ]; then
  JAVAC="$SANGO_JAVA_BIN/javac -Xlint:unchecked"
  JAVA=$SANGO_JAVA_BIN/java
else
  JAVAC="javac -Xlint:unchecked"
  JAVA=java
fi
echo "-- JAVA platform information  --"
echo "SANGO_JAVA_BIN=$SANGO_JAVA_BIN"
$JAVAC -version
$JAVA -version
echo

SANGOC="$JAVA -cp src org.sango_lang.Compiler"

if [ $1 = "-all" ]; then
  compile_all
elif [ $1 = "-sys" ]; then
  compile_sys
elif [ $1 = "-lib" ]; then
  compile_lib
elif [ $1 = "-sample" ]; then
  compile_sample
elif [ $1 = "-misc" ]; then
  compile_misc
else
  compile_targets $*
fi

exit 0

