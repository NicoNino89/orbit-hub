package br.com.orbithub.identidade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Contagem de falhas de login
 * Objetivo: Contar falhas seguidas de login de um e-mail e bloquear temporariamente ao passar do limite.
 * Decisões: Indexado pelo hash do e-mail (não guarda o endereço). O bloqueio expira sozinho; um login
 *           correto zera a contagem. Durante o bloqueio, nem a senha certa entra.
 */
@Entity
@Table(name = "bloqueio_login")
public class BloqueioDeLogin {

    @Id
    @Column(name = "email_hash", length = 64)
    private String emailHash;

    @Column(nullable = false)
    private int falhas;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a entidade.
     * Decisões: Protegido; a aplicação usa {@link #novo}.
     */
    protected BloqueioDeLogin() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Contagem nova
     * Objetivo: Começar a contagem de um e-mail sem falhas.
     * Decisões: Criada na primeira falha.
     */
    public static BloqueioDeLogin novo(String emailHash, Instant agora) {
        BloqueioDeLogin bloqueio = new BloqueioDeLogin();
        bloqueio.emailHash = emailHash;
        bloqueio.atualizadoEm = agora;
        return bloqueio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Bloqueado agora
     * Objetivo: Dizer se o e-mail está bloqueado no instante informado.
     * Decisões: O fim do bloqueio já libera.
     */
    public boolean bloqueado(Instant agora) {
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Registrar falha
     * Objetivo: Somar uma falha e bloquear ao atingir o limite.
     * Decisões: Depois de um bloqueio vencido, a contagem recomeça; ao bloquear, a contagem zera para o
     *           próximo ciclo começar do zero.
     */
    public void registrarFalha(Instant agora, int limite, Duration duracao) {
        if (bloqueadoAte != null && !agora.isBefore(bloqueadoAte)) {
            bloqueadoAte = null;
            falhas = 0;
        }
        falhas++;
        if (falhas >= limite) {
            bloqueadoAte = agora.plus(duracao);
            falhas = 0;
        }
        atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Acesso às falhas
     * Objetivo: Expor quantas falhas seguidas há no ciclo atual.
     * Decisões: Somente leitura.
     */
    public int getFalhas() {
        return falhas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Acesso ao fim do bloqueio
     * Objetivo: Expor até quando o e-mail está bloqueado.
     * Decisões: Nulo quando não há bloqueio.
     */
    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }
}
