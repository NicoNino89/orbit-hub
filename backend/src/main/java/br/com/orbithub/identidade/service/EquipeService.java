package br.com.orbithub.identidade.service;

import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.PoliticaDeSenha;
import br.com.orbithub.identidade.domain.RevisaoDeAcesso;
import br.com.orbithub.identidade.domain.StatusUsuario;
import br.com.orbithub.identidade.domain.TokenDeConta;
import br.com.orbithub.identidade.domain.TokenDeConta.Finalidade;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.RevisaoDeAcessoRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Gestão da equipe interna
 * Objetivo: Convidar, trocar papéis, desligar e redefinir o segundo fator dos membros da equipe, e conduzir a
 *           revisão periódica dos acessos.
 * Decisões: Só o administrador usa este serviço (as rotas ficam em /api/v1/admin/equipe). A equipe nunca define
 *           senha por ninguém: o membro recebe um convite e escolhe a própria senha. A conta da equipe é separada
 *           da conta pública: e-mail já usado na plataforma é recusado. A plataforma nunca fica sem administrador
 *           ativo. Trocar papéis, desligar ou redefinir o segundo fator encerra as sessões do membro na hora,
 *           para a mudança valer imediatamente. Cada ação recebe um nome de negócio na trilha de auditoria.
 */
@Service
public class EquipeService {

    static final String ACAO_CONVITE = "EQUIPE_MEMBRO_CONVIDADO";
    static final String ACAO_PAPEIS = "EQUIPE_PAPEIS_ALTERADOS";
    static final String ACAO_DESLIGAMENTO = "EQUIPE_MEMBRO_DESLIGADO";
    static final String ACAO_SEGUNDO_FATOR = "EQUIPE_SEGUNDO_FATOR_REDEFINIDO";
    static final String ACAO_REVISAO = "EQUIPE_REVISAO_CONFIRMADA";
    static final String ACAO_CONVITE_REENVIADO = "EQUIPE_CONVITE_REENVIADO";
    static final String OBJETO = "conta";

    private final UsuarioRepository usuarios;
    private final RevisaoDeAcessoRepository revisoes;
    private final PasswordEncoder senhas;
    private final TokenDeContaService links;
    private final SessaoService sessoes;
    private final EnvioDeEmail email;
    private final ContaProperties conta;
    private final EquipeProperties equipe;
    private final List<CasosDoMembro> filas;
    private final Clock relogio;
    private final SecureRandom aleatorio = new SecureRandom();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Criação do serviço
     * Objetivo: Receber contas, revisões, senhas, links, sessões, e-mail, configurações, filas e relógio.
     * Decisões: As filas chegam como lista (vazia até existirem casos).
     */
    public EquipeService(UsuarioRepository usuarios, RevisaoDeAcessoRepository revisoes, PasswordEncoder senhas,
                         TokenDeContaService links, SessaoService sessoes, EnvioDeEmail email,
                         ContaProperties conta, EquipeProperties equipe, List<CasosDoMembro> filas, Clock relogio) {
        this.usuarios = usuarios;
        this.revisoes = revisoes;
        this.senhas = senhas;
        this.links = links;
        this.sessoes = sessoes;
        this.email = email;
        this.conta = conta;
        this.equipe = equipe;
        this.filas = filas;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Listar a equipe
     * Objetivo: Listar todos os membros, com papéis, situação e segundo fator.
     * Decisões: Inclui convidados e desligados, para o administrador ver o histórico.
     */
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findByInternaTrueOrderByNomeAsc();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Convidar membro
     * Objetivo: Criar a conta interna e enviar o convite para o membro escolher a senha.
     * Decisões: E-mail já usado em qualquer conta é recusado (a conta da equipe precisa de um e-mail próprio).
     *           A senha inicial é aleatória e descartada: ninguém a conhece.
     */
    @Transactional
    public Usuario convidar(String nome, String emailInformado, Set<Papel> papeis) {
        String emailNormalizado = Usuario.normalizarEmail(emailInformado);
        if (usuarios.existsByEmail(emailNormalizado)) {
            throw new RegraDeNegocioException("email_em_uso", "Este e-mail já tem uma conta na plataforma. A conta "
                    + "da equipe é separada e precisa de um e-mail próprio (por exemplo, o corporativo).");
        }
        exigirPapeisInternos(papeis);
        Usuario membro = usuarios.save(Usuario.novoInterno(nome, emailNormalizado, senhaInutilizavel(), papeis,
                relogio.instant()));
        enviarConvite(membro);
        ContextoDeAuditoria.descrever(ACAO_CONVITE, OBJETO, membro.getId(), "Papéis: " + texto(papeis));
        return membro;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Reenviar convite
     * Objetivo: Mandar um convite novo para quem ainda não aceitou.
     * Decisões: O convite anterior deixa de valer.
     */
    @Transactional
    public void reenviarConvite(UUID membroId) {
        Usuario membro = membro(membroId);
        ContextoDeAuditoria.descrever(ACAO_CONVITE_REENVIADO, OBJETO, membroId, null);
        if (membro.getStatus() != StatusUsuario.PENDENTE_CONFIRMACAO) {
            throw new RegraDeNegocioException("convite_aceito", "Este membro já aceitou o convite.");
        }
        enviarConvite(membro);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Aceitar convite
     * Objetivo: Ativar a conta interna com a senha escolhida pelo membro.
     * Decisões: Mesma política de senha do cadastro; link de uso único.
     */
    @Transactional
    public void aceitarConvite(String token, String senha) {
        TokenDeConta convite = links.buscarValido(token, Finalidade.CONVITE_EQUIPE);
        Usuario membro = usuarios.findById(convite.getUsuarioId())
                .filter(u -> u.isInterna() && u.getStatus() == StatusUsuario.PENDENTE_CONFIRMACAO)
                .orElseThrow(() -> new RegraDeNegocioException("link_invalido",
                        "Este link é inválido ou expirou. Peça um novo ao administrador."));
        PoliticaDeSenha.problema(senha, membro.getEmail()).ifPresent(motivo -> {
            throw new RegraDeNegocioException("senha_fraca", motivo);
        });
        links.consumir(token, Finalidade.CONVITE_EQUIPE);
        membro.aceitarConvite(senhas.encode(senha), relogio.instant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Trocar papéis
     * Objetivo: Definir os papéis de um membro.
     * Decisões: Recusa tirar o papel de administrador do último administrador ativo; encerra as sessões do
     *           membro para os papéis novos valerem já; registra o antes e o depois.
     */
    @Transactional
    public Usuario alterarPapeis(UUID membroId, Set<Papel> novos) {
        Usuario membro = membro(membroId);
        ContextoDeAuditoria.descrever(ACAO_PAPEIS, OBJETO, membroId, "Para: " + texto(novos));
        exigirPapeisInternos(novos);
        if (membro.getDesligadoEm() != null) {
            throw new RegraDeNegocioException("membro_desligado", "Este membro foi desligado.");
        }
        boolean perdeAdmin = membro.getPapeis().contains(Papel.ADMIN) && !novos.contains(Papel.ADMIN);
        if (perdeAdmin && membro.getStatus() == StatusUsuario.ATIVO && ultimoAdministrador()) {
            throw new RegraDeNegocioException("ultimo_administrador",
                    "A plataforma precisa de pelo menos um administrador ativo.");
        }
        Set<Papel> anteriores = membro.definirPapeisInternos(novos, relogio.instant());
        sessoes.encerrarTodas(membroId, "papeis_alterados");
        ContextoDeAuditoria.descrever(ACAO_PAPEIS, OBJETO, membroId,
                "De: " + texto(anteriores) + " | Para: " + texto(novos));
        return membro;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Desligar membro
     * Objetivo: Revogar o acesso de quem saiu da equipe e devolver os casos abertos à fila.
     * Decisões: Exige justificativa; ninguém se desliga sozinho, e o último administrador ativo não é desligado.
     *           As sessões caem na hora (o access token é recusado na próxima requisição, KAN-57).
     */
    @Transactional
    public int desligar(UUID administradorId, UUID membroId, String justificativa) {
        ContextoDeAuditoria.descrever(ACAO_DESLIGAMENTO, OBJETO, membroId, null);
        ContextoDeAuditoria.justificar(justificativa);
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraDeNegocioException("justificativa_obrigatoria", "Informe o motivo do desligamento.");
        }
        if (administradorId.equals(membroId)) {
            throw new RegraDeNegocioException("auto_desligamento",
                    "Você não pode desligar a própria conta. Peça a outro administrador.");
        }
        Usuario membro = membro(membroId);
        if (membro.getPapeis().contains(Papel.ADMIN) && membro.getStatus() == StatusUsuario.ATIVO
                && ultimoAdministrador()) {
            throw new RegraDeNegocioException("ultimo_administrador",
                    "A plataforma precisa de pelo menos um administrador ativo.");
        }
        membro.desligar(relogio.instant());
        sessoes.encerrarTodas(membroId, "desligamento");
        int devolvidos = filas.stream().mapToInt(f -> f.devolverParaFila(membroId)).sum();
        ContextoDeAuditoria.descrever(ACAO_DESLIGAMENTO, OBJETO, membroId,
                "Papéis: " + texto(membro.getPapeis()) + " | " + devolvidos + " caso(s) devolvido(s) à fila");
        return devolvidos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Redefinir segundo fator
     * Objetivo: Apagar o segundo fator de quem perdeu o aplicativo, para configurar de novo no próximo login.
     * Decisões: Feito por outro administrador (nunca o próprio), e as sessões do membro caem.
     */
    @Transactional
    public void redefinirSegundoFator(UUID administradorId, UUID membroId) {
        ContextoDeAuditoria.descrever(ACAO_SEGUNDO_FATOR, OBJETO, membroId, null);
        if (administradorId.equals(membroId)) {
            throw new RegraDeNegocioException("auto_redefinicao",
                    "Peça a outro administrador para redefinir o seu segundo fator.");
        }
        Usuario membro = membro(membroId);
        membro.redefinirSegundoFator(relogio.instant());
        sessoes.encerrarTodas(membroId, "segundo_fator_redefinido");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Situação da revisão de acessos
     * Objetivo: Informar a última revisão, a data da próxima e se está vencida.
     * Decisões: Sem revisão anterior, a primeira vence na data de hoje (deve ser feita logo).
     */
    @Transactional(readOnly = true)
    public SituacaoRevisao situacaoRevisao() {
        Instant agora = relogio.instant();
        Optional<RevisaoDeAcesso> ultima = revisoes.findFirstByOrderByRealizadaEmDesc();
        Instant proxima = ultima
                .map(r -> r.getRealizadaEm().atZone(ZoneOffset.UTC).plusMonths(equipe.revisaoMeses()).toInstant())
                .orElse(agora);
        String revisor = ultima.flatMap(r -> usuarios.findById(r.getRevisorId())).map(Usuario::getNome).orElse(null);
        return new SituacaoRevisao(ultima.map(RevisaoDeAcesso::getRealizadaEm).orElse(null), revisor, proxima,
                !proxima.isAfter(agora), equipe.revisaoMeses());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Confirmar revisão de acessos
     * Objetivo: Registrar que o administrador conferiu quem continua na equipe e com quais papéis.
     * Decisões: Fotografa os membros ativos naquele momento; quem não deve continuar é desligado ou tem os
     *           papéis trocados antes de confirmar.
     */
    @Transactional
    public RevisaoDeAcesso confirmarRevisao(UUID administradorId, String observacao) {
        List<RevisaoDeAcesso.Item> itens = usuarios.findByInternaTrueOrderByNomeAsc().stream()
                .filter(u -> u.getStatus() == StatusUsuario.ATIVO)
                .map(u -> new RevisaoDeAcesso.Item(u.getId(), u.getNome(), texto(u.getPapeis())))
                .toList();
        RevisaoDeAcesso revisao = revisoes.save(RevisaoDeAcesso.registrar(administradorId, observacao, itens,
                relogio.instant()));
        ContextoDeAuditoria.descrever(ACAO_REVISAO, "revisao_acesso", revisao.getId(),
                itens.size() + " membro(s) confirmado(s)");
        ContextoDeAuditoria.justificar(observacao);
        return revisao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Existe administrador ativo
     * Objetivo: Dizer se já há algum administrador ativo na equipe.
     * Decisões: Usado pelo procedimento do primeiro administrador.
     */
    @Transactional(readOnly = true)
    public boolean existeAdministradorAtivo() {
        return usuarios.contarEquipeAtivaComPapel(Papel.ADMIN) > 0;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Membro da equipe
     * Objetivo: Buscar uma conta interna pelo id.
     * Decisões: Conta pública ou inexistente: 404, para a rota não revelar contas de usuários.
     */
    private Usuario membro(UUID membroId) {
        return usuarios.findById(membroId).filter(Usuario::isInterna)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro da equipe não encontrado."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Último administrador
     * Objetivo: Dizer se só resta um administrador ativo.
     * Decisões: Contado no banco.
     */
    private boolean ultimoAdministrador() {
        return usuarios.contarEquipeAtivaComPapel(Papel.ADMIN) <= 1;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Exigir papéis internos
     * Objetivo: Recusar conjunto vazio ou com papel público.
     * Decisões: Mensagem para a tela do administrador.
     */
    private static void exigirPapeisInternos(Set<Papel> papeis) {
        if (papeis == null || papeis.isEmpty() || !papeis.stream().allMatch(Papel::interno)) {
            throw new RegraDeNegocioException("papeis_invalidos",
                    "Escolha pelo menos um papel da equipe (investidor e empreendedor não valem aqui).");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Enviar convite
     * Objetivo: Gerar o link do convite e mandar o e-mail ao membro.
     * Decisões: Link para a tela /equipe/convite da web, com validade de convite.validade_dias. Devolve o
     *           link, para o procedimento do primeiro administrador.
     */
    String enviarConvite(Usuario membro) {
        String link = novoLinkDeConvite(membro);
        email.enviar(new EnvioDeEmail.MensagemEmail(membro.getEmail(), "Convite para a equipe do Orbit Hub",
                "Olá, " + membro.getNome() + ".\n\nVocê foi convidado para a equipe interna do Orbit Hub "
                        + "(papéis: " + texto(membro.getPapeis()) + ").\n\nPara escolher sua senha, abra o link:\n\n"
                        + link + "\n\nO link vale por " + equipe.convite().toDays() + " dias. No primeiro acesso, "
                        + "você vai configurar um aplicativo autenticador (segundo fator).",
                "convite_equipe"));
        return link;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Link de convite
     * Objetivo: Gerar um link de convite novo para o membro.
     * Decisões: O link anterior deixa de valer (o serviço de links invalida os da mesma finalidade).
     */
    private String novoLinkDeConvite(Usuario membro) {
        String token = links.gerar(membro.getId(), Finalidade.CONVITE_EQUIPE, equipe.convite(), null);
        return conta.webUrl() + "/equipe/convite?token=" + token;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Garantir o primeiro administrador
     * Objetivo: Na implantação, criar (ou reconvidar) o primeiro administrador quando ainda não há nenhum ativo.
     * Decisões: Idempotente: com administrador ativo, não faz nada. Se o e-mail já é de uma conta pública,
     *           recusa (conta da equipe é separada). Devolve o link do convite enviado, para o procedimento de
     *           implantação poder mostrá-lo quando não há provedor de e-mail.
     */
    @Transactional
    public Optional<String> garantirPrimeiroAdministrador(String nome, String emailInformado) {
        if (existeAdministradorAtivo()) {
            return Optional.empty();
        }
        String emailNormalizado = Usuario.normalizarEmail(emailInformado);
        Optional<Usuario> existente = usuarios.findByEmail(emailNormalizado);
        if (existente.isPresent()) {
            Usuario conta = existente.get();
            if (!conta.isInterna() || conta.getStatus() != StatusUsuario.PENDENTE_CONFIRMACAO) {
                throw new IllegalStateException("O e-mail do primeiro administrador já é usado por outra conta");
            }
            return Optional.of(enviarConvite(conta));
        }
        Usuario membro = usuarios.save(Usuario.novoInterno(nome, emailNormalizado, senhaInutilizavel(),
                Set.of(Papel.ADMIN), relogio.instant()));
        return Optional.of(enviarConvite(membro));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Senha inutilizável
     * Objetivo: Gerar o hash de uma senha aleatória que ninguém conhece, até o convite ser aceito.
     * Decisões: 32 bytes aleatórios, descartados depois do hash.
     */
    private String senhaInutilizavel() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return senhas.encode(Base64.getEncoder().encodeToString(bytes));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Papéis em texto
     * Objetivo: Escrever os papéis em ordem alfabética, separados por vírgula.
     * Decisões: Usado na trilha e na fotografia da revisão.
     */
    static String texto(Set<Papel> papeis) {
        return papeis == null ? "" : papeis.stream().map(Papel::name).sorted().collect(Collectors.joining(", "));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Situação da revisão
     * Objetivo: Levar à tela a última revisão, a próxima e se está vencida.
     * Decisões: Datas em UTC; a tela converte.
     */
    public record SituacaoRevisao(Instant ultimaEm, String ultimoRevisor, Instant proximaEm, boolean vencida,
                                  int intervaloMeses) {
    }
}
