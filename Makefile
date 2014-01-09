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
ARCH_ACTIVEOBJ 	:= activeobjects-0.9-m17-bin.tar.gz
ARCH_COMMONSIO	:= commons-io-2.4-bin.tar.gz
ARCH_GSEARCH		:= gsearch-java-client
ARCH_GSON				:= google-gson-2.2.4-release.zip
ARCH_ONEJAR			:= one-jar-boot-0.97.jar
ARCH_JUNIT			:= junit-4.11.jar
ARCH_SCALA			:= scala-2.10.3.tgz
ARCH_IMGSCALR		:= imgscalr-lib-4.2.zip
ARCH_DERBY			:= db-derby-10.10.1.1-bin.tar.gz

#### END OK-TO-MODIFY ZONE ####

#### START MODIFY-AT-YOUR-PERIL ZONE ####

## BINARY UTILITY LOCATIONS (DO NOT MODIFY)
TAR		:= $(shell which tar)
UNZIP	:= $(shell which unzip)
CURL	:= $(shell which curl)
WGET	:= $(shell which wget)
SVN		:= $(shell which svn)
ANT		:= $(shell which ant)
JAR		:= $(shell which jar)
JAVAC := $(shell which javac)
PATCH	:= $(shell which patch)

## UNPACK DIR NAMES (DO NOT MODIFY)
DIR_MTURKSDK 	:= $(patsubst %.tar.gz,%,$(ARCH_MTURKSDK))
DIR_ACTIVEOBJ := $(patsubst %.tar.gz,%,$(ARCH_ACTIVEOBJ))
DIR_COMMONSIO	:= $(patsubst %-bin.tar.gz,%,$(ARCH_COMMONSIO))
DIR_GSEARCH		:= $(ARCH_GSEARCH)
DIR_GSON			:= $(patsubst %-release.zip,%,$(ARCH_GSON))
DIR_SCALA			:= $(patsubst %.tgz,%,$(ARCH_SCALA))
DIR_AWSSDK		:= $(patsubst %.zip,%,$(ARCH_AWSSDK))
DIR_JUNIT			:= $(patsubst %.jar,%,$(ARCH_JUNIT))
DIR_IMGSCALR	:= $(patsubst %.zip,%,$(ARCH_IMGSCALR))
DIR_DERBY			:= $(patsubst %.tar.gz,%,$(ARCH_DERBY))

## VERSION STRINGS (DO NOT MODIFY)
VER_MTURKSDK 	:= $(patsubst java-aws-mturk-%.tar.gz,%,$(ARCH_MTURKSDK))
VER_ACTIVEOBJ := $(patsubst activeobjects-%-bin.tar.gz,%,$(ARCH_ACTIVEOBJ))
VER_COMMONSIO	:= $(patsubst commons-io-%-bin.tar.gz,%,$(ARCH_COMMONSIO))
VER_GSON			:= $(patsubst google-gson-%-release.zip,%,$(ARCH_GSON))
VER_ONEJAR		:= $(patsubst one-jar-boot-%.jar,%,$(ARCH_ONEJAR))
VER_SCALA			:= $(patsubst scala-%.tgz,%,$(ARCH_SCALA))
VER_JUNIT			:= $(patsubst junit-%.jar,%,$(ARCH_JUNIT))
VER_IMGSCALR	:= $(patsubst imgscalr-lib-%.zip,%,$(ARCH_IMGSCALR))
VER_DERBY			:= $(patsubst db-derby-%-bin.tar.gz,%,$(ARCH_DERBY))

## JARS (DO NOT MODIFY)
JAR_AUTOMAN		:= automan-${VER_AUTOMAN}.jar
JAR_ACTIVEOBJ	:= activeobjects-$(VER_ACTIVEOBJ).jar
JAR_COMMONSIO	:= commons-io-$(VER_COMMONSIO).jar
JAR_GSON			:= gson-$(VER_GSON).jar
JAR_ONEJAR		:= $(ARCH_ONEJAR)
JAR_JUNIT			:= $(ARCH_JUNIT)
JAR_GSEARCH		:= $(ARCH_GSEARCH).jar
JAR_IMGSCALR	:= imgscalr-lib-$(VER_IMGSCALR).jar
JAR_DERBY			:= db-derby-$(VER_DERBY).jar

## LIBRARY URLS (DO NOT MODIFY)
URL_MTURKSDK 	:= http://downloads.sourceforge.net/project/mturksdk-java/mturksdk-java/$(VER_MTURKSDK)/$(ARCH_MTURKSDK)
URL_AWSSDK		:= http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip
URL_ACTIVEOBJ := http://java.net/projects/activeobjects/downloads/download/$(VER_ACTIVEOBJ)/$(ARCH_ACTIVEOBJ)
URL_COMMONSIO	:= http://mirror.tcpdiag.net/apache/commons/io/binaries/$(ARCH_COMMONSIO)
URL_GSEARCH		:= http://gsearch-java-client.googlecode.com/svn/trunk/gsearch-java-client
URL_GSON			:= http://google-gson.googlecode.com/files/$(ARCH_GSON)
URL_ONEJAR 		:= https://sourceforge.net/projects/one-jar/files/one-jar/one-jar-$(VER_ONEJAR)/$(ARCH_ONEJAR)
URL_JUNIT			:= http://repo1.maven.org/maven2/junit/junit/$(VER_JUNIT)/$(ARCH_JUNIT)
URL_SCALA			:= http://www.scala-lang.org/files/archive/$(ARCH_SCALA)
URL_IMGSCALR	:= https://github.com/downloads/thebuzzmedia/imgscalr/$(ARCH_IMGSCALR)
URL_DERBY			:= http://apache.spinellicreations.com//db/derby/db-derby-10.10.1.1/$(ARCH_DERBY)

## STATIC VARS (DO NOT MODIFY)
PREFIX			:= $(strip $(shell pwd))
OUTJARS 		:= jars
TEMPDIR 		:= tmp
AOUTJARS		:= $(PREFIX)/$(OUTJARS)
ATEMPDIR 		:= $(PREFIX)/$(TEMPDIR)
APATCHDIR		:= $(PREFIX)/patches
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
unexport DOWNLOAD MAKEAPP CHECKOUT CP SCALAC AUTOMAN_JAVA_SRC AUTOMAN_SCALA_SRC DIR_AWSSDK GSEARCH_JAVA_SRC

define DOWNLOAD
ifeq ($(CURL),)
$(info $(WGET) --no-check-certificate -O $(2) '$(1)')
$(shell $(WGET) --no-check-certificate -O $(2) '$(1)')
else
$(info $(CURL) -L -o $(2) '$(1)')
$(shell $(CURL) -L -o $(2) '$(1)')
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
GSEARCH_JAVA_SRC	= $(shell find $(UNPACKDIR)/$(DIR_GSEARCH) -type f -iname "*.java" | tr '\n' ' ')

## BUILD TARGETS
default:
	@echo "\nBuild targets are:"
	@echo "  all\t\t\t\tBuild all targets."
	@echo "  automan\t\t\tJust the AutoMan library. Note that dependencies will be"
	@echo "         \t\t\tplaced in the 'jars/deps' folder."
	@echo "  HowManyThings\t\t\tSample app.  Shows a simple object-counting function."
	@echo "  anpr\t\t\t\tSample app.  A license-plate reader."
	@echo "  banana_question\t\tSample app.  Classification task that shows interesting"
	@echo "  \t\t\t\tcrowd biases."
	@echo "  license_plate_reader\t\tSample app.  Like 'anpr' but uses an amusing"
	@echo "  \t\t\t\timage source."
	@echo "  simple_program\t\tSample app.  Very simple classification app."
	@echo "  simple_checkbox_program\tSample app.  Variation on 'simple_question'."
	@echo "\nNOTE:\tSample applications have NO dependencies, as they are"
	@echo "\tpackaged using OneJAR.\n"

all: .jarcheck \
	$(OUTJARS)/$(JAR_AUTOMAN) \
	$(OUTJARS)/simple_program.jar \
	$(OUTJARS)/simple_checkbox_program.jar \
	$(OUTJARS)/anpr.jar \
	$(OUTJARS)/banana_question.jar \
	$(OUTJARS)/HowManyThings.jar \
	$(OUTJARS)/license_plate_reader.jar

.PHONY = automan HowManyThings anpr banana_question license_plate_reader simple_program simple_checkbox_program

# shortcuts
automan: $(OUTJARS)/$(JAR_AUTOMAN) $(OUTJARS)/deps
HowManyThings: $(OUTJARS)/HowManyThings.jar
anpr: $(OUTJARS)/anpr.jar
banana_question: $(OUTJARS)/banana_question.jar
license_plate_reader: $(OUTJARS)/license_plate_reader.jar
simple_program: $(OUTJARS)/simple_program.jar
simple_checkbox_program: $(OUTJARS)/simple_checkbox_program.jar

.jarcheck:
ifeq ($(JAR),)
$(error Must have the "jar" utility installed)
else
	touch .jarcheck
endif

.unzipcheck:
ifeq ($(UNZIP),)
$(error Must have the "unzip" utility installed)
else
	touch .unzipcheck
endif

.svncheck:
ifeq ($(SVN),)
$(error Must have the "svn" utility installed)
else
	touch .svncheck
endif

.javaccheck:
ifeq ($(JAVAC),)
$(error Must have the "javac" utility installed)
else
	touch .javaccheck
endif

.patchcheck:
ifeq ($(PATCH),)
$(error Must have the "javac" utility installed)
else
	touch .patchcheck
endif

clean:
	@-rm -rf $(ATEMPDIR) $(OUTJARS)
	@-rm -f .jarcheck
	@-rm -f .javaccheck
	@-rm -f .patchcheck
	@-rm -f .svncheck
	@-rm -f .unzipcheck

# AUTOMAN
$(OUTJARS)/$(JAR_AUTOMAN) $(OUTJARS)/deps: $(AUTOMAN_SCALA_SRC) \
	$(AUTOMAN_JAVA_SRC) \
	$(JARDIR)/mturk-sdk \
	$(JARDIR)/activeobjects \
	$(JARDIR)/scala | \
	$(JARDIR) \
	$(CLASSDIR) \
	$(OUTJARS) \
	$(JARDIR)/$(DIR_DERBY)/$(JAR_DERBY)
	$(SCALAC) -unchecked -deprecation -explaintypes -classpath $(CP) -d $(CLASSDIR) $(AUTOMAN_SCALA_SRC) $(AUTOMAN_JAVA_SRC)
	cd $(CLASSDIR); $(JAR) cvf $(AOUTJARS)/$(JAR_AUTOMAN) edu
	cd $(OUTJARS); $(JAR) i $(JAR_AUTOMAN)
	mkdir -p $(OUTJARS)/deps
	cp -Rp $(JARDIR)/* $(OUTJARS)/deps/

# SAMPLE PROGRAMS

# simple_program
$(OUTJARS)/simple_program.jar: $(OUTJARS)/$(JAR_AUTOMAN) \
	$(JARDIR)/onejar/$(JAR_ONEJAR) \
	apps/simple_program/src/main/scala/simple_program.scala | \
	$(APPCLASSES)/simple_program \
	$(APPJARS)/simple_program
	$(call MAKEAPP,simple_program,simple_program)

# anpr
$(OUTJARS)/anpr.jar: .unzipcheck \
	$(OUTJARS)/$(JAR_AUTOMAN) \
	$(JARDIR)/aws-sdk \
	$(JARDIR)/onejar/$(JAR_ONEJAR) \
	apps/anpr/src/main/scala/anpr.scala | \
	$(APPCLASSES)/anpr \
	$(APPJARS)/anpr
	$(call MAKEAPP,anpr,anpr)

# banana question
$(OUTJARS)/banana_question.jar: $(OUTJARS)/$(JAR_AUTOMAN) \
	$(JARDIR)/onejar/$(JAR_ONEJAR) \
	apps/banana_question/src/main/scala/banana_question.scala | \
	$(APPCLASSES)/banana_question \
	$(APPJARS)/banana_question
	$(call MAKEAPP,banana_question,banana_question)

# HowManyThings
$(OUTJARS)/HowManyThings.jar: $(OUTJARS)/$(JAR_AUTOMAN) \
	$(JARDIR)/aws-sdk \
	$(JARDIR)/$(DIR_COMMONSIO) \
	$(JARDIR)/onejar/$(JAR_ONEJAR) \
	apps/HowManyThings/src/main/scala/HowManyThings.scala | \
	$(APPCLASSES)/HowManyThings \
	$(APPJARS)/HowManyThings \
	$(JARDIR)/$(DIR_GSEARCH) \
	$(JARDIR)/$(DIR_IMGSCALR)
	$(call MAKEAPP,HowManyThings,HowManyThings)

# license_plate_reader (oldparkedcars.com!)
$(OUTJARS)/license_plate_reader.jar: $(OUTJARS)/$(JAR_AUTOMAN) \
	$(JARDIR)/aws-sdk \
	$(JARDIR)/$(DIR_COMMONSIO) \
	$(JARDIR)/onejar/$(JAR_ONEJAR) \
	apps/license_plate_reader/src/main/scala/license_plate_reader.scala | \
	$(APPCLASSES)/license_plate_reader \
	$(APPJARS)/license_plate_reader \
	$(JARDIR)/$(DIR_GSEARCH) \
	$(JARDIR)/$(DIR_IMGSCALR)
	$(call MAKEAPP,license_plate_reader,license_plate_reader)

# simple_checkbox_program
$(OUTJARS)/simple_checkbox_program.jar: $(OUTJARS)/$(JAR_AUTOMAN) \
	$(JARDIR)/onejar/$(JAR_ONEJAR) \
	apps/simple_checkbox_program/src/main/scala/simple_checkbox_program.scala | \
	$(APPCLASSES)/simple_checkbox_program \
	$(APPJARS)/simple_checkbox_program
	$(call MAKEAPP,simple_checkbox_program,simple_checkbox_program)

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
$(JARDIR)/activeobjects: | $(UNPACKDIR)/$(DIR_ACTIVEOBJ)
	mkdir -p $(JARDIR)/activeobjects
	cp $(UNPACKDIR)/$(DIR_ACTIVEOBJ)/bin/$(JAR_ACTIVEOBJ) $(JARDIR)/activeobjects/

# untarball ActiveObjects lib
$(UNPACKDIR)/$(DIR_ACTIVEOBJ): | $(JARDIR) $(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ)
	mkdir -p $(UNPACKDIR)/$(DIR_ACTIVEOBJ)
	$(TAR) xzvf $(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ) -C $(UNPACKDIR)/$(DIR_ACTIVEOBJ)

# fetch ActiveObjects lib
$(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ): | $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(URL_ACTIVEOBJ),$(DOWNLOADDIR)/$(ARCH_ACTIVEOBJ)))

## COMMONS IO
# copy Commons IO libs to JARDIR
$(JARDIR)/$(DIR_COMMONSIO): | $(UNPACKDIR)/$(DIR_COMMONSIO)
	mkdir -p $(JARDIR)/$(DIR_COMMONSIO)
	cp $(UNPACKDIR)/$(DIR_COMMONSIO)/$(JAR_COMMONSIO) $(JARDIR)/$(DIR_COMMONSIO)/

# untarball Commons IO libs
$(UNPACKDIR)/$(DIR_COMMONSIO): | $(JARDIR) $(DOWNLOADDIR)/$(ARCH_COMMONSIO)
	mkdir -p $(UNPACKDIR)/$(DIR_COMMONSIO)
	$(TAR) xzvf $(DOWNLOADDIR)/$(ARCH_COMMONSIO) -C $(UNPACKDIR)

# fetch Commons IO libs
$(DOWNLOADDIR)/$(ARCH_COMMONSIO): | $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(URL_COMMONSIO),$(DOWNLOADDIR)/$(ARCH_COMMONSIO)))

## GSEARCH
$(JARDIR)/$(DIR_GSEARCH): | $(JARDIR) $(CLASSDIR)/gsearch
	mkdir -p $(JARDIR)/$(DIR_GSEARCH)
	$(JAR) cvf $(JARDIR)/$(DIR_GSEARCH)/$(JAR_GSEARCH) -C $(CLASSDIR) gsearch

# build gsearch using MTurk's JAR deps instead of the supplied JARs
$(CLASSDIR)/gsearch: .javaccheck | $(UNPACKDIR)/$(DIR_GSEARCH) $(JARDIR)/$(DIR_GSON) $(JARDIR)/$(DIR_JUNIT)/$(JAR_JUNIT) $(JARDIR)/mturk-sdk
	$(JAVAC) -classpath $(CP) -d $(CLASSDIR) $(GSEARCH_JAVA_SRC)

# fetch and patch gsearch libs
$(UNPACKDIR)/$(DIR_GSEARCH): .patchcheck .svncheck | $(UNPACKDIR)
	$(SVN) co $(URL_GSEARCH) $(UNPACKDIR)/$(DIR_GSEARCH)
	cd $(UNPACKDIR)/$(DIR_GSEARCH)/src/main/gsearch; \
	patch < $(APATCHDIR)/gsearch_client_patch.patch; \
	patch < $(APATCHDIR)/gsearch_client_patch_2.patch

## GSON
# copy GSON libs to JARDIR
$(JARDIR)/$(DIR_GSON): | $(UNPACKDIR)/$(DIR_GSON) $(JARDIR)
	mkdir -p $(JARDIR)/$(DIR_GSON)
	cp $(UNPACKDIR)/$(DIR_GSON)/$(JAR_GSON) $(JARDIR)/$(DIR_GSON)/

# unzip GSON libs
$(UNPACKDIR)/$(DIR_GSON): | $(DOWNLOADDIR)/$(ARCH_GSON)
	mkdir -p $(UNPACKDIR)/$(DIR_GSON)
	$(UNZIP) -o $(DOWNLOADDIR)/$(ARCH_GSON) -d $(UNPACKDIR)

# fetch GSON libs
$(DOWNLOADDIR)/$(ARCH_GSON): | $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(URL_GSON),$(DOWNLOADDIR)/$(ARCH_GSON)))

## JUNIT
# download JUNIT lib to JARDIR
$(JARDIR)/$(DIR_JUNIT)/$(JAR_JUNIT): | $(JARDIR)/$(DIR_JUNIT)
	$(eval $(call DOWNLOAD,$(URL_JUNIT),$(JARDIR)/$(DIR_JUNIT)/$(ARCH_JUNIT)))

## IMGSCALR
# copy imgscalr libs to JARDIR
$(JARDIR)/$(DIR_IMGSCALR): | $(UNPACKDIR)/$(DIR_IMGSCALR) $(JARDIR)
	mkdir -p $(JARDIR)/$(DIR_IMGSCALR)
	cp $(UNPACKDIR)/$(DIR_IMGSCALR)/$(JAR_IMGSCALR) $(JARDIR)/$(DIR_IMGSCALR)/

# unzip imgscalr libs
$(UNPACKDIR)/$(DIR_IMGSCALR): | $(DOWNLOADDIR)/$(ARCH_IMGSCALR)
	mkdir -p $(UNPACKDIR)/$(DIR_IMGSCALR)
	$(UNZIP) -o $(DOWNLOADDIR)/$(ARCH_IMGSCALR) -d $(UNPACKDIR)/$(DIR_IMGSCALR)

# fetch imgscalr libs
$(DOWNLOADDIR)/$(ARCH_IMGSCALR): | $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(URL_IMGSCALR),$(DOWNLOADDIR)/$(ARCH_IMGSCALR)))

## ONEJAR
# fetch One-JAR lib
$(JARDIR)/onejar/$(JAR_ONEJAR): | $(DOWNLOADDIR) $(JARDIR)/onejar
	$(eval $(call DOWNLOAD,$(URL_ONEJAR),$(JARDIR)/onejar/$(ARCH_ONEJAR)))

## DERBY
# copy DERBY JAR to JARDIR
$(JARDIR)/$(DIR_DERBY)/$(JAR_DERBY): | $(UNPACKDIR)/$(DIR_DERBY) $(JARDIR)
	mkdir -p $(JARDIR)/$(DIR_DERBY)
	cp $(UNPACKDIR)/$(DIR_DERBY)/lib/derby.jar $(JARDIR)/$(DIR_DERBY)/$(JAR_DERBY)

# untarball DERBY libs
$(UNPACKDIR)/$(DIR_DERBY): | $(DOWNLOADDIR)/$(ARCH_DERBY)
	mkdir -p $(UNPACKDIR)/$(DIR_DERBY)
	$(TAR) xzvf $(DOWNLOADDIR)/$(ARCH_DERBY) -C $(UNPACKDIR)

# fetch DERBY libs
$(DOWNLOADDIR)/$(ARCH_DERBY): | $(DOWNLOADDIR)
	$(eval $(call DOWNLOAD,$(URL_DERBY),$(DOWNLOADDIR)/$(ARCH_DERBY)))

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

$(JARDIR)/%: | $(JARDIR)
	mkdir -p $@

$(JARDIR):
	mkdir -p $(JARDIR)

$(CLASSDIR):
	mkdir -p $(CLASSDIR)

$(DOWNLOADDIR):
	mkdir -p $(DOWNLOADDIR)

#### END MODIFY-AT-YOUR-PERIL ZONE ####
