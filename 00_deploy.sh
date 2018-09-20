# CLEANUP

cf urs cfapps.io cyi-gateway-trial --hostname cool-app-trial -f
cf unmap-route cool-app cfapps.io --hostname cool-app-trial
cf ds cyi-gateway-trial -f

cf urs cfapps.io cyi-gateway-standard --hostname cool-app-standard -f
cf unmap-route cool-app cfapps.io --hostname cool-app-standard
cf ds cyi-gateway-standard -f

cf urs cfapps.io cyi-gateway-premium --hostname cool-app-premium -f
cf unmap-route cool-app cfapps.io --hostname cool-app-premium
cf ds cyi-gateway-premium -f

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

cf cs route-service trial cyi-gateway-trial 
cf map-route cool-app cfapps.io --hostname cool-app-trial
cf brs cfapps.io cyi-gateway-trial --hostname cool-app-trial

cf cs route-service standard cyi-gateway-standard
cf map-route cool-app cfapps.io --hostname cool-app-standard
cf brs cfapps.io cyi-gateway-standard --hostname cool-app-standard

cf cs route-service premium cyi-gateway-premium
cf map-route cool-app cfapps.io --hostname cool-app-premium
cf brs cfapps.io cyi-gateway-premium --hostname cool-app-premium
