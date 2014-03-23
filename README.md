BitcoinCore
===========

BitcoinCore is a library of support routines for the bitcoin protocol.  It handles elliptic curve signature operations and message encoding/decoding.

A compiled version is available here: https://drive.google.com/folderview?id=0B1312_6UqRHPYjUtbU1hdW9VMW8&usp=sharing.  Download the desired archive file and extract the files to a directory of your choice.  If you are building from the source, the dependent jar files can also be obtained here.


Build
=====

I use the Netbeans IDE but any build environment with Maven and the Java compiler available should work.  The documentation is generated from the source code using javadoc.

Here are the steps for a manual build.  You will need to install Maven 3 and Java SE Development Kit 7 if you don't already have them.

  - Create the executable: mvn clean install
  - [Optional] Create the documentation: mvn javadoc:javadoc
  - [Optional] Copy target/BitcoinCore-v.r.jar to wherever you want to store the library.
