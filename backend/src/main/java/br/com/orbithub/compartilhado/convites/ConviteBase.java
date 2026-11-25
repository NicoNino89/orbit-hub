package br.com.orbithub.compartilhado.convites;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-65
 * Título: Base dos convites por e-mail
 * Objetivo: Reunir o que todo convite por e-mail tem (destino, hash do token, quem convidou, validade, aceite e
 *           cancelamento), para as organizações (KAN-60) e as equipes de startup (KAN-65) não repetirem o mesmo código.
 * Decisões: Superclasse mapeada: cada convite segue na própria tabela, com as mesmas colunas. Guarda só o hash do token.
 *           Aceito uma vez, enquanto não vence nem é cancelado.
 */
@MappedSuperclass
public abstract class ConviteBase {

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "convidado_por", nullable = false)
    private UUID convidadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "aceito_em")
    private Instant aceitoEm;

    @Column(name = "aceito_por")
    private UUID aceitoPor;

    @Column(name = "cancelado_em")
    private Instant canceladoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o convite ao ler do banco.
     * Decisões: Protegido; as subclasses criam pelo {@link #iniciar}.
     */
    protected ConviteBase() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Iniciar convite
     * Objetivo: Preencher os dados comuns de um convite novo, pendente.
     * Decisões: E-mail sem espaços e em minúsculas.
     */
    protected void iniciar(String destino, String hashDoToken, UUID quemConvidou, Instant agora, Instant validade) {
        this.id = UUID.randomUUID();
        this.email = destino.strip().toLowerCase(Locale.ROOT);
        this.tokenHash = hashDoToken;
        this.convidadoPor = quemConvidou;
        this.criadoEm = agora;
        this.expiraEm = validade;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Pendente
     * Objetivo: Dizer se o convite ainda pode ser aceito.
     * Decisões: Não aceito, não cancelado e dentro da validade.
     */
    public boolean pendente(Instant agora) {
        return aceitoEm == null && canceladoEm == null && agora.isBefore(expiraEm);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Aceitar
     * Objetivo: Registrar que a conta aceitou.
     * Decisões: A conferência (mesmo e-mail, papel) fica no serviço.
     */
    public void aceitar(UUID conta, Instant agora) {
        this.aceitoEm = agora;
        this.aceitoPor = conta;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Cancelar
     * Objetivo: Invalidar o convite antes do aceite.
     * Decisões: Idempotente.
     */
    public void cancelar(Instant agora) {
        if (canceladoEm == null) {
            this.canceladoEm = agora;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador do convite.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Acesso ao e-mail
     * Objetivo: Expor o e-mail convidado.
     * Decisões: Somente leitura.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Acesso à validade
     * Objetivo: Expor quando o convite vence.
     * Decisões: Somente leitura.
     */
    public Instant getExpiraEm() {
        return expiraEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Acesso à criação
     * Objetivo: Expor quando o convite foi enviado.
     * Decisões: Somente leitura.
     */
    public Instant getCriadoEm() {
        return criadoEm;
    }
}
