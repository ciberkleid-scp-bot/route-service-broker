package org.springframework.cloud.sample.routeservice.servicebroker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@AllArgsConstructor
@NoArgsConstructor
public class ServiceInstance {

    @Id
    @Getter
    private String serviceInstanceId;
    @Getter
    private String logLevel = "INFO";
    @Getter
    private Integer replenishRate = null;
    @Getter
    private Integer burstCapacity = null;

    public static ServiceInstanceBuilder builder() {
        return new ServiceInstanceBuilder();
    }

    @Override
    public String toString() {
        return "ServiceInstance{" +
                "serviceInstanceId='" + serviceInstanceId + '\'' +
                ", logLevel='" + logLevel + '\'' +
                ", replenishRate='" + replenishRate + '\'' +
                ", burstCapacity='" + burstCapacity + '\'' +
                '}';
    }

    @NoArgsConstructor
    static class ServiceInstanceBuilder {

        private String serviceInstanceId;
        private String logLevel;
        private Integer replenishRate;
        private Integer burstCapacity;

        ServiceInstanceBuilder serviceInstanceId(String serviceInstanceId) {
            this.serviceInstanceId = serviceInstanceId;
            return this;
        }

        ServiceInstanceBuilder logLevel(String logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        ServiceInstanceBuilder replenishRate(Integer replenishRate) {
            this.replenishRate = replenishRate;
            return this;
        }

        ServiceInstanceBuilder burstCapacity(Integer burstCapacity) {
            this.burstCapacity = burstCapacity;
            return this;
        }

        ServiceInstance build() {
            return new ServiceInstance(serviceInstanceId, logLevel, replenishRate, burstCapacity);
        }

    }

}

