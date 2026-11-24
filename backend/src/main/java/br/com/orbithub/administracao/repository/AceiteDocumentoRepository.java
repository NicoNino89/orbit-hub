package br.com.orbithub.administracao.repository;

import br.com.orbithub.administracao.domain.AceiteDocumento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Repositório de aceites
 * Objetivo: Ler e gravar os aceites de documentos.
 * Decisões: Aceites só são inseridos, nunca alterados.
 */
public interface AceiteDocumentoRepository extends JpaRepository<AceiteDocumento, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Aceite já registrado
     * Objetivo: Dizer se o usuário já aceitou a versão.
     * Decisões: Evita duplicar o registro; a unicidade final é do banco.
     */
    boolean existsByUsuarioIdAndDocumentoId(UUID usuarioId, UUID documentoId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Aceites de um usuário
     * Objetivo: Listar todos os aceites do usuário, do mais recente ao mais antigo.
     * Decisões: Usado no histórico da conta e nos pedidos do titular (LGPD).
     */
    List<AceiteDocumento> findByUsuarioIdOrderByAceitoEmDesc(UUID usuarioId);
}
