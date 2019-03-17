#!/bin/sh
# Usage: nativeimpl.sh src/sango/foo/bar/hoge.sg ...  => src/sni_sango/sni_foo/sni_bar/SNIhoge.java ...
bad_arg() {
  echo "Argument must start with 'src/'."
  exit 1
}
IMPLS=""
SEP=" "
while [ "$1" != "" ] ; do
  test `echo $1 | cut -c1-4` = "src/" || bad_arg
  DIR=`dirname $1 | sed -e 's!src/!sni_!' -e 's!/!/sni_!g'`
  BASE=SNI`basename $1 | sed -e 's!.sg$!.java!'`
  IMPL="src/$DIR/$BASE"
  if [ -e $IMPL ]; then
    IMPLS="${IMPLS}${SEP}${IMPL}"
    SEP=" "
  fi
  shift 1
done
echo $IMPLS
