package br.com.orbithub.empreendimentos.repository;

import br.com.orbithub.empreendimentos.domain.ValorDeIndicador;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: Repositório dos indicadores
 * Objetivo: Listar o histórico dos indicadores e encontrar startups com indicador desatualizado.
 * Decisões: O valor atual é calculado no serviço (referência mais recente; empate, o último declarado).
 */
public interface ValorDeIndicadorRepository extends JpaRepository<ValorDeIndicador, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Histórico da startup
     * Objetivo: Listar todos os valores declarados, do mais recente para o mais antigo.
     * Decisões: Ordenado por referência e, no empate, pela declaração.
     */
    List<ValorDeIndicador> findByStartupIdOrderByReferenciaDescRegistradoEmDesc(UUID startupId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Startups com indicador desatualizado
     * Objetivo: Encontrar as startups em que algum indicador tem a referência mais recente antes do limite.
     * Decisões: Agrupa por startup e indicador.
     */
    @Query("select distinct v.startupId from ValorDeIndicador v group by v.startupId, v.indicador having max(v.referencia) < :limite")
    List<UUID> startupsComIndicadorAntesDe(LocalDate limite);
}
