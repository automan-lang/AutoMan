## START INFORMATION FOR HUMANS
# This Makefile does the following things:
# 1. Downloads AutoMan dependencies, including the Scala compiler.
# 2. Unpacks dependencies so that they may be linked against AutoMan code.
# 3. Makes an AutoMan JAR.
# 4. Runs One-JAR to create an AutoMan überJAR (AutoMan + deps in one JAR).
## END INFORMATION FOR HUMANS

## BINARY UTILITY LOCATIONS
TAR		:= $(shell which tar)
CURL	:= $(shell which curl)
WGET	:= $(shell which wget)
ANT		:= $(shell which ant)
JAR		:= $(shell which jar)
JAVAC := $(shell which javac)

## ARCHIVE NAMES (MODIFY HERE FOR NEW VERSIONS)
ARCH_MTURKSDK 	:= java-aws-mturk-1.6.2.tar.gz
ARCH_ACTIVEOBJ 	:= activeobjects-0.8.2.tar.gz
ARCH_ONEJAR			:= one-jar-boot-0.97.jar
ARCH_SCALA			:= scala-2.10.3.tgz

## UNPACK DIR NAMES (DO NOT MODIFY)
DIR_MTURKSDK 	:= $(patsubst %.tar.gz,%,$(ARCH_MTURKSDK))
DIR_ACTIVEOBJ := $(patsubst %.tar.gz,%,$(ARCH_ACTIVEOBJ))
DIR_SCALA			:= $(patsubst %.tgz,%,$(ARCH_SCALA))

## VERSION STRINGS (DO NOT MODIFY)
VER_MTURKSDK 	:= $(patsubst java-aws-mturk-%.tar.gz,%,$(ARCH_MTURKSDK))
VER_ACTIVEOBJ := $(patsubst activeobjects-%.tar.gz,%,$(ARCH_ACTIVEOBJ))
VER_ONEJAR		:= $(patsubst one-jar-boot-%.jar,%,$(ARCH_ONEJAR))
VER_SCALA			:= $(patsubst scala-%.tgz,%,$(ARCH_SCALA))

## JARS (DO NOT MODIFY)
JAR_ACTIVEOBJ	:= activeobjects-$(VER_ACTIVEOBJ).jar
JAR_ONEJAR	:= $(ARCH_ONEJAR)

## LIBRARY URLS (DO NOT MODIFY)
MTURKSDK 	:= http://downloads.sourceforge.net/project/mturksdk-java/mturksdk-java/$(VER_MTURKSDK)/$(ARCH_MTURKSDK)
ACTIVEOBJ := http://java.net/projects/activeobjects/downloads/download/$(VER_ACTIVEOBJ)/$(ARCH_ACTIVEOBJ)
ONEJAR 		:= https://sourceforge.net/projects/one-jar/files/one-jar/one-jar-$(VER_ONEJAR)/$(ARCH_ONEJAR)
SCALA			:= http://www.scala-lang.org/files/archive/$(ARCH_SCALA)

## STATIC VARS
OUTJARS := jars
TEMPDIR := temp_output
UNPACKDIR := $(TEMPDIR)/libs
TURKDIR := $(UNPACKDIR)/$(DIR_MTURKSDK)
AODIR := $(UNPACKDIR)/$(DIR_ACTIVEOBJ)
SCALADIR := $(UNPACKDIR)/$(DIR_SCALA)
JARDIR := $(TEMPDIR)/jars
DOWNLOADDIR := $(TEMPDIR)/downloads
CLASSDIR := $(TEMPDIR)/classes
APPCLASSES := $(TEMPDIR)/apps/classes
AUTOMAN_JAVA_SRC := $(shell find lib -iname "*.java" -type f | tr '\n' ' ')
AUTOMAN_SCALA_SRC := $(shell find lib -type f -iname "*.scala" | tr '\n' ' ')

## FUNCTIONS
define DOWNLOAD
ifeq ($(CURL),)
$(shell $(WGET) -O $(2) $(1))
else
$(shell $(CURL) -L -o $(2) $(1))
endif
endef
CLASSPATH = $(shell find $(JARDIR) -iname "*.jar" -type f | tr '\n' ':')
SCALAC = $(SCALADIR)/bin/scalac

## BUILD TARGETS
all: scalacheck jarcheck $(OUTJARS)/automan.jar $(OUTJARS)/simple_program.jar

$(OUTJARS)/automan.jar: $(JARDIR)/java-aws-mturk.jar \
	$(JARDIR)/aws-mturk-wsdl.jar \
	$(JARDIR)/aws-mturk-dataschema.jar \
	$(JARDIR)/log4j-1.2.15.jar \
	$(JARDIR)/axis-ant.jar \
	$(JARDIR)/axis.jar \
	$(JARDIR)/commons-discovery-0.2.jar \
	$(JARDIR)/jaxrpc.jar \
	$(JARDIR)/saaj.jar \
	$(JARDIR)/commons-beanutils.jar \
	$(JARDIR)/commons-collections-3.2.jar \
	$(JARDIR)/commons-collections-testframework-3.2.jar \
	$(JARDIR)/commons-dbcp-1.2.2.jar \
	$(JARDIR)/commons-digester-1.8.jar \
	$(JARDIR)/commons-httpclient-3.1.jar \
	$(JARDIR)/commons-httpclient-contrib-3.1.jar \
	$(JARDIR)/commons-lang-2.3.jar \
	$(JARDIR)/commons-logging-api.jar \
	$(JARDIR)/commons-logging.jar \
	$(JARDIR)/commons-pool-1.3.jar \
	$(JARDIR)/dom4j-1.6.1.jar \
	$(JARDIR)/geronimo-activation_1.0.2_spec-1.2.jar \
	$(JARDIR)/geronimo-javamail_1.3.1_spec-1.3.jar \
	$(JARDIR)/commons-codec-1.4.jar \
	$(JARDIR)/commons-logging-1.1.1.jar \
	$(JARDIR)/httpclient-4.1.2.jar \
	$(JARDIR)/httpclient-cache-4.1.2.jar \
	$(JARDIR)/httpcore-4.1.2.jar \
	$(JARDIR)/httpmime-4.1.2.jar \
	$(JARDIR)/opencsv-1.8.jar \
	$(JARDIR)/velocity-1.5.jar \
	$(JARDIR)/velocity-tools-1.4.jar \
	$(JARDIR)/jaxme2-0.5.2.jar \
	$(JARDIR)/jaxme2-rt-0.5.2.jar \
	$(JARDIR)/jaxmeapi-0.5.2.jar \
	$(JARDIR)/jaxmejs-0.5.2.jar \
	$(JARDIR)/jaxmepm-0.5.2.jar \
	$(JARDIR)/jaxmexs-0.5.2.jar \
	$(JARDIR)/wsdl4j.jar \
	$(JARDIR)/wstx-asl-3.2.3.jar \
	$(JARDIR)/xalan.jar \
	$(JARDIR)/resolver.jar \
	$(JARDIR)/xercesImpl.jar \
	$(JARDIR)/xml-apis.jar \
	$(JARDIR)/$(JAR_ACTIVEOBJ) \
	$(JARDIR)/$(JAR_ONEJAR) \
	$(JARDIR)/$(JAR_SCALA) \
	$(JARDIR)/diffutils.jar \
	$(JARDIR)/jline.jar \
	$(JARDIR)/scala-actors-migration.jar \
	$(JARDIR)/scala-actors.jar \
	$(JARDIR)/scala-compiler.jar \
	$(JARDIR)/scala-library.jar \
	$(JARDIR)/scala-partest.jar \
	$(JARDIR)/scala-reflect.jar \
	$(JARDIR)/scala-swing.jar \
	$(JARDIR)/scalap.jar \
	$(JARDIR)/typesafe-config.jar \
	$(CLASSDIR) \
	$(OUTJARS)
	$(SCALAC) -classpath $(CLASSPATH) -d $(CLASSDIR) $(AUTOMAN_SCALA_SRC) $(AUTOMAN_JAVA_SRC)
	cd $(CLASSDIR); $(JAR) cvf ../../$(OUTJARS)/automan.jar edu
	cd $(OUTJARS); $(JAR) i automan.jar

$(OUTJARS)/simple_program.jar: $(APPCLASSES)/simple_program \
	$(OUTJARS)/automan.jar \
	$(JARDIR)/diffutils.jar \
	$(JARDIR)/jline.jar \
	$(JARDIR)/scala-actors-migration.jar \
	$(JARDIR)/scala-actors.jar \
	$(JARDIR)/scala-compiler.jar \
	$(JARDIR)/scala-library.jar \
	$(JARDIR)/scala-partest.jar \
	$(JARDIR)/scala-reflect.jar \
	$(JARDIR)/scala-swing.jar \
	$(JARDIR)/scalap.jar \
	$(JARDIR)/typesafe-config.jar
	$(SCALAC) -classpath $(OUTJARS)/automan.jar:$(CLASSPATH) -d $(APPCLASSES)/simple_program apps/simple_program/src/main/scala/simple_program.scala
	cd $(APPCLASSES)/simple_program; $(JAR) cvfe ../../../../$(OUTJARS)/simple_program.jar simple_program/main *

# antcheck:
# ifeq ($(ANT),)
# $(error Must have Apache Ant)
# endif
# 
jarcheck:
ifeq ($(JAR),)
$(error Must have the JAR utility installed)
endif

scalacheck:
ifeq ($(SCALAC),)
$(error Must have the Scala compiler installed)
endif

clean:
	@-rm -rf $(TEMPDIR) $(OUTJARS)

## MTURK SDK
$(JARDIR)/java-aws-mturk.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/java-aws-mturk.jar $(JARDIR)/

$(JARDIR)/aws-mturk-wsdl.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/aws-mturk-wsdl.jar $(JARDIR)/

$(JARDIR)/aws-mturk-dataschema.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/aws-mturk-dataschema.jar $(JARDIR)/

$(JARDIR)/log4j-1.2.15.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/apache-log4j-1.2.15/log4j-1.2.15.jar $(JARDIR)/

$(JARDIR)/axis-ant.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/axis-1.4/axis-ant.jar $(JARDIR)/

$(JARDIR)/axis.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/axis-1.4/axis.jar $(JARDIR)/

$(JARDIR)/commons-discovery-0.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/axis-1.4/commons-discovery-0.2.jar $(JARDIR)/

$(JARDIR)/jaxrpc.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/axis-1.4/jaxrpc.jar $(JARDIR)/

$(JARDIR)/saaj.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/axis-1.4/saaj.jar $(JARDIR)/

$(JARDIR)/commons-beanutils.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-beanutils-1.7.0/commons-beanutils.jar $(JARDIR)/

$(JARDIR)/commons-collections-3.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-collections-3.2/commons-collections-3.2.jar $(JARDIR)/

$(JARDIR)/commons-collections-testframework-3.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-collections-3.2/commons-collections-testframework-3.2.jar $(JARDIR)/

$(JARDIR)/commons-dbcp-1.2.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-dbcp-1.2.2/commons-dbcp-1.2.2.jar $(JARDIR)/

$(JARDIR)/commons-digester-1.8.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-digester-1.8/commons-digester-1.8.jar $(JARDIR)/

$(JARDIR)/commons-httpclient-3.1.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-httpclient-3.1/commons-httpclient-3.1.jar $(JARDIR)/

$(JARDIR)/commons-httpclient-contrib-3.1.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-httpclient-3.1/commons-httpclient-contrib-3.1.jar $(JARDIR)/

$(JARDIR)/commons-lang-2.3.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-lang-2.3/commons-lang-2.3.jar $(JARDIR)/

$(JARDIR)/commons-logging-api.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-logging-1.0.4/commons-logging-api.jar $(JARDIR)/

$(JARDIR)/commons-logging.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-logging-1.0.4/commons-logging.jar $(JARDIR)/

$(JARDIR)/commons-pool-1.3.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/commons-pool-1.3/commons-pool-1.3.jar $(JARDIR)/

$(JARDIR)/dom4j-1.6.1.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/dom4j-1.6.1/dom4j-1.6.1.jar $(JARDIR)/

$(JARDIR)/geronimo-activation_1.0.2_spec-1.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/geronimo-activation-1.0.2/geronimo-activation_1.0.2_spec-1.2.jar $(JARDIR)/

$(JARDIR)/geronimo-javamail_1.3.1_spec-1.3.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/geronimo-javamail-1.3.1/geronimo-javamail_1.3.1_spec-1.3.jar $(JARDIR)/

$(JARDIR)/commons-codec-1.4.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/httpcomponents-client-4.1.2/commons-codec-1.4.jar $(JARDIR)/

$(JARDIR)/commons-logging-1.1.1.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/httpcomponents-client-4.1.2/commons-logging-1.1.1.jar $(JARDIR)/

$(JARDIR)/httpclient-4.1.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/httpcomponents-client-4.1.2/httpclient-4.1.2.jar $(JARDIR)/

$(JARDIR)/httpclient-cache-4.1.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/httpcomponents-client-4.1.2/httpclient-cache-4.1.2.jar $(JARDIR)/

$(JARDIR)/httpcore-4.1.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/httpcomponents-client-4.1.2/httpcore-4.1.2.jar $(JARDIR)/

$(JARDIR)/httpmime-4.1.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/httpcomponents-client-4.1.2/httpmime-4.1.2.jar $(JARDIR)/

$(JARDIR)/opencsv-1.8.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/opencsv-1.8/opencsv-1.8.jar $(JARDIR)/

$(JARDIR)/velocity-1.5.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/velocity-1.5/velocity-1.5.jar $(JARDIR)/

$(JARDIR)/velocity-tools-1.4.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/velocity-tools-1.4/velocity-tools-1.4.jar $(JARDIR)/

$(JARDIR)/jaxme2-0.5.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/ws-jaxme-0.5.2/jaxme2-0.5.2.jar $(JARDIR)/

$(JARDIR)/jaxme2-rt-0.5.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/ws-jaxme-0.5.2/jaxme2-rt-0.5.2.jar $(JARDIR)/

$(JARDIR)/jaxmeapi-0.5.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/ws-jaxme-0.5.2/jaxmeapi-0.5.2.jar $(JARDIR)/

$(JARDIR)/jaxmejs-0.5.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/ws-jaxme-0.5.2/jaxmejs-0.5.2.jar $(JARDIR)/

$(JARDIR)/jaxmepm-0.5.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/ws-jaxme-0.5.2/jaxmepm-0.5.2.jar $(JARDIR)/

$(JARDIR)/jaxmexs-0.5.2.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/ws-jaxme-0.5.2/jaxmexs-0.5.2.jar $(JARDIR)/

$(JARDIR)/wsdl4j.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/wsdl4j-1.5.1/wsdl4j.jar $(JARDIR)/

$(JARDIR)/wstx-asl-3.2.3.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/wstx-asl-3.2.3/wstx-asl-3.2.3.jar $(JARDIR)/

$(JARDIR)/xalan.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/xalan-j-2.7.1/xalan.jar $(JARDIR)/

$(JARDIR)/resolver.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/xerces-2.9.1/resolver.jar $(JARDIR)/

$(JARDIR)/xercesImpl.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/xerces-2.9.1/xercesImpl.jar $(JARDIR)/

$(JARDIR)/xml-apis.jar: $(TURKDIR)
	cp $(TURKDIR)/lib/third-party/xerces-2.9.1/xml-apis.jar $(JARDIR)/

# untarball MTurk SDK
$(TURKDIR): $(JARDIR) $(DOWNLOADDIR)/$(ARCH_MTURKSDK)
	mkdir -p $(TURKDIR)
	$(TAR) xzvf $(DOWNLOADDIR)/$(ARCH_MTURKSDK) -C $(UNPACKDIR)

# fetch MTurk SDK
$(DOWNLOADDIR)/$(ARCH_MTURKSDK): $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(MTURKSDK),$(DOWNLOADDIR)/$(ARCH_MTURKSDK)))

## ACTIVEOBJECTS

# copy ActiveObjects lib to JARDIR
$(JARDIR)/$(JAR_ACTIVEOBJ): $(AODIR)
	cp $(AODIR)/$(JAR_ACTIVEOBJ) $(JARDIR)/

# untarball ActiveObjects lib
$(AODIR): $(JARDIR) $(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ)
	mkdir -p $(AODIR)
	$(TAR) xzvf $(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ) -C $(UNPACKDIR)

# fetch ActiveObjects lib
$(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ): $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(ACTIVEOBJ),$(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ)))

## ONEJAR

# fetch One-JAR lib
$(JARDIR)/$(JAR_ONEJAR): $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(ONEJAR),$(JARDIR)/$(ARCH_ONEJAR)))

## SCALA
# fetch Scala libs
$(DOWNLOADDIR)/$(ARCH_SCALA):
	$(eval $(call DOWNLOAD,$(SCALA),$(DOWNLOADDIR)/$(ARCH_SCALA)))

# untarball Scala libs
$(SCALADIR): $(JARDIR) $(DOWNLOADDIR)/$(ARCH_SCALA)
	mkdir -p $(SCALADIR)
	$(TAR) xzvf $(DOWNLOADDIR)/$(ARCH_SCALA) -C $(UNPACKDIR)

$(JARDIR)/diffutils.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/diffutils.jar $(JARDIR)/

$(JARDIR)/jline.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/jline.jar $(JARDIR)/

$(JARDIR)/scala-actors-migration.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/scala-actors-migration.jar $(JARDIR)/

$(JARDIR)/scala-actors.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/scala-actors.jar $(JARDIR)/

$(JARDIR)/scala-compiler.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/scala-compiler.jar $(JARDIR)/

$(JARDIR)/scala-library.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/scala-library.jar $(JARDIR)/

$(JARDIR)/scala-partest.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/scala-partest.jar $(JARDIR)/

$(JARDIR)/scala-reflect.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/scala-reflect.jar $(JARDIR)/

$(JARDIR)/scala-swing.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/scala-swing.jar $(JARDIR)/

$(JARDIR)/scalap.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/scalap.jar $(JARDIR)/

$(JARDIR)/typesafe-config.jar: $(SCALADIR)
	cp $(SCALADIR)/lib/typesafe-config.jar $(JARDIR)/

## DIRECTORIES

# create binary class directory
$(APPCLASSES)/simple_program:
	mkdir -p $(APPCLASSES)/simple_program

# create output JAR directory
$(OUTJARS):
	mkdir -p $(OUTJARS)

# create binary class directory
$(CLASSDIR):
	mkdir -p $(CLASSDIR)

# create library JAR directory
$(JARDIR):
	mkdir -p $(JARDIR)

# create target downloads directory
$(DOWNLOADDIR):
	mkdir -p $(DOWNLOADDIR)

# create target temp directory
