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

    @Column(length = 20)
    private String telefone;

    @Column(name = "foto_chave", length = 300)
    private String fotoChave;

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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Trocar a senha
     * Objetivo: Substituir o hash da senha da conta.
     * Decisões: Recebe o hash pronto; a política e a revogação das sessões ficam no serviço.
     */
    public void trocarSenha(String novoHash, Instant agora) {
        this.senhaHash = Objects.requireNonNull(novoHash, "novoHash");
        this.atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Atualizar dados do perfil
     * Objetivo: Trocar nome e telefone de contato.
     * Decisões: Telefone opcional (vazio vira nulo); e-mail e senha têm fluxos próprios.
     */
    public void atualizarPerfil(String novoNome, String novoTelefone, Instant agora) {
        this.nome = Objects.requireNonNull(novoNome, "novoNome").strip();
        this.telefone = novoTelefone == null || novoTelefone.isBlank() ? null : novoTelefone.strip();
        this.atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Definir foto
     * Objetivo: Guardar a chave da foto no armazenamento de arquivos.
     * Decisões: Devolve a chave anterior, para o serviço apagar o arquivo antigo.
     */
    public String definirFoto(String chave, Instant agora) {
        String anterior = this.fotoChave;
        this.fotoChave = chave;
        this.atualizadoEm = agora;
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Trocar e-mail
     * Objetivo: Substituir o e-mail depois da confirmação pelo novo endereço.
     * Decisões: Normaliza como no cadastro.
     */
    public void trocarEmail(String novoEmail, Instant agora) {
        this.email = normalizarEmail(Objects.requireNonNull(novoEmail, "novoEmail"));
        this.atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Ativar papel
     * Objetivo: Acrescentar um papel público à conta.
     * Decisões: Devolve falso se a conta já tinha o papel (nada muda).
     */
    public boolean ativarPapel(Papel papel, Instant agora) {
        if (!papel.publico()) {
            throw new IllegalArgumentException("Papel interno não é ativado pelo usuário");
        }
        boolean mudou = papeis.add(papel);
        if (mudou) {
            atualizadoEm = agora;
        }
        return mudou;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Desativar papel
     * Objetivo: Retirar um papel público da conta.
     * Decisões: A conta precisa continuar com pelo menos um papel público; pendências do papel são
     *           conferidas no serviço antes de chamar aqui.
     */
    public void desativarPapel(Papel papel, Instant agora) {
        if (!papeis.contains(papel)) {
            return;
        }
        long publicosRestantes = papeis.stream().filter(p -> p.publico() && p != papel).count();
        if (papel.publico() && publicosRestantes == 0) {
            throw new IllegalStateException("A conta precisa de pelo menos um papel");
        }
        papeis.remove(papel);
        atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Acesso ao telefone
     * Objetivo: Expor o telefone de contato.
     * Decisões: Pode ser nulo.
     */
    public String getTelefone() {
        return telefone;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Acesso à foto
     * Objetivo: Expor a chave da foto no armazenamento.
     * Decisões: Nula quando não há foto.
     */
    public String getFotoChave() {
        return fotoChave;
    }
}
