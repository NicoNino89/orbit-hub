package br.com.orbithub.notificacoes.repository;

import br.com.orbithub.notificacoes.domain.Notificacao;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Repositório das notificações
 * Objetivo: Ler, contar e marcar as notificações de cada pessoa.
 * Decisões: A lista mostra só os avisos da plataforma (o sino), do mais novo para o mais antigo.
 */
public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Avisos da pessoa
     * Objetivo: Listar os avisos da plataforma de uma pessoa, paginados.
     * Decisões: Mais novos primeiro.
     */
    Page<Notificacao> findByUsuarioIdAndNaPlataformaTrueOrderByCriadaEmDesc(UUID usuarioId, Pageable pagina);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Não lidos
     * Objetivo: Contar os avisos não lidos da plataforma, para o contador do sino.
     * Decisões: Índice parcial no banco.
     */
    long countByUsuarioIdAndNaPlataformaTrueAndLidaEmIsNull(UUID usuarioId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Aviso da pessoa
     * Objetivo: Buscar um aviso conferindo o dono.
     * Decisões: Aviso de outra pessoa não é encontrado.
     */
    Optional<Notificacao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Marcar todas como lidas
     * Objetivo: Marcar de uma vez os avisos não lidos da pessoa.
     * Decisões: Devolve quantos foram marcados.
     */
    @Modifying
    @Query("update Notificacao n set n.lidaEm = :agora where n.usuarioId = :usuario and n.lidaEm is null")
    int marcarTodasLidas(@Param("usuario") UUID usuario, @Param("agora") Instant agora);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Apagar antigas
     * Objetivo: Remover os avisos criados antes do limite de retenção.
     * Decisões: Os e-mails da fila saem junto (ON DELETE CASCADE).
     */
    @Modifying
    @Query("delete from Notificacao n where n.criadaEm < :limite")
    int apagarAntesDe(@Param("limite") Instant limite);
}
