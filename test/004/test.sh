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

# Perform test.
# Usage:
#   (enable execution if needed)
#   ./test.sh -all
#   ./test.sh -children <module_name_prefix>  ex) sango.util
#   ./test.sh -under <module_name_prefix>
#   ./test.sh <module_name>  ex) sango.util.map

test_all() {
  test_under "sango" || test_error
}

test_children() {
  find `$TEST_HOME/module-prefix-to-dir.sh $1` -maxdepth 1 -name 'test_*.sg' | \
    sed -e 's!^!/run-driver.sh !' -e "s!^!$TEST_HOME!" -e 's!$! || exit 1!' | sh || test_error
}

test_under() {
  find `$TEST_HOME/module-prefix-to-dir.sh $1` -name 'test_*.sg' | \
    sed -e 's!^!/run-driver.sh !' -e "s!^!$TEST_HOME!" -e 's!$! || exit 1!' | sh || test_error
}

test_target() {
  $TEST_HOME/run-driver.sh `$TEST_HOME/module-to-driver.sh $1` || test_error
}

print_usage() {
  echo "Usage:"
  echo "  ./test.sh -all"
  echo "  ./test.sh -children <module_name_prefix>  ex) sango.util"
  echo "  ./test.sh -under <module_name_prefix>"
  echo "  ./test.sh <module_name>  ex) sango.util.map"
  exit 1
}

test_error() {
  echo "** ERROR."
  cd $TEST_HOME
  exit 1
}

# -- start --

test "$1" != "" || print_usage

TEST_HOME=`pwd`
TEST_ROOT=$TEST_HOME/..
TEST_SANGOC=$TEST_HOME/../inst/bin/sangoc
TEST_SANGO=$TEST_HOME/../inst/bin/sango

if [ "$1" = "-all" ]; then
  test_all
elif [ "$1" = "-children" ]; then
  test_children $2
elif [ "$1" = "-under" ]; then
  test_under $2
elif [ "$1" != "" ]; then
  test_target $1
fi

cd $TEST_HOME
exit 0

