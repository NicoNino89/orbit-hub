package br.com.orbithub.investidores.domain;

import br.com.orbithub.compartilhado.convites.ConviteBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-65
 * Título: Convite para a organização
 * Objetivo: Representar o convite enviado por e-mail para alguém entrar na organização com um papel.
 * Decisões: O ciclo do convite (token, validade, aceite, cancelamento) vem de ConviteBase (KAN-65); aqui ficam a
 *           organização e o papel.
 */
@Entity
@Table(name = "organizacao_convite")
public class ConviteDeOrganizacao extends ConviteBase {

    @Column(name = "organizacao_id", nullable = false)
    private UUID organizacaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelNaOrganizacao papel;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o convite ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #novo}.
     */
    protected ConviteDeOrganizacao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Novo convite
     * Objetivo: Criar o convite pendente com a validade informada.
     * Decisões: E-mail em minúsculas, para comparar com a conta.
     */
    public static ConviteDeOrganizacao novo(UUID organizacaoId, String email, PapelNaOrganizacao papel, String tokenHash,
                                            UUID convidadoPor, Instant agora, Instant expiraEm) {
        ConviteDeOrganizacao c = new ConviteDeOrganizacao();
        c.iniciar(email, tokenHash, convidadoPor, agora, expiraEm);
        c.organizacaoId = organizacaoId;
        c.papel = papel;
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso à organização
     * Objetivo: Expor a organização do convite.
     * Decisões: Somente leitura.
     */
    public UUID getOrganizacaoId() {
        return organizacaoId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso ao papel
     * Objetivo: Expor o papel oferecido.
     * Decisões: Somente leitura.
     */
    public PapelNaOrganizacao getPapel() {
        return papel;
    }
}
