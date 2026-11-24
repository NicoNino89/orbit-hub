import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ErroApi } from '../api/cliente'
import { cadastrar, reenviarConfirmacao, type ContaCriada, type Papel } from '../api/contas'
import { ENDERECOS, listarVigentes, type DocumentoLegal } from '../api/documentos'
import { AlertaErro, Campo, MolduraConta } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const PAPEIS: { papel: Papel; titulo: string; descricao: string }[] = [
  { papel: 'INVESTIDOR', titulo: 'Investidor', descricao: 'Quero conhecer startups e me conectar com empreendedores.' },
  { papel: 'EMPREENDEDOR', titulo: 'Empreendedor', descricao: 'Quero apresentar minha startup e encontrar investidores.' },
]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Tela de cadastro
 * Objetivo: Criar a conta com nome, e-mail, senha, papéis (investidor, empreendedor ou os dois) e o aceite
 *           das versões vigentes dos termos.
 * Decisões: Os termos são lidos da API na hora, e os ids enviados são exatamente os das versões exibidas.
 *           O Termo de Confidencialidade só aparece quando o papel de investidor é marcado. Erros de campo
 *           aparecem no próprio campo; os demais, num alerta acima do botão.
 */
export function PaginaCadastro() {
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [papeis, setPapeis] = useState<Papel[]>([])
  const [aceitouTermos, setAceitouTermos] = useState(false)
  const [aceitouConfidencialidade, setAceitouConfidencialidade] = useState(false)
  const [documentos, setDocumentos] = useState<DocumentoLegal[]>([])
  const [erros, setErros] = useState<Record<string, string>>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [criada, setCriada] = useState<ContaCriada | null>(null)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Carregar termos vigentes
   * Objetivo: Buscar as versões vigentes para exibir os links e enviar os ids aceitos.
   * Decisões: Falha aqui impede o cadastro, com mensagem; sem os ids não há como registrar o aceite.
   */
  useEffect(() => {
    const controle = new AbortController()
    listarVigentes(controle.signal)
      .then(setDocumentos)
      .catch(() => {
        if (!controle.signal.aborted) setErroGeral('Não foi possível carregar os termos. Tente novamente em instantes.')
      })
    return () => controle.abort()
  }, [])

  const investidor = papeis.includes('INVESTIDOR')
  const idDe = (tipo: DocumentoLegal['tipo']) => documentos.find((d) => d.tipo === tipo)?.id

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Marcar ou desmarcar papel
   * Objetivo: Alternar um papel na lista escolhida.
   * Decisões: Os dois podem ficar marcados.
   */
  function alternarPapel(papel: Papel) {
    setPapeis((atuais) => (atuais.includes(papel) ? atuais.filter((p) => p !== papel) : [...atuais, papel]))
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Validar no navegador
   * Objetivo: Conferir papel e aceites antes de enviar, para dar retorno imediato.
   * Decisões: As regras finais (senha, e-mail único, versões) continuam no servidor.
   */
  function validar(): Record<string, string> {
    const problemas: Record<string, string> = {}
    if (papeis.length === 0) problemas.papeis = 'Escolha pelo menos um papel.'
    if (!aceitouTermos) problemas.termos = 'Para criar a conta, aceite os Termos de Uso e a Política de Privacidade.'
    if (investidor && !aceitouConfidencialidade) {
      problemas.confidencialidade = 'Como investidor, aceite o Termo de Confidencialidade.'
    }
    return problemas
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Enviar cadastro
   * Objetivo: Validar, enviar e tratar a resposta da API.
   * Decisões: Os aceites enviados são os ids das versões exibidas na tela.
   */
  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErroGeral(null)
    const problemas = validar()
    setErros(problemas)
    if (Object.keys(problemas).length > 0) return

    const aceites = [idDe('TERMOS_DE_USO'), idDe('POLITICA_DE_PRIVACIDADE'),
      investidor ? idDe('TERMO_DE_CONFIDENCIALIDADE') : undefined].filter((id): id is string => Boolean(id))
    setEnviando(true)
    try {
      setCriada(await cadastrar({ nome, email, senha, papeis, aceites }))
    } catch (erro) {
      if (erro instanceof ErroApi && Object.keys(erro.campos).length > 0) {
        setErros(erro.campos)
      } else {
        setErroGeral(erro instanceof Error ? erro.message : 'Não foi possível criar a conta.')
      }
    } finally {
      setEnviando(false)
    }
  }

  if (criada) {
    return <ContaCriadaComSucesso conta={criada} email={email} />
  }

  return (
    <MolduraConta titulo="Criar conta">
      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        <Campo rotulo="Nome" autoComplete="name" value={nome} onChange={(e) => setNome(e.target.value)}
          erro={erros.nome} required maxLength={120} />
        <Campo rotulo="E-mail" type="email" autoComplete="email" value={email}
          onChange={(e) => setEmail(e.target.value)} erro={erros.email} required />
        <Campo rotulo="Senha" type="password" autoComplete="new-password" value={senha}
          onChange={(e) => setSenha(e.target.value)} erro={erros.senha} required minLength={10} maxLength={128}
          dica="Pelo menos 10 caracteres. Uma frase fácil de lembrar é mais segura que uma senha curta e complicada." />

        <fieldset className="flex flex-col gap-2" aria-describedby={erros.papeis ? 'erro-papeis' : undefined}>
          <legend className="mb-1 text-sm font-semibold">Como você vai usar o Orbit Hub?</legend>
          {PAPEIS.map(({ papel, titulo, descricao }) => (
            <label key={papel} className={`grid cursor-pointer grid-cols-[auto_1fr] gap-x-3 rounded-md border p-3 ${
              papeis.includes(papel) ? 'border-primary bg-primary-soft' : 'border-line'}`}>
              <input type="checkbox" className="row-span-2 mt-1" checked={papeis.includes(papel)}
                onChange={() => alternarPapel(papel)} />
              <span className="font-semibold">{titulo}</span>
              <span className="text-sm text-muted">{descricao}</span>
            </label>
          ))}
          <p className="text-xs text-muted">Pode marcar os dois. Dá para ativar o outro papel depois, pelo perfil.</p>
          {erros.papeis && <p id="erro-papeis" className="text-sm text-crit">{erros.papeis}</p>}
        </fieldset>

        <div className="flex flex-col gap-2 text-sm">
          <label className="flex gap-2">
            <input type="checkbox" checked={aceitouTermos} onChange={(e) => setAceitouTermos(e.target.checked)} />
            <span>
              Li e aceito os{' '}
              <a href={`/termos/${ENDERECOS.TERMOS_DE_USO}`} target="_blank" rel="noreferrer" className={CLASSE_LINK}>Termos de Uso</a>
              {' '}e a{' '}
              <a href={`/termos/${ENDERECOS.POLITICA_DE_PRIVACIDADE}`} target="_blank" rel="noreferrer" className={CLASSE_LINK}>Política de Privacidade</a>.
            </span>
          </label>
          {erros.termos && <p className="text-crit">{erros.termos}</p>}
          {investidor && (
            <label className="flex gap-2">
              <input type="checkbox" checked={aceitouConfidencialidade}
                onChange={(e) => setAceitouConfidencialidade(e.target.checked)} />
              <span>
                Li e aceito o{' '}
                <a href={`/termos/${ENDERECOS.TERMO_DE_CONFIDENCIALIDADE}`} target="_blank" rel="noreferrer" className={CLASSE_LINK}>
                  Termo de Confidencialidade</a>, exigido de investidores.
              </span>
            </label>
          )}
          {erros.confidencialidade && <p className="text-crit">{erros.confidencialidade}</p>}
        </div>

        {erroGeral && <AlertaErro>{erroGeral}</AlertaErro>}
        <button type="submit" disabled={enviando || documentos.length === 0} className={CLASSE_BOTAO_PRIMARIO}>
          {enviando ? 'Criando conta…' : 'Criar conta'}
        </button>
        <p className="text-sm text-muted">Já tem conta? <Link to="/entrar" className={CLASSE_LINK}>Entrar</Link></p>
      </form>
    </MolduraConta>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Conta criada
 * Objetivo: Orientar o próximo passo: confirmar o e-mail (com reenvio) ou entrar.
 * Decisões: O reenvio responde sempre igual, como a API.
 */
function ContaCriadaComSucesso({ conta, email }: { conta: ContaCriada; email: string }) {
  const [reenviado, setReenviado] = useState(false)

  if (!conta.confirmacaoPendente) {
    return (
      <MolduraConta titulo="Conta criada">
        <p>Sua conta está pronta. Entre para começar.</p>
        <Link to="/entrar" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Entrar</Link>
      </MolduraConta>
    )
  }
  return (
    <MolduraConta titulo="Confirme seu e-mail">
      <p role="status">
        Enviamos um link de confirmação para <strong>{email}</strong>. Abra o e-mail e clique no link para ativar a conta.
      </p>
      {reenviado ? (
        <p className="text-sm text-muted">Se a conta ainda estiver pendente, um novo link foi enviado.</p>
      ) : (
        <button type="button" className={`${CLASSE_LINK} self-start`}
          onClick={() => reenviarConfirmacao(email).finally(() => setReenviado(true))}>
          Não recebeu? Reenviar o link
        </button>
      )}
    </MolduraConta>
  )
}
