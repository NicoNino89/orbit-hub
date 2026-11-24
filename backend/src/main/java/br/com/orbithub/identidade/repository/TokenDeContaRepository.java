package br.com.orbithub.identidade.repository;

import br.com.orbithub.identidade.domain.TokenDeConta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Repositório de tokens de conta
 * Objetivo: Ler e gravar os tokens de uso único enviados por e-mail.
 * Decisões: Busca sempre pelo hash.
 */
public interface TokenDeContaRepository extends JpaRepository<TokenDeConta, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Busca pelo hash
     * Objetivo: Encontrar o token apresentado no link.
     * Decisões: Índice único do hash.
     */
    Optional<TokenDeConta> findByHash(String hash);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Tokens de uma finalidade
     * Objetivo: Listar os tokens de um usuário para uma finalidade.
     * Decisões: Usado para invalidar os antigos quando um novo é emitido.
     */
    List<TokenDeConta> findByUsuarioIdAndFinalidade(UUID usuarioId, TokenDeConta.Finalidade finalidade);
}
