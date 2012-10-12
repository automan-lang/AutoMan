require 'buildr/scala'

# METADATA
A_COPYRIGHT = '(C) 2012 University of Massachusetts, Amherst'
A_VERSION = '0.3.0'

# Repositories
repositories.remote << 'http://repo1.maven.org/maven2'

# Maven artifacts
POM_AMAZON_AWS_SDK = transitive('com.amazonaws:aws-java-sdk:jar:1.3.14')
A_BASE = []
A_BASE << POM_AO = 'net.java.ao:activeobjects:jar:0.8.2'
A_BASE << transitive('axis:axis:jar:1.4')
A_BASE << transitive('commons-codec:commons-codec:jar:1.3')
A_BASE << transitive('commons-beanutils:commons-beanutils:jar:1.7.0')
A_BASE << transitive('commons-collections:commons-collections:jar:3.2')
A_BASE << transitive('commons-dbcp:commons-dbcp:jar:1.2.2')
A_BASE << transitive('commons-digester:commons-digester:jar:1.8')
A_BASE << transitive('commons-httpclient:commons-httpclient:jar:3.1')
A_BASE << transitive('commons-io:commons-io:jar:2.4')
A_BASE << transitive('commons-lang:commons-lang:jar:2.3')
A_BASE << transitive('commons-pool:commons-pool:jar:1.3')
A_BASE << 'org.apache.derby:derby:jar:10.9.1.0'
A_BASE << transitive('dom4j:dom4j:jar:1.6.1')
A_BASE << 'ca.juliusdavies:not-yet-commons-ssl:jar:0.3.11'
A_BASE << POM_MTURK_SDK = 'com.amazonaws.mturk:mturk:jar:1.6.2'
A_BASE << POM_MTURK_DS = 'com.amazonaws.mturk.dataschema:aws-mturk-dataschema:jar:1.6.2'
A_BASE << POM_MTURK_WSDL = 'com.amazonaws.mturk.requester:aws-mturk-wsdl:jar:1.6.2'

# Non-Maven artifact URLs
URL_AO = 'http://java.net/projects/activeobjects/downloads/download/0.8.2/activeobjects-0.8.2.tar.gz'
URL_MTURK = 'http://downloads.sourceforge.net/project/mturksdk-java/mturksdk-java/1.6.2/java-aws-mturk-1.6.2.tar.gz'

# Non-Maven artifacts need to be downloaded and installed
mturk_tgz = download("scratch/zip/java-aws-mturk-1.6.2.tar.gz" => URL_MTURK)
mturk_jar = file("scratch/jar/java-aws-mturk-1.6.2/lib/java-aws-mturk.jar" => unzip("scratch/jar" => mturk_tgz))
mturk_dataschema_jar = file("scratch/jar/java-aws-mturk-1.6.2/lib/aws-mturk-dataschema.jar" => unzip("scratch/jar" => mturk_tgz))
mturk_wsdl_jar = file("scratch/jar/java-aws-mturk-1.6.2/lib/aws-mturk-wsdl.jar" => unzip("scratch/jar" => mturk_tgz))
ao_tgz = download("scratch/zip/activeobjects-0.8.2.tar.gz" => URL_AO)
ao_jar = file("scratch/jar/activeobjects-0.8.2/activeobjects-0.8.2.jar" => unzip("scratch/jar" => ao_tgz))
install artifact(POM_MTURK_SDK).from(mturk_jar)
install artifact(POM_MTURK_DS).from(mturk_dataschema_jar)
install artifact(POM_MTURK_WSDL).from(mturk_wsdl_jar)
install artifact(POM_AO).from(ao_jar)

# compute base classpath
BASE_CLASSPATH = Buildr.artifacts(A_BASE).map { |a| artifact(a).to_s }.join(":")

task :runner_anpr do
  cp = BASE_CLASSPATH + ":" + POM_AMAZON_AWS_SDK.join(":").to_s
  make_runner("scripts", "anpr", cp)
end

task :runner_banana_question do
  make_runner("scripts", "banana_question", BASE_CLASSPATH)
end

task :runner_how_many_things do
  cp = BASE_CLASSPATH + ":" + POM_AMAZON_AWS_SDK.join(":").to_s
  make_runner("scripts", "HowManyThings", cp)
end

task :runner_simple_checkbox_program do
  make_runner("scripts", "simple_checkbox_program", BASE_CLASSPATH)
end

task :runner_simple_program do
  make_runner("scripts", "simple_program", BASE_CLASSPATH)
end

# Build targets
define 'automan' do
  define 'lib' do
    # declare dependencies
    compile.with A_BASE
    
    # version string
    project.version = A_VERSION
    
    # include copyright in package manifest
    package(:jar).with :manifest => { 'Copyright' => A_COPYRIGHT }
  end
  
  define 'apps' do
    define 'anpr' do
      compile.with project('automan:lib').package(:jar), A_BASE, POM_AMAZON_AWS_SDK
      task :build => :runner_anpr
    end
    
    define 'banana_question' do
      compile.with project('automan:lib').package(:jar), A_BASE
      task :build => :runner_banana_question
    end
    
    # FIXME: these subprojects depend on a library, modified by me,
    # that must be fetched from a svn repository.
    # define 'license_plate_reader' do
    #   compile.with project('automan:lib').package(:jar), A_BASE, POM_AMAZON_AWS_SDK
    #   cp = BASE_CLASSPATH + ":" + POM_AMAZON_AWS_SDK.join(":").to_s
    #   task :build => :runner_license_plate_reader
    # end
    # 
    # define 'HowManyThings' do
    #   compile.with project('automan:lib').package(:jar), A_BASE, POM_AMAZON_AWS_SDK
    #   task :build => :how_many_things 
    # end
    
    define 'simple_checkbox_program' do
      compile.with project('automan:lib').package(:jar), A_BASE
      task :build => :runner_simple_checkbox_program
    end
    
    define 'simple_program' do
      compile.with project('automan:lib').package(:jar), A_BASE
      task :build => :runner_simple_program 
    end
  end
  
  # extended clean
  clean {
    rm_rf _('scratch')
    rm_rf _('scripts')
  }
end

# create script runner that constructs horrible Java classpath
def make_runner(script_path, script_name, classpath)
  puts "CALLING make_runner(#{script_path}, #{script_name}, #{classpath})"
  abspath = make_directory script_path
  if abspath.nil?
    throw "Cannot make directory for scripts!"
  end
  
  # various important paths
  progcp = File.expand_path("apps/#{script_name}/target/classes").to_s
  cpath = classpath + ":" +
          project('automan:lib').compile.target.to_s + ":" +
          progcp
  
  File.open( "#{File.join([abspath,script_name])}.sh", 'w') do |f|
    f.write("\#!/bin/sh\n")
    f.write("scala -cp #{cpath} #{script_name} \"$@\"\n")
  end
end

def make_directory(d)
  begin
    if File.directory? d
      return File.expand_path d
    else
      FileUtils.mkdir d
    end
    
    # return the absolute path name
    File.expand_path d
  rescue SystemCallError => e
    return nil
  end
end

def target_dir(path)
  File.join(File.dirname(path.to_s))
end

 