package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.compartilhado.convites.TokensDeConvite;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.Chaves;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.empreendimentos.domain.ConviteDaStartup;
import br.com.orbithub.empreendimentos.domain.MembroDaStartup;
import br.com.orbithub.empreendimentos.domain.PapelNaStartup;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.repository.ConviteDaStartupRepository;
import br.com.orbithub.empreendimentos.repository.MembroDaStartupRepository;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.identidade.service.ContaProperties;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Equipe da startup
 * Objetivo: Convidar fundadores e colaboradores por e-mail, trocar papéis e seções, remover e sair, garantindo que a
 *           startup nunca fique sem fundador administrador.
 * Decisões: Mesmo modelo das organizações (KAN-60). Só o fundador administrador gerencia a equipe. O convite vai por
 *           e-mail (a pessoa pode não ter conta) e, se ela já tiver conta ativa, também pelo sino. Só entra quem tem o
 *           papel de empreendedor e a conta com o mesmo e-mail. Colaborador precisa de pelo menos uma seção.
 */
@Service
public class EquipeDaStartupService {


    private final StartupRepository startups;
    private final MembroDaStartupRepository membros;
    private final ConviteDaStartupRepository convites;
    private final StartupsService cadastro;
    private final ConsultaDeContas contas;
    private final NotificacoesService notificacoes;
    private final EnvioDeEmail email;
    private final ContaProperties conta;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, o cadastro, contas, notificações, e-mail, parâmetros e relógio.
     * Decisões: Injeção por construtor.
     */
    public EquipeDaStartupService(StartupRepository startups, MembroDaStartupRepository membros,
                                  ConviteDaStartupRepository convites, StartupsService cadastro, ConsultaDeContas contas,
                                  NotificacoesService notificacoes, EnvioDeEmail email, ContaProperties conta,
                                  LeituraDeParametros parametros, Clock relogio) {
        this.startups = startups;
        this.membros = membros;
        this.convites = convites;
        this.cadastro = cadastro;
        this.contas = contas;
        this.notificacoes = notificacoes;
        this.email = email;
        this.conta = conta;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Equipe
     * Objetivo: Listar os membros ativos com nome, papel e seções.
     * Decisões: Qualquer membro vê; sem e-mail.
     */
    @Transactional(readOnly = true)
    public List<Membro> equipe(UUID eu, UUID startupId) {
        cadastro.papel(eu, startupId);
        List<MembroDaStartup> ativos = membros.findByStartupIdAndSaiuEmIsNullOrderByEntrouEm(startupId);
        Map<UUID, String> nomes = contas.nomes(ativos.stream().map(MembroDaStartup::getUsuarioId).toList());
        return ativos.stream().map(m -> new Membro(m.getUsuarioId(), nomes.getOrDefault(m.getUsuarioId(), "Conta removida"),
                m.getPapel(), m.getSecoes(), m.getEntrouEm())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Convidar
     * Objetivo: Enviar um convite por e-mail com o papel (e as seções, para colaborador).
     * Decisões: Novo convite para o mesmo e-mail substitui o anterior; quem já é membro não é convidado; validade
     *           convite.validade_dias.
     */
    @Transactional
    public ConviteDaStartup convidar(UUID eu, UUID startupId, String emailConvidado, PapelNaStartup papel, Set<SecaoDaStartup> secoes) {
        cadastro.exigirAdministrador(eu, startupId);
        String destino = emailConvidado == null ? "" : emailConvidado.strip().toLowerCase(Locale.ROOT);
        if (!TokensDeConvite.emailValido(destino)) {
            throw new RegraDeNegocioException("email_invalido", "Informe um e-mail válido.");
        }
        conferirSecoes(papel, secoes);
        contas.contaAtivaDoEmail(destino).ifPresent(id -> membros.findByStartupIdAndUsuarioIdAndSaiuEmIsNull(startupId, id)
                .ifPresent(m -> {
                    throw new RegraDeNegocioException("ja_membro", "Esta pessoa já faz parte da equipe.");
                }));
        Instant agora = relogio.instant();
        convites.findByStartupIdAndAceitoEmIsNullAndCanceladoEmIsNullOrderByCriadoEmDesc(startupId).stream()
                .filter(c -> c.getEmail().equals(destino)).forEach(c -> c.cancelar(agora));
        String token = TokensDeConvite.novoToken();
        ConviteDaStartup convite = convites.save(ConviteDaStartup.novo(startupId, destino, papel, secoes, TokensDeConvite.hash(token), eu, agora,
                agora.plus(parametros.duracao(Chaves.CONVITE_VALIDADE))));
        Startup startup = startup(startupId);
        String quem = contas.nome(eu).orElse("Um fundador");
        email.enviar(new EnvioDeEmail.MensagemEmail(destino, "Convite para a equipe da " + startup.getNome(),
                quem + " convidou você para a equipe da startup " + startup.getNome() + " no Orbit Hub, como "
                        + nomeDoPapel(papel) + ".\n\nPara aceitar, entre (ou crie sua conta de empreendedor com este e-mail) e abra o link:\n"
                        + conta.webUrl() + "/startups/convite?token=" + token + "\n\nO convite vale por "
                        + parametros.duracao(Chaves.CONVITE_VALIDADE).toDays() + " dias.", "convite_startup"));
        contas.contaAtivaDoEmail(destino).ifPresent(id -> notificacoes.notificar(id, TipoDeNotificacao.STARTUP,
                "Convite para " + startup.getNome(), quem + " convidou você para a equipe como " + nomeDoPapel(papel)
                        + ". O link do convite foi enviado para o seu e-mail.", null));
        return convite;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Convites pendentes
     * Objetivo: Listar os convites ainda válidos.
     * Decisões: Só o administrador vê.
     */
    @Transactional(readOnly = true)
    public List<ConviteDaStartup> convitesPendentes(UUID eu, UUID startupId) {
        cadastro.exigirAdministrador(eu, startupId);
        Instant agora = relogio.instant();
        return convites.findByStartupIdAndAceitoEmIsNullAndCanceladoEmIsNullOrderByCriadoEmDesc(startupId).stream()
                .filter(c -> c.pendente(agora)).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Cancelar convite
     * Objetivo: Invalidar um convite pendente.
     * Decisões: Convite de outra startup: 404.
     */
    @Transactional
    public void cancelarConvite(UUID eu, UUID startupId, UUID conviteId) {
        cadastro.exigirAdministrador(eu, startupId);
        convites.findById(conviteId).filter(c -> c.getStartupId().equals(startupId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado.")).cancelar(relogio.instant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Consultar convite
     * Objetivo: Mostrar de qual startup é o convite, com o papel e as seções.
     * Decisões: Convite inválido: mensagem única.
     */
    @Transactional(readOnly = true)
    public ResumoConvite consultarConvite(String token) {
        ConviteDaStartup c = conviteValido(token);
        return new ResumoConvite(startup(c.getStartupId()).getNome(), c.getPapel(), c.getSecoes(), c.getEmail(), c.getExpiraEm());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Aceitar convite
     * Objetivo: Tornar a conta logada membro da equipe com o papel e as seções do convite.
     * Decisões: Mesmo e-mail e papel de empreendedor; os administradores são avisados.
     */
    @Transactional
    public Startup aceitarConvite(UUID eu, String token) {
        ConviteDaStartup convite = conviteValido(token);
        if (!contas.emailParaAviso(eu).orElse("").equalsIgnoreCase(convite.getEmail())) {
            throw new RegraDeNegocioException("convite_outro_email",
                    "Este convite foi enviado para outro e-mail. Entre com a conta de " + convite.getEmail() + ".");
        }
        if (!contas.temPapelAtivo(eu, Papel.EMPREENDEDOR)) {
            throw new RegraDeNegocioException("sem_papel_empreendedor", "Ative o papel de empreendedor na sua conta para entrar na equipe.");
        }
        UUID startupId = convite.getStartupId();
        if (membros.findByStartupIdAndUsuarioIdAndSaiuEmIsNull(startupId, eu).isPresent()) {
            throw new RegraDeNegocioException("ja_membro", "Você já faz parte desta equipe.");
        }
        Instant agora = relogio.instant();
        MembroDaStartup membro = MembroDaStartup.novo(startupId, eu, convite.getPapel(), agora);
        membro.trocarPapel(convite.getPapel(), convite.getSecoes());
        membros.save(membro);
        convite.aceitar(eu, agora);
        Startup startup = startup(startupId);
        String nome = contas.nome(eu).orElse("Uma pessoa");
        notificacoes.notificarTodos(administradores(startupId).stream().filter(id -> !id.equals(eu)).toList(), TipoDeNotificacao.STARTUP,
                nome + " entrou na equipe da " + startup.getNome(), nome + " aceitou o convite como " + nomeDoPapel(convite.getPapel()) + ".",
                "/empreendedor/startups/" + startupId);
        return startup;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Trocar papel
     * Objetivo: Mudar o papel e as seções de um membro.
     * Decisões: Só o administrador; o último administrador não perde o papel; o membro é avisado.
     */
    @Transactional
    public void trocarPapel(UUID eu, UUID startupId, UUID alvo, PapelNaStartup novo, Set<SecaoDaStartup> secoes) {
        cadastro.exigirAdministrador(eu, startupId);
        conferirSecoes(novo, secoes);
        MembroDaStartup membro = membros.findByStartupIdAndUsuarioIdAndSaiuEmIsNull(startupId, alvo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado."));
        if (membro.getPapel() == PapelNaStartup.FUNDADOR_ADMINISTRADOR && novo != PapelNaStartup.FUNDADOR_ADMINISTRADOR) {
            exigirOutroAdministrador(startupId);
        }
        membro.trocarPapel(novo, secoes);
        notificacoes.notificar(alvo, TipoDeNotificacao.STARTUP, "Seu papel na " + startup(startupId).getNome() + " mudou",
                "Agora você é " + nomeDoPapel(novo) + " da startup.", "/empreendedor/startups/" + startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Remover ou sair
     * Objetivo: Encerrar o vínculo: a própria pessoa sai, ou o administrador remove alguém.
     * Decisões: O acesso acaba na hora; o último administrador não sai; quem é removido é avisado.
     */
    @Transactional
    public void encerrarVinculo(UUID eu, UUID startupId, UUID alvo) {
        boolean saindo = eu.equals(alvo);
        if (saindo) {
            cadastro.papel(eu, startupId);
        } else {
            cadastro.exigirAdministrador(eu, startupId);
        }
        MembroDaStartup membro = membros.findByStartupIdAndUsuarioIdAndSaiuEmIsNull(startupId, alvo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado."));
        if (membro.getPapel() == PapelNaStartup.FUNDADOR_ADMINISTRADOR) {
            exigirOutroAdministrador(startupId);
        }
        membro.encerrar(saindo ? "SAIU" : "REMOVIDO", relogio.instant());
        if (!saindo) {
            notificacoes.notificar(alvo, TipoDeNotificacao.STARTUP, "Você saiu da equipe da " + startup(startupId).getNome(),
                    "Um fundador administrador removeu você da equipe. Você não tem mais acesso à startup.", null);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Conferir seções
     * Objetivo: Exigir pelo menos uma seção para colaborador.
     * Decisões: Fundadores ignoram as seções (editam tudo).
     */
    private static void conferirSecoes(PapelNaStartup papel, Set<SecaoDaStartup> secoes) {
        if (papel == null) {
            throw new RegraDeNegocioException("papel_obrigatorio", "Escolha o papel.");
        }
        if (papel == PapelNaStartup.COLABORADOR && (secoes == null || secoes.isEmpty())) {
            throw new RegraDeNegocioException("colaborador_sem_secao", "Escolha pelo menos uma seção que o colaborador pode editar.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Outro administrador
     * Objetivo: Impedir que a startup fique sem fundador administrador.
     * Decisões: Mensagem que explica o que fazer.
     */
    private void exigirOutroAdministrador(UUID startupId) {
        if (membros.countByStartupIdAndPapelAndSaiuEmIsNull(startupId, PapelNaStartup.FUNDADOR_ADMINISTRADOR) <= 1) {
            throw new RegraDeNegocioException("ultimo_administrador",
                    "A startup precisa de pelo menos um fundador administrador. Indique outro antes.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Administradores
     * Objetivo: Listar os fundadores administradores ativos.
     * Decisões: Para avisos.
     */
    private List<UUID> administradores(UUID startupId) {
        return membros.findByStartupIdAndSaiuEmIsNullOrderByEntrouEm(startupId).stream()
                .filter(m -> m.getPapel() == PapelNaStartup.FUNDADOR_ADMINISTRADOR).map(MembroDaStartup::getUsuarioId).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Convite válido
     * Objetivo: Encontrar o convite do token e conferir que ainda pode ser aceito.
     * Decisões: Mensagem única.
     */
    private ConviteDaStartup conviteValido(String token) {
        return convites.findByTokenHash(TokensDeConvite.hash(token)).filter(c -> c.pendente(relogio.instant()))
                .orElseThrow(() -> new RegraDeNegocioException("convite_invalido", "Este convite não vale mais. Peça um novo a um fundador administrador."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Startup
     * Objetivo: Ler a startup pelo identificador.
     * Decisões: 404 se não existir.
     */
    private Startup startup(UUID id) {
        return startups.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Startup não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Nome do papel
     * Objetivo: Escrever o papel em português, para os avisos.
     * Decisões: Minúsculas.
     */
    static String nomeDoPapel(PapelNaStartup papel) {
        return switch (papel) {
            case FUNDADOR_ADMINISTRADOR -> "fundador administrador";
            case FUNDADOR -> "fundador";
            case COLABORADOR -> "colaborador";
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Membro
     * Objetivo: Levar um membro com nome, papel, seções e entrada.
     * Decisões: Sem e-mail.
     */
    public record Membro(UUID usuarioId, String nome, PapelNaStartup papel, Set<SecaoDaStartup> secoes, Instant entrouEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Resumo do convite
     * Objetivo: Levar o que a pessoa precisa ver antes de aceitar.
     * Decisões: Inclui o e-mail de destino.
     */
    public record ResumoConvite(String startup, PapelNaStartup papel, Set<SecaoDaStartup> secoes, String email, Instant expiraEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Item para o detalhe
     * Objetivo: Ajudar o detalhe da startup a saber as seções de quem pergunta.
     * Decisões: Vazio para fundadores.
     */
    @Transactional(readOnly = true)
    public Set<SecaoDaStartup> minhasSecoes(UUID eu, UUID startupId) {
        return membros.findByStartupIdAndUsuarioIdAndSaiuEmIsNull(startupId, eu).map(MembroDaStartup::getSecoes)
                .orElseThrow(() -> new AccessDeniedException("Sem acesso à startup."));
    }
}
