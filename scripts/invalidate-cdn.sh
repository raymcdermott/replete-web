#!/usr/bin/env bash

set -ex

CDN=${CF_DISTRIBUTION_ID:?"Must set CF_DISTRIBUTION_ID"}

JS="/js/compiled*"

ID=$(aws cloudfront create-invalidation --distribution-id ${CDN} --paths ${JS} |
     jq -r '.Invalidation.Id')

STATE=incomplete

until [[ ${STATE} = Completed ]]
do
    STATE=$(aws cloudfront get-invalidation --id ${ID} --distribution-id ${CDN} |
            jq -r '.Invalidation.Status')

    sleep 15
done
