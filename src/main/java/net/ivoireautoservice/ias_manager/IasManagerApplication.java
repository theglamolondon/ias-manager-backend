package net.ivoireautoservice.ias_manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IasManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IasManagerApplication.class, args);
    }

}
