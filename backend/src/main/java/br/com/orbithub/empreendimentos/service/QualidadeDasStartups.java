package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.empreendimentos.domain.MembroDaStartup;
import br.com.orbithub.empreendimentos.domain.PapelNaStartup;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.repository.MembroDaStartupRepository;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: R8 – Qualidade das startups
 * Objetivo: Lembrar a equipe da startup quando algum indicador de tração fica desatualizado e tratar as startups
 *           publicadas sem atualização (aviso e, sem resposta em 30 dias, despublicação).
 * Decisões: Avisa quem pode editar a tração (fundadores e colaboradores com a seção). Lembra de novo só depois de 30
 *           dias, para não repetir todo dia; é idempotente no mesmo dia. A inatividade (startup.inatividade_meses) avisa
 *           os fundadores administradores (KAN-27).
 */
@Component
class QualidadeDasStartups extends RotinaBase {

    static final Duration INTERVALO_DO_LEMBRETE = Duration.ofDays(30);

    static final String CHAVE_INATIVIDADE = "startup.inatividade_meses";

    private final IndicadoresDaStartupService indicadores;
    private final PublicacaoDaStartupService publicacao;
    private final LeituraDeParametros parametros;
    private final StartupRepository startups;
    private final MembroDaStartupRepository membros;
    private final NotificacoesService notificacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Criação da rotina
     * Objetivo: Registrar a R8, diária às 6h15.
     * Decisões: Módulo empreendimentos; usa os parâmetros dos indicadores e da inatividade.
     */
    QualidadeDasStartups(IndicadoresDaStartupService indicadores, PublicacaoDaStartupService publicacao, StartupRepository startups,
                         MembroDaStartupRepository membros, NotificacoesService notificacoes, LeituraDeParametros parametros) {
        super("R8", "Lembra dos indicadores desatualizados e trata startups publicadas inativas", "0 15 6 * * *", "empreendimentos",
                List.of(IndicadoresDaStartupService.CHAVE_DESATUALIZADO, CHAVE_INATIVIDADE));
        this.indicadores = indicadores;
        this.publicacao = publicacao;
        this.parametros = parametros;
        this.startups = startups;
        this.membros = membros;
        this.notificacoes = notificacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Executar
     * Objetivo: Avisar as equipes das startups com indicador desatualizado e tratar as publicadas inativas.
     * Decisões: Conta as startups lembradas, avisadas e despublicadas.
     */
    @Override
    @Transactional
    public Resultado executar(Instant agora) {
        int lembradas = 0;
        for (UUID id : indicadores.startupsComIndicadorDesatualizado()) {
            Startup startup = startups.findById(id).orElse(null);
            if (startup == null || !startup.deveLembrarIndicadores(agora, INTERVALO_DO_LEMBRETE)) {
                continue;
            }
            List<UUID> quem = membros.findByStartupIdAndSaiuEmIsNullOrderByEntrouEm(id).stream()
                    .filter(m -> m.podeEditar(SecaoDaStartup.TRACAO)).map(MembroDaStartup::getUsuarioId).toList();
            notificacoes.notificarTodos(quem, TipoDeNotificacao.STARTUP, "Atualize os indicadores da " + startup.getNome(),
                    "Há indicadores de tração com o mês de referência antigo. Eles aparecem como desatualizados até você "
                            + "declarar um valor mais recente.", "/empreendedor/startups/" + id);
            startup.registrarLembreteDeIndicadores(agora);
            lembradas++;
        }
        int[] inativas = inatividade(agora);
        return new Resultado(lembradas + inativas[0] + inativas[1], 0, lembradas + " startup(s) lembrada(s) dos indicadores, "
                + inativas[0] + " avisada(s) de inatividade e " + inativas[1] + " despublicada(s) por inatividade");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Inatividade
     * Objetivo: Avisar a equipe das startups publicadas sem atualização há startup.inatividade_meses e despublicar as que
     *           continuam paradas 30 dias depois do aviso.
     * Decisões: Atualização depois do aviso limpa o aviso. A despublicação pode ser desfeita reenviando para publicação.
     */
    private int[] inatividade(Instant agora) {
        int despublicadas = 0;
        for (Startup s : startups.findBySituacaoAndAvisoInatividadeEmIsNotNull(SituacaoDaStartup.PUBLICADA)) {
            if (s.getAtualizadaEm().isAfter(s.getAvisoInatividadeEm())) {
                s.limparAvisoDeInatividade();
            } else if (!s.getAvisoInatividadeEm().plus(INTERVALO_DO_LEMBRETE).isAfter(agora)) {
                publicacao.despublicarPorInatividade(s, agora);
                notificacoes.notificarTodos(administradores(s.getId()), TipoDeNotificacao.STARTUP, s.getNome() + " foi despublicada",
                        "A startup ficou sem atualização depois do aviso e saiu do catálogo. Atualize o cadastro e envie de novo para "
                                + "publicação quando quiser.", "/empreendedor/startups/" + s.getId());
                despublicadas++;
            }
        }
        int avisadas = 0;
        Instant limite = agora.atZone(ZoneOffset.UTC).minusMonths(parametros.inteiro(CHAVE_INATIVIDADE)).toInstant();
        for (Startup s : startups.findBySituacaoAndAtualizadaEmBeforeAndAvisoInatividadeEmIsNull(SituacaoDaStartup.PUBLICADA, limite)) {
            notificacoes.notificarTodos(administradores(s.getId()), TipoDeNotificacao.STARTUP, s.getNome() + " está sem atualização",
                    "A startup publicada não é atualizada há muito tempo. Atualize o cadastro em até 30 dias, ou ela sai do catálogo.",
                    "/empreendedor/startups/" + s.getId());
            s.registrarAvisoDeInatividade(agora);
            avisadas++;
        }
        return new int[] {avisadas, despublicadas};
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Administradores
     * Objetivo: Listar os fundadores administradores ativos da startup.
     * Decisões: Quem decide publicar ou não.
     */
    private List<UUID> administradores(UUID startupId) {
        return membros.findByStartupIdAndSaiuEmIsNullOrderByEntrouEm(startupId).stream()
                .filter(m -> m.getPapel() == PapelNaStartup.FUNDADOR_ADMINISTRADOR).map(MembroDaStartup::getUsuarioId).toList();
    }
}
