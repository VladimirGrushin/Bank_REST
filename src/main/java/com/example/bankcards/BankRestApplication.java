package com.example.bankcards;

import com.example.bankcards.config.JwtConfig;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtConfig.class)

public class BankRestApplication {
    public static void main(String[] args) {
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            dotenv.entries().forEach(entry ->
                    System.setProperty(entry.getKey(), entry.getValue()));
        } catch (Exception e) {
            System.out.println("Note: .env file not found, using system properties");
        }


        if (dotenv != null) {
            System.setProperty("DATABASE_URL", dotenv.get("DATABASE_URL", "jdbc:postgresql://localhost:5432/bank_cards_db"));
            System.setProperty("DATABASE_USERNAME", dotenv.get("DATABASE_USERNAME", "postgres"));
            System.setProperty("DATABASE_PASSWORD", dotenv.get("DATABASE_PASSWORD", "password"));
        }

        SpringApplication.run(BankRestApplication.class, args);
    }
}
