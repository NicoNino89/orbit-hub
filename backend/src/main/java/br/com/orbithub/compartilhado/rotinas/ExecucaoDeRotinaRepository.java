package br.com.orbithub.compartilhado.rotinas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Leitura das execuções de rotina
 * Objetivo: Consultar a última execução e o histórico de cada rotina.
 * Decisões: Só leitura; a gravação é do executor.
 */
public interface ExecucaoDeRotinaRepository extends Repository<ExecucaoDeRotina, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Últimas execuções
     * Objetivo: Listar as 20 execuções mais recentes de uma rotina.
     * Decisões: Para o painel.
     */
    List<ExecucaoDeRotina> findTop20ByRotinaOrderByIniciadaEmDesc(String rotina);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Última execução
     * Objetivo: Buscar a execução mais recente de uma rotina.
     * Decisões: Vazio se nunca rodou.
     */
    Optional<ExecucaoDeRotina> findFirstByRotinaOrderByIniciadaEmDesc(String rotina);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Última execução automática
     * Objetivo: Buscar a última execução agendada, para o vigia de atraso.
     * Decisões: Execuções manuais não contam.
     */
    Optional<ExecucaoDeRotina> findFirstByRotinaAndDisparoOrderByIniciadaEmDesc(String rotina, String disparo);
}
