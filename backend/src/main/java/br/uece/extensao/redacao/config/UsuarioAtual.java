package br.uece.extensao.redacao.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Quem está fazendo a requisição, para separar o histórico de cada estudante. */
public final class UsuarioAtual {

    /** Dono das redações quando a autenticação está desligada ({@code AUTH_HABILITADA=false}). */
    public static final String ANONIMO = "anonimo";

    private UsuarioAtual() {
    }

    /** O {@code sub} do token do Keycloak, que não muda mesmo se o estudante trocar nome ou e-mail. */
    public static String id() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken token && token.getToken().getSubject() != null) {
            return token.getToken().getSubject();
        }
        return ANONIMO;
    }
}
