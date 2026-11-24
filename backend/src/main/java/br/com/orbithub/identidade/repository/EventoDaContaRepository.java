package br.com.orbithub.identidade.repository;

import br.com.orbithub.identidade.domain.EventoDaConta;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Repositório do histórico da conta
 * Objetivo: Gravar e listar os eventos da conta.
 * Decisões: Leitura do mais recente ao mais antigo.
 */
public interface EventoDaContaRepository extends JpaRepository<EventoDaConta, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Histórico de uma conta
     * Objetivo: Listar os eventos da conta do mais recente ao mais antigo.
     * Decisões: Índice (usuario_id, ocorrido_em desc).
     */
    List<EventoDaConta> findByUsuarioIdOrderByOcorridoEmDesc(UUID usuarioId);
}
