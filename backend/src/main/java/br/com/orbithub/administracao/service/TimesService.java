package br.com.orbithub.administracao.service;

import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-76
 * Título: Times internos
 * Objetivo: Organizar a equipe em times (verificação, curadoria, moderação, atendimento, privacidade), com um líder por
 *           time, ausências dos membros, distribuição automática por rodízio e transferência dos casos de quem se ausenta.
 * Decisões: O time é definido pelo papel interno. Sem líder definido, o administrador faz as vezes de líder. O rodízio
 *           pula quem está ausente hoje e quem tem conflito de interesse com o caso (KAN-87); a fila em MANUAL
 *           (equipe.distribuicao_*) não distribui. Horário de atendimento: um calendário para todos os times
 *           (equipe.horario_atendimento, dias úteis sem feriados), como provisório.
 */
@Service
public class TimesService {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Times
     * Objetivo: Enumerar os times, os papéis de cada um e o parâmetro de distribuição.
     * Decisões: Publicidade entra na fase 2.
     */
    public enum Time {
        VERIFICACAO("Verificação", EnumSet.of(Papel.ANALISTA_VERIFICACAO), null),
        CURADORIA("Curadoria", EnumSet.of(Papel.CURADOR), null),
        MODERACAO("Moderação", EnumSet.of(Papel.MODERADOR), "equipe.distribuicao_moderacao"),
        ATENDIMENTO("Atendimento", EnumSet.of(Papel.SUPORTE_N1, Papel.SUPORTE_N2), "equipe.distribuicao_atendimento"),
        PRIVACIDADE("Privacidade", EnumSet.of(Papel.ENCARREGADO_DADOS), "equipe.distribuicao_privacidade");

        private final String nome;
        private final Set<Papel> papeis;
        private final String parametro;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: Time
     * Objetivo: Rotina auxiliar Time.
     * Decisões: Ver a documentação da classe.
     */
        Time(String nome, Set<Papel> papeis, String parametro) {
            this.nome = nome;
            this.papeis = papeis;
            this.parametro = parametro;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-76
         * Título: Nome do time
         * Objetivo: Devolver o nome para as telas.
         * Decisões: Português.
         */
        public String nome() {
            return nome;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-76
         * Título: Papéis do time
         * Objetivo: Devolver os papéis que formam o time.
         * Decisões: Conjunto imutável na prática.
         */
        public Set<Papel> papeis() {
            return papeis;
        }
    }

    private final JdbcTemplate jdbc;
    private final ConsultaDeContas contas;
    private final ConflitosDeInteresse conflitos;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    public TimesService(JdbcTemplate jdbc, ConsultaDeContas contas, ConflitosDeInteresse conflitos, LeituraDeParametros parametros, Clock relogio) {
        this.jdbc = jdbc;
        this.contas = contas;
        this.conflitos = conflitos;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Visão dos times
     * Objetivo: Listar cada time com o líder, os membros, quem está ausente hoje e a forma de distribuição.
     * Decisões: Membros = contas ativas da equipe com o papel do time.
     */
    @Transactional(readOnly = true)
    public List<VisaoDoTime> times() {
        LocalDate hoje = LocalDate.now(relogio.withZone(BRASILIA));
        List<VisaoDoTime> lista = new ArrayList<>();
        for (Time t : Time.values()) {
            List<UUID> ids = contas.equipeAtivaComPapeis(t.papeis);
            var nomes = contas.nomes(ids);
            List<Membro> membros = ids.stream().map(id -> new Membro(id, nomes.get(id), ausente(id, hoje)))
                    .sorted(Comparator.comparing(m -> m.nome() == null ? "" : m.nome())).toList();
            UUID lider = liderDefinido(t).orElse(null);
            lista.add(new VisaoDoTime(t, t.nome, lider, lider == null ? null : contas.nome(lider).orElse(null), membros,
                    t.parametro == null ? "MANUAL" : parametros.opcao(t.parametro)));
        }
        return lista;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Definir líder
     * Objetivo: Escolher o líder do time entre os membros.
     * Decisões: Só o administrador (no controlador); substitui o anterior.
     */
    @Transactional
    public void definirLider(Time time, UUID membro) {
        if (!contas.equipeAtivaComPapeis(time.papeis).contains(membro)) {
            throw new RegraDeNegocioException("fora_do_time", "Escolha um membro ativo do time.");
        }
        jdbc.update("INSERT INTO time_lider (time, membro_id, definido_em) VALUES (?, ?, ?) ON CONFLICT (time) DO UPDATE SET membro_id = EXCLUDED.membro_id, "
                + "definido_em = EXCLUDED.definido_em", time.name(), membro, Timestamp.from(relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76, KAN-108
     * Título: Quem lidera
     * Objetivo: Devolver quem recebe os alertas do líder: o líder definido ou, sem ele, os administradores.
     * Decisões: Lista, para os administradores.
     */
    @Transactional(readOnly = true)
    public List<UUID> lideranca(Time time) {
        return liderDefinido(time).map(List::of).orElseGet(() -> contas.equipeAtivaComPapeis(Set.of(Papel.ADMIN)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Registrar ausência
     * Objetivo: Marcar a ausência (férias, folga) de um membro, que deixa de receber casos novos no período.
     * Decisões: Fim igual ou depois do início; até um ano.
     */
    @Transactional
    public Ausencia registrarAusencia(UUID membro, LocalDate inicio, LocalDate fim, String motivo) {
        if (inicio == null || fim == null || fim.isBefore(inicio) || fim.isAfter(inicio.plusYears(1))) {
            throw new RegraDeNegocioException("periodo_invalido", "Informe um período válido (até um ano).");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO membro_ausencia (id, membro_id, inicio, fim, motivo, criada_em) VALUES (?, ?, ?, ?, ?, ?)", id, membro, Date.valueOf(inicio),
                Date.valueOf(fim), motivo == null || motivo.isBlank() ? null : motivo.strip(), Timestamp.from(relogio.instant()));
        return new Ausencia(id, membro, inicio, fim, motivo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Ausências do membro
     * Objetivo: Listar as ausências atuais e futuras do membro.
     * Decisões: Pelo início.
     */
    @Transactional(readOnly = true)
    public List<Ausencia> ausencias(UUID membro) {
        return jdbc.query("SELECT id, membro_id, inicio, fim, motivo FROM membro_ausencia WHERE membro_id = ? AND fim >= ? ORDER BY inicio",
                (rs, i) -> new Ausencia(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getDate(3).toLocalDate(), rs.getDate(4).toLocalDate(),
                        rs.getString(5)), membro, Date.valueOf(LocalDate.now(relogio.withZone(BRASILIA))));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Remover ausência
     * Objetivo: Apagar uma ausência registrada.
     * Decisões: Só a própria ou pelo administrador (no controlador).
     */
    @Transactional
    public void removerAusencia(UUID membro, UUID id, boolean admin) {
        int n = admin ? jdbc.update("DELETE FROM membro_ausencia WHERE id = ?", id)
                : jdbc.update("DELETE FROM membro_ausencia WHERE id = ? AND membro_id = ?", id, membro);
        if (n == 0) {
            throw new RecursoNaoEncontradoException("Ausência não encontrada.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Devolver casos à fila
     * Objetivo: Tirar o responsável dos casos abertos de um membro (ausente ou de saída), para outra pessoa assumir.
     * Decisões: Vale para moderação, chamados, pedidos do titular, verificação e curadoria; devolve a quantidade.
     */
    @Transactional
    public int devolverCasos(UUID membro) {
        return jdbc.update("UPDATE caso_moderacao SET responsavel_id = NULL WHERE responsavel_id = ? AND situacao <> 'DECIDIDO'", membro)
                + jdbc.update("UPDATE chamado SET responsavel_id = NULL WHERE responsavel_id = ? AND situacao NOT IN ('RESOLVIDO', 'FECHADO')", membro)
                + jdbc.update("UPDATE pedido_titular SET responsavel_id = NULL WHERE responsavel_id = ? AND situacao IN ('ABERTO', 'EM_ANALISE')", membro)
                + jdbc.update("UPDATE pedido_verificacao SET responsavel_id = NULL WHERE responsavel_id = ? AND situacao = 'EM_ANALISE'", membro)
                + jdbc.update("UPDATE revisao_curadoria SET responsavel_id = NULL WHERE responsavel_id = ? AND situacao = 'EM_ANALISE'", membro);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76, KAN-87
     * Título: Distribuir por rodízio
     * Objetivo: Escolher o próximo membro disponível do time para um caso novo.
     * Decisões: Ordem estável (por identificador) a partir do último escolhido; pula ausentes e quem tem conflito. Vazio
     *           quando a fila é manual ou ninguém pode receber.
     */
    @Transactional
    public Optional<UUID> distribuir(Time time, Set<Papel> papeis, String casoTipo, UUID casoId, ConflitosDeInteresse.Envolvidos envolvidos) {
        if (time.parametro == null || !"RODIZIO".equals(parametros.opcao(time.parametro))) {
            return Optional.empty();
        }
        LocalDate hoje = LocalDate.now(relogio.withZone(BRASILIA));
        List<UUID> candidatos = contas.equipeAtivaComPapeis(papeis).stream().sorted().filter(m -> !ausente(m, hoje))
                .filter(m -> !conflitos.temConflito(m, casoTipo, casoId, envolvidos)).toList();
        if (candidatos.isEmpty()) {
            return Optional.empty();
        }
        UUID ultimo = jdbc.query("SELECT ultimo_membro FROM fila_rodizio WHERE fila = ?", (rs, i) -> rs.getObject(1, UUID.class), time.name()).stream()
                .findFirst().orElse(null);
        UUID escolhido = candidatos.stream().filter(m -> ultimo == null || m.compareTo(ultimo) > 0).findFirst().orElse(candidatos.get(0));
        jdbc.update("INSERT INTO fila_rodizio (fila, ultimo_membro, atualizado_em) VALUES (?, ?, ?) ON CONFLICT (fila) DO UPDATE SET ultimo_membro = "
                + "EXCLUDED.ultimo_membro, atualizado_em = EXCLUDED.atualizado_em", time.name(), escolhido, Timestamp.from(relogio.instant()));
        return Optional.of(escolhido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: ausente
     * Objetivo: Rotina auxiliar ausente.
     * Decisões: Ver a documentação da classe.
     */
    private boolean ausente(UUID membro, LocalDate dia) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM membro_ausencia WHERE membro_id = ? AND ? BETWEEN inicio AND fim", Integer.class, membro,
                Date.valueOf(dia));
        return n != null && n > 0;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: liderDefinido
     * Objetivo: Rotina auxiliar liderDefinido.
     * Decisões: Ver a documentação da classe.
     */
    private Optional<UUID> liderDefinido(Time time) {
        return jdbc.query("SELECT membro_id FROM time_lider WHERE time = ?", (rs, i) -> rs.getObject(1, UUID.class), time.name()).stream().findFirst();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Dados dos times
     * Objetivo: Tipar as respostas dos times.
     * Decisões: Distribuição como texto (RODIZIO ou MANUAL).
     */
    public record Membro(UUID id, String nome, boolean ausenteHoje) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: VisaoDoTime
     * Objetivo: Rotina auxiliar VisaoDoTime.
     * Decisões: Ver a documentação da classe.
     */
    public record VisaoDoTime(Time time, String nome, UUID liderId, String lider, List<Membro> membros, String distribuicao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: Ausencia
     * Objetivo: Rotina auxiliar Ausencia.
     * Decisões: Ver a documentação da classe.
     */
    public record Ausencia(UUID id, UUID membroId, LocalDate inicio, LocalDate fim, String motivo) {
    }
}
