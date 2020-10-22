#!/bin/sh

# This script must be run from the 'server' directory
export OVSX_APP_PROFILE=$PWD/src/dev/resources/application-ovsx.properties

# Clear the content of the ovsx application profile
echo "# Generated by scripts/generate-properties.sh" > $OVSX_APP_PROFILE

# Set the Elasticsearch host
echo "ovsx.elasticsearch.host=localhost:9200" >> $OVSX_APP_PROFILE

# Set the web UI URL
if command -v gp > /dev/null
then
    echo "Using web frontend in Gitpod: `gp url 3000`"
    echo "ovsx.webui.url=`gp url 3000`" >> $OVSX_APP_PROFILE
else
    echo "Using web frontend on local machine: http://localhost:3000"
    echo "ovsx.webui.url=http://localhost:3000" >> $OVSX_APP_PROFILE
fi

# Set the GitHub OAuth client id and client secret
echo "spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID:-none}" >> $OVSX_APP_PROFILE
echo "spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET:-none}" >> $OVSX_APP_PROFILE
if [ -n "$GITHUB_CLIENT_ID" ] && [ -n "$GITHUB_CLIENT_SECRET" ]
then
    echo "GitHub OAuth is enabled."
fi

# Set the Eclipse OAuth client id and client secret
echo "spring.security.oauth2.client.registration.eclipse.client-id=${ECLIPSE_CLIENT_ID:-none}" >> $OVSX_APP_PROFILE
echo "spring.security.oauth2.client.registration.eclipse.client-secret=${ECLIPSE_CLIENT_SECRET:-none}" >> $OVSX_APP_PROFILE
if [ -n "$ECLIPSE_CLIENT_ID" ] && [ -n "$ECLIPSE_CLIENT_SECRET" ]
then
    echo "ovsx.eclipse.publisher-agreement.version=1" >> $OVSX_APP_PROFILE
    echo "Eclipse OAuth is enabled."
fi

# Set the Google Cloud Storage project id and bucket id
if [ -n "$GCP_PROJECT_ID" ] && [ -n "$GCS_BUCKET_ID" ]
then
    echo "ovsx.storage.gcp.project-id=$GCP_PROJECT_ID" >> $OVSX_APP_PROFILE
    echo "ovsx.storage.gcp.bucket-id=$GCS_BUCKET_ID" >> $OVSX_APP_PROFILE
    echo "Using Google Cloud Storage: https://storage.googleapis.com/$GCS_BUCKET_ID/"
fi
