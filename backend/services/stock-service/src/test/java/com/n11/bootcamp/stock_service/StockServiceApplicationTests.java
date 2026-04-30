package com.n11.bootcamp.stock_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16:///stock_db",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers:localhost:9092}",
        "spring.sql.init.mode=never",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
class StockServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
