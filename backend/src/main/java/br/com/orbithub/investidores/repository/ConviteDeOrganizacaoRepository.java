package br.com.orbithub.investidores.repository;

import br.com.orbithub.investidores.domain.ConviteDeOrganizacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Repositório dos convites
 * Objetivo: Buscar convites pelo hash do token e listar os pendentes de uma organização.
 * Decisões: A validade é conferida no serviço, com o relógio.
 */
public interface ConviteDeOrganizacaoRepository extends JpaRepository<ConviteDeOrganizacao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Convite pelo token
     * Objetivo: Encontrar o convite a partir do hash do token recebido.
     * Decisões: Hash SHA-256 em hexadecimal.
     */
    Optional<ConviteDeOrganizacao> findByTokenHash(String tokenHash);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Convites em aberto
     * Objetivo: Listar os convites não aceitos nem cancelados de uma organização.
     * Decisões: Os vencidos são filtrados no serviço.
     */
    List<ConviteDeOrganizacao> findByOrganizacaoIdAndAceitoEmIsNullAndCanceladoEmIsNullOrderByCriadoEmDesc(UUID organizacaoId);
}
