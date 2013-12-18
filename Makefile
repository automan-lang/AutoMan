# Copyright 2013 University of Massachusetts Amherst

## START INFORMATION FOR HUMAN-HUMANS
#
# This Makefile does the following things:
# 1. Downloads AutoMan dependencies, including the Scala compiler.
# 2. Unpacks dependencies so that they may be linked against AutoMan code.
# 3. Makes an AutoMan JAR.
# 4. Puts all AutoMan and all its dependencies in the "jars" folder.
#
#    If you build sample application targets, the Makefile will build
#    them using One-JAR so that they can be used and distributed as
#    self-contained executable JARs.
#
## END INFORMATION FOR HUMAN-HUMANS

#### START OK-TO-MODIFY ZONE ####

## AUTOMAN VERSION
VER_AUTOMAN	:= 0.3

## ARCHIVE NAMES
ARCH_MTURKSDK 	:= java-aws-mturk-1.6.2.tar.gz
ARCH_AWSSDK 		:= aws-java-sdk.zip
ARCH_ACTIVEOBJ 	:= activeobjects-0.8.2.tar.gz
ARCH_ONEJAR			:= one-jar-boot-0.97.jar
ARCH_SCALA			:= scala-2.10.3.tgz

#### END OK-TO-MODIFY ZONE ####

#### START MODIFY-AT-YOUR-PERIL ZONE ####

## BINARY UTILITY LOCATIONS (DO NOT MODIFY)
TAR		:= $(shell which tar)
UNZIP	:= $(shell which unzip)
CURL	:= $(shell which curl)
WGET	:= $(shell which wget)
ANT		:= $(shell which ant)
JAR		:= $(shell which jar)
JAVAC := $(shell which javac)

## UNPACK DIR NAMES (DO NOT MODIFY)
DIR_MTURKSDK 	:= $(patsubst %.tar.gz,%,$(ARCH_MTURKSDK))
DIR_ACTIVEOBJ := $(patsubst %.tar.gz,%,$(ARCH_ACTIVEOBJ))
DIR_SCALA			:= $(patsubst %.tgz,%,$(ARCH_SCALA))
DIR_AWSSDK		:= $(patsubst %.zip,%,$(ARCH_AWSSDK))

## VERSION STRINGS (DO NOT MODIFY)
VER_MTURKSDK 	:= $(patsubst java-aws-mturk-%.tar.gz,%,$(ARCH_MTURKSDK))
VER_ACTIVEOBJ := $(patsubst activeobjects-%.tar.gz,%,$(ARCH_ACTIVEOBJ))
VER_ONEJAR		:= $(patsubst one-jar-boot-%.jar,%,$(ARCH_ONEJAR))
VER_SCALA			:= $(patsubst scala-%.tgz,%,$(ARCH_SCALA))

## JARS (DO NOT MODIFY)
JAR_AUTOMAN		:= automan-${VER_AUTOMAN}.jar
JAR_ACTIVEOBJ	:= activeobjects-$(VER_ACTIVEOBJ).jar
JAR_ONEJAR		:= $(ARCH_ONEJAR)

## LIBRARY URLS (DO NOT MODIFY)
URL_MTURKSDK 	:= http://downloads.sourceforge.net/project/mturksdk-java/mturksdk-java/$(VER_MTURKSDK)/$(ARCH_MTURKSDK)
URL_AWSSDK		:= http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip
URL_ACTIVEOBJ := http://java.net/projects/activeobjects/downloads/download/$(VER_ACTIVEOBJ)/$(ARCH_ACTIVEOBJ)
URL_ONEJAR 		:= https://sourceforge.net/projects/one-jar/files/one-jar/one-jar-$(VER_ONEJAR)/$(ARCH_ONEJAR)
URL_SCALA			:= http://www.scala-lang.org/files/archive/$(ARCH_SCALA)

## STATIC VARS (DO NOT MODIFY)
PREFIX			:= $(strip $(shell pwd))
OUTJARS 		:= jars
TEMPDIR 		:= tmp
AOUTJARS		:= $(PREFIX)/$(OUTJARS)
ATEMPDIR 		:= $(PREFIX)/$(TEMPDIR)
UNPACKDIR 	:= $(ATEMPDIR)/unpack
JARDIR 			:= $(ATEMPDIR)/lib
DOWNLOADDIR := $(ATEMPDIR)/downloads
CLASSDIR 		:= $(ATEMPDIR)/classes
APPCLASSES	:= $(TEMPDIR)/apps/classes
APPJARS			:= $(TEMPDIR)/apps/jars
AAPPJARS		:= $(ATEMPDIR)/apps/jars

## FUNCTIONS

# NOTE: functions go bonkers if the environment already contains
#       a variable with the same name; thus I pre-emptively "unexport"
#       all of these names to avoid the pain.
unexport DOWNLOAD MAKEAPP CP SCALAC AUTOMAN_JAVA_SRC AUTOMAN_SCALA_SRC DIR_AWSSDK

define DOWNLOAD
ifeq ($(CURL),)
$(shell $(WGET) -O $(2) $(1))
else
$(shell $(CURL) -L -o $(2) $(1))
endif
endef

# First arg: basename of class file
# Second arg: entry point class name
define MAKEAPP
$(SCALAC) -classpath $(OUTJARS)/$(JAR_AUTOMAN):$(CP) -d $(APPCLASSES)/$(1) apps/$(1)/src/main/scala/$(1).scala
mkdir -p $(APPJARS)/$(1)/main $(APPJARS)/$(1)/lib
cd $(APPCLASSES)/$(1); $(JAR) cvfe $(AAPPJARS)/$(1)/main/main.jar $(2) *
cp $(OUTJARS)/$(JAR_AUTOMAN) $(APPJARS)/$(1)/lib
find $(JARDIR) -iname "*.jar" -type f -exec cp {} $(APPJARS)/$(1)/lib \;
cd $(APPJARS)/$(1); $(JAR) xfv $(JARDIR)/onejar/$(JAR_ONEJAR)
rm -rf $(APPJARS)/$(1)/src
echo "One-Jar-Main-Class: $(2)" >> $(APPJARS)/$(1)/boot-manifest.mf
cd $(APPJARS)/$(1); $(JAR) cvfm $(AOUTJARS)/$(1).jar boot-manifest.mf .
endef

CP 								= $(shell find $(JARDIR) -iname "*.jar" -type f | tr '\n' ':')
SCALAC 						= $(UNPACKDIR)/$(DIR_SCALA)/bin/scalac
AUTOMAN_JAVA_SRC 	= $(shell find lib -iname "*.java" -type f | tr '\n' ' ')
AUTOMAN_SCALA_SRC = $(shell find lib -type f -iname "*.scala" | tr '\n' ' ')

## BUILD TARGETS
all: jarcheck $(OUTJARS)/$(JAR_AUTOMAN) $(OUTJARS)/simple_program.jar $(OUTJARS)/anpr.jar

jarcheck:
ifeq ($(JAR),)
$(error Must have the "jar" utility installed)
endif

unzipcheck:
ifeq ($(UNZIP),)
$(error Must have the "unzip" utility installed)
endif

clean:
	@-rm -rf $(ATEMPDIR) $(OUTJARS)

# AUTOMAN
$(OUTJARS)/$(JAR_AUTOMAN): $(AUTOMAN_SCALA_SRC) $(AUTOMAN_JAVA_SRC) $(JARDIR)/mturk-sdk $(JARDIR)/activeobject $(JARDIR)/scala | $(JARDIR) $(CLASSDIR) $(OUTJARS)
	$(SCALAC) -classpath $(CP) -d $(CLASSDIR) $(AUTOMAN_SCALA_SRC) $(AUTOMAN_JAVA_SRC)
	cd $(CLASSDIR); $(JAR) cvf $(AOUTJARS)/$(JAR_AUTOMAN) edu
	cd $(OUTJARS); $(JAR) i $(JAR_AUTOMAN)

# SAMPLE PROGRAMS

# simple_program
$(OUTJARS)/simple_program.jar: $(OUTJARS)/$(JAR_AUTOMAN) $(JARDIR)/onejar/$(JAR_ONEJAR) | $(APPCLASSES)/simple_program $(APPJARS)/simple_program
	$(call MAKEAPP,simple_program,simple_program)

# anpr
$(OUTJARS)/anpr.jar: unzipcheck $(OUTJARS)/$(JAR_AUTOMAN) $(JARDIR)/aws-sdk $(JARDIR)/onejar/$(JAR_ONEJAR) | $(APPCLASSES)/anpr $(APPJARS)/anpr
	$(call MAKEAPP,anpr,anpr)

## MTURK SDK
$(JARDIR)/mturk-sdk: | $(UNPACKDIR)/$(DIR_MTURKSDK)
	mkdir -p $(JARDIR)/mturk-sdk
	find $(UNPACKDIR)/$(DIR_MTURKSDK)/lib -iname "*.jar" -type f -exec cp {} $(JARDIR)/mturk-sdk \;

# untarball MTurk SDK
$(UNPACKDIR)/$(DIR_MTURKSDK): | $(JARDIR) $(DOWNLOADDIR)/$(ARCH_MTURKSDK)
	mkdir -p $(UNPACKDIR)/$(DIR_MTURKSDK)
	$(TAR) xzvf $(DOWNLOADDIR)/$(ARCH_MTURKSDK) -C $(UNPACKDIR)

# fetch MTurk SDK
$(DOWNLOADDIR)/$(ARCH_MTURKSDK): | $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(URL_MTURKSDK),$(DOWNLOADDIR)/$(ARCH_MTURKSDK)))

## AWS SDK
$(JARDIR)/aws-sdk: | $(JARDIR) $(UNPACKDIR)/$(DIR_AWSSDK)
	mkdir -p $(JARDIR)/aws-sdk
	find $(UNPACKDIR)/$(DIR_AWSSDK)/lib -iname "*.jar" -type f -exec cp {} $(JARDIR)/aws-sdk \;

# unzip AWS SDK
$(UNPACKDIR)/$(DIR_AWSSDK): | $(DOWNLOADDIR)/$(ARCH_AWSSDK)
	$(UNZIP) -o $(DOWNLOADDIR)/$(ARCH_AWSSDK) -d $(UNPACKDIR)
	# We have no way of knowing the name of the folder until we unpack it. THANKS, OBAMA.
	find $(UNPACKDIR) -type d -iname "aws-java-sdk*" -maxdepth 1 -exec mv {} $(UNPACKDIR)/$(DIR_AWSSDK) \;

# fetch AWS SDK
$(DOWNLOADDIR)/$(ARCH_AWSSDK): | $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(URL_AWSSDK),$(DOWNLOADDIR)/$(ARCH_AWSSDK)))

## ACTIVEOBJECTS
# copy ActiveObjects lib to JARDIR
$(JARDIR)/activeobject: | $(UNPACKDIR)/$(DIR_ACTIVEOBJ)
	mkdir -p $(JARDIR)/activeobject
	cp $(UNPACKDIR)/$(DIR_ACTIVEOBJ)/$(JAR_ACTIVEOBJ) $(JARDIR)/activeobject/

# untarball ActiveObjects lib
$(UNPACKDIR)/$(DIR_ACTIVEOBJ): | $(JARDIR) $(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ)
	mkdir -p $(UNPACKDIR)/$(DIR_ACTIVEOBJ)
	$(TAR) xzvf $(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ) -C $(UNPACKDIR)

# fetch ActiveObjects lib
$(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ): | $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(URL_ACTIVEOBJ),$(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ)))

## ONEJAR
# fetch One-JAR lib
$(JARDIR)/onejar/$(JAR_ONEJAR): | $(DOWNLOADDIR) $(JARDIR)/onejar
	$(eval $(call DOWNLOAD,$(URL_ONEJAR),$(JARDIR)/onejar/$(ARCH_ONEJAR)))

## SCALA
# untarball Scala libs
$(UNPACKDIR)/$(DIR_SCALA): $(DOWNLOADDIR)/$(ARCH_SCALA) | $(UNPACKDIR)
	mkdir -p $(UNPACKDIR)/$(DIR_SCALA)
	$(TAR) xzvf $(DOWNLOADDIR)/$(ARCH_SCALA) -C $(UNPACKDIR)

# fetch Scala libs
$(DOWNLOADDIR)/$(ARCH_SCALA): | $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(URL_SCALA),$(DOWNLOADDIR)/$(ARCH_SCALA)))

# copy scala libs
$(JARDIR)/scala: | $(UNPACKDIR)/$(DIR_SCALA)
	mkdir -p $(JARDIR)/scala
	find $(UNPACKDIR)/$(DIR_SCALA)/lib -iname "*.jar" -type f -exec cp {} $(JARDIR)/scala \;

## DIRECTORIES
$(UNPACKDIR):
	mkdir -p $(UNPACKDIR)

$(JARDIR)/onejar: | $(JARDIR)
	mkdir -p $(JARDIR)/onejar

$(APPCLASSES)/%:
	mkdir -p $@

$(APPJARS)/%:
	mkdir -p $@

$(OUTJARS):
	mkdir -p $(OUTJARS)

$(JARDIR):
	mkdir -p $(JARDIR)

$(CLASSDIR):
	mkdir -p $(CLASSDIR)

$(DOWNLOADDIR):
	mkdir -p $(DOWNLOADDIR)

.PHONY : clean jarcheck

#### END MODIFY-AT-YOUR-PERIL ZONE ####