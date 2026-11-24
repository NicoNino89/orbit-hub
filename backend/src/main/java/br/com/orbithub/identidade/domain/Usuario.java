package br.com.orbithub.identidade.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Conta de usuário
 * Objetivo: Representar a conta com que uma pessoa entra na plataforma, seus papéis e sua situação.
 * Decisões: E-mail normalizado em minúsculas na criação, para a unicidade não depender de maiúsculas;
 *           papéis num conjunto carregado junto (são poucos e entram em todo token); versão otimista
 *           para duas alterações simultâneas não se sobrescreverem.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusUsuario status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_papel", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 30)
    private Set<Papel> papeis = EnumSet.noneOf(Papel.class);

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    private long versao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a entidade ao ler do banco.
     * Decisões: Protegido, para o código da aplicação usar sempre a fábrica {@link #novo}.
     */
    protected Usuario() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Criação de uma conta
     * Objetivo: Criar uma conta nova, ainda aguardando a confirmação do e-mail.
     * Decisões: Exige pelo menos um papel; o hash da senha chega pronto (a política e o algoritmo ficam no
     *           serviço de cadastro).
     */
    public static Usuario novo(String nome, String email, String senhaHash, Set<Papel> papeis, Instant agora) {
        Objects.requireNonNull(senhaHash, "senhaHash");
        if (papeis == null || papeis.isEmpty()) {
            throw new IllegalArgumentException("A conta precisa de pelo menos um papel");
        }
        Usuario usuario = new Usuario();
        usuario.id = UUID.randomUUID();
        usuario.nome = nome.strip();
        usuario.email = normalizarEmail(email);
        usuario.senhaHash = senhaHash;
        usuario.status = StatusUsuario.PENDENTE_CONFIRMACAO;
        usuario.papeis = EnumSet.copyOf(papeis);
        usuario.criadoEm = agora;
        usuario.atualizadoEm = agora;
        return usuario;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Normalização de e-mail
     * Objetivo: Padronizar o e-mail antes de gravar ou buscar.
     * Decisões: Remove espaços e usa minúsculas com Locale.ROOT, para não depender do idioma do servidor.
     */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Confirmação do e-mail
     * Objetivo: Ativar a conta depois que o e-mail é confirmado.
     * Decisões: Só sai de PENDENTE_CONFIRMACAO; confirmar de novo não reativa uma conta desativada.
     */
    public void confirmarEmail(Instant agora) {
        if (status == StatusUsuario.PENDENTE_CONFIRMACAO) {
            status = StatusUsuario.ATIVO;
            atualizadoEm = agora;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Conta pode ter sessão
     * Objetivo: Dizer se a conta pode entrar e renovar a sessão.
     * Decisões: Só contas ativas.
     */
    public boolean podeTerSessao() {
        return status == StatusUsuario.ATIVO;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador da conta.
     * Decisões: Sem setter: o identificador nasce com a conta.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso ao nome
     * Objetivo: Expor o nome de exibição.
     * Decisões: Alteração só por métodos de negócio (perfil, KAN-16).
     */
    public String getNome() {
        return nome;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso ao e-mail
     * Objetivo: Expor o e-mail normalizado.
     * Decisões: Troca de e-mail exige confirmação (KAN-16).
     */
    public String getEmail() {
        return email;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso ao hash da senha
     * Objetivo: Permitir a conferência da senha no login.
     * Decisões: Só o hash existe na entidade; a senha em texto nunca é guardada.
     */
    public String getSenhaHash() {
        return senhaHash;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso à situação
     * Objetivo: Expor a situação da conta.
     * Decisões: Mudanças de situação só por métodos de negócio.
     */
    public StatusUsuario getStatus() {
        return status;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso aos papéis
     * Objetivo: Expor os papéis da conta.
     * Decisões: Conjunto somente leitura, para nenhum chamador alterar papéis sem passar pelas regras.
     */
    public Set<Papel> getPapeis() {
        return Collections.unmodifiableSet(papeis);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso à data de criação
     * Objetivo: Expor quando a conta foi criada.
     * Decisões: Imutável.
     */
    public Instant getCriadoEm() {
        return criadoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso à última alteração
     * Objetivo: Expor quando a conta mudou pela última vez.
     * Decisões: Atualizada pelos métodos de negócio.
     */
    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
