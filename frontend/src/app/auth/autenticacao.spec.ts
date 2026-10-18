import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';

import { ConfigAutenticacao } from '../models/avaliacao.model';
import { definirDonoDosDados, salvarRascunho } from '../utils/armazenamento';
import { autenticacaoInterceptor } from './autenticacao.interceptor';
import { AutenticacaoService, CRIAR_KEYCLOAK } from './autenticacao.service';

const CONFIG: ConfigAutenticacao = {
  habilitada: true, url: 'http://localhost:8081', realm: 'redacao-enem', clientId: 'corretor-frontend',
};

function keycloakFalso() {
  return {
    authenticated: true,
    token: 'token-abc',
    subject: 'usuario-1',
    tokenParsed: { given_name: 'Ana', preferred_username: 'ana' },
    init: vi.fn().mockResolvedValue(true),
    updateToken: vi.fn().mockResolvedValue(false),
    login: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn().mockResolvedValue(undefined),
  };
}

describe('Autenticação com Keycloak', () => {
  let keycloak: ReturnType<typeof keycloakFalso>;
  let criar: ReturnType<typeof vi.fn>;
  let autenticacao: AutenticacaoService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    keycloak = keycloakFalso();
    criar = vi.fn(() => keycloak as unknown as Keycloak);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([autenticacaoInterceptor])),
        provideHttpClientTesting(),
        { provide: CRIAR_KEYCLOAK, useValue: criar },
      ],
    });
    autenticacao = TestBed.inject(AutenticacaoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    definirDonoDosDados('');
    http.verify();
  });

  it('não usa o Keycloak quando o backend está com a autenticação desligada', async () => {
    await autenticacao.iniciar({ ...CONFIG, habilitada: false });
    await autenticacao.iniciar(undefined);

    expect(criar).not.toHaveBeenCalled();
    expect(autenticacao.habilitada()).toBe(false);
  });

  it('exige login com PKCE e mostra o nome do estudante', async () => {
    await autenticacao.iniciar(CONFIG);

    expect(criar).toHaveBeenCalledWith({ url: CONFIG.url, realm: CONFIG.realm, clientId: CONFIG.clientId });
    expect(keycloak.init).toHaveBeenCalledWith(expect.objectContaining({ onLoad: 'login-required', pkceMethod: 'S256' }));
    expect(autenticacao.usuario()).toBe('Ana');
  });

  it('separa os dados salvos de cada estudante', async () => {
    await autenticacao.iniciar(CONFIG);
    salvarRascunho({ tema: 't', texto: 'x' });

    expect(localStorage.getItem('corretor-enem:usuario-1:rascunho')).not.toBeNull();
    expect(localStorage.getItem('corretor-enem:rascunho')).toBeNull();
  });

  it('envia o token nas chamadas da API, mas não na rota pública de status', async () => {
    await autenticacao.iniciar(CONFIG);
    const cliente = TestBed.inject(HttpClient);

    cliente.post('/api/avaliacoes', {}).subscribe();
    // O token é obtido de forma assíncrona, então a requisição sai um pouco depois.
    let req!: ReturnType<HttpTestingController['expectOne']>;
    await vi.waitFor(() => (req = http.expectOne('/api/avaliacoes')));
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-abc');
    expect(keycloak.updateToken).toHaveBeenCalledWith(30);
    req.flush({});

    cliente.get('/api/status').subscribe();
    expect(http.expectOne('/api/status').request.headers.has('Authorization')).toBe(false);
  });

  it('leva ao login quando o backend recusa o token', async () => {
    await autenticacao.iniciar(CONFIG);
    let erro: unknown;

    TestBed.inject(HttpClient).post('/api/avaliacoes', {}).subscribe({ error: e => (erro = e) });
    let req!: ReturnType<HttpTestingController['expectOne']>;
    await vi.waitFor(() => (req = http.expectOne('/api/avaliacoes')));
    req.flush({ mensagem: 'Faça login' }, { status: 401, statusText: 'Unauthorized' });

    expect(keycloak.login).toHaveBeenCalled();
    expect(erro).toBeDefined();
  });

  it('pede novo login quando a sessão expirou e o token não pode ser renovado', async () => {
    await autenticacao.iniciar(CONFIG);
    keycloak.updateToken.mockRejectedValue(new Error('sessão expirada'));

    expect(await autenticacao.token()).toBeNull();
    expect(keycloak.login).toHaveBeenCalled();
  });
});
