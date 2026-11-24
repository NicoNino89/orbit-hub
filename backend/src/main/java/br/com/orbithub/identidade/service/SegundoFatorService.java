package br.com.orbithub.identidade.service;

import br.com.orbithub.auditoria.domain.RegistroAuditoria.Acao;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Resultado;
import br.com.orbithub.auditoria.service.TrilhaDeAuditoria;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.seguranca.CifraDeSegredos;
import br.com.orbithub.identidade.domain.BloqueioDeLogin;
import br.com.orbithub.identidade.domain.TokenDeConta;
import br.com.orbithub.identidade.domain.TokenDeConta.Finalidade;
import br.com.orbithub.identidade.domain.Totp;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.BloqueioDeLoginRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.OptionalLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:50
 * Cartão Jira: KAN-75
 * Título: Segundo fator das contas da equipe
 * Objetivo: Exigir, depois da senha, o código do aplicativo autenticador de toda conta interna, e conduzir a
 *           configuração do aplicativo no primeiro acesso.
 * Decisões: Acertar a senha de uma conta interna não abre sessão: devolve um desafio (token de uso único,
 *           válido por orbit.equipe.desafio-segundo-fator) que só vira sessão com o código. Erros de código contam
 *           no mesmo bloqueio do login (KAN-13), e o bloqueio também invalida o desafio. Um código vale uma única
 *           vez. A entrada do membro e a configuração do aplicativo entram na trilha de auditoria (KAN-80).
 */
@Service
public class SegundoFatorService {

    static final String ACAO_ENTRADA = "EQUIPE_ENTROU";
    static final String ACAO_CONFIGURACAO = "SEGUNDO_FATOR_CONFIGURADO";

    private final UsuarioRepository usuarios;
    private final TokenDeContaService links;
    private final BloqueioDeLoginRepository bloqueios;
    private final LoginService.Parametros parametros;
    private final SessaoService sessoes;
    private final CifraDeSegredos cifra;
    private final EquipeProperties equipe;
    private final TrilhaDeAuditoria trilha;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Criação do serviço
     * Objetivo: Receber contas, desafios, bloqueios, sessões, cifra, configuração, trilha e relógio.
     * Decisões: Injeção por construtor.
     */
    public SegundoFatorService(UsuarioRepository usuarios, TokenDeContaService links,
                               BloqueioDeLoginRepository bloqueios, LoginService.Parametros parametros,
                               SessaoService sessoes, CifraDeSegredos cifra, EquipeProperties equipe,
                               TrilhaDeAuditoria trilha, Clock relogio) {
        this.usuarios = usuarios;
        this.links = links;
        this.bloqueios = bloqueios;
        this.parametros = parametros;
        this.sessoes = sessoes;
        this.cifra = cifra;
        this.equipe = equipe;
        this.trilha = trilha;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Emitir desafio
     * Objetivo: Criar o desafio que a conta interna resolve com o código, depois de acertar a senha.
     * Decisões: Diz se o membro ainda precisa configurar o aplicativo.
     */
    @Transactional
    public Desafio emitir(Usuario conta) {
        String token = links.gerar(conta.getId(), Finalidade.SEGUNDO_FATOR, equipe.desafioSegundoFator(), null);
        Etapa etapa = conta.temSegundoFator() ? Etapa.CODIGO : Etapa.CONFIGURAR;
        return new Desafio(token, etapa, relogio.instant().plus(equipe.desafioSegundoFator()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Configurar o aplicativo
     * Objetivo: Gerar um segredo novo e devolver o que o membro cadastra no aplicativo autenticador.
     * Decisões: Só para quem ainda não tem segundo fator; o segredo fica pendente (cifrado) até o primeiro
     *           código correto. Pedir de novo troca o segredo pendente.
     */
    @Transactional
    public Configuracao configurar(String desafio) {
        Usuario conta = contaDoDesafio(desafio);
        if (conta.temSegundoFator()) {
            throw new RegraDeNegocioException("segundo_fator_configurado",
                    "O segundo fator já está configurado. Digite o código do aplicativo.");
        }
        String segredo = Totp.gerarSegredo();
        conta.iniciarSegundoFator(cifra.cifrar(segredo), relogio.instant());
        return new Configuracao(agrupar(segredo), Totp.endereco(conta.getEmail(), segredo));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Confirmar o código
     * Objetivo: Conferir o código do aplicativo e abrir a sessão do membro.
     * Decisões: Na configuração, o primeiro código correto ativa o segredo. Código errado conta como falha de
     *           login; ao bloquear, o desafio é consumido e a pessoa precisa entrar de novo depois do prazo.
     */
    @Transactional(noRollbackFor = {RegraDeNegocioException.class, LoginService.LoginBloqueadoException.class})
    public SessaoService.SessaoEmitida confirmar(String desafio, String codigo) {
        Instant agora = relogio.instant();
        Usuario conta = contaDoDesafio(desafio);
        String chave = SessaoService.hash(conta.getEmail());
        BloqueioDeLogin bloqueio = bloqueios.findById(chave).orElse(null);
        if (bloqueio != null && bloqueio.bloqueado(agora)) {
            throw new LoginService.LoginBloqueadoException();
        }
        boolean configurando = !conta.temSegundoFator();
        String guardado = configurando ? conta.getTotpPendente() : conta.getTotpSegredo();
        if (guardado == null) {
            throw new RegraDeNegocioException("segundo_fator_nao_iniciado",
                    "Configure o aplicativo autenticador antes de digitar o código.");
        }
        OptionalLong passo = Totp.conferir(cifra.decifrar(guardado), codigo, agora);
        boolean valido = passo.isPresent() && (configurando || conta.usarPassoDoCodigo(passo.getAsLong()));
        if (!valido) {
            BloqueioDeLogin contagem = bloqueio != null ? bloqueio : BloqueioDeLogin.novo(chave, agora);
            contagem.registrarFalha(agora, parametros.maxTentativas(), parametros.bloqueio());
            bloqueios.save(contagem);
            if (contagem.bloqueado(agora)) {
                links.consumir(desafio, Finalidade.SEGUNDO_FATOR);
                throw new LoginService.LoginBloqueadoException();
            }
            throw new RegraDeNegocioException("codigo_invalido",
                    "Código incorreto ou expirado. Confira o aplicativo e tente de novo.");
        }
        if (configurando) {
            conta.concluirSegundoFator(passo.getAsLong(), agora);
            registrar(conta, ACAO_CONFIGURACAO, "Aplicativo autenticador configurado");
        }
        if (bloqueio != null) {
            bloqueios.delete(bloqueio);
        }
        links.consumir(desafio, Finalidade.SEGUNDO_FATOR);
        registrar(conta, ACAO_ENTRADA, "Entrada com senha e segundo fator");
        return sessoes.abrir(conta);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Conta do desafio
     * Objetivo: Encontrar a conta interna ativa a que o desafio pertence.
     * Decisões: Desafio vencido, usado ou de conta que não pode entrar responde "desafio_invalido".
     */
    private Usuario contaDoDesafio(String desafio) {
        TokenDeConta token;
        try {
            token = links.buscarValido(desafio, Finalidade.SEGUNDO_FATOR);
        } catch (RegraDeNegocioException vencido) {
            throw desafioInvalido();
        }
        return usuarios.findById(token.getUsuarioId())
                .filter(u -> u.isInterna() && u.podeTerSessao())
                .orElseThrow(SegundoFatorService::desafioInvalido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Desafio inválido
     * Objetivo: Montar a recusa de desafio vencido ou desconhecido.
     * Decisões: Pede para entrar de novo, sem dizer o motivo exato.
     */
    private static RegraDeNegocioException desafioInvalido() {
        return new RegraDeNegocioException("desafio_invalido",
                "O tempo para digitar o código acabou. Entre de novo com e-mail e senha.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Registrar na trilha
     * Objetivo: Gravar na trilha de auditoria uma ação do membro fora do backoffice.
     * Decisões: Mesma transação: se a sessão não abrir, o registro também não fica.
     */
    private void registrar(Usuario conta, String acao, String detalhe) {
        trilha.registrar(new Acao(conta.getId(), conta.getNome(), acao, "conta", conta.getId().toString(),
                Resultado.SUCESSO, null, detalhe, null));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Segredo em blocos
     * Objetivo: Mostrar o segredo em blocos de 4 letras, mais fácil de digitar no celular.
     * Decisões: Os aplicativos ignoram os espaços.
     */
    static String agrupar(String segredo) {
        return segredo.replaceAll("(.{4})(?!$)", "$1 ");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Etapa do segundo fator
     * Objetivo: Dizer o que falta para a conta interna entrar.
     * Decisões: CONFIGURAR no primeiro acesso (ou depois de uma redefinição); CODIGO nos demais.
     */
    public enum Etapa {
        CONFIGURAR,
        CODIGO
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Desafio emitido
     * Objetivo: Levar o token do desafio, a etapa e o prazo para a tela.
     * Decisões: O toString não mostra o token.
     */
    public record Desafio(String token, Etapa etapa, Instant expiraEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:50
         * Cartão Jira: KAN-75
         * Título: Texto sem o token
         * Objetivo: Evitar que o token apareça em logs.
         * Decisões: Mostra só a etapa e o prazo.
         */
        @Override
        public String toString() {
            return "Desafio[etapa=" + etapa + ", expiraEm=" + expiraEm + "]";
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:50
     * Cartão Jira: KAN-75
     * Título: Dados para o aplicativo
     * Objetivo: Levar o segredo (para digitar) e o endereço otpauth:// (para o QR Code ou o link).
     * Decisões: O toString não mostra o segredo.
     */
    public record Configuracao(String segredo, String endereco) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:50
         * Cartão Jira: KAN-75
         * Título: Texto sem o segredo
         * Objetivo: Evitar que o segredo apareça em logs.
         * Decisões: Texto fixo.
         */
        @Override
        public String toString() {
            return "Configuracao[...]";
        }
    }
}
