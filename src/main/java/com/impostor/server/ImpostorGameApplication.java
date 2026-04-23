package com.impostor.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.impostor")
public class ImpostorGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImpostorGameApplication.class, args);
    }

    @Bean
    CommandLineRunner startSocketServer() {
        return args -> {
            String port = args.length > 0 ? args[0] : "5555";
            String players = args.length > 1 ? args[1] : "4";
            String impostors = args.length > 2 ? args[2] : "1";
            String hint = args.length > 3 ? args[3] : "true";

            Thread serverThread = new Thread(
                    () -> GameServer.main(new String[] { port, players, impostors, hint }),
                    "game-server-thread");
            serverThread.setDaemon(false);
            serverThread.start();
        };
    }
}
