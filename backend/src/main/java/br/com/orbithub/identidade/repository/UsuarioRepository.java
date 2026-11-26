package br.com.orbithub.identidade.repository;

import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Contas da equipe
     * Objetivo: Listar todas as contas internas, inclusive convidadas e desligadas.
     * Decisões: Em ordem de nome, para a tela da equipe.
     */
    List<Usuario> findByInternaTrueOrderByNomeAsc();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Membros ativos com um papel
     * Objetivo: Contar os membros ativos da equipe que têm um papel (por exemplo, administradores).
     * Decisões: Usado para impedir que a plataforma fique sem administrador.
     */
    @Query("""
            select count(u) from Usuario u join u.papeis p
            where u.interna = true and u.status = br.com.orbithub.identidade.domain.StatusUsuario.ATIVO
            and p = :papel""")
    long contarEquipeAtivaComPapel(@Param("papel") Papel papel);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Equipe ativa com algum dos papéis
     * Objetivo: Listar os ids dos membros ativos da equipe que têm pelo menos um dos papéis (destinatários de avisos).
     * Decisões: Sem repetição.
     */
    @Query("""
            select distinct u.id from Usuario u join u.papeis p
            where u.interna = true and u.status = br.com.orbithub.identidade.domain.StatusUsuario.ATIVO
            and p in :papeis""")
    List<UUID> idsDaEquipeAtivaComPapeis(@Param("papeis") Collection<Papel> papeis);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Contas públicas ativas
     * Objetivo: Listar os ids das contas de investidor e empreendedor ativas (aviso de termos atualizados).
     * Decisões: Sem contas da equipe nem desativadas.
     */
    @Query("""
            select u.id from Usuario u
            where u.interna = false and u.status = br.com.orbithub.identidade.domain.StatusUsuario.ATIVO""")
    List<UUID> idsPublicosAtivos();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-107
     * Título: Registrar acesso
     * Objetivo: Guardar o último acesso da conta e limpar o aviso de inatividade.
     * Decisões: Atualização direta, sem carregar a conta; a coluna não fica na entidade para não ser sobrescrita.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE usuario SET ultimo_acesso_em = :agora, aviso_inatividade_em = NULL WHERE id = :id", nativeQuery = true)
    int registrarAcesso(@Param("id") UUID id, @Param("agora") Instant agora);
}
