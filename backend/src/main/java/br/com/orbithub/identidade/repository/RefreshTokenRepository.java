package br.com.orbithub.identidade.repository;

import br.com.orbithub.identidade.domain.RefreshToken;
import br.com.orbithub.identidade.domain.StatusRefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Repositório de refresh tokens
 * Objetivo: Ler e gravar refresh tokens pelo hash, pela família e pelo usuário.
 * Decisões: Nunca há busca pelo valor do token, só pelo hash.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Busca pelo hash
     * Objetivo: Encontrar o registro de um token apresentado.
     * Decisões: O índice único do hash atende a busca.
     */
    Optional<RefreshToken> findByHash(String hash);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Tokens de uma família por situação
     * Objetivo: Listar os tokens de uma sessão numa situação.
     * Decisões: Usado para revogar a sessão inteira no logout.
     */
    List<RefreshToken> findByFamiliaIdAndStatus(UUID familiaId, StatusRefreshToken status);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Tokens de um usuário por situação
     * Objetivo: Listar os tokens de um usuário numa situação.
     * Decisões: Usado para revogar todas as sessões (troca de senha, reuso, exclusão de conta).
     */
    List<RefreshToken> findByUsuarioIdAndStatus(UUID usuarioId, StatusRefreshToken status);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Sessão ativa
     * Objetivo: Dizer se a família ainda tem um token ativo e dentro da validade.
     * Decisões: É a conferência feita a cada requisição com access token, para o logout valer na hora.
     */
    boolean existsByFamiliaIdAndStatusAndExpiraEmAfter(UUID familiaId, StatusRefreshToken status, Instant agora);
}
