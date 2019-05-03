#!/usr/bin/env bash

APP_NAME=rmq-integration

cf push -b java_buildpack --no-start -p target/integration-0.0.1-SNAPSHOT.jar ${APP_NAME}

$ cf set-env ${APP_NAME} JBP_CONFIG_OPEN_JDK_JRE '{ jre: { version: 11.+ }, memory_calculator: { stack_threads: 25 } }'

cf set-env $APP_NAME CF_API $CF_API
cf set-env $APP_NAME CF_API_ENDPOINT $CF_API_ENDPOINT
cf set-env $APP_NAME CF_ORG $CF_ORG
cf set-env $APP_NAME CF_PASSWORD $CF_PASSWORD
cf set-env $APP_NAME CF_SPACE $CF_SPACE
cf set-env $APP_NAME CF_USER $CF_USER

cf restart $APP_NAME

