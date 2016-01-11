#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v21 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

function strongEcho {
  echo ""
  echo "================ $1 ================="
}

case "$TARGET" in

CI)
  if [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ "${TRAVIS_BRANCH}" == "master" ] && [ -n "$GITHUB_TOKEN" ]; then
    # For security reasons environment variables are not available on the pull requests
    # coming from outside repositories
    # http://docs.travis-ci.com/user/pull-requests/#Security-Restrictions-when-testing-Pull-Requests
    # That's why the analysis does not need to be executed if the variable GITHUB_TOKEN is not defined.

    strongEcho 'Build and analyze pull request'
    installTravisTools
    build_snapshot "mojohaus/sonar-maven-plugin"

    # this pull request must be built and analyzed (without upload of report)
    mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify org.codehaus.mojo:sonar-maven-plugin:2.8-SNAPSHOT:sonar \
       -Panalysis -Dclirr=true -B -e -V \
       -Dsonar.analysis.mode=issues \
       -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
       -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
       -Dsonar.github.oauth=$GITHUB_TOKEN \
       -Dsonar.host.url=$SONAR_HOST_URL \
       -Dsonar.login=$SONAR_TOKEN


  else
    strongEcho 'Build, no analysis'
    # Build branch, without any analysis

    # No need for Maven goal "install" as the generated JAR file does not need to be installed
    # in Maven local repository
    mvn verify -B -e -V
  fi
  ;;

POSTGRES)
  installTravisTools

  psql -c 'create database sonar;' -U postgres

  runDatabaseCI "postgresql" "jdbc:postgresql://localhost/sonar" "postgres" ""
  ;;

MYSQL)
  installTravisTools

  mysql -e "CREATE DATABASE sonar CHARACTER SET UTF8;" -uroot
  mysql -e "CREATE USER 'sonar'@'localhost' IDENTIFIED BY 'sonar';" -uroot
  mysql -e "GRANT ALL ON sonar.* TO 'sonar'@'localhost';" -uroot
  mysql -e "FLUSH PRIVILEGES;" -uroot

  runDatabaseCI "mysql" "jdbc:mysql://localhost/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance" "sonar" "sonar"
  ;;

WEB)
  set +eu
  source ~/.nvm/nvm.sh && nvm install 4
  npm install -g npm@3.5.2
  cd server/sonar-web && npm install && npm test
  ;;

IT)
  if [ "$IT_CATEGORY" == "Plugins" ] && [ ! -n "$GITHUB_TOKEN" ]; then
    echo "This job is ignored as it needs to access a private GitHub repository"
  else
    installTravisTools

    start_xvfb

    mvn install -Pit,dev -DskipTests -Dcategory=$IT_CATEGORY -Dmaven.test.redirectTestOutputToFile=false -e -Dsource.skip=true
  fi
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac
