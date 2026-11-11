package ucr.ac.cr.medivault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MedivaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedivaultApplication.class, args);
    }
}