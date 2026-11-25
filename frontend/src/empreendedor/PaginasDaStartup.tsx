import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import {
  CADASTRO_VAZIO, criarStartup, lerLogotipo, lerStartup, NOMES_DA_SITUACAO_DA_STARTUP, NOMES_DO_PAPEL_NA_STARTUP,
  NOMES_DAS_SECOES, podeEditar, salvarCadastro, type SecaoDaStartup, type Startup, trocarLogotipo,
} from '../api/startups'
import { type CampoDoCatalogo, catalogoDeCampos, salvarSecao, type ValoresDasSecoes, valoresDasSecoes } from '../api/secoes'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_LINK } from '../ui/estilos'
import { EquipeDaStartup } from './EquipeDaStartup'
import { FormularioCadastral } from './FormularioCadastral'
import { FormularioDaSecao } from './FormularioDaSecao'
import { PainelDeIndicadores } from './PainelDeIndicadores'

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
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: Editor da startup
 * Objetivo: Mostrar a startup com a situação, o selo de empresa não constituída, o logotipo e a etapa dos dados
 *           cadastrais.
 * Decisões: Navegação por etapas: dados cadastrais, as seções que o catálogo do servidor descreve (KAN-22 em diante)
 *           e a equipe (KAN-65).
 *           Colaborador vê bloqueadas as seções que não foram autorizadas a ele.
 */
export function PaginaStartup() {
  const { id = '' } = useParams()
  const [startup, setStartup] = useState<Startup | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [logo, setLogo] = useState<string | null>(null)
  const [aba, setAba] = useState<'cadastro' | 'equipe' | SecaoDaStartup>('cadastro')
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
  ]
  const secaoAberta = aba === 'cadastro' || aba === 'equipe' ? null : aba

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
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}

      <nav aria-label="Etapas do cadastro" className="flex flex-wrap gap-2 text-sm">
        {etapas.map(([chave, nome]) => (
          <button key={chave} type="button" onClick={() => setAba(chave)} aria-current={aba === chave ? 'step' : undefined}
            className={aba === chave ? 'rounded-md bg-primary px-3 py-1 font-semibold text-on-primary' : 'rounded-md border border-line px-3 py-1'}>
            {nome}
          </button>
        ))}
      </nav>

      {secaoAberta === 'TRACAO' && <PainelDeIndicadores startupId={id} podeEditar={podeEditar(startup, 'TRACAO')} />}

      {secaoAberta && valores && (
        <FormularioDaSecao key={secaoAberta} titulo={NOMES_DAS_SECOES[secaoAberta]} inicial={valores}
          campos={catalogo.filter((c) => c.secao === secaoAberta)} bloqueado={!podeEditar(startup, secaoAberta)}
          aoSalvar={async (dados) => {
            try {
              setValores(await salvarSecao(id, secaoAberta, dados))
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
