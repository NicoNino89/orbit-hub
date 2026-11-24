package br.com.orbithub.auditoria.repository;

import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Resultado;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:00
 * Cartão Jira: KAN-80
 * Título: Repositório da trilha de auditoria
 * Objetivo: Gravar e consultar registros de auditoria.
 * Decisões: Estende o Repository mínimo (e não JpaRepository), para não existir delete nem saveAll de
 *           atualização no código: só inserir e ler. As consultas com filtro usam Specification.
 */
public interface RegistroAuditoriaRepository extends Repository<RegistroAuditoria, UUID>,
        JpaSpecificationExecutor<RegistroAuditoria> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Gravar registro
     * Objetivo: Inserir um registro novo na trilha.
     * Decisões: A entidade é @Immutable; um registro já gravado nunca é atualizado.
     */
    RegistroAuditoria save(RegistroAuditoria registro);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Ações registradas
     * Objetivo: Listar os códigos de ação que já aparecem na trilha, para o filtro da tela.
     * Decisões: Distintos e em ordem alfabética.
     */
    @Query("select distinct r.acao from RegistroAuditoria r order by r.acao")
    List<String> acoesRegistradas();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:45
     * Cartão Jira: KAN-80
     * Título: Pessoas presentes na trilha
     * Objetivo: Listar quem já fez alguma ação, para o filtro por pessoa da tela.
     * Decisões: Usa o nome mais recente gravado para cada conta; ações do sistema (sem autor) ficam fora.
     */
    @Query("""
            select new br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository$Ator(r.atorId, max(r.atorNome))
            from RegistroAuditoria r where r.atorId is not null
            group by r.atorId order by max(r.atorNome)""")
    List<Ator> atoresRegistrados();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:45
     * Cartão Jira: KAN-80
     * Título: Pessoa da trilha
     * Objetivo: Representar uma pessoa que aparece na trilha.
     * Decisões: Só id e nome.
     */
    record Ator(UUID id, String nome) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Registros sobre um objeto
     * Objetivo: Listar as ações de um tipo sobre um objeto (por exemplo, acessos internos a uma startup).
     * Decisões: Mais recentes primeiro.
     */
    List<RegistroAuditoria> findByAcaoAndObjetoTipoAndObjetoIdAndResultadoOrderByOcorridoEmDesc(
            String acao, String objetoTipo, String objetoId, Resultado resultado);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:00
     * Cartão Jira: KAN-80
     * Título: Registros de uma conta
     * Objetivo: Listar as ações feitas por uma conta.
     * Decisões: Usado nos testes e na revisão de acessos.
     */
    List<RegistroAuditoria> findByAtorIdOrderByOcorridoEmDesc(UUID atorId);
}
