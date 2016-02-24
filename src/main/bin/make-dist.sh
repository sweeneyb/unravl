#!/usr/bin/env bash

# Build a distribution zip and javadoc zip
#
# OK, this should all be possible and easy in Gradle but the distZip
# target is not as easy as I'd like. So I do this with a bash script instead.
# Run this from the root source directory: ./src/main/bin/make-dist.sh

VERSION=`grep '^version' build.gradle | sed -e 's/^.*= *//' -e "s/'//g"`

./gradlew build copyDeps javadoc -x test || exit $? # Skip tests; I should have run tests before this...

if [[ ! -f build/libs/sas.unravl-$VERSION.jar ]]
then echo "build/libs/sas.unravl-$VERSION.jar does not exist. Is version $VERSION correct in $0 ?"
     exit 1
fi

SRC=$PWD
DIST=$SRC/build/dist

if [[ -d $DIST ]]; then rm -rf $DIST; fi

mkdir -p $DIST/bin

cp src/main/bin/unravl.sh $DIST/bin/unravl.sh  || exit $?
chmod +x $DIST/bin/unravl.sh  || exit $?

cp src/main/bin/unravl.bat $DIST/bin/unravl.bat  || exit $?

mkdir -p $DIST/lib
cp build/libs/sas.unravl-$VERSION.jar $DIST/lib || exit $?
cp build/output/lib/*.jar $DIST/lib  || exit $?

cp -rf README.md LICENSE CONTRIBUTORS.md ContributorAgreement.txt doc $DIST  || exit $?
cp $SRC/src/test/scripts/hello.json $DIST  || exit $?
cd $DIST

find . -type f -name .DS_Store -exec rm \{\} \;
zip -r $DIST/unravl-$VERSION.zip README.md LICENSE bin lib doc hello.json || exit $?

cd $SRC/build/docs/javadoc
zip -r $DIST/unravl-$VERSION-javadoc.zip *  || exit $?

echo
echo $0 complete.
echo distribution zip is ~/dev/unravl/build/dist/unravl-$VERSION.zip
echo javadoc is ~/dev/unravl/build/dist/unravl-$VERSION-javadoc.zip
