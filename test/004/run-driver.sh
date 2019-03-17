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

error_end() {
  echo Error in $1.
  cd $TEST_HOME
  exit 1
}

# -- start --

TEST_HOME=`pwd`
TEST_ROOT=$TEST_HOME/..
TEST_SANGOC=$TEST_HOME/../inst/bin/sangoc
TEST_SANGO=$TEST_HOME/../inst/bin/sango

D=`dirname $1`
B=`basename $1 | sed -e 's/^test_//' -e 's/\.sg$//'`
cd $D
echo "Running $1 ..."
# echo "$TEST_SANGOC -m .:$TEST_HOME test_$B.sg > result/$B.txt 2>&1"
$TEST_SANGOC -m .:$TEST_HOME test_$B.sg > result/$B.txt 2>&1 || error_end $1
if [ -e "run_$B.sh" ]; then
  # echo "./run_$B.sh >> result/$B.txt 2>&1"
  ./run_$B.sh >> result/$B.txt 2>&1 || error_end $1
else
  # echo "$TEST_SANGO -m .:$TEST_HOME test_$B >> result/$B.txt 2>&1"
  $TEST_SANGO -m .:$TEST_HOME test_$B >> result/$B.txt 2>&1 || error_end $1
fi

cd $TEST_HOME
exit 0

