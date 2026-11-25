package br.com.orbithub.empreendimentos.repository;

import br.com.orbithub.empreendimentos.domain.ConviteDaStartup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Repositório dos convites da startup
 * Objetivo: Buscar convites pelo hash do token e listar os em aberto.
 * Decisões: A validade é conferida no serviço.
 */
public interface ConviteDaStartupRepository extends JpaRepository<ConviteDaStartup, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Convite pelo token
     * Objetivo: Encontrar o convite a partir do hash do token.
     * Decisões: SHA-256 em hexadecimal.
     */
    Optional<ConviteDaStartup> findByTokenHash(String tokenHash);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Convites em aberto
     * Objetivo: Listar os convites não aceitos nem cancelados da startup.
     * Decisões: Mais novos primeiro.
     */
    List<ConviteDaStartup> findByStartupIdAndAceitoEmIsNullAndCanceladoEmIsNullOrderByCriadoEmDesc(UUID startupId);
}
