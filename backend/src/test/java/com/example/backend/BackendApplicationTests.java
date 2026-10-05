package com.example.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires running DB and Redis — run separately with full infrastructure")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
