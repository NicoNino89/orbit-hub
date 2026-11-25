package br.com.orbithub.agenda.service;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.conexoes.domain.Conexao;
import br.com.orbithub.conexoes.domain.ConexaoFinalizada;
import br.com.orbithub.conexoes.service.ConducaoDaRodadaService;
import br.com.orbithub.conexoes.service.ConexoesService;
import br.com.orbithub.empreendimentos.domain.InvestidorBloqueado;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.identidade.domain.ContaSuspensa;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.investidores.service.AtuacaoService;
import br.com.orbithub.investidores.service.OrganizacoesService;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-94, KAN-95, KAN-96, KAN-97, KAN-98
 * Título: Agenda de reuniões
 * Objetivo: Propor reunião numa conexão aberta com até 3 horários, aceitar, contrapropor ou recusar, mandar o convite
 *           (.ics), lembrar, remarcar e cancelar (marcando o tardio), registrar o resultado com próximo passo e ausência,
 *           guardar anotações privadas, convocar reuniões coletivas da rodada e cancelar sozinho quando a conexão termina
 *           ou o investidor é bloqueado.
 * Decisões: Lado da vez = quem precisa responder. Horários com antecedência mínima de reuniao.antecedencia_minima_horas;
 *           contrapropostas até reuniao.max_contrapropostas. Participantes: quem age, os membros da organização indicados
 *           e o outro lado (investidor da conexão ou fundadores). Horários guardados em UTC; a tela mostra no fuso de cada um.
 *           Ausência registrada pela outra parte conta para quem faltou; ao chegar a reuniao.limite_ausencias em 90 dias, o
 *           atendimento (administradores) recebe alerta. Reunião coletiva: horário definido por quem convoca (fundador
 *           administrador ou líder), presença confirmada por cada convidado.
 */
@Service
public class AgendaService {

    static final String ANTECEDENCIA = "reuniao.antecedencia_minima_horas";
    static final String MAX_CONTRAPROPOSTAS = "reuniao.max_contrapropostas";
    static final String CANCELAMENTO = "reuniao.antecedencia_cancelamento_horas";
    static final String LIMITE_AUSENCIAS = "reuniao.limite_ausencias";
    static final String PRAZO_REGISTRO = "reuniao.prazo_registro_dias";
    static final String LEMBRETES = "reuniao.lembretes";
    static final Set<Conexao.Situacao> ABERTAS = EnumSet.of(Conexao.Situacao.MATCH, Conexao.Situacao.EM_ANALISE, Conexao.Situacao.EM_NEGOCIACAO);
    static final DateTimeFormatter ICS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-94
     * Título: Tipos e formatos
     * Objetivo: Enumerar os tipos de reunião, os formatos, os resultados e os próximos passos.
     * Decisões: Mesmos nomes da API.
     */
    public enum Tipo { APRESENTACAO, DUE_DILIGENCE, NEGOCIACAO, RODADA }

    public enum Formato { VIDEO, PRESENCIAL }

    public enum ResultadoDaReuniao { REALIZADA, AUSENCIA_OUTRA_PARTE, REMARCADA, CANCELADA }

    public enum ProximoPasso { ANALISE, NEGOCIACAO, NOVA_REUNIAO, ENCERRAR }

    private final JdbcTemplate jdbc;
    private final ConexoesService conexoes;
    private final ConducaoDaRodadaService rodadas;
    private final StartupsService startups;
    private final AtuacaoService atuacao;
    private final OrganizacoesService organizacoes;
    private final ConsultaDeContas contas;
    private final ListasService listas;
    private final NotificacoesService notificacoes;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-94
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public AgendaService(JdbcTemplate jdbc, ConexoesService conexoes, ConducaoDaRodadaService rodadas, StartupsService startups,
                         AtuacaoService atuacao, OrganizacoesService organizacoes, ConsultaDeContas contas, ListasService listas,
                         NotificacoesService notificacoes, LeituraDeParametros parametros, Clock relogio) {
        this.jdbc = jdbc;
        this.conexoes = conexoes;
        this.rodadas = rodadas;
        this.startups = startups;
        this.atuacao = atuacao;
        this.organizacoes = organizacoes;
        this.contas = contas;
        this.listas = listas;
        this.notificacoes = notificacoes;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-94
     * Título: Propor reunião
     * Objetivo: Criar a proposta numa conexão aberta, com até 3 horários, e avisar o outro lado.
     * Decisões: Vídeo exige o link; presencial, o endereço. Membros da organização só se quem propõe atua por ela.
     */
    @Transactional
    public Reuniao propor(UUID eu, String organizacao, UUID conexaoId, Proposta p) {
        Conexao c = conexoes.ler(conexaoId);
        if (!ABERTAS.contains(c.getSituacao())) {
            throw new RegraDeNegocioException("conexao_fechada", "Proponha reuniões só em conexões em Match, em análise ou em negociação.");
        }
        Conexao.Lado lado = lado(eu, organizacao, c);
        conferir(p, p.opcoes());
        Set<UUID> meus = new LinkedHashSet<>(List.of(eu));
        if (p.membros() != null && !p.membros().isEmpty()) {
            if (lado != Conexao.Lado.INVESTIDOR || c.getOrganizacaoId() == null) {
                throw new RegraDeNegocioException("membros_invalidos", "Só quem atua por uma organização indica outros membros.");
            }
            Set<UUID> daOrg = new java.util.HashSet<>(organizacoes.detalhe(eu, c.getOrganizacaoId()).membros().stream()
                    .map(OrganizacoesService.Membro::usuarioId).toList());
            if (!daOrg.containsAll(p.membros())) {
                throw new RegraDeNegocioException("membros_invalidos", "Indique só membros da organização.");
            }
            meus.addAll(p.membros());
        }
        Set<UUID> outros = new LinkedHashSet<>(lado == Conexao.Lado.INVESTIDOR ? startups.fundadores(c.getStartupId(), false) : List.of(c.getPessoaId()));
        UUID id = inserir(c.getId(), null, c.getStartupId(), p, false, outro(lado), eu);
        opcoes(id, p.opcoes());
        participantes(id, meus, lado);
        participantes(id, outros, outro(lado));
        notificacoes.notificarTodos(outros, TipoDeNotificacao.REUNIAO_CONVITE, "Proposta de reunião",
                contas.nome(eu).orElse("Alguém") + " propôs uma reunião. Escolha um horário na Agenda.", "/agenda");
        return reuniao(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-95
     * Título: Aceitar horário
     * Objetivo: Confirmar um dos horários propostos e mandar o convite a todos.
     * Decisões: Só o lado da vez; o horário precisa estar no futuro.
     */
    @Transactional
    public Reuniao aceitar(UUID eu, UUID reuniaoId, int ordem) {
        Reuniao r = daVez(eu, reuniaoId);
        Instant inicio = r.opcoes().stream().filter(o -> o.ordem() == ordem).map(Opcao::inicio).findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("opcao_invalida", "Escolha um dos horários propostos."));
        if (!inicio.isAfter(relogio.instant())) {
            throw new RegraDeNegocioException("horario_passado", "Este horário já passou. Contraproponha outros.");
        }
        jdbc.update("UPDATE reuniao SET situacao = 'CONFIRMADA', inicio = ?, lado_da_vez = NULL, atualizada_em = ? WHERE id = ?",
                Timestamp.from(inicio), agora(), reuniaoId);
        avisar(r, eu, TipoDeNotificacao.REUNIAO_CONVITE, "Reunião confirmada", "A reunião foi confirmada. Baixe o convite na Agenda.");
        return reuniao(reuniaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-95
     * Título: Contrapropor
     * Objetivo: Trocar os horários por até 3 novos, passando a vez para o outro lado.
     * Decisões: Até reuniao.max_contrapropostas vezes.
     */
    @Transactional
    public Reuniao contrapropor(UUID eu, UUID reuniaoId, List<Instant> novas) {
        Reuniao r = daVez(eu, reuniaoId);
        if (r.contrapropostas() >= parametros.inteiro(MAX_CONTRAPROPOSTAS)) {
            throw new RegraDeNegocioException("contrapropostas_esgotadas", "O limite de contrapropostas foi atingido. Recuse ou aceite um horário.");
        }
        conferirHorarios(novas);
        jdbc.update("DELETE FROM reuniao_opcao WHERE reuniao_id = ?", reuniaoId);
        opcoes(reuniaoId, novas);
        jdbc.update("UPDATE reuniao SET lado_da_vez = ?, contrapropostas = contrapropostas + 1, atualizada_em = ? WHERE id = ?",
                outro(Conexao.Lado.valueOf(r.ladoDaVez())).name(), agora(), reuniaoId);
        avisar(r, eu, TipoDeNotificacao.REUNIAO_CONVITE, "Novos horários para a reunião", "Há novos horários propostos. Responda na Agenda.");
        return reuniao(reuniaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-95
     * Título: Recusar
     * Objetivo: Recusar a proposta com motivo da lista, sem mudar a conexão.
     * Decisões: Lista MOTIVO_RECUSA_REUNIAO.
     */
    @Transactional
    public Reuniao recusar(UUID eu, UUID reuniaoId, String motivo) {
        Reuniao r = daVez(eu, reuniaoId);
        String codigo = listas.exigirAtivo(Lista.MOTIVO_RECUSA_REUNIAO, motivo).getCodigo();
        jdbc.update("UPDATE reuniao SET situacao = 'RECUSADA', motivo = ?, lado_da_vez = NULL, atualizada_em = ? WHERE id = ?", codigo, agora(), reuniaoId);
        avisar(r, eu, TipoDeNotificacao.REUNIAO_ALTERADA, "Reunião recusada",
                "A proposta foi recusada: " + listas.rotulo(Lista.MOTIVO_RECUSA_REUNIAO, codigo) + ".");
        return reuniao(reuniaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-96
     * Título: Cancelar
     * Objetivo: Cancelar a reunião proposta ou confirmada, com motivo, marcando o cancelamento tardio.
     * Decisões: Qualquer participante; tardio quando faltam menos de reuniao.antecedencia_cancelamento_horas.
     */
    @Transactional
    public Reuniao cancelar(UUID eu, UUID reuniaoId, String motivo) {
        Reuniao r = deParticipante(eu, reuniaoId);
        if (!"PROPOSTA".equals(r.situacao()) && !"CONFIRMADA".equals(r.situacao())) {
            throw new RegraDeNegocioException("reuniao_encerrada", "Esta reunião não pode mais ser cancelada.");
        }
        String m = motivo == null || motivo.isBlank() ? null : motivo.strip();
        if (m == null) {
            throw new RegraDeNegocioException("motivo_obrigatorio", "Diga o motivo do cancelamento.");
        }
        jdbc.update("UPDATE reuniao SET situacao = 'CANCELADA', motivo = ?, tardia = ?, lado_da_vez = NULL, atualizada_em = ? WHERE id = ?",
                m.length() > 300 ? m.substring(0, 300) : m, tardio(r), agora(), reuniaoId);
        avisar(r, eu, TipoDeNotificacao.REUNIAO_ALTERADA, "Reunião cancelada", "A reunião foi cancelada: " + m);
        return reuniao(reuniaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-96
     * Título: Remarcar
     * Objetivo: Encerrar a reunião confirmada como remarcada e abrir uma proposta nova com novos horários.
     * Decisões: A proposta nova aguarda o outro lado. Tardio pela mesma regra do cancelamento.
     */
    @Transactional
    public Reuniao remarcar(UUID eu, String organizacao, UUID reuniaoId, List<Instant> novas) {
        Reuniao r = deParticipante(eu, reuniaoId);
        if (!"CONFIRMADA".equals(r.situacao()) || r.conexaoId() == null) {
            throw new RegraDeNegocioException("reuniao_nao_confirmada", "Só reuniões individuais confirmadas podem ser remarcadas.");
        }
        conferirHorarios(novas);
        jdbc.update("UPDATE reuniao SET situacao = 'REMARCADA', tardia = ?, atualizada_em = ? WHERE id = ?", tardio(r), agora(), reuniaoId);
        Conexao c = conexoes.ler(r.conexaoId());
        Conexao.Lado lado = lado(eu, organizacao, c);
        UUID nova = inserir(r.conexaoId(), null, r.startupId(), new Proposta(Tipo.valueOf(r.tipo()), Formato.valueOf(r.formato()), r.local(), r.pauta(),
                r.duracaoMinutos(), novas, List.of()), false, outro(lado), eu);
        opcoes(nova, novas);
        jdbc.update("INSERT INTO reuniao_participante (reuniao_id, usuario_id, lado) SELECT ?, usuario_id, lado FROM reuniao_participante WHERE reuniao_id = ?",
                nova, reuniaoId);
        avisar(r, eu, TipoDeNotificacao.REUNIAO_ALTERADA, "Reunião remarcada", "A reunião foi remarcada. Escolha um dos novos horários na Agenda.");
        return reuniao(nova);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-97
     * Título: Registrar resultado
     * Objetivo: Registrar, por lado, como foi a reunião e o próximo passo, aplicando a mudança na conexão se pedido.
     * Decisões: Só depois do horário. A ausência conta para quem faltou e pode gerar alerta ao atendimento. Na coletiva,
     *           só quem convocou registra, com resumo opcional.
     */
    @Transactional
    public Reuniao registrar(UUID eu, String organizacao, UUID reuniaoId, Registro registro) {
        Reuniao r = deParticipante(eu, reuniaoId);
        if (!"CONFIRMADA".equals(r.situacao()) || r.inicio() == null || r.inicio().isAfter(relogio.instant())) {
            throw new RegraDeNegocioException("antes_da_reuniao", "Registre o resultado depois do horário da reunião confirmada.");
        }
        String lado;
        if (r.coletiva()) {
            if (!eu.equals(r.convocadaPor())) {
                throw new AccessDeniedException("Na reunião coletiva, quem convocou registra o resultado.");
            }
            lado = "CONVOCACAO";
            jdbc.update("UPDATE reuniao SET resumo = ? WHERE id = ?", registro.resumo(), reuniaoId);
        } else {
            lado = r.participantes().stream().filter(p -> p.usuarioId().equals(eu)).map(Participante::lado).findFirst().orElseThrow();
        }
        jdbc.update("INSERT INTO reuniao_registro (reuniao_id, lado, resultado, proximo_passo, por, em) VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (reuniao_id, lado) DO UPDATE SET resultado = EXCLUDED.resultado, proximo_passo = EXCLUDED.proximo_passo, "
                + "por = EXCLUDED.por, em = EXCLUDED.em", reuniaoId, lado, registro.resultado().name(),
                registro.proximoPasso() == null ? null : registro.proximoPasso().name(), eu, agora());
        if (registro.resultado() == ResultadoDaReuniao.AUSENCIA_OUTRA_PARTE && !r.coletiva()) {
            conferirAusencias(r, lado);
        }
        if (Boolean.TRUE.equals(registro.aplicarNaConexao()) && r.conexaoId() != null && registro.proximoPasso() != null) {
            switch (registro.proximoPasso()) {
                case ANALISE -> conexoes.avancar(eu, organizacao, r.conexaoId(), Conexao.Situacao.EM_ANALISE);
                case NEGOCIACAO -> conexoes.avancar(eu, organizacao, r.conexaoId(), Conexao.Situacao.EM_NEGOCIACAO);
                case ENCERRAR -> conexoes.encerrar(eu, organizacao, r.conexaoId(), "OUTRO");
                default -> { }
            }
        }
        return reuniao(reuniaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-97
     * Título: Anotar
     * Objetivo: Guardar a anotação privada de quem escreve, opcionalmente compartilhada com a organização.
     * Decisões: Uma por pessoa e reunião; até 4.000 caracteres.
     */
    @Transactional
    public void anotar(UUID eu, UUID reuniaoId, String texto, boolean compartilhar) {
        deParticipante(eu, reuniaoId);
        String t = texto == null ? "" : texto.strip();
        if (t.isEmpty() || t.length() > 4000) {
            throw new RegraDeNegocioException("anotacao_invalida", "Escreva a anotação (até 4.000 caracteres).");
        }
        jdbc.update("INSERT INTO reuniao_anotacao (reuniao_id, usuario_id, texto, compartilhada, em) VALUES (?, ?, ?, ?, ?) ON CONFLICT (reuniao_id, usuario_id) "
                + "DO UPDATE SET texto = EXCLUDED.texto, compartilhada = EXCLUDED.compartilhada, em = EXCLUDED.em", reuniaoId, eu, t, compartilhar, agora());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-98
     * Título: Convocar reunião da rodada
     * Objetivo: Convocar, com horário definido, todos ou parte dos participantes da rodada.
     * Decisões: Fundador administrador ou investidor líder. Já nasce confirmada; cada convidado responde a presença.
     */
    @Transactional
    public Reuniao convocarDaRodada(UUID eu, String organizacao, UUID startupId, Instant inicio, Proposta p, List<UUID> participantes) {
        ConducaoDaRodadaService.Painel painel;
        boolean admin = startups.fundadores(startupId, true).contains(eu);
        UUID rodadaId;
        List<ConducaoDaRodadaService.Participante> todos;
        if (admin) {
            UUID id = jdbc.queryForList("SELECT id FROM rodada WHERE startup_id = ? AND situacao IN ('ABERTA', 'EM_NEGOCIACAO')", UUID.class, startupId)
                    .stream().findFirst().orElseThrow(() -> new RegraDeNegocioException("sem_rodada", "Não há rodada em curso para convocar."));
            painel = rodadas.daStartup(eu, id);
            rodadaId = painel.id();
            todos = painel.participantes();
        } else {
            ConducaoDaRodadaService.ParaInvestidor r = rodadas.paraInvestidor(eu, organizacao, startupId);
            if (!r.souLider()) {
                throw new AccessDeniedException("Só o fundador administrador ou o investidor líder convocam reuniões da rodada.");
            }
            rodadaId = r.id();
            todos = jdbc.query("SELECT id, pessoa_id FROM rodada_participante WHERE rodada_id = ? AND situacao = 'ATIVO'",
                    (rs, i) -> new ConducaoDaRodadaService.Participante(rs.getObject(1, UUID.class), rodadaId, null, rs.getObject(2, UUID.class),
                            null, null, null, "ATIVO", null, null, null, null), rodadaId);
        }
        conferir(new Proposta(Tipo.RODADA, p.formato(), p.local(), p.pauta(), p.duracaoMinutos(), List.of(inicio), List.of()), List.of(inicio));
        List<ConducaoDaRodadaService.Participante> convidados = todos.stream().filter(x -> "ATIVO".equals(x.situacao()))
                .filter(x -> participantes == null || participantes.isEmpty() || participantes.contains(x.id())).toList();
        if (convidados.isEmpty()) {
            throw new RegraDeNegocioException("sem_convidados", "Escolha ao menos um participante da rodada.");
        }
        UUID id = inserir(null, rodadaId, startupId, new Proposta(Tipo.RODADA, p.formato(), p.local(), p.pauta(), p.duracaoMinutos(), List.of(inicio),
                List.of()), true, null, eu);
        jdbc.update("UPDATE reuniao SET situacao = 'CONFIRMADA', inicio = ? WHERE id = ?", Timestamp.from(inicio), id);
        Set<UUID> pessoas = new LinkedHashSet<>(List.of(eu));
        if (!admin) {
            pessoas.addAll(startups.fundadores(startupId, true));
        }
        convidados.forEach(x -> pessoas.add(x.pessoaId()));
        for (UUID u : pessoas) {
            jdbc.update("INSERT INTO reuniao_participante (reuniao_id, usuario_id, lado, resposta) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING", id, u,
                    u.equals(eu) ? "CONVOCACAO" : "CONVIDADO", u.equals(eu) ? "CONFIRMADO" : "PENDENTE");
        }
        avisar(reuniao(id), eu, TipoDeNotificacao.REUNIAO_CONVITE, "Reunião da rodada", "Você foi convidado para uma reunião da rodada. Confirme a presença.");
        return reuniao(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-98
     * Título: Responder presença
     * Objetivo: Confirmar, recusar ou dizer "talvez" para a reunião coletiva.
     * Decisões: Só convidados.
     */
    @Transactional
    public Reuniao responderPresenca(UUID eu, UUID reuniaoId, String resposta) {
        Reuniao r = deParticipante(eu, reuniaoId);
        if (!r.coletiva() || !Set.of("CONFIRMADO", "RECUSADO", "TALVEZ").contains(resposta)) {
            throw new RegraDeNegocioException("resposta_invalida", "Responda confirmado, recusado ou talvez numa reunião coletiva.");
        }
        jdbc.update("UPDATE reuniao_participante SET resposta = ? WHERE reuniao_id = ? AND usuario_id = ?", resposta, reuniaoId, eu);
        return reuniao(reuniaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-95
     * Título: Minha agenda
     * Objetivo: Listar as reuniões em que a pessoa participa, futuras e passadas, com as próprias anotações.
     * Decisões: Mais recentes primeiro.
     */
    @Transactional(readOnly = true)
    public List<Reuniao> minhas(UUID eu) {
        return jdbc.queryForList("SELECT reuniao_id FROM reuniao_participante p JOIN reuniao r ON r.id = p.reuniao_id WHERE p.usuario_id = ? "
                + "ORDER BY coalesce(r.inicio, r.criada_em) DESC", UUID.class, eu).stream().map(id -> paraQuemVe(reuniao(id), eu)).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-95
     * Título: Convite .ics
     * Objetivo: Gerar o convite de calendário da reunião confirmada.
     * Decisões: iCalendar simples, em UTC.
     */
    @Transactional(readOnly = true)
    public String convite(UUID eu, UUID reuniaoId) {
        Reuniao r = deParticipante(eu, reuniaoId);
        if (!"CONFIRMADA".equals(r.situacao())) {
            throw new RegraDeNegocioException("reuniao_nao_confirmada", "O convite sai quando a reunião é confirmada.");
        }
        String nome = startups.ler(r.startupId()).getNome();
        return String.join("\r\n", "BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Orbit Hub//Agenda//PT", "BEGIN:VEVENT", "UID:" + r.id() + "@orbithub",
                "DTSTAMP:" + ICS.format(relogio.instant()), "DTSTART:" + ICS.format(r.inicio()),
                "DTEND:" + ICS.format(r.inicio().plus(Duration.ofMinutes(r.duracaoMinutos()))), "SUMMARY:" + escapar("Orbit Hub · " + nome),
                "DESCRIPTION:" + escapar(r.pauta()), "LOCATION:" + escapar(r.local()), "END:VEVENT", "END:VCALENDAR", "");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-96
     * Título: Ao finalizar a conexão
     * Objetivo: Cancelar as reuniões futuras da conexão que terminou e avisar.
     * Decisões: Mesma transação.
     */
    @EventListener
    public void aoFinalizarConexao(ConexaoFinalizada evento) {
        cancelarFuturas(jdbc.queryForList("SELECT id FROM reuniao WHERE conexao_id = ? AND situacao IN ('PROPOSTA', 'CONFIRMADA')", UUID.class,
                evento.conexaoId()), "A conexão foi encerrada.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-96
     * Título: Ao bloquear
     * Objetivo: Cancelar as reuniões futuras com o investidor bloqueado.
     * Decisões: Silencioso para o motivo: o aviso diz só que foi cancelada.
     */
    @EventListener
    public void aoBloquear(InvestidorBloqueado evento) {
        cancelarFuturas(jdbc.queryForList("SELECT r.id FROM reuniao r JOIN conexao c ON c.id = r.conexao_id WHERE c.startup_id = ? AND "
                + "r.situacao IN ('PROPOSTA', 'CONFIRMADA') AND (c.investidor_chave = ? OR c.pessoa_id = ?)", UUID.class, evento.startupId(),
                evento.alvoId(), evento.alvoId()), "A reunião foi cancelada.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Ao suspender a conta
     * Objetivo: Cancelar as reuniões futuras de que a pessoa participa.
     * Decisões: A medida só do Nível 2 não cancela reuniões. O aviso não diz o motivo.
     */
    @EventListener
    public void aoSuspenderConta(ContaSuspensa evento) {
        if (evento.apenasNivel2()) {
            return;
        }
        cancelarFuturas(jdbc.queryForList("SELECT DISTINCT r.id FROM reuniao r JOIN reuniao_participante p ON p.reuniao_id = r.id "
                + "WHERE p.usuario_id = ? AND r.situacao IN ('PROPOSTA', 'CONFIRMADA')", UUID.class, evento.usuarioId()), "A reunião foi cancelada.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-94, KAN-96, KAN-97
     * Título: Rotina R5
     * Objetivo: Expirar propostas cujo primeiro horário passou, mandar os lembretes antes das confirmadas e lembrar uma
     *           vez o registro do resultado.
     * Decisões: De hora em hora; lembretes pelos prazos de reuniao.lembretes, do mais distante ao mais próximo.
     */
    @Transactional
    public int rotina() {
        Instant agora = relogio.instant();
        int n = jdbc.update("UPDATE reuniao SET situacao = 'EXPIRADA', lado_da_vez = NULL, atualizada_em = ? WHERE situacao = 'PROPOSTA' "
                + "AND (SELECT min(inicio) FROM reuniao_opcao o WHERE o.reuniao_id = reuniao.id) < ?", Timestamp.from(agora), Timestamp.from(agora));
        List<Long> prazos = new ArrayList<>(parametros.listaDeInteiros(LEMBRETES));
        prazos.sort(Comparator.reverseOrder());
        for (Map<String, Object> l : jdbc.queryForList("SELECT id, inicio, lembretes FROM reuniao WHERE situacao = 'CONFIRMADA' AND inicio > ?",
                Timestamp.from(agora))) {
            Instant inicio = ((Timestamp) l.get("inicio")).toInstant();
            long devidos = prazos.stream().filter(h -> !inicio.minus(Duration.ofHours(h)).isAfter(agora)).count();
            if (devidos > ((Number) l.get("lembretes")).intValue()) {
                Reuniao r = reuniao((UUID) l.get("id"));
                avisar(r, null, TipoDeNotificacao.REUNIAO_LEMBRETE, "Lembrete de reunião", "Você tem uma reunião em breve. Veja os detalhes na Agenda.");
                jdbc.update("UPDATE reuniao SET lembretes = ? WHERE id = ?", (int) devidos, r.id());
                n++;
            }
        }
        Instant limite = agora.minus(Duration.ofDays(parametros.inteiro(PRAZO_REGISTRO)));
        for (UUID id : jdbc.queryForList("SELECT id FROM reuniao r WHERE situacao = 'CONFIRMADA' AND inicio < ? AND lembrete_registro_em IS NULL "
                + "AND NOT EXISTS (SELECT 1 FROM reuniao_registro g WHERE g.reuniao_id = r.id)", UUID.class, Timestamp.from(limite))) {
            avisar(reuniao(id), null, TipoDeNotificacao.REUNIAO_LEMBRETE, "Registre como foi a reunião", "Conte na Agenda como foi a reunião e o próximo passo.");
            jdbc.update("UPDATE reuniao SET lembrete_registro_em = ? WHERE id = ?", Timestamp.from(agora), id);
            n++;
        }
        return n;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: cancelarFuturas
     * Objetivo: Rotina auxiliar cancelarFuturas.
     * Decisões: Ver a documentação da classe.
     */
    private void cancelarFuturas(List<UUID> ids, String texto) {
        for (UUID id : ids) {
            jdbc.update("UPDATE reuniao SET situacao = 'CANCELADA', motivo = ?, lado_da_vez = NULL, atualizada_em = ? WHERE id = ?", texto, agora(), id);
            avisar(reuniao(id), null, TipoDeNotificacao.REUNIAO_ALTERADA, "Reunião cancelada", texto);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: conferirAusencias
     * Objetivo: Rotina auxiliar conferirAusencias.
     * Decisões: Ver a documentação da classe.
     */
    private void conferirAusencias(Reuniao r, String meuLado) {
        List<UUID> faltosos = r.participantes().stream().filter(p -> !p.lado().equals(meuLado)).map(Participante::usuarioId).toList();
        for (UUID u : faltosos) {
            Integer n = jdbc.queryForObject("SELECT count(*) FROM reuniao_registro g JOIN reuniao_participante p ON p.reuniao_id = g.reuniao_id "
                    + "AND p.usuario_id = ? AND p.lado <> g.lado WHERE g.resultado = 'AUSENCIA_OUTRA_PARTE' AND g.em > ?", Integer.class, u,
                    Timestamp.from(relogio.instant().minus(Duration.ofDays(90))));
            if (n != null && n == parametros.inteiro(LIMITE_AUSENCIAS)) {
                notificacoes.notificarTodos(contas.equipeAtivaComPapeis(Set.of(Papel.ADMIN)), TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO,
                        "Usuário com ausências repetidas", contas.nome(u).orElse("Um usuário") + " faltou a " + n
                                + " reuniões em 90 dias. Avalie uma advertência.", "/admin");
            }
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: paraQuemVe
     * Objetivo: Rotina auxiliar paraQuemVe.
     * Decisões: Ver a documentação da classe.
     */
    private Reuniao paraQuemVe(Reuniao r, UUID eu) {
        List<Map<String, Object>> notas = jdbc.queryForList("SELECT texto FROM reuniao_anotacao WHERE reuniao_id = ? AND usuario_id = ?", r.id(), eu);
        return new Reuniao(r.id(), r.conexaoId(), r.rodadaId(), r.startupId(), r.startup(), r.tipo(), r.formato(), r.local(), r.pauta(), r.duracaoMinutos(),
                r.coletiva(), r.situacao(), r.ladoDaVez(), r.contrapropostas(), r.inicio(), r.convocadaPor(), r.motivo(), r.tardia(), r.resumo(), r.opcoes(),
                r.participantes(), r.registros(), notas.isEmpty() ? null : (String) notas.get(0).get("texto"),
                r.participantes().stream().filter(p -> p.usuarioId().equals(eu)).map(Participante::lado).findFirst()
                        .map(l -> l.equals(r.ladoDaVez())).orElse(false));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: daVez
     * Objetivo: Rotina auxiliar daVez.
     * Decisões: Ver a documentação da classe.
     */
    private Reuniao daVez(UUID eu, UUID reuniaoId) {
        Reuniao r = deParticipante(eu, reuniaoId);
        boolean minha = r.participantes().stream().anyMatch(p -> p.usuarioId().equals(eu) && p.lado().equals(r.ladoDaVez()));
        if (!"PROPOSTA".equals(r.situacao()) || !minha) {
            throw new RegraDeNegocioException("nao_e_sua_vez", "Esta proposta não aguarda a sua resposta.");
        }
        return r;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: deParticipante
     * Objetivo: Rotina auxiliar deParticipante.
     * Decisões: Ver a documentação da classe.
     */
    private Reuniao deParticipante(UUID eu, UUID reuniaoId) {
        Reuniao r = reuniao(reuniaoId);
        if (r.participantes().stream().noneMatch(p -> p.usuarioId().equals(eu))) {
            throw new RecursoNaoEncontradoException("Reunião não encontrada.");
        }
        return r;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: lado
     * Objetivo: Rotina auxiliar lado.
     * Decisões: Ver a documentação da classe.
     */
    private Conexao.Lado lado(UUID eu, String organizacao, Conexao c) {
        if (startups.fundadores(c.getStartupId(), false).contains(eu)) {
            return Conexao.Lado.STARTUP;
        }
        AtuacaoService.Atuacao a = atuacao.paraAgir(eu, organizacao);
        UUID chave = a.organizacao() != null ? a.organizacao() : a.pessoa();
        if (chave.equals(c.getInvestidorChave())) {
            return Conexao.Lado.INVESTIDOR;
        }
        throw new RecursoNaoEncontradoException("Conexão não encontrada.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: outro
     * Objetivo: Rotina auxiliar outro.
     * Decisões: Ver a documentação da classe.
     */
    private static Conexao.Lado outro(Conexao.Lado lado) {
        return lado == Conexao.Lado.INVESTIDOR ? Conexao.Lado.STARTUP : Conexao.Lado.INVESTIDOR;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: conferir
     * Objetivo: Rotina auxiliar conferir.
     * Decisões: Ver a documentação da classe.
     */
    private void conferir(Proposta p, List<Instant> opcoes) {
        if (p.tipo() == null || p.formato() == null) {
            throw new RegraDeNegocioException("proposta_invalida", "Escolha o tipo e o formato da reunião.");
        }
        String local = p.local() == null ? "" : p.local().strip();
        if (local.isEmpty() || local.length() > 500) {
            throw new RegraDeNegocioException("local_obrigatorio", p.formato() == Formato.VIDEO ? "Informe o link da reunião." : "Informe o endereço.");
        }
        if (p.formato() == Formato.VIDEO && !local.startsWith("https://")) {
            throw new RegraDeNegocioException("link_invalido", "O link da reunião precisa começar com https://.");
        }
        if (p.pauta() == null || p.pauta().isBlank() || p.pauta().length() > 1000) {
            throw new RegraDeNegocioException("pauta_invalida", "Escreva a pauta (até 1.000 caracteres).");
        }
        if (p.duracaoMinutos() < 15 || p.duracaoMinutos() > 480) {
            throw new RegraDeNegocioException("duracao_invalida", "A duração vai de 15 minutos a 8 horas.");
        }
        conferirHorarios(opcoes);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: conferirHorarios
     * Objetivo: Rotina auxiliar conferirHorarios.
     * Decisões: Ver a documentação da classe.
     */
    private void conferirHorarios(List<Instant> opcoes) {
        if (opcoes == null || opcoes.isEmpty() || opcoes.size() > 3) {
            throw new RegraDeNegocioException("horarios_invalidos", "Ofereça de 1 a 3 horários.");
        }
        Instant minimo = relogio.instant().plus(Duration.ofHours(parametros.inteiro(ANTECEDENCIA)));
        if (opcoes.stream().anyMatch(o -> o == null || o.isBefore(minimo))) {
            throw new RegraDeNegocioException("horario_cedo", "Os horários precisam ter ao menos " + parametros.inteiro(ANTECEDENCIA) + " horas de antecedência.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: tardio
     * Objetivo: Rotina auxiliar tardio.
     * Decisões: Ver a documentação da classe.
     */
    private boolean tardio(Reuniao r) {
        return "CONFIRMADA".equals(r.situacao()) && r.inicio() != null
                && r.inicio().minus(Duration.ofHours(parametros.inteiro(CANCELAMENTO))).isBefore(relogio.instant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: inserir
     * Objetivo: Rotina auxiliar inserir.
     * Decisões: Ver a documentação da classe.
     */
    private UUID inserir(UUID conexaoId, UUID rodadaId, UUID startupId, Proposta p, boolean coletiva, Conexao.Lado vez, UUID por) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO reuniao (id, conexao_id, rodada_id, startup_id, tipo, formato, local, pauta, duracao_minutos, coletiva, situacao, "
                + "lado_da_vez, convocada_por, criada_em, atualizada_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PROPOSTA', ?, ?, ?, ?)", id, conexaoId,
                rodadaId, startupId, p.tipo().name(), p.formato().name(), p.local().strip(), p.pauta().strip(), p.duracaoMinutos(), coletiva,
                vez == null ? null : vez.name(), por, agora(), agora());
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: opcoes
     * Objetivo: Rotina auxiliar opcoes.
     * Decisões: Ver a documentação da classe.
     */
    private void opcoes(UUID reuniaoId, List<Instant> opcoes) {
        for (int i = 0; i < opcoes.size(); i++) {
            jdbc.update("INSERT INTO reuniao_opcao (reuniao_id, ordem, inicio) VALUES (?, ?, ?)", reuniaoId, i + 1, Timestamp.from(opcoes.get(i)));
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: participantes
     * Objetivo: Rotina auxiliar participantes.
     * Decisões: Ver a documentação da classe.
     */
    private void participantes(UUID reuniaoId, Set<UUID> pessoas, Conexao.Lado lado) {
        for (UUID u : pessoas) {
            jdbc.update("INSERT INTO reuniao_participante (reuniao_id, usuario_id, lado) VALUES (?, ?, ?) ON CONFLICT DO NOTHING", reuniaoId, u, lado.name());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: avisar
     * Objetivo: Rotina auxiliar avisar.
     * Decisões: Ver a documentação da classe.
     */
    private void avisar(Reuniao r, UUID menos, TipoDeNotificacao tipo, String titulo, String texto) {
        notificacoes.notificarTodos(r.participantes().stream().map(Participante::usuarioId).filter(u -> !u.equals(menos)).toList(), tipo, titulo, texto,
                "/agenda");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: agora
     * Objetivo: Rotina auxiliar agora.
     * Decisões: Ver a documentação da classe.
     */
    private Timestamp agora() {
        return Timestamp.from(relogio.instant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: escapar
     * Objetivo: Rotina auxiliar escapar.
     * Decisões: Ver a documentação da classe.
     */
    private static String escapar(String t) {
        return t == null ? "" : t.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r", "").replace("\n", "\\n");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: reuniao
     * Objetivo: Rotina auxiliar reuniao.
     * Decisões: Ver a documentação da classe.
     */
    private Reuniao reuniao(UUID id) {
        Map<String, Object> l = jdbc.queryForList("SELECT * FROM reuniao WHERE id = ?", id).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reunião não encontrada."));
        List<Opcao> opcoes = jdbc.query("SELECT ordem, inicio FROM reuniao_opcao WHERE reuniao_id = ? ORDER BY ordem",
                (rs, i) -> new Opcao(rs.getInt(1), rs.getTimestamp(2).toInstant()), id);
        List<Participante> participantes = jdbc.query("SELECT usuario_id, lado, resposta FROM reuniao_participante WHERE reuniao_id = ?",
                (rs, i) -> new Participante(rs.getObject(1, UUID.class), contas.nome(rs.getObject(1, UUID.class)).orElse(null), rs.getString(2),
                        rs.getString(3)), id);
        List<RegistroFeito> registros = jdbc.query("SELECT lado, resultado, proximo_passo, em FROM reuniao_registro WHERE reuniao_id = ?",
                (rs, i) -> new RegistroFeito(rs.getString(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant()), id);
        UUID startupId = (UUID) l.get("startup_id");
        return new Reuniao(id, (UUID) l.get("conexao_id"), (UUID) l.get("rodada_id"), startupId, startups.ler(startupId).getNome(),
                (String) l.get("tipo"), (String) l.get("formato"), (String) l.get("local"), (String) l.get("pauta"),
                ((Number) l.get("duracao_minutos")).intValue(), (Boolean) l.get("coletiva"), (String) l.get("situacao"), (String) l.get("lado_da_vez"),
                ((Number) l.get("contrapropostas")).intValue(), l.get("inicio") == null ? null : ((Timestamp) l.get("inicio")).toInstant(),
                (UUID) l.get("convocada_por"), (String) l.get("motivo"), (Boolean) l.get("tardia"), (String) l.get("resumo"), opcoes, participantes,
                registros, null, false);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Proposta
     * Objetivo: Rotina auxiliar Proposta.
     * Decisões: Ver a documentação da classe.
     */
    public record Proposta(Tipo tipo, Formato formato, String local, String pauta, int duracaoMinutos, List<Instant> opcoes, List<UUID> membros) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Registro
     * Objetivo: Rotina auxiliar Registro.
     * Decisões: Ver a documentação da classe.
     */
    public record Registro(ResultadoDaReuniao resultado, ProximoPasso proximoPasso, Boolean aplicarNaConexao, String resumo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Opcao
     * Objetivo: Rotina auxiliar Opcao.
     * Decisões: Ver a documentação da classe.
     */
    public record Opcao(int ordem, Instant inicio) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Participante
     * Objetivo: Rotina auxiliar Participante.
     * Decisões: Ver a documentação da classe.
     */
    public record Participante(UUID usuarioId, String nome, String lado, String resposta) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: RegistroFeito
     * Objetivo: Rotina auxiliar RegistroFeito.
     * Decisões: Ver a documentação da classe.
     */
    public record RegistroFeito(String lado, String resultado, String proximoPasso, Instant em) {
    }

    public record Reuniao(UUID id, UUID conexaoId, UUID rodadaId, UUID startupId, String startup, String tipo, String formato, String local,
                          String pauta, int duracaoMinutos, boolean coletiva, String situacao, String ladoDaVez, int contrapropostas, Instant inicio,
                          UUID convocadaPor, String motivo, boolean tardia, String resumo, List<Opcao> opcoes, List<Participante> participantes,
                          List<RegistroFeito> registros, String minhaAnotacao, boolean aguardaVoce) {
    }
}
