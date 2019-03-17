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

not_setup() {
  echo "Not configured. Install directory missing."
  exit 1
}

target_already_exists_error() {
  echo "Install directory $INSTALL_DIR already exists. Delete it in advance."
  exit 1
}

copy_error() {
  echo "** ERROR: Failed to copy files."
  exit 1
}

# -- start --

INSTALL_DIR=_INSTALL_DIR_

test "$INSTALL_DIR" != "" || not_setup

REPLACE_LIB=s:LLIIBB:$INSTALL_DIR/lib:g

test ! -e $INSTALL_DIR || target_already_exists_error

echo "mkdir $INSTALL_DIR"
mkdir $INSTALL_DIR || copy_error

echo "cp ./*.txt $INSTALL_DIR"
cp ./*.txt $INSTALL_DIR || copy_error

echo "cp -R ./src $INSTALL_DIR/src"
cp -R ./src $INSTALL_DIR/src || copy_error

echo "cp -R ./lib $INSTALL_DIR/lib"
cp -R ./lib $INSTALL_DIR/lib || copy_error
echo "mkdir $INSTALL_DIR/lib/etc"
mkdir $INSTALL_DIR/lib/etc || copy_error
echo "cp ./system.props $INSTALL_DIR/lib/etc/"
cp ./system.props $INSTALL_DIR/lib/etc/ || copy_error

echo "mkdir $INSTALL_DIR/bin"
mkdir $INSTALL_DIR/bin || copy_error

echo "cat ./bin/unix/sangoc | sed -e "$REPLACE_LIB" > $INSTALL_DIR/bin/sangoc"
cat ./bin/unix/sangoc | sed -e "$REPLACE_LIB" > $INSTALL_DIR/bin/sangoc || copy_error

echo "cat ./bin/unix/sango | sed -e "$REPLACE_LIB" > $INSTALL_DIR/bin/sango"
cat ./bin/unix/sango | sed -e "$REPLACE_LIB" > $INSTALL_DIR/bin/sango || copy_error

echo "cp -R ./doc $INSTALL_DIR/doc"
cp -R ./doc $INSTALL_DIR/doc || copy_error

echo "cp -R ./sample $INSTALL_DIR/sample"
cp -R ./sample $INSTALL_DIR/sample || copy_error

echo "cp -R ./etc $INSTALL_DIR/etc"
cp -R ./etc $INSTALL_DIR/etc || copy_error

echo "find $INSTALL_DIR -type d | xargs chmod 755"
find $INSTALL_DIR -type d | xargs chmod 755 || copy_error

echo "find $INSTALL_DIR -type f | xargs chmod 644"
find $INSTALL_DIR -type f | xargs chmod 644 || copy_error

echo "chmod a+x $INSTALL_DIR/bin/*"
chmod a+x $INSTALL_DIR/bin/* || copy_error

exit 0
