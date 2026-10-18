import { Routes } from '@angular/router';

import { Corrigir } from './pages/corrigir/corrigir';

export const routes: Routes = [
  { path: '', component: Corrigir, title: 'Corretor de Redação ENEM' },
  {
    path: 'desempenho',
    loadComponent: () => import('./pages/painel/painel').then(m => m.Painel),
    title: 'Meu desempenho · Corretor de Redação ENEM',
  },
  {
    path: 'redacoes/:id',
    loadComponent: () => import('./pages/redacao/redacao').then(m => m.RedacaoPagina),
    title: 'Redação corrigida · Corretor de Redação ENEM',
  },
  { path: '**', redirectTo: '' },
];
