# YADA Jar File Build and Deploy

# Build

## Prerequisites

### Executables

<aside>
⚠️ Assume Linux or MacOS.  Windows *might* work—probably will, but build and run never tested there.

</aside>

- Java 11+
- Maven 3.8
- tar
- gzip

### Environment Variables

```bash
# build-only properties
#  required for artifact naming and assembly
YADA_SRC=/path/to/local/YADA/repo
YADA_HOME=/opt/yada
YADA_BUILD_PROPS="-Dmaven.javadoc.skip=true -DskipTests=true -Dsurefire.skip=true -Dskip.tests=true -Dskip.war.deploy=trues -Dlog.level=info"
YADA_VERSION=$(awk '/artifactId>YADA/{getline;print;}' ${YADA_SRC}/pom.xml | awk -F'[<>]' '{ print $3 }')

# build/runtime properties
#  required for runtime configuration, but set during build
YADA_SERVER_HTTP_PORT=80
YADA_SERVER_HTTPS_PORT=443
YADA_SERVER_KEYSTORE_PATH=/path/to/jks
YADA_SERVER_KEYSTORE_SECRET=secret
YADA_SERVER_CORS_ALLOW_ORIGIN=https?://[a-zA-Z0-9\-\.]+(:[0-9]+)?$
YADA_SERVER_REQUEST_LOG_FILE=/var/log/yada/yada-access.log
```

## Package

```bash
cd $YADA_SRC;
mvn -Pproduction $(echo -n ${YADA_BUILD_PROPS}) clean package;
```

# Deploy

<aside>
⚠️ Assumes build and deploy in the same environment

</aside>

## Prerequisites

### Environment Configuration

<aside>
⚠️ If not running as root, set up user

</aside>

```bash
# if $YADA_HOME is not home dir of `yada` user, it must be created separately
# Also create the log directory

mkdir -p $YADA_HOME /var/log/yada

# -m create home directory if not exists (using $YADA_HOME makes things easier)
# -d set home directory 
# -s shell
# group `yada` should be created automatically

useradd -md $YADA_HOME -s /bin/bash yada
```

### Environment Variables

```bash
# build-only properties
#  required for artifact naming and assembly
YADA_SRC=/path/to/local/YADA/repo
YADA_HOME=/opt/yada
YADA_BUILD_PROPS="-Dmaven.javadoc.skip=true -DskipTests=true -Dsurefire.skip=true -Dskip.tests=true -Dskip.war.deploy=trues -Dlog.level=info"
YADA_VERSION=$(awk '/artifactId>YADA/{getline;print;}' ${YADA_SRC}/pom.xml | awk -F'[<>]' '{ print $3 }')
YADA_LOGDIR=/var/log/yada

#if not running as `root`
YADA_USER=yada
YADA_GROUP=yada
```

## Deployment and Launch

### Deployment

<aside>
⚠️ The functions below are defined in `$YADA_SRC/yada-assembly/src/main/resources/conf/deploy.bash`.  This script can be sourced in your profile to make the functions always callable on the command line

</aside>

<aside>
⚠️ Below function assumes execution as a “sudoer”, i.e., `ubuntu` , and not `root` or `$YADA_USER`

</aside>

<aside>
⚠️ Below function assumes maven build has occurred

</aside>

```bash
function yadadeploy {
  # nav to `target` dir
  cd "${YADA_SRC}/yada-assembly/target"

  # copy gzip to `$YADA_HOME`
  DIST=$(ls YADA*tar.gz)
  sudo cp $DIST $YADA_HOME

  # nav to $YADA_HOME
  cd $YADA_HOME

  # decompress and clean
  sudo tar xzf $DIST && sudo rm YADA*tar.gz

  # create symlink to latest version 
  #   (convenient when upgrading, so `serv` always points
  #    to latest version, but not necessary)
  sudo ln -nsf $YADA_VERSION serv

  # change ownership if not running as `root`
  sudo chown -R ${YADA_USER}:${YADA_GROUP} $YADA_HOME
}
```

### Launch

<aside>
⚠️ Assumes launch as “sudoer”, e.g., `ubuntu`, transferring process ownership to `${YADA_USER}`, e.g., `yada`

</aside>

```bash
function yadalaunch {
  # path to `YADA.properties` file, references symlink 
  YADA_PROPS="-DYADA.properties.path=${YADA_HOME}/serv/YADA.properties"
  # path to jarfile, references symlink "serv"
  YADA_JAR="${YADA_HOME}/serv/yada-api-${YADA_VERSION}.jar"

  # current launch log file, assume
  LOG="${YADA_LOGDIR}/yada$(date +%Y%m%d%H%M%S).log"

  # launch command
  sudo -H -u ${YADA_USER} bash -c "java ${YADA_PROPS} -jar ${YADA_JAR} &> ${LOG} &"
}
```