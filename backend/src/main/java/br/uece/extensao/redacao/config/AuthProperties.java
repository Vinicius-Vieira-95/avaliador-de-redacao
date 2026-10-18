package br.uece.extensao.redacao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dados do Keycloak. Também são repassados ao front por {@code GET /api/status},
 * para que a configuração do login fique num lugar só.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(boolean habilitada, String keycloakUrl, String realm, String clientId) {
}
