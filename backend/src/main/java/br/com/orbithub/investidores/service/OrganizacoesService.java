package br.com.orbithub.investidores.service;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.convites.TokensDeConvite;
import br.com.orbithub.compartilhado.documentos.Cnpj;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.Chaves;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.identidade.service.ContaProperties;
import br.com.orbithub.investidores.domain.ConviteDeOrganizacao;
import br.com.orbithub.investidores.domain.MembroDaOrganizacao;
import br.com.orbithub.investidores.domain.MembroDaOrganizacao.MotivoSaida;
import br.com.orbithub.investidores.domain.Organizacao;
import br.com.orbithub.investidores.domain.PapelNaOrganizacao;
import br.com.orbithub.investidores.repository.ConviteDeOrganizacaoRepository;
import br.com.orbithub.investidores.repository.MembroDaOrganizacaoRepository;
import br.com.orbithub.investidores.repository.OrganizacaoRepository;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Organizações investidoras
 * Objetivo: Criar organizações, convidar e gerenciar membros, e deixar cada pessoa ver e sair das organizações de que
 *           participa.
 * Decisões: Quem não é membro ativo recebe 404 (não revela que a organização existe). Só o administrador gerencia; ação
 *           sem permissão dá 403. A organização nunca fica sem administrador: o último não pode sair nem perder o papel.
 *           O convite vai por e-mail sempre (a pessoa pode não ter conta) e, se ela já tiver conta ativa, também pelo
 *           sino. Só entra quem tem papel de investidor e a conta com o mesmo e-mail do convite.
 */
@Service
public class OrganizacoesService {

    static final String TIPO_PESSOA_FISICA = "ANJO";

    private final OrganizacaoRepository organizacoes;
    private final MembroDaOrganizacaoRepository membros;
    private final ConviteDeOrganizacaoRepository convites;
    private final ListasService listas;
    private final ConsultaDeContas contas;
    private final NotificacoesService notificacoes;
    private final EnvioDeEmail email;
    private final ContaProperties conta;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, listas, contas, notificações, e-mail, parâmetros e relógio.
     * Decisões: Injeção por construtor.
     */
    public OrganizacoesService(OrganizacaoRepository organizacoes, MembroDaOrganizacaoRepository membros,
                               ConviteDeOrganizacaoRepository convites, ListasService listas, ConsultaDeContas contas,
                               NotificacoesService notificacoes, EnvioDeEmail email, ContaProperties conta,
                               LeituraDeParametros parametros, Clock relogio) {
        this.organizacoes = organizacoes;
        this.membros = membros;
        this.convites = convites;
        this.listas = listas;
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
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Criar organização
     * Objetivo: Cadastrar a organização e tornar o criador o primeiro administrador.
     * Decisões: Tipo da lista TIPO_INVESTIDOR, exceto investidor-anjo (pessoa física); CNPJ válido e ainda não usado.
     */
    @Transactional
    public Organizacao criar(UUID criador, Dados dados) {
        String tipo = conferirTipo(dados.tipo());
        String cnpj = Cnpj.validar(dados.cnpj()).orElseThrow(() -> new RegraDeNegocioException("cnpj_invalido",
                "CNPJ inválido. Confira os 14 dígitos."));
        if (organizacoes.existsByCnpj(cnpj)) {
            throw new RegraDeNegocioException("cnpj_em_uso",
                    "Já existe uma organização com este CNPJ. Peça a um administrador dela para convidar você.");
        }
        Instant agora = relogio.instant();
        Organizacao organizacao = organizacoes.save(Organizacao.nova(dados.nome(), tipo, cnpj, dados.site(),
                dados.descricao(), criador, agora));
        membros.save(MembroDaOrganizacao.novo(organizacao.getId(), criador, PapelNaOrganizacao.ADMINISTRADOR, agora));
        return organizacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Minhas organizações
     * Objetivo: Listar as organizações de que a pessoa participa, com o papel dela em cada uma.
     * Decisões: Só vínculos ativos.
     */
    @Transactional(readOnly = true)
    public List<Vinculo> minhas(UUID eu) {
        List<MembroDaOrganizacao> vinculos = membros.findByUsuarioIdAndSaiuEmIsNullOrderByEntrouEm(eu);
        Map<UUID, Organizacao> porId = organizacoes.findAllById(vinculos.stream().map(MembroDaOrganizacao::getOrganizacaoId)
                .toList()).stream().collect(Collectors.toMap(Organizacao::getId, o -> o));
        return vinculos.stream().map(v -> new Vinculo(porId.get(v.getOrganizacaoId()), v.getPapel())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Detalhe da organização
     * Objetivo: Devolver os dados, o papel de quem pergunta e a lista de membros.
     * Decisões: Qualquer membro ativo vê, inclusive o observador.
     */
    @Transactional(readOnly = true)
    public Detalhe detalhe(UUID eu, UUID organizacaoId) {
        MembroDaOrganizacao meu = vinculo(eu, organizacaoId);
        Organizacao organizacao = organizacao(organizacaoId);
        List<MembroDaOrganizacao> ativos = membros.findByOrganizacaoIdAndSaiuEmIsNullOrderByEntrouEm(organizacaoId);
        Map<UUID, String> nomes = contas.nomes(ativos.stream().map(MembroDaOrganizacao::getUsuarioId).toList());
        return new Detalhe(organizacao, meu.getPapel(), ativos.stream()
                .map(m -> new Membro(m.getUsuarioId(), nomes.getOrDefault(m.getUsuarioId(), "Conta removida"), m.getPapel(),
                        m.getEntrouEm()))
                .toList());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Editar dados
     * Objetivo: Trocar nome, tipo, site e descrição da organização.
     * Decisões: Só o administrador; o CNPJ não muda.
     */
    @Transactional
    public Organizacao editar(UUID eu, UUID organizacaoId, Dados dados) {
        exigirAdministrador(eu, organizacaoId);
        Organizacao organizacao = organizacao(organizacaoId);
        organizacao.atualizar(dados.nome(), conferirTipo(dados.tipo()), dados.site(), dados.descricao(), relogio.instant());
        return organizacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Convidar
     * Objetivo: Enviar um convite por e-mail para alguém entrar com o papel escolhido.
     * Decisões: Um novo convite para o mesmo e-mail cancela o anterior; quem já é membro não é convidado. O link vale por
     *           convite.validade_dias.
     */
    @Transactional
    public ConviteDeOrganizacao convidar(UUID eu, UUID organizacaoId, String emailConvidado, PapelNaOrganizacao papel) {
        exigirAdministrador(eu, organizacaoId);
        String destino = emailConvidado == null ? "" : emailConvidado.strip().toLowerCase(Locale.ROOT);
        if (!TokensDeConvite.emailValido(destino)) {
            throw new RegraDeNegocioException("email_invalido", "Informe um e-mail válido.");
        }
        contas.contaAtivaDoEmail(destino).ifPresent(id -> membros.findByOrganizacaoIdAndUsuarioIdAndSaiuEmIsNull(organizacaoId, id)
                .ifPresent(m -> {
                    throw new RegraDeNegocioException("ja_membro", "Esta pessoa já faz parte da organização.");
                }));
        Instant agora = relogio.instant();
        convites.findByOrganizacaoIdAndAceitoEmIsNullAndCanceladoEmIsNullOrderByCriadoEmDesc(organizacaoId).stream()
                .filter(c -> c.getEmail().equals(destino)).forEach(c -> c.cancelar(agora));
        String token = TokensDeConvite.novoToken();
        ConviteDeOrganizacao convite = convites.save(ConviteDeOrganizacao.novo(organizacaoId, destino, papel, TokensDeConvite.hash(token), eu,
                agora, agora.plus(parametros.duracao(Chaves.CONVITE_VALIDADE))));
        Organizacao organizacao = organizacao(organizacaoId);
        String quem = contas.nome(eu).orElse("Um administrador");
        String link = conta.webUrl() + "/organizacoes/convite?token=" + token;
        email.enviar(new EnvioDeEmail.MensagemEmail(destino, "Convite para a organização " + organizacao.getNome(),
                quem + " convidou você para participar da organização " + organizacao.getNome() + " no Orbit Hub, como "
                        + nomeDoPapel(papel) + ".\n\nPara aceitar, entre (ou crie sua conta de investidor com este e-mail) e "
                        + "abra o link:\n" + link + "\n\nO convite vale por "
                        + parametros.duracao(Chaves.CONVITE_VALIDADE).toDays() + " dias.", "convite_organizacao"));
        contas.contaAtivaDoEmail(destino).ifPresent(id -> notificacoes.notificar(id, TipoDeNotificacao.ORGANIZACAO,
                "Convite para " + organizacao.getNome(), quem + " convidou você para a organização como "
                        + nomeDoPapel(papel) + ". O link do convite foi enviado para o seu e-mail.", null));
        return convite;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Convites pendentes
     * Objetivo: Listar os convites ainda válidos da organização.
     * Decisões: Só o administrador vê.
     */
    @Transactional(readOnly = true)
    public List<ConviteDeOrganizacao> convitesPendentes(UUID eu, UUID organizacaoId) {
        exigirAdministrador(eu, organizacaoId);
        Instant agora = relogio.instant();
        return convites.findByOrganizacaoIdAndAceitoEmIsNullAndCanceladoEmIsNullOrderByCriadoEmDesc(organizacaoId).stream()
                .filter(c -> c.pendente(agora)).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Cancelar convite
     * Objetivo: Invalidar um convite antes do aceite.
     * Decisões: Convite de outra organização não é encontrado.
     */
    @Transactional
    public void cancelarConvite(UUID eu, UUID organizacaoId, UUID conviteId) {
        exigirAdministrador(eu, organizacaoId);
        convites.findById(conviteId).filter(c -> c.getOrganizacaoId().equals(organizacaoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado."))
                .cancelar(relogio.instant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Consultar convite
     * Objetivo: Mostrar, antes do aceite, de qual organização é o convite e com qual papel.
     * Decisões: Convite inválido, vencido, usado ou cancelado: mesma mensagem, sem detalhar.
     */
    @Transactional(readOnly = true)
    public ResumoConvite consultarConvite(String token) {
        ConviteDeOrganizacao convite = conviteValido(token);
        Organizacao organizacao = organizacao(convite.getOrganizacaoId());
        return new ResumoConvite(organizacao.getNome(), organizacao.getTipo(), convite.getPapel(), convite.getEmail(),
                convite.getExpiraEm());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Aceitar convite
     * Objetivo: Tornar a pessoa logada membro da organização com o papel do convite.
     * Decisões: A conta precisa ter o e-mail do convite e o papel de investidor. Os administradores são avisados.
     */
    @Transactional
    public Organizacao aceitarConvite(UUID eu, String token) {
        ConviteDeOrganizacao convite = conviteValido(token);
        String meuEmail = contas.emailParaAviso(eu).orElse("");
        if (!meuEmail.equalsIgnoreCase(convite.getEmail())) {
            throw new RegraDeNegocioException("convite_outro_email",
                    "Este convite foi enviado para outro e-mail. Entre com a conta de " + convite.getEmail() + ".");
        }
        if (!contas.investidoraAtiva(eu)) {
            throw new RegraDeNegocioException("sem_papel_investidor",
                    "Ative o papel de investidor na sua conta para entrar na organização.");
        }
        UUID organizacaoId = convite.getOrganizacaoId();
        if (membros.findByOrganizacaoIdAndUsuarioIdAndSaiuEmIsNull(organizacaoId, eu).isPresent()) {
            throw new RegraDeNegocioException("ja_membro", "Você já faz parte desta organização.");
        }
        Instant agora = relogio.instant();
        membros.save(MembroDaOrganizacao.novo(organizacaoId, eu, convite.getPapel(), agora));
        convite.aceitar(eu, agora);
        Organizacao organizacao = organizacao(organizacaoId);
        String nome = contas.nome(eu).orElse("Uma pessoa");
        notificacoes.notificarTodos(administradores(organizacaoId).stream().filter(id -> !id.equals(eu)).toList(),
                TipoDeNotificacao.ORGANIZACAO, nome + " entrou em " + organizacao.getNome(),
                nome + " aceitou o convite e entrou como " + nomeDoPapel(convite.getPapel()) + ".",
                "/investidor/organizacoes/" + organizacaoId);
        return organizacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Trocar o papel de um membro
     * Objetivo: Mudar o papel de alguém da organização.
     * Decisões: Só o administrador; o último administrador não perde o papel. O membro é avisado.
     */
    @Transactional
    public void trocarPapel(UUID eu, UUID organizacaoId, UUID usuarioAlvo, PapelNaOrganizacao novo) {
        exigirAdministrador(eu, organizacaoId);
        MembroDaOrganizacao alvo = membros.findByOrganizacaoIdAndUsuarioIdAndSaiuEmIsNull(organizacaoId, usuarioAlvo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado."));
        if (alvo.getPapel() == novo) {
            return;
        }
        if (alvo.getPapel() == PapelNaOrganizacao.ADMINISTRADOR) {
            exigirOutroAdministrador(organizacaoId);
        }
        alvo.trocarPapel(novo);
        Organizacao organizacao = organizacao(organizacaoId);
        notificacoes.notificar(usuarioAlvo, TipoDeNotificacao.ORGANIZACAO, "Seu papel em " + organizacao.getNome() + " mudou",
                "Agora você é " + nomeDoPapel(novo) + " da organização.", "/investidor/organizacoes/" + organizacaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Remover ou sair
     * Objetivo: Encerrar o vínculo de um membro: a própria pessoa sai, ou o administrador remove alguém.
     * Decisões: O acesso aos dados da organização acaba na hora (o vínculo é conferido a cada requisição). O último
     *           administrador não sai. Quem é removido é avisado.
     */
    @Transactional
    public void encerrarVinculo(UUID eu, UUID organizacaoId, UUID usuarioAlvo) {
        boolean saindo = eu.equals(usuarioAlvo);
        if (saindo) {
            vinculo(eu, organizacaoId);
        } else {
            exigirAdministrador(eu, organizacaoId);
        }
        MembroDaOrganizacao alvo = membros.findByOrganizacaoIdAndUsuarioIdAndSaiuEmIsNull(organizacaoId, usuarioAlvo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado."));
        if (alvo.getPapel() == PapelNaOrganizacao.ADMINISTRADOR) {
            exigirOutroAdministrador(organizacaoId);
        }
        alvo.encerrar(saindo ? MotivoSaida.SAIU : MotivoSaida.REMOVIDO, relogio.instant());
        if (!saindo) {
            Organizacao organizacao = organizacao(organizacaoId);
            notificacoes.notificar(usuarioAlvo, TipoDeNotificacao.ORGANIZACAO, "Você saiu de " + organizacao.getNome(),
                    "Um administrador removeu você da organização. Você não tem mais acesso aos dados dela.", null);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Vínculo ativo
     * Objetivo: Exigir que a pessoa seja membro ativo da organização.
     * Decisões: Sem vínculo: 404, sem revelar se a organização existe.
     */
    private MembroDaOrganizacao vinculo(UUID eu, UUID organizacaoId) {
        return membros.findByOrganizacaoIdAndUsuarioIdAndSaiuEmIsNull(organizacaoId, eu)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Organização não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Exigir administrador
     * Objetivo: Recusar quem é membro mas não administrador.
     * Decisões: 403, registrado como acesso negado.
     */
    private void exigirAdministrador(UUID eu, UUID organizacaoId) {
        if (vinculo(eu, organizacaoId).getPapel() != PapelNaOrganizacao.ADMINISTRADOR) {
            throw new AccessDeniedException("Só o administrador da organização pode fazer isso.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Outro administrador
     * Objetivo: Impedir que a organização fique sem administrador.
     * Decisões: Mensagem que explica o que fazer.
     */
    private void exigirOutroAdministrador(UUID organizacaoId) {
        if (membros.countByOrganizacaoIdAndPapelAndSaiuEmIsNull(organizacaoId, PapelNaOrganizacao.ADMINISTRADOR) <= 1) {
            throw new RegraDeNegocioException("ultimo_administrador",
                    "A organização precisa de pelo menos um administrador. Torne outro membro administrador antes.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: CNPJ da organização
     * Objetivo: Devolver o CNPJ de uma organização, para a checagem de conflito de interesse.
     * Decisões: Vazio se não existir.
     */
    @Transactional(readOnly = true)
    public Optional<String> cnpj(UUID organizacaoId) {
        return organizacoes.findById(organizacaoId).map(Organizacao::getCnpj);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Nome da organização
     * Objetivo: Devolver o nome de uma organização, para telas de outros módulos (por exemplo, a fila de verificação).
     * Decisões: Vazio se não existir.
     */
    @Transactional(readOnly = true)
    public Optional<String> nome(UUID organizacaoId) {
        return organizacoes.findById(organizacaoId).map(Organizacao::getNome);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Administradores da organização
     * Objetivo: Listar os administradores ativos, para avisos de outros módulos (por exemplo, o resultado da verificação).
     * Decisões: Serviço público; sem conferir quem pergunta.
     */
    @Transactional(readOnly = true)
    public List<UUID> administradoresDe(UUID organizacaoId) {
        return administradores(organizacaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Administradores
     * Objetivo: Listar as contas dos administradores ativos.
     * Decisões: Para avisos.
     */
    private List<UUID> administradores(UUID organizacaoId) {
        return membros.findByOrganizacaoIdAndSaiuEmIsNullOrderByEntrouEm(organizacaoId).stream()
                .filter(m -> m.getPapel() == PapelNaOrganizacao.ADMINISTRADOR).map(MembroDaOrganizacao::getUsuarioId)
                .toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Organização
     * Objetivo: Ler a organização pelo identificador.
     * Decisões: 404 se não existir.
     */
    private Organizacao organizacao(UUID organizacaoId) {
        return organizacoes.findById(organizacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Organização não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Conferir o tipo
     * Objetivo: Aceitar só um tipo ativo da lista que seja de entidade, não de pessoa física.
     * Decisões: Investidor-anjo atua como pessoa, sem organização.
     */
    private String conferirTipo(String tipo) {
        if (TIPO_PESSOA_FISICA.equals(tipo)) {
            throw new RegraDeNegocioException("tipo_pessoa_fisica",
                    "Investidor-anjo atua como pessoa física. Escolha o tipo da entidade (fundo, rede, CVC...).");
        }
        return listas.exigirAtivo(Lista.TIPO_INVESTIDOR, tipo).getCodigo();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Convite válido
     * Objetivo: Encontrar o convite do token e conferir que ainda pode ser aceito.
     * Decisões: Mensagem única para qualquer problema.
     */
    private ConviteDeOrganizacao conviteValido(String token) {
        return convites.findByTokenHash(TokensDeConvite.hash(token)).filter(c -> c.pendente(relogio.instant()))
                .orElseThrow(() -> new RegraDeNegocioException("convite_invalido",
                        "Este convite não vale mais. Peça um novo ao administrador da organização."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Nome do papel
     * Objetivo: Escrever o papel em português, para os textos de aviso.
     * Decisões: Minúsculas, para o meio da frase.
     */
    static String nomeDoPapel(PapelNaOrganizacao papel) {
        return switch (papel) {
            case ADMINISTRADOR -> "administrador";
            case MEMBRO -> "membro";
            case OBSERVADOR -> "observador";
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Dados da organização
     * Objetivo: Levar os campos informados no cadastro ou na edição.
     * Decisões: O CNPJ só é usado no cadastro.
     */
    public record Dados(String nome, String tipo, String cnpj, String site, String descricao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Vínculo
     * Objetivo: Levar uma organização com o papel da pessoa nela.
     * Decisões: Para a lista "minhas organizações" e o seletor de atuação.
     */
    public record Vinculo(Organizacao organizacao, PapelNaOrganizacao papel) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Membro
     * Objetivo: Levar um membro ativo com nome, papel e entrada.
     * Decisões: Sem e-mail, para não expor dado pessoal a todos os membros.
     */
    public record Membro(UUID usuarioId, String nome, PapelNaOrganizacao papel, Instant entrouEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Detalhe
     * Objetivo: Levar a organização, o papel de quem pergunta e os membros.
     * Decisões: Os convites ficam numa consulta à parte, só do administrador.
     */
    public record Detalhe(Organizacao organizacao, PapelNaOrganizacao meuPapel, List<Membro> membros) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Resumo do convite
     * Objetivo: Levar o que a pessoa precisa ver antes de aceitar.
     * Decisões: Inclui o e-mail de destino, para quem estiver na conta errada perceber.
     */
    public record ResumoConvite(String organizacao, String tipo, PapelNaOrganizacao papel, String email,
                                Instant expiraEm) {
    }
}
