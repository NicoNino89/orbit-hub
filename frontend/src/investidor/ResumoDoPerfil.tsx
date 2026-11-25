import { useEffect, useState } from 'react'
import { opcoesDaLista } from '../api/organizacoes'
import {
  type DadosDoPerfil, NOMES_DA_LIDERANCA, NOMES_DA_MATURIDADE, NOMES_DA_PARTICIPACAO,
} from '../api/perfilDeInvestimento'

const LISTAS = ['TIPO_INVESTIDOR', 'INSTRUMENTO', 'SEGMENTO', 'ESTAGIO_RODADA', 'LOCALIZACAO', 'MODELO_NEGOCIO',
  'MODALIDADE_INVESTIMENTO', 'HORIZONTE_INVESTIMENTO', 'PERFIL_RISCO', 'EXPERIENCIA_INVESTIDOR', 'INTERESSE_ESTRATEGICO']

const REAIS = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:06
 * Cartão Jira: KAN-20
 * Título: Resumo do perfil de investimento
 * Objetivo: Mostrar, em leitura, o perfil atual: tipo, ticket, liderança, gestão, instrumentos, tese e preferências.
 * Decisões: Os rótulos vêm das listas públicas (o que o administrador renomear aparece aqui); campo vazio não aparece,
 *           exceto a tese, que é obrigatória. Ticket em reais, sem centavos.
 */
export function ResumoDoPerfil({ perfil }: { perfil: DadosDoPerfil }) {
  const [rotulos, setRotulos] = useState<Record<string, Record<string, string>>>({})

  useEffect(() => {
    let ativo = true
    for (const lista of LISTAS) {
      opcoesDaLista(lista)
        .then((r) => { if (ativo) setRotulos((atual) => ({ ...atual, [lista]: Object.fromEntries(r.map((o) => [o.codigo, o.rotulo])) })) })
        .catch(() => undefined)
    }
    return () => { ativo = false }
  }, [])

  const nome = (lista: string, codigo: string | null | undefined) => (codigo ? rotulos[lista]?.[codigo] ?? codigo : '')
  const nomes = (lista: string, codigos: string[] | undefined) => (codigos ?? []).map((c) => nome(lista, c)).join(', ')
  const comp = perfil.complementares
  const linhas: [string, string][] = [
    ['Tipo de investidor', perfil.tipo === 'OUTROS' && perfil.tipoOutro ? perfil.tipoOutro : nome('TIPO_INVESTIDOR', perfil.tipo)],
    ['Ticket', `${REAIS.format(perfil.ticketMinimo)} a ${REAIS.format(perfil.ticketMaximo)}`],
    ['Liderança', NOMES_DA_LIDERANCA[perfil.lideranca]],
    ['Follow-on', perfil.followOn ? 'Reserva capital para as próximas rodadas' : 'Não reserva capital'],
    ['Participação na gestão', NOMES_DA_PARTICIPACAO[perfil.participacaoNaGestao]],
    ['Instrumentos', nomes('INSTRUMENTO', perfil.instrumentos)],
    ['Segmentos', nomes('SEGMENTO', perfil.segmentos)],
    ['Estágios da rodada', nomes('ESTAGIO_RODADA', perfil.estagios)],
    ['Maturidade do negócio', (perfil.maturidades ?? []).map((m) => NOMES_DA_MATURIDADE[m]).join(', ') || 'Qualquer'],
    ['Localização', nomes('LOCALIZACAO', perfil.localizacoes)],
    ['Modelos de negócio', nomes('MODELO_NEGOCIO', comp?.modelos)],
    ['Modalidade', nomes('MODALIDADE_INVESTIMENTO', comp?.modalidades)],
    ['Horizonte', nome('HORIZONTE_INVESTIMENTO', comp?.horizonte)],
    ['Perfil de risco', nome('PERFIL_RISCO', comp?.perfilRisco)],
    ['Experiência', nome('EXPERIENCIA_INVESTIDOR', comp?.experiencia)],
    ['Interesses estratégicos', nomes('INTERESSE_ESTRATEGICO', comp?.interesses)],
  ]

  return (
    <dl aria-label="Resumo do perfil de investimento" className="grid gap-x-6 gap-y-2 rounded-lg border border-line bg-surface p-5 text-sm sm:grid-cols-[max-content_1fr]">
      {linhas.filter(([, valor]) => valor).map(([rotulo, valor]) => (
        <div key={rotulo} className="contents">
          <dt className="font-semibold">{rotulo}</dt>
          <dd className="text-muted">{valor}</dd>
        </div>
      ))}
    </dl>
  )
}
