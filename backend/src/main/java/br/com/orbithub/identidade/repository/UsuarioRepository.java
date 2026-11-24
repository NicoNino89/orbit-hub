package br.com.orbithub.identidade.repository;

import br.com.orbithub.identidade.domain.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Repositório de contas
 * Objetivo: Ler e gravar contas de usuário.
 * Decisões: Busca por e-mail sempre com o valor já normalizado (minúsculas), igual ao gravado.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Busca por e-mail
     * Objetivo: Encontrar a conta pelo e-mail normalizado.
     * Decisões: Consulta derivada; o índice único do e-mail atende a busca.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: E-mail já cadastrado
     * Objetivo: Dizer se já existe conta com o e-mail.
     * Decisões: Usado no cadastro e na troca de e-mail; a unicidade final é garantida pelo banco.
     */
    boolean existsByEmail(String email);
}
