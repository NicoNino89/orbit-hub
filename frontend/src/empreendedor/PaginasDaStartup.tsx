import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import {
  CADASTRO_VAZIO, criarStartup, lerLogotipo, lerStartup, NOMES_DA_SITUACAO_DA_STARTUP, NOMES_DO_PAPEL_NA_STARTUP,
  NOMES_DAS_SECOES, podeEditar, salvarCadastro, type SecaoDaStartup, type Startup, trocarLogotipo,
} from '../api/startups'
import { revisoesDaStartup, type RevisaoParaEquipe } from '../api/catalogo'
import { progressoDaStartup, type ProgressoDaStartup } from '../api/publicacao'
import { type CampoDoCatalogo, catalogoDeCampos, salvarSecao, type ValoresDasSecoes, valoresDasSecoes } from '../api/secoes'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_LINK } from '../ui/estilos'
import { EquipeDaStartup } from './EquipeDaStartup'
import { FormularioCadastral } from './FormularioCadastral'
import { FormularioDaSecao } from './FormularioDaSecao'
import { PainelDeIndicadores } from './PainelDeIndicadores'
import { PainelDePublicacao } from './PainelDePublicacao'
import { PainelDeRodada } from './PainelDeRodada'
import { PainelDeVisibilidade } from './PainelDeVisibilidade'
import { ConexoesDaStartup } from '../conexoes/Conexoes'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Nova startup
 * Objetivo: Cadastrar a startup com os dados básicos e abrir o editor.
 * Decisões: Só nome, segmento e localização são obrigatórios; o resto pode vir depois.
 */
export function PaginaNovaStartup() {
  const navegar = useNavigate()
  const [erro, setErro] = useState<string | null>(null)
  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Cadastrar startup</h1>
        <p className="text-sm text-muted">
          Comece pelos dados básicos. Você vira fundador administrador e pode completar o cadastro em etapas, salvando como
          rascunho.
        </p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <FormularioCadastral inicial={CADASTRO_VAZIO} rotuloDoBotao="Criar startup"
        aoSalvar={async (dados) => {
          try {
            const criada = await criarStartup(dados)
            void navegar(`/empreendedor/startups/${criada.id}`)
          } catch (e) {
            setErro(e instanceof Error ? e.message : 'Não foi possível criar.')
          }
        }} />
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Editor da startup
 * Objetivo: Mostrar a startup com a situação, o selo de empresa não constituída, o logotipo e a etapa dos dados
 *           cadastrais.
 * Decisões: Navegação por etapas: dados cadastrais, as seções que o catálogo do servidor descreve (KAN-22 em diante),
 *           a equipe (KAN-65), a rodada (KAN-59) e a publicação, com a barra de progresso dos obrigatórios e a marca nas
 *           etapas completas (KAN-27).
 *           Colaborador vê bloqueadas as seções que não foram autorizadas a ele. Etapa Visibilidade com a pré-visualização
 *           (KAN-28/29) e os comentários da curadoria na seção devolvida (KAN-84), em 25/11/2026.
 */
export function PaginaStartup() {
  const { id = '' } = useParams()
  const [startup, setStartup] = useState<Startup | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [logo, setLogo] = useState<string | null>(null)
  const [aba, setAba] = useState<'cadastro' | 'equipe' | 'publicacao' | 'rodada' | 'visibilidade' | 'conexoes' | SecaoDaStartup>('cadastro')
  const [progresso, setProgresso] = useState<ProgressoDaStartup | null>(null)
  const [revisoes, setRevisoes] = useState<RevisaoParaEquipe[]>([])
  const [versao, setVersao] = useState(0)
  const recarregar = () => setVersao((v) => v + 1)

  useEffect(() => {
    let ativo = true
    progressoDaStartup(id).then((p) => { if (ativo) setProgresso(p) }).catch(() => undefined)
    revisoesDaStartup(id).then((r) => { if (ativo) setRevisoes(r) }).catch(() => undefined)
    return () => { ativo = false }
  }, [id, versao])
  const [catalogo, setCatalogo] = useState<CampoDoCatalogo[]>([])
  const [valores, setValores] = useState<ValoresDasSecoes | null>(null)

  useEffect(() => {
    let ativo = true
    catalogoDeCampos().then((c) => { if (ativo) setCatalogo(c) }).catch(() => undefined)
    valoresDasSecoes(id).then((v) => { if (ativo) setValores(v) }).catch(() => undefined)
    return () => { ativo = false }
  }, [id])

  useEffect(() => {
    let ativo = true
    lerStartup(id)
      .then((s) => { if (ativo) setStartup(s) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [id])

  useEffect(() => {
    if (!startup?.temLogotipo) return
    let endereco: string | null = null
    let ativo = true
    lerLogotipo(id).then((arquivo) => {
      if (!ativo) return
      endereco = URL.createObjectURL(arquivo)
      setLogo(endereco)
    }).catch(() => undefined)
    return () => {
      ativo = false
      if (endereco) URL.revokeObjectURL(endereco)
    }
  }, [id, startup?.temLogotipo, startup?.atualizadaEm])

  if (!startup) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  const bloqueado = !podeEditar(startup, 'CADASTRO')
  const secoesComCampos = [...new Set(catalogo.map((c) => c.secao))]
  const etapas: [typeof aba, string][] = [
    ['cadastro', 'Dados cadastrais'],
    ...secoesComCampos.map((secao): [SecaoDaStartup, string] => [secao, NOMES_DAS_SECOES[secao]]),
    ['equipe', 'Equipe'],
    ['rodada', 'Rodada'],
    ['visibilidade', 'Visibilidade'],
    ['publicacao', 'Publicação'],
    ['conexoes', 'Conexões'],
  ]
  const secaoAberta = aba === 'cadastro' || aba === 'equipe' || aba === 'publicacao' || aba === 'rodada' || aba === 'visibilidade' || aba === 'conexoes' ? null : aba
  const devolvida = revisoes[0]?.situacao === 'DEVOLVIDA' ? revisoes[0] : null
  const apontamentoDaSecao = (secao: string) => devolvida?.apontamentos.find((a) => a.tipo === 'SECAO' && a.codigo === secao)
  const completa = (chave: typeof aba) => progresso?.secoes.find((s) => (chave === 'cadastro' ? s.secao === 'CADASTRO' : s.secao === chave))?.completa ?? false

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-wrap items-center gap-4">
        {logo ? <img src={logo} alt={`Logotipo de ${startup.nome}`} className="h-16 w-16 rounded-lg border border-line object-contain" />
          : <div aria-hidden="true" className="flex h-16 w-16 items-center justify-center rounded-lg border border-line bg-surface-2 text-2xl font-bold">{startup.nome[0]}</div>}
        <div className="flex flex-col gap-1">
          <Link to="/empreendedor" className={`${CLASSE_LINK} text-sm`}>← Minhas startups</Link>
          <h1 className="font-display text-3xl font-semibold tracking-tight">{startup.nome}</h1>
          <p className="flex flex-wrap items-center gap-2 text-sm text-muted">
            <span className="rounded bg-surface-2 px-2 py-0.5 text-xs font-semibold">{NOMES_DA_SITUACAO_DA_STARTUP[startup.situacao]}</span>
            {!startup.constituida && <span className="rounded bg-warn-soft px-2 py-0.5 text-xs font-semibold text-warn">Empresa ainda não constituída</span>}
            Você é {NOMES_DO_PAPEL_NA_STARTUP[startup.meuPapel].toLowerCase()}
          </p>
        </div>
      </header>
      {progresso && (
        <div className="flex flex-col gap-1 text-sm">
          <div className="flex justify-between"><span>Obrigatórios para publicar</span><span>{progresso.percentual}%</span></div>
          <div role="progressbar" aria-label="Progresso do cadastro" aria-valuenow={progresso.percentual} aria-valuemin={0} aria-valuemax={100}
            className="h-2 overflow-hidden rounded-full bg-surface-2">
            <div className="h-full bg-primary" style={{ width: `${progresso.percentual}%` }} />
          </div>
        </div>
      )}
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}

      <nav aria-label="Etapas do cadastro" className="flex flex-wrap gap-2 text-sm">
        {etapas.map(([chave, nome]) => (
          <button key={chave} type="button" onClick={() => setAba(chave)} aria-current={aba === chave ? 'step' : undefined}
            className={aba === chave ? 'rounded-md bg-primary px-3 py-1 font-semibold text-on-primary' : 'rounded-md border border-line px-3 py-1'}>
            {nome}{completa(chave) && <span aria-hidden="true"> ✓</span>}
          </button>
        ))}
      </nav>

      {aba === 'rodada' && <PainelDeRodada startupId={id} admin={startup.meuPapel === 'FUNDADOR_ADMINISTRADOR'} constituida={startup.constituida} />}

      {aba === 'publicacao' && progresso && (
        <PainelDePublicacao startupId={id} admin={startup.meuPapel === 'FUNDADOR_ADMINISTRADOR'} progresso={progresso} revisoes={revisoes}
          aoMudar={() => { recarregar(); lerStartup(id).then(setStartup).catch(() => undefined) }} />
      )}

      {aba === 'conexoes' && <ConexoesDaStartup startupId={id} admin={startup.meuPapel === 'FUNDADOR_ADMINISTRADOR'} />}

      {aba === 'visibilidade' && <PainelDeVisibilidade startupId={id} admin={startup.meuPapel === 'FUNDADOR_ADMINISTRADOR'} />}

      {(secaoAberta ?? (aba === 'cadastro' ? 'CADASTRO' : null)) && apontamentoDaSecao(secaoAberta ?? 'CADASTRO') && (
        <p role="note" className="rounded-md border border-warn bg-warn-soft px-3 py-2 text-sm text-warn">
          <span className="font-semibold">Comentário da curadoria:</span> {apontamentoDaSecao(secaoAberta ?? 'CADASTRO')?.comentario}
        </p>
      )}

      {secaoAberta === 'TRACAO' && <PainelDeIndicadores startupId={id} podeEditar={podeEditar(startup, 'TRACAO')} />}

      {secaoAberta && valores && (
        <FormularioDaSecao key={secaoAberta} titulo={NOMES_DAS_SECOES[secaoAberta]} inicial={valores}
          campos={catalogo.filter((c) => c.secao === secaoAberta)} bloqueado={!podeEditar(startup, secaoAberta)}
          aoSalvar={async (dados) => {
            try {
              setValores(await salvarSecao(id, secaoAberta, dados))
              recarregar()
              setErro(null)
              setAviso(`${NOMES_DAS_SECOES[secaoAberta]}: salvo.`)
            } catch (e) {
              setAviso(null)
              setErro(e instanceof Error ? e.message : 'Não foi possível salvar.')
            }
          }} />
      )}

      {aba === 'equipe' && <EquipeDaStartup startupId={id} admin={startup.meuPapel === 'FUNDADOR_ADMINISTRADOR'} />}

      {aba === 'cadastro' && !bloqueado && (
        <label className={`${CLASSE_LINK} self-start text-sm`}>
          {startup.temLogotipo ? 'Trocar logotipo' : 'Enviar logotipo'} (PNG, JPG ou WebP)
          <input type="file" accept="image/png,image/jpeg,image/webp" className="sr-only" aria-label="Arquivo do logotipo"
            onChange={(e) => {
              const arquivo = e.target.files?.[0]
              e.target.value = ''
              if (!arquivo) return
              trocarLogotipo(id, arquivo).then((s) => { setStartup(s); setErro(null); setAviso('Logotipo atualizado.') })
                .catch((falha: unknown) => setErro(falha instanceof Error ? falha.message : 'Não foi possível enviar.'))
            }} />
        </label>
      )}

      {aba === 'cadastro' && <FormularioCadastral key={startup.atualizadaEm} inicial={startup} rotuloDoBotao="Salvar dados cadastrais" bloqueado={bloqueado}
        aoSalvar={async (dados) => {
          try {
            setStartup(await salvarCadastro(id, dados))
            recarregar()
            setErro(null)
            setAviso('Dados cadastrais salvos.')
          } catch (e) {
            setAviso(null)
            setErro(e instanceof Error ? e.message : 'Não foi possível salvar.')
          }
        }} />}
    </section>
  )
}
