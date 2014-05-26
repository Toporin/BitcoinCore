BitcoinCore
===========

BitcoinCore is a library of support routines for the bitcoin protocol.  It handles elliptic curve signature operations and message encoding/decoding.


Build
=====

I use the Netbeans IDE but any build environment with Maven and the Java compiler available should work.  The documentation is generated from the source code using javadoc.

Here are the steps for a manual build.  You will need to install Maven 3 and Java SE Development Kit 7 if you don't already have them.

  - Create the executable: mvn clean install
  - [Optional] Create the documentation: mvn javadoc:javadoc
  - [Optional] Copy target/BitcoinCore-v.r.jar to wherever you want to store the library.

  
Usage Notes
===========

BitcoinCore provides a combined jar (BitcoinCore-n.n.jar) containing BitcoinCore and BouncyCastle and the original jar (original-BitcoinCore-n.n.jar) containing just BitcoinCore.  If you don't need to use any of the BouncyCastle routines in your application, you should use the combined jar since that bundles everything together into a single file.  However, if you need BouncyCastle routines that are not included in the combined jar or you need a later version, you should use the original jar containing just BitcoinCore and provide the BouncyCastle routines in a separate jar (the minimum BouncyCastle version is listed in the BitcoinCore POM).

You must call NetParams.configure() before you call any other BitcoinCore routine.  This initializes the library data areas for the specified network (production or test).
