package br.uece.extensao.redacao.config;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * A API é um resource server OAuth2: o front obtém o token no Keycloak e o envia em
 * {@code Authorization: Bearer ...}; aqui só se valida o JWT, sem sessão no servidor.
 * {@code GET /api/status} fica público porque o front precisa dele para saber como fazer login.
 */
@Configuration
public class SegurancaConfig {

    private static final String MENSAGEM_NAO_AUTENTICADO =
            "{\"mensagem\":\"Sua sessão expirou ou você não entrou. Faça login para avaliar a redação.\"}";

    @Bean
    SecurityFilterChain filtroDeSeguranca(HttpSecurity http, AuthProperties auth) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!auth.habilitada()) {
            http.authorizeHttpRequests(regras -> regras.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(regras -> regras
                        .requestMatchers(HttpMethod.GET, "/api/status").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(naoAutenticado()));
        return http.build();
    }

    /** Responde 401 no mesmo formato {@code { "mensagem": ... }} dos demais erros da API. */
    private static AuthenticationEntryPoint naoAutenticado() {
        return (requisicao, resposta, erro) -> {
            resposta.setStatus(HttpStatus.UNAUTHORIZED.value());
            resposta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resposta.getWriter().write(MENSAGEM_NAO_AUTENTICADO);
        };
    }
}
