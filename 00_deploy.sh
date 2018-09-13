# CLEANUP

cf urs cfapps.io my-gateway-basic --hostname cool-app-basic -f
cf unmap-route cool-app cfapps.io --hostname cool-app-basic
cf ds my-gateway-basic -f

cf urs cfapps.io my-gateway-premium --hostname cool-app-premium -f
cf unmap-route cool-app cfapps.io --hostname cool-app-premium
cf ds my-gateway-premium -f

cf delete-service-broker route-service -f

#cf d route-service-broker -f
#cf ds route-service-mongodb -f
#cf ds route-service-redis -f

cf delete-orphaned-routes -f

# DEPLOY

#cd ~/workspace/route-service-broker-thumbnail
#cf push

cd ~/workspace/route-service-broker
./gradlew assemble
#cf cs mlab sandbox route-service-mongodb
#cf cs rediscloud 30mb route-service-redis
cf push

# RECONFIGURE

cf create-service-broker route-service admin supersecret https://route-service-broker.cfapps.io --space-scoped

cf cs route-service standard my-gateway-basic  -c '{"log-level": "DEBUG", "replenish-rate": 3, "burst-capacity": 3}'
cf map-route cool-app cfapps.io --hostname cool-app-basic
cf brs cfapps.io my-gateway-basic --hostname cool-app-basic

cf cs route-service standard my-gateway-premium  -c '{"log-level": "DEBUG", "replenish-rate": 10, "burst-capacity": 10}'
cf map-route cool-app cfapps.io --hostname cool-app-premium
cf brs cfapps.io my-gateway-premium --hostname cool-app-premium
