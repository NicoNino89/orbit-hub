package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.empreendimentos.domain.MembroDaStartup;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.repository.MembroDaStartupRepository;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: R8 – Qualidade das startups
 * Objetivo: Lembrar a equipe da startup quando algum indicador de tração fica desatualizado.
 * Decisões: Avisa quem pode editar a tração (fundadores e colaboradores com a seção). Lembra de novo só depois de 30
 *           dias, para não repetir todo dia; é idempotente no mesmo dia. A parte das startups inativas
 *           (startup.inatividade_meses) chega com a publicação (KAN-27).
 */
@Component
class QualidadeDasStartups extends RotinaBase {

    static final Duration INTERVALO_DO_LEMBRETE = Duration.ofDays(30);

    private final IndicadoresDaStartupService indicadores;
    private final StartupRepository startups;
    private final MembroDaStartupRepository membros;
    private final NotificacoesService notificacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Criação da rotina
     * Objetivo: Registrar a R8, diária às 6h15.
     * Decisões: Módulo empreendimentos; usa o parâmetro dos indicadores.
     */
    QualidadeDasStartups(IndicadoresDaStartupService indicadores, StartupRepository startups, MembroDaStartupRepository membros,
                         NotificacoesService notificacoes) {
        super("R8", "Lembra a equipe dos indicadores desatualizados", "0 15 6 * * *", "empreendimentos",
                List.of(IndicadoresDaStartupService.CHAVE_DESATUALIZADO));
        this.indicadores = indicadores;
        this.startups = startups;
        this.membros = membros;
        this.notificacoes = notificacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Executar
     * Objetivo: Avisar as equipes das startups com indicador desatualizado e registrar o lembrete.
     * Decisões: Conta as startups lembradas.
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
        return new Resultado(lembradas, 0, lembradas + " startup(s) lembrada(s) dos indicadores");
    }
}
