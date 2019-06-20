#!/usr/bin/env bash
# curl -F"file=@./mvnw" http://localhost:8080/s3
curl -F"file=@/Users/joshlong/Desktop/pkg.zip" -XPOST "http://service-spontaneous-dingo.cfapps.io/production?uid=13232"