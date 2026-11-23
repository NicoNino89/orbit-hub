/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Dados fictícios da prova de conceito
 * Objetivo: Fornecer as startups e páginas usadas no teste do catálogo.
 * Decisões: Instrumentos de rodada reais do mercado brasileiro (mútuo conversível, LC 155/2016,
 *           LC 182/2021), para o conteúdo ser verossímil.
 *
 * Startups fictícias da prova de conceito (KAN-58). Nenhum dado é real.
 *
 * Regras de domínio respeitadas no conteúdo:
 * - a plataforma não recomenda investimento nem mostra percentual de compatibilidade;
 * - aparecem só os critérios da tese que combinaram com a startup.
 */

export type TipoPagina = 'capa' | 'negocio' | 'rodada'

export interface Pagina {
  id: string
  startup: string
  tipo: TipoPagina
  titulo: string
}

export interface StartupFicticia {
  slug: string
  nome: string
  setor: string
  cidade: string
  estagio: string
  frase: string
  problema: string
  solucao: string
  tracao: string[]
  rodada: {
    instrumento: string
    alvo: string
    minimo: string
    lider: string
  }
  criteriosQueCombinaram: string[]
}

export const STARTUPS: StartupFicticia[] = [
  {
    slug: 'novasafra',
    nome: 'NovaSafra',
    setor: 'Agtech',
    cidade: 'Londrina, PR',
    estagio: 'Tração inicial',
    frase: 'Sensores de solo acessíveis para quem planta em pequena escala.',
    problema: 'Pequenos produtores perdem parte da safra por falta de monitoramento do solo e decidem a irrigação no olho.',
    solucao: 'Sensores de baixo custo e um aplicativo que avisa quando irrigar e adubar, com dados por talhão.',
    tracao: ['120 propriedades atendidas', 'Receita recorrente mensal em crescimento', 'Parceria com 2 cooperativas'],
    rodada: { instrumento: 'Mútuo conversível', alvo: 'R$ 1.500.000', minimo: 'R$ 25.000', lider: 'Rede de anjos (líder confirmado)' },
    criteriosQueCombinaram: ['Setor: Agtech', 'Estágio: tração inicial', 'Faixa de aporte: R$ 25 mil a R$ 100 mil', 'Região: Sul'],
  },
  {
    slug: 'lumen-saude',
    nome: 'Lumen Saúde',
    setor: 'Healthtech',
    cidade: 'Recife, PE',
    estagio: 'MVP validado',
    frase: 'Triagem de exames de imagem para clínicas do interior.',
    problema: 'Clínicas pequenas esperam dias por um laudo e perdem pacientes para centros maiores.',
    solucao: 'Fila inteligente que prioriza os exames urgentes e conecta a clínica a radiologistas parceiros.',
    tracao: ['14 clínicas em piloto', 'Tempo médio de laudo caiu de 4 dias para 1', 'Equipe com 2 médicos sócios'],
    rodada: { instrumento: 'Investimento-anjo (LC 155/2016)', alvo: 'R$ 800.000', minimo: 'R$ 10.000', lider: 'Em busca de investidor líder' },
    criteriosQueCombinaram: ['Setor: Healthtech', 'Modelo de negócio: B2B', 'Faixa de aporte: R$ 10 mil a R$ 50 mil'],
  },
  {
    slug: 'trama-logistica',
    nome: 'Trama Logística',
    setor: 'Logtech',
    cidade: 'Campinas, SP',
    estagio: 'Escala',
    frase: 'Rotas compartilhadas para entregas de pequenas lojas.',
    problema: 'Lojas de bairro pagam caro por entregas avulsas e não conseguem prometer prazo.',
    solucao: 'Plataforma que junta pedidos de lojas vizinhas na mesma rota e divide o custo do frete.',
    tracao: ['3.400 lojas cadastradas', 'Operação em 5 cidades', 'Margem bruta positiva há 6 meses'],
    rodada: { instrumento: 'Contrato de participação (Marco Legal, LC 182/2021)', alvo: 'R$ 4.000.000', minimo: 'R$ 100.000', lider: 'Fundo de venture capital (líder confirmado)' },
    criteriosQueCombinaram: ['Setor: Logtech', 'Estágio: escala', 'Região: Sudeste'],
  },
]

const TITULOS: Record<TipoPagina, string> = {
  capa: 'Capa',
  negocio: 'Problema e solução',
  rodada: 'Rodada e critérios',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Páginas do catálogo
 * Objetivo: Gerar as três páginas de cada startup, na ordem de leitura.
 * Decisões: Derivadas das startups, para uma startup nova trazer as suas páginas sem cadastro manual.
 */
export const PAGINAS: Pagina[] = STARTUPS.flatMap((s) =>
  (['capa', 'negocio', 'rodada'] as const).map((tipo) => ({
    id: `${s.slug}-${tipo}`,
    startup: s.slug,
    tipo,
    titulo: `${s.nome}: ${TITULOS[tipo]}`,
  })),
)

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Buscar startup pelo slug
 * Objetivo: Encontrar a startup fictícia de uma página.
 * Decisões: Falha com mensagem clara se o slug não existir, em vez de renderizar uma página vazia.
 */
export function startupPorSlug(slug: string): StartupFicticia {
  const s = STARTUPS.find((x) => x.slug === slug)
  if (!s) throw new Error(`Startup fictícia não encontrada: ${slug}`)
  return s
}
