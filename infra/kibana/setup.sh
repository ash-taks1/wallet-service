#!/bin/sh
# Runs once after Kibana is up (compose service "kibana-setup"). Safe to run again: fixed ids + overwrite.
#  1. data view "Wallet logs" over the Filebeat indices, time field @timestamp
#  2. makes it the default data view
#  3. saved search "Wallet trace": oldest first, with the columns needed to follow one request
set -e
K=http://kibana:5601
H='-H kbn-xsrf:true -H Content-Type:application/json'

curl -fsS -X POST $K/api/data_views/data_view $H -d '{
  "override": true,
  "data_view": {"id": "wallet-logs", "name": "Wallet logs", "title": "filebeat-*", "timeFieldName": "@timestamp"}
}' > /dev/null

curl -fsS -X POST $K/api/data_views/default $H -d '{"data_view_id": "wallet-logs", "force": true}' > /dev/null

curl -fsS -X POST "$K/api/saved_objects/search/wallet-trace?overwrite=true" $H -d '{
  "attributes": {
    "title": "Wallet trace",
    "columns": ["service.name", "log.level", "event.action", "message", "traceId"],
    "sort": [["@timestamp", "asc"], ["log.offset", "asc"]],
    "kibanaSavedObjectMeta": {
      "searchSourceJSON": "{\"query\":{\"query\":\"\",\"language\":\"kuery\"},\"filter\":[],\"indexRefName\":\"kibanaSavedObjectMeta.searchSourceJSON.index\"}"
    }
  },
  "references": [
    {"name": "kibanaSavedObjectMeta.searchSourceJSON.index", "type": "index-pattern", "id": "wallet-logs"}
  ]
}' > /dev/null

echo "Kibana is ready: data view 'Wallet logs' and saved search 'Wallet trace' (http://localhost:5602)"
