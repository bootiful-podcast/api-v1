#!/usr/bin/env bash
source "$(cd $(dirname $0) && pwd)/env.sh"
##
## S3 buckets
BUCKET_SUFFIX="-development"
if [[ "$BP_MODE" = "production" ]]; then
    BUCKET_SUFFIX=""
fi
export PODCAST_PIPELINE_S3_INPUT_BUCKET_NAME=podcast-input-bucket${BUCKET_SUFFIX}
export PODCAST_PIPELINE_S3_OUTPUT_BUCKET_NAME=podcast-output-bucket${BUCKET_SUFFIX}




#####
export BP_MODE="development"
if [ "$GITHUB_EVENT_NAME" = "create" ]; then
  if [[ "${GITHUB_REF}" =~ "tags" ]]; then
    BP_MODE="production"
  fi
fi

echo "BP_MODE=${BP_MODE}"

VARIABLE_NAMES=(AWS_REGION PODCAST_RMQ_ADDRESS PODBEAN_CLIENT_ID PODBEAN_CLIENT_SECRET)
for V in ${VARIABLE_NAMES[*]}; do
  TO_EVAL="export ${V}=\$${V}_${BP_MODE}"
  echo $TO_EVAL
  eval $TO_EVAL
done
#####

mvn -Dspring.profiles.active=ci verify deploy || die "could not build and deploy the artifact to Artifactory."

APP_NAME=api
if [[ "$BP_MODE" = "development" ]]; then
    APP_NAME=${APP_NAME}_${BP_MODE}
fi

cf push -k 2GB -m 2GB -b java_buildpack --no-start -p target/api-0.0.1-SNAPSHOT.jar ${APP_NAME}

cf set-env $APP_NAME JBP_CONFIG_OPEN_JDK_JRE '{ jre: { version: 11.+}}'
cf set-env $APP_NAME BP_MODE $BP_MODE

cf set-env $APP_NAME SPRING_PROFILES_ACTIVE cloud

##
## CloudFoundry
cf set-env $APP_NAME CF_API $CF_API
cf set-env $APP_NAME CF_API_ENDPOINT $CF_API_ENDPOINT
cf set-env $APP_NAME CF_ORG $CF_ORG
cf set-env $APP_NAME CF_PASSWORD $CF_PASSWORD
cf set-env $APP_NAME CF_SPACE $CF_SPACE
cf set-env $APP_NAME CF_USER $CF_USER
##
## RabbitMQ
#cf set-env $APP_NAME RMQ_ADDRESS $RMQ_ADDRESS
cf set-env $APP_NAME PODCAST_RMQ_ADDRESS $PODCAST_RMQ_ADDRESS
##
## Sendgrid
cf set-env $APP_NAME SENDGRID_API_KEY $SENDGRID_API_KEY
##
## Podbean
cf set-env $APP_NAME PODBEAN_CLIENT_ID $PODBEAN_CLIENT_ID
cf set-env $APP_NAME PODBEAN_CLIENT_SECRET $PODBEAN_CLIENT_SECRET
##
## AWS
cf set-env $APP_NAME AWS_SECRET_ACCESS_KEY $AWS_SECRET_ACCESS_KEY
cf set-env $APP_NAME AWS_REGION $AWS_REGION
cf set-env $APP_NAME AWS_ACCESS_KEY_ID $AWS_ACCESS_KEY_ID
##
## S3 Buckets
cf set-env $APP_NAME PODCAST_PIPELINE_S3_INPUT_BUCKET_NAME $PODCAST_PIPELINE_S3_INPUT_BUCKET_NAME
cf set-env $APP_NAME PODCAST_PIPELINE_S3_OUTPUT_BUCKET_NAME $PODCAST_PIPELINE_S3_OUTPUT_BUCKET_NAME
##
## We need to correctly bind either the DEV or the PROD PWS services
SVC_SUFFIX=""
if [[  "$BP_MODE" = "development"  ]]; then
 SVC_SUFFIX="-dev"
fi
DB_SVC_NAME=bootiful-podcast-db${SVC_SUFFIX}
MQ_SVC_NAME=bootiful-podcast-mq${SVC_SUFFIX}
cf bs ${APP_NAME} ${MQ_SVC_NAME}
cf bs ${APP_NAME} ${DB_SVC_NAME}

cf restart $APP_NAME

