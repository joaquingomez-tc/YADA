#!/bin/bash

#####
#
#  deploy.bash
#  
#  Source this script in profile to enable command line execution of functions
#
#####

#####
# Below function assumes execution as a “sudoer”, i.e., ubuntu , and not root or $YADA_USER
#####

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

#####
# Assumes launch as “sudoer”, e.g., ubuntu, transferring process ownership to ${YADA_USER}, e.g., yada 
#####

function yadalaunch {
  # path to `YADA.properties` file
  YADA_PROPS="-DYADA.properties.path=${YADA_HOME}/serv/YADA.properties"

  # current launch log file
  LOG=/var/log/yada/yada$(date +%Y%m%d%H%M%S).log

  # launch command
  sudo -H -u ${YADA_USER} bash -c "java ${YADA_PROPS} -jar ${YADA_HOME}/serv/yada-api-${YADA_VERSION}.jar &> ${LOG} &"
}