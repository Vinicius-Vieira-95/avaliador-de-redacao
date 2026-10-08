package br.uece.extensao.redacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AvaliadorRedacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvaliadorRedacaoApplication.class, args);
    }
}
