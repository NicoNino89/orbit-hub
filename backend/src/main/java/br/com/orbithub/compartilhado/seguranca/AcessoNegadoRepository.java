package br.com.orbithub.compartilhado.seguranca;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Repositório de acessos negados
 * Objetivo: Gravar e consultar as tentativas recusadas.
 * Decisões: A consulta por conta serve à auditoria (KAN-80) e aos testes.
 */
public interface AcessoNegadoRepository extends JpaRepository<AcessoNegado, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Tentativas de uma conta
     * Objetivo: Listar as tentativas recusadas de uma conta.
     * Decisões: Índice por conta e data.
     */
    List<AcessoNegado> findByUsuarioId(UUID usuarioId);
}
