package br.com.orbithub.investidores.repository;

import br.com.orbithub.investidores.domain.MembroDaOrganizacao;
import br.com.orbithub.investidores.domain.PapelNaOrganizacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Repositório dos membros
 * Objetivo: Consultar os vínculos ativos entre pessoas e organizações.
 * Decisões: Ativo = sem data de saída; consultado a cada requisição, para a saída valer na hora.
 */
public interface MembroDaOrganizacaoRepository extends JpaRepository<MembroDaOrganizacao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Vínculo ativo
     * Objetivo: Buscar o vínculo ativo de uma pessoa com uma organização.
     * Decisões: No máximo um (índice único parcial).
     */
    Optional<MembroDaOrganizacao> findByOrganizacaoIdAndUsuarioIdAndSaiuEmIsNull(UUID organizacaoId, UUID usuarioId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Organizações da pessoa
     * Objetivo: Listar os vínculos ativos de uma pessoa.
     * Decisões: Mais antigos primeiro.
     */
    List<MembroDaOrganizacao> findByUsuarioIdAndSaiuEmIsNullOrderByEntrouEm(UUID usuarioId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Membros da organização
     * Objetivo: Listar os membros ativos de uma organização.
     * Decisões: Pela data de entrada.
     */
    List<MembroDaOrganizacao> findByOrganizacaoIdAndSaiuEmIsNullOrderByEntrouEm(UUID organizacaoId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Quantos com o papel
     * Objetivo: Contar os membros ativos com um papel (regra do último administrador).
     * Decisões: Só ativos.
     */
    long countByOrganizacaoIdAndPapelAndSaiuEmIsNull(UUID organizacaoId, PapelNaOrganizacao papel);
}
