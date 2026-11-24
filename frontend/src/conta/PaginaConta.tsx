import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { ErroApi } from '../api/cliente'
import {
  ativarPapel, buscarConta, buscarFoto, buscarHistorico, desativarPapel, enviarFoto, pedirTrocaDeEmail, salvarPerfil,
  trocarSenha, type Conta, type EventoConta,
} from '../api/conta'
import { ehPapelPublico, NOMES_DOS_PAPEIS, type Papel } from '../api/contas'
import { ENDERECOS, listarVigentes } from '../api/documentos'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const NOMES_PAPEL = NOMES_DOS_PAPEIS
const EVENTOS: Record<EventoConta['evento'], string> = {
  PAPEL_ATIVADO: 'Papel ativado', PAPEL_DESATIVADO: 'Papel desativado',
  EMAIL_ALTERADO: 'E-mail alterado', SENHA_ALTERADA: 'Senha alterada',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Mensagem de erro de chamada
 * Objetivo: Extrair a mensagem a mostrar de um erro qualquer.
 * Decisões: A mensagem da API já é escrita para o usuário.
 */
function mensagemDe(erro: unknown) {
  return erro instanceof Error ? erro.message : 'Não foi possível concluir.'
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Seção da tela de conta
 * Objetivo: Moldura com título para cada bloco da página.
 * Decisões: Cada seção tem o próprio formulário e as próprias mensagens.
 */
function Secao({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section aria-label={titulo} className="flex flex-col gap-4 rounded-lg border border-line bg-surface p-5">
      <h2 className="text-lg font-semibold">{titulo}</h2>
      {children}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Tela "Minha conta"
 * Objetivo: Reunir dados do perfil e foto, troca de e-mail e de senha, papéis e histórico da conta.
 * Decisões: Carrega a conta e o histórico ao abrir e recarrega depois de cada mudança.
 */
export function PaginaConta() {
  const [conta, setConta] = useState<Conta | null>(null)
  const [historico, setHistorico] = useState<EventoConta[]>([])
  const [erro, setErro] = useState<string | null>(null)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Recarregar dados
   * Objetivo: Buscar a conta e o histórico.
   * Decisões: Usado ao abrir e depois de cada alteração.
   */
  const recarregar = useCallback(async () => {
    try {
      const [dados, eventos] = await Promise.all([buscarConta(), buscarHistorico()])
      setConta(dados)
      setHistorico(eventos)
    } catch (e) {
      setErro(mensagemDe(e))
    }
  }, [])

  useEffect(() => {
    Promise.all([buscarConta(), buscarHistorico()])
      .then(([dados, eventos]) => {
        setConta(dados)
        setHistorico(eventos)
      })
      .catch((e: unknown) => setErro(mensagemDe(e)))
  }, [])

  if (erro) return <AlertaErro>{erro}</AlertaErro>
  if (!conta) return <p role="status" className="text-muted">Carregando sua conta…</p>

  return (
    <div className="flex flex-col gap-6">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Minha conta</h1>
      <SecaoPerfil conta={conta} aoMudar={setConta} />
      <SecaoPapeis conta={conta} aoMudar={recarregar} />
      <SecaoEmail emailAtual={conta.email} />
      <SecaoSenha aoMudar={recarregar} />
      <Secao titulo="Histórico da conta">
        {historico.length === 0 ? (
          <p className="text-sm text-muted">Nenhuma mudança registrada ainda.</p>
        ) : (
          <ul className="flex flex-col gap-1 text-sm">
            {historico.map((h) => (
              <li key={`${h.evento}-${h.ocorridoEm}`}>
                {EVENTOS[h.evento]}{h.detalhe ? `: ${NOMES_PAPEL[h.detalhe as Papel] ?? h.detalhe}` : ''}
                <span className="text-muted"> · {new Date(h.ocorridoEm).toLocaleString('pt-BR', { timeZone: 'America/Sao_Paulo' })}</span>
              </li>
            ))}
          </ul>
        )}
      </Secao>
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Seção de dados e foto
 * Objetivo: Editar nome e telefone e trocar a foto.
 * Decisões: A foto é baixada com o token e exibida por um endereço local, liberado ao trocar ou sair.
 */
function SecaoPerfil({ conta, aoMudar }: { conta: Conta; aoMudar: (c: Conta) => void }) {
  const [nome, setNome] = useState(conta.nome)
  const [telefone, setTelefone] = useState(conta.telefone ?? '')
  const [foto, setFoto] = useState<string | null>(null)
  const [erros, setErros] = useState<Record<string, string>>({})
  const [aviso, setAviso] = useState<string | null>(null)
  const [erroFoto, setErroFoto] = useState<string | null>(null)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Carregar a foto
   * Objetivo: Baixar a foto quando a conta tem uma.
   * Decisões: Revoga o endereço local anterior para não vazar memória.
   */
  useEffect(() => {
    if (!conta.temFoto) return
    let endereco: string | null = null
    buscarFoto().then((blob) => {
      endereco = URL.createObjectURL(blob)
      setFoto(endereco)
    }).catch(() => setFoto(null))
    return () => {
      if (endereco) URL.revokeObjectURL(endereco)
    }
  }, [conta.temFoto, conta])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Salvar dados
   * Objetivo: Enviar nome e telefone.
   * Decisões: Erros de campo aparecem no campo.
   */
  async function salvar(evento: FormEvent) {
    evento.preventDefault()
    setErros({})
    setAviso(null)
    try {
      aoMudar(await salvarPerfil(nome, telefone))
      setAviso('Dados salvos.')
    } catch (e) {
      setErros(e instanceof ErroApi ? { ...e.campos, geral: Object.keys(e.campos).length ? '' : e.message } : { geral: mensagemDe(e) })
    }
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Trocar foto
   * Objetivo: Enviar o arquivo escolhido.
   * Decisões: Formato e tamanho conferidos no servidor; a mensagem dele aparece aqui.
   */
  async function escolherFoto(arquivo: File | undefined) {
    if (!arquivo) return
    setErroFoto(null)
    try {
      aoMudar(await enviarFoto(arquivo))
    } catch (e) {
      setErroFoto(mensagemDe(e))
    }
  }

  return (
    <Secao titulo="Dados do perfil">
      <div className="flex items-center gap-4">
        {foto
          ? <img src={foto} alt="Sua foto" className="h-16 w-16 rounded-full object-cover" />
          : <div aria-hidden className="flex h-16 w-16 items-center justify-center rounded-full bg-surface-2 text-xl font-semibold">{conta.nome[0]}</div>}
        <label className={`${CLASSE_LINK} cursor-pointer text-sm`}>
          Trocar foto
          <input type="file" accept="image/jpeg,image/png,image/webp" className="sr-only"
            onChange={(e) => void escolherFoto(e.target.files?.[0])} />
        </label>
      </div>
      {erroFoto && <AlertaErro>{erroFoto}</AlertaErro>}
      <form onSubmit={salvar} className="flex flex-col gap-4">
        <Campo rotulo="Nome" value={nome} onChange={(e) => setNome(e.target.value)} erro={erros.nome} required maxLength={120} />
        <Campo rotulo="Telefone" type="tel" value={telefone} onChange={(e) => setTelefone(e.target.value)}
          erro={erros.telefone} maxLength={20} dica="Opcional." />
        <p className="text-sm text-muted">E-mail: <strong>{conta.email}</strong></p>
        {erros.geral && <AlertaErro>{erros.geral}</AlertaErro>}
        {aviso && <p role="status" className="text-sm text-ok">{aviso}</p>}
        <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Salvar dados</button>
      </form>
    </Secao>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:55
 * Cartão Jira: KAN-75
 * Título: Seção de papéis
 * Objetivo: Mostrar os papéis da conta e ativar ou desativar.
 * Decisões: Ativar investidor pede o aceite do Termo de Confidencialidade vigente. Depois da mudança, a sessão
 *           é renovada para o token levar os papéis novos; ao ativar, a pessoa é levada ao painel do papel.
 *           Conta da equipe só vê os papéis, definidos pelo administrador (KAN-75).
 */
function SecaoPapeis({ conta, aoMudar }: { conta: Conta; aoMudar: () => Promise<void> }) {
  const { renovar, escolherPapel } = useSessao()
  const navegar = useNavigate()
  const [aceitou, setAceitou] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [termoId, setTermoId] = useState<string | null>(null)

  useEffect(() => {
    listarVigentes().then((docs) => setTermoId(docs.find((d) => d.tipo === 'TERMO_DE_CONFIDENCIALIDADE')?.id ?? null))
      .catch(() => setTermoId(null))
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Ativar um papel
   * Objetivo: Pedir a ativação, renovar a sessão e passar a atuar no papel novo.
   * Decisões: O onboarding do papel chega na S3/S4; por ora a pessoa vai ao painel já no papel novo.
   */
  async function ativar(papel: Papel) {
    setErro(null)
    try {
      await ativarPapel(papel, papel === 'INVESTIDOR' && termoId ? [termoId] : [])
      await renovar()
      escolherPapel(papel)
      navegar('/painel')
    } catch (e) {
      setErro(mensagemDe(e))
    }
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Desativar um papel
   * Objetivo: Pedir a desativação e atualizar a tela e a sessão.
   * Decisões: A mensagem de pendência vem da API.
   */
  async function desativar(papel: Papel) {
    setErro(null)
    try {
      await desativarPapel(papel)
      await renovar()
      await aoMudar()
    } catch (e) {
      setErro(mensagemDe(e))
    }
  }

  const publicos: Papel[] = ['INVESTIDOR', 'EMPREENDEDOR']
  const ativos = conta.papeis.filter(ehPapelPublico)

  if (conta.papeis.some((p) => !ehPapelPublico(p))) {
    return (
      <Secao titulo="Papéis">
        <p className="text-sm">
          Conta da equipe: {conta.papeis.map((p) => NOMES_PAPEL[p]).join(', ')}. Os papéis são definidos pelo
          administrador. Para investir ou empreender, use uma conta pessoal separada.
        </p>
      </Secao>
    )
  }

  return (
    <Secao titulo="Papéis">
      <ul className="flex flex-col gap-3">
        {publicos.map((papel) => {
          const tem = conta.papeis.includes(papel)
          return (
            <li key={papel} className="flex flex-wrap items-center justify-between gap-3 rounded-md border border-line p-3">
              <span>
                <strong>{NOMES_PAPEL[papel]}</strong>
                <span className={`ml-2 rounded px-2 py-0.5 text-xs ${tem ? 'bg-ok-soft text-ok' : 'bg-surface-2 text-muted'}`}>
                  {tem ? 'Ativo' : 'Inativo'}
                </span>
              </span>
              {tem ? (
                ativos.length > 1 && (
                  <button type="button" className={CLASSE_LINK} onClick={() => void desativar(papel)}>
                    Desativar {NOMES_PAPEL[papel].toLowerCase()}
                  </button>
                )
              ) : (
                <div className="flex flex-col items-end gap-2">
                  {papel === 'INVESTIDOR' && (
                    <label className="flex gap-2 text-sm">
                      <input type="checkbox" checked={aceitou} onChange={(e) => setAceitou(e.target.checked)} />
                      <span>
                        Li e aceito o{' '}
                        <a href={`/termos/${ENDERECOS.TERMO_DE_CONFIDENCIALIDADE}`} target="_blank" rel="noreferrer"
                          className={CLASSE_LINK}>Termo de Confidencialidade</a>
                      </span>
                    </label>
                  )}
                  <button type="button" className={CLASSE_BOTAO_PRIMARIO}
                    disabled={papel === 'INVESTIDOR' && !aceitou} onClick={() => void ativar(papel)}>
                    Ativar {NOMES_PAPEL[papel].toLowerCase()}
                  </button>
                </div>
              )}
            </li>
          )
        })}
      </ul>
      {erro && <AlertaErro>{erro}</AlertaErro>}
    </Secao>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Seção de troca de e-mail
 * Objetivo: Pedir a troca com o novo e-mail e a senha atual.
 * Decisões: Explica que a troca só vale depois de confirmar pelo novo endereço.
 */
function SecaoEmail({ emailAtual }: { emailAtual: string }) {
  const [novoEmail, setNovoEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [estado, setEstado] = useState<{ erro?: ErroApi; enviado?: string }>({})

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Pedir troca
   * Objetivo: Enviar o pedido e mostrar o resultado.
   * Decisões: Limpa a senha depois do envio.
   */
  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    try {
      await pedirTrocaDeEmail(novoEmail, senha)
      setEstado({ enviado: novoEmail })
      setSenha('')
    } catch (e) {
      setEstado({ erro: e instanceof ErroApi ? e : new ErroApi(0, mensagemDe(e)) })
    }
  }

  return (
    <Secao titulo="Trocar e-mail">
      {estado.enviado ? (
        <p role="status">Enviamos um link para <strong>{estado.enviado}</strong>. Seu e-mail continua {emailAtual} até você confirmar.</p>
      ) : (
        <form onSubmit={enviar} className="flex flex-col gap-4">
          <Campo rotulo="Novo e-mail" type="email" autoComplete="email" required value={novoEmail}
            onChange={(e) => setNovoEmail(e.target.value)} erro={estado.erro?.campos.novoEmail} />
          <Campo rotulo="Senha atual (para trocar o e-mail)" type="password" autoComplete="current-password" required
            value={senha} onChange={(e) => setSenha(e.target.value)} erro={estado.erro?.campos.senhaAtual} />
          {estado.erro && Object.keys(estado.erro.campos).length === 0 && <AlertaErro>{estado.erro.message}</AlertaErro>}
          <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Enviar link de confirmação</button>
        </form>
      )}
    </Secao>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Seção de troca de senha
 * Objetivo: Trocar a senha informando a atual.
 * Decisões: A senha nova é digitada duas vezes; os outros aparelhos são desconectados.
 */
function SecaoSenha({ aoMudar }: { aoMudar: () => Promise<void> }) {
  const [atual, setAtual] = useState('')
  const [nova, setNova] = useState('')
  const [repetida, setRepetida] = useState('')
  const [erro, setErro] = useState<ErroApi | null>(null)
  const [diferentes, setDiferentes] = useState(false)
  const [feito, setFeito] = useState(false)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Trocar
   * Objetivo: Conferir a repetição e enviar.
   * Decisões: Limpa os campos depois da troca.
   */
  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setFeito(false)
    setDiferentes(nova !== repetida)
    if (nova !== repetida) return
    try {
      await trocarSenha(atual, nova)
      setAtual('')
      setNova('')
      setRepetida('')
      setFeito(true)
      await aoMudar()
    } catch (e) {
      setErro(e instanceof ErroApi ? e : new ErroApi(0, mensagemDe(e)))
    }
  }

  return (
    <Secao titulo="Trocar senha">
      <form onSubmit={enviar} className="flex flex-col gap-4">
        <Campo rotulo="Senha atual" type="password" autoComplete="current-password" required value={atual}
          onChange={(e) => setAtual(e.target.value)}
          erro={erro?.codigo === 'senha_atual_incorreta' ? erro.message : erro?.campos.senhaAtual} />
        <Campo rotulo="Senha nova" type="password" autoComplete="new-password" required minLength={10} maxLength={128}
          value={nova} onChange={(e) => setNova(e.target.value)}
          erro={erro?.codigo === 'senha_fraca' ? erro.message : erro?.campos.novaSenha} />
        <Campo rotulo="Repita a senha nova" type="password" autoComplete="new-password" required value={repetida}
          onChange={(e) => setRepetida(e.target.value)} erro={diferentes ? 'As duas senhas precisam ser iguais.' : undefined} />
        {feito && <p role="status" className="text-sm text-ok">Senha trocada. Os outros aparelhos foram desconectados.</p>}
        <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Trocar senha</button>
      </form>
    </Secao>
  )
}
