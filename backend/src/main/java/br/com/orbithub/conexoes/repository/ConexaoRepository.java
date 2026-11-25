package br.com.orbithub.conexoes.repository;

import br.com.orbithub.conexoes.domain.Conexao;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-45, KAN-49, KAN-105
 * Título: Repositório das conexões
 * Objetivo: Achar a conexão ativa do investidor com a startup, a última encerrada, as listas dos painéis e as pendentes
 *           para as rotinas.
 * Decisões: Consultas derivadas.
 */
public interface ConexaoRepository extends JpaRepository<Conexao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Conexões do investidor com a startup
     * Objetivo: Listar as conexões do investidor (pessoa ou organização) com a startup, da mais recente.
     * Decisões: Para a regra de uma ativa e da nova tentativa.
     */
    List<Conexao> findByStartupIdAndInvestidorChaveOrderByAtualizadaEmDesc(UUID startupId, UUID investidorChave);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-47
     * Título: Conexões da startup
     * Objetivo: Listar as conexões da startup, da mais recente.
     * Decisões: Painel do empreendedor.
     */
    List<Conexao> findByStartupIdOrderByAtualizadaEmDesc(UUID startupId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Conexões do investidor
     * Objetivo: Listar as conexões do investidor, da mais recente.
     * Decisões: Painel do investidor, pela pessoa ou pela organização.
     */
    List<Conexao> findByInvestidorChaveOrderByAtualizadaEmDesc(UUID investidorChave);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-105
     * Título: Por situação criadas antes
     * Objetivo: Listar as conexões numa situação criadas antes de um instante.
     * Decisões: R1 e R2.
     */
    List<Conexao> findBySituacaoAndCriadaEmBefore(Conexao.Situacao situacao, Instant antes);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Ativas da startup por investidores
     * Objetivo: Achar a conexão ativa de um investidor com a startup.
     * Decisões: No máximo uma.
     */
    Optional<Conexao> findFirstByStartupIdAndInvestidorChaveAndSituacaoIn(UUID startupId, UUID investidorChave,
                                                                         Collection<Conexao.Situacao> situacoes);
}
