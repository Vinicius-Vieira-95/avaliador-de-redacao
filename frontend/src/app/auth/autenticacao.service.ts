import { inject, Injectable, InjectionToken, signal } from '@angular/core';
import Keycloak, { KeycloakConfig } from 'keycloak-js';

import { ConfigAutenticacao } from '../models/avaliacao.model';
import { definirDonoDosDados } from '../utils/armazenamento';

/** Cria o cliente do Keycloak; substituído nos testes por um falso. */
export const CRIAR_KEYCLOAK = new InjectionToken<(config: KeycloakConfig) => Keycloak>('CRIAR_KEYCLOAK', {
  providedIn: 'root',
  factory: () => config => new Keycloak(config),
});

/** Segundos de folga: o token é renovado se for expirar antes disso. */
const FOLGA_RENOVACAO = 30;

/**
 * Login com o Keycloak (OpenID Connect, fluxo de autorização com PKCE).
 * Se o backend estiver com a autenticação desligada, nada é feito e a API é chamada sem token.
 */
@Injectable({ providedIn: 'root' })
export class AutenticacaoService {
  private readonly criarKeycloak = inject(CRIAR_KEYCLOAK);
  private keycloak?: Keycloak;

  readonly habilitada = signal(false);
  /** Nome exibido no topo da página; nulo enquanto ninguém entrou. */
  readonly usuario = signal<string | null>(null);

  /** Chamado antes de a aplicação aparecer: quem não entrou é levado à tela de login do Keycloak. */
  async iniciar(config: ConfigAutenticacao | undefined): Promise<void> {
    if (!config?.habilitada) return;

    this.keycloak = this.criarKeycloak({ url: config.url, realm: config.realm, clientId: config.clientId });
    this.habilitada.set(true);
    this.keycloak.onAuthLogout = () => this.usuario.set(null);

    const autenticado = await this.keycloak.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
    if (autenticado) {
      definirDonoDosDados(this.keycloak.subject ?? '');
      this.usuario.set(this.nomeDoUsuario());
    }
  }

  /** Token de acesso válido para enviar à API, renovado se estiver perto de expirar. */
  async token(): Promise<string | null> {
    if (!this.keycloak?.authenticated) return null;
    try {
      await this.keycloak.updateToken(FOLGA_RENOVACAO);
      return this.keycloak.token ?? null;
    } catch {
      // A sessão no Keycloak acabou: só um novo login resolve.
      await this.entrar();
      return null;
    }
  }

  async entrar(): Promise<void> {
    await this.keycloak?.login();
  }

  async sair(): Promise<void> {
    await this.keycloak?.logout({ redirectUri: window.location.origin });
  }

  private nomeDoUsuario(): string {
    const dados = this.keycloak?.tokenParsed as { given_name?: string; preferred_username?: string } | undefined;
    return dados?.given_name || dados?.preferred_username || 'estudante';
  }
}
