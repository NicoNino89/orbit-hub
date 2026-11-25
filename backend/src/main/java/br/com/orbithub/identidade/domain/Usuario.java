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

    @Column(nullable = false)
    private boolean interna;

    @Column(name = "totp_segredo", length = 200)
    private String totpSegredo;

    @Column(name = "totp_pendente", length = 200)
    private String totpPendente;

    @Column(name = "totp_ultimo_passo")
    private Long totpUltimoPasso;

    @Column(name = "desligado_em")
    private Instant desligadoEm;

    @Column(name = "suspensa_ate")
    private Instant suspensaAte;

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
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Criação de uma conta
     * Objetivo: Criar uma conta nova, ainda aguardando a confirmação do e-mail.
     * Decisões: Exige pelo menos um papel; o hash da senha chega pronto (a política e o algoritmo ficam no
     *           serviço de cadastro). Papéis internos e públicos nunca se misturam: com papel interno, a conta já
     *           nasce interna (KAN-75).
     */
    public static Usuario novo(String nome, String email, String senhaHash, Set<Papel> papeis, Instant agora) {
        Objects.requireNonNull(senhaHash, "senhaHash");
        if (papeis == null || papeis.isEmpty()) {
            throw new IllegalArgumentException("A conta precisa de pelo menos um papel");
        }
        boolean comInterno = papeis.stream().anyMatch(Papel::interno);
        if (comInterno && papeis.stream().anyMatch(Papel::publico)) {
            throw new IllegalArgumentException("Conta interna não tem papel de investidor ou empreendedor");
        }
        Usuario usuario = new Usuario();
        usuario.id = UUID.randomUUID();
        usuario.nome = nome.strip();
        usuario.email = normalizarEmail(email);
        usuario.senhaHash = senhaHash;
        usuario.status = StatusUsuario.PENDENTE_CONFIRMACAO;
        usuario.papeis = EnumSet.copyOf(papeis);
        usuario.interna = comInterno;
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
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Ativar papel
     * Objetivo: Acrescentar um papel público à conta.
     * Decisões: Devolve falso se a conta já tinha o papel (nada muda). Conta interna nunca recebe papel
     *           público: quem é da equipe e também investe usa outra conta (KAN-75).
     */
    public boolean ativarPapel(Papel papel, Instant agora) {
        if (!papel.publico()) {
            throw new IllegalArgumentException("Papel interno não é ativado pelo usuário");
        }
        if (interna) {
            throw new IllegalStateException("Conta interna não recebe papel de investidor ou empreendedor");
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
    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Conta da equipe interna
     * Objetivo: Criar a conta de um membro da equipe, convidado pelo administrador.
     * Decisões: Nasce pendente, com uma senha impossível de acertar (o hash chega pronto do serviço), até o
     *           membro aceitar o convite e definir a própria senha: a equipe nunca define senha por ninguém.
     */
    public static Usuario novoInterno(String nome, String email, String senhaInutilizavel, Set<Papel> papeis,
                                      Instant agora) {
        exigirPapeisInternos(papeis);
        return novo(nome, email, senhaInutilizavel, papeis, agora);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Exigir papéis internos
     * Objetivo: Recusar conjunto vazio ou com papel público numa conta interna.
     * Decisões: Mensagem de negócio, porque chega à tela do administrador.
     */
    private static void exigirPapeisInternos(Set<Papel> papeis) {
        if (papeis == null || papeis.isEmpty() || !papeis.stream().allMatch(Papel::interno)) {
            throw new IllegalArgumentException("Escolha pelo menos um papel interno");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Aceitar o convite
     * Objetivo: Ativar a conta interna com a senha escolhida pelo próprio membro.
     * Decisões: Só vale para conta interna pendente; o segundo fator é configurado no primeiro login.
     */
    public void aceitarConvite(String senhaHash, Instant agora) {
        if (!interna || status != StatusUsuario.PENDENTE_CONFIRMACAO) {
            throw new IllegalStateException("Convite já usado ou conta não é da equipe");
        }
        this.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash");
        this.status = StatusUsuario.ATIVO;
        this.atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Trocar papéis internos
     * Objetivo: Definir o novo conjunto de papéis de um membro da equipe.
     * Decisões: Só em conta interna e só com papéis internos; devolve os papéis anteriores, para a trilha
     *           registrar o antes e o depois.
     */
    public Set<Papel> definirPapeisInternos(Set<Papel> novos, Instant agora) {
        if (!interna) {
            throw new IllegalStateException("Só contas da equipe têm papéis internos");
        }
        exigirPapeisInternos(novos);
        Set<Papel> anteriores = EnumSet.copyOf(papeis);
        papeis.clear();
        papeis.addAll(novos);
        atualizadoEm = agora;
        return anteriores;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Desligar da equipe
     * Objetivo: Encerrar a conta de um membro que saiu da equipe.
     * Decisões: A conta fica desativada (não entra nem renova sessão) e guarda a data; os papéis ficam
     *           registrados para o histórico. O segundo fator é apagado. Desligar de novo não muda nada.
     */
    public void desligar(Instant agora) {
        if (!interna) {
            throw new IllegalStateException("Só contas da equipe são desligadas");
        }
        if (desligadoEm != null) {
            return;
        }
        status = StatusUsuario.DESATIVADO;
        desligadoEm = agora;
        totpSegredo = null;
        totpPendente = null;
        atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Iniciar a configuração do segundo fator
     * Objetivo: Guardar o segredo novo (cifrado) até o membro confirmar com o primeiro código.
     * Decisões: O segredo em uso continua valendo até a confirmação.
     */
    public void iniciarSegundoFator(String segredoCifrado, Instant agora) {
        this.totpPendente = Objects.requireNonNull(segredoCifrado, "segredoCifrado");
        this.atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Concluir a configuração do segundo fator
     * Objetivo: Passar o segredo pendente a valer, depois do primeiro código correto.
     * Decisões: Guarda o passo do código usado, para ele não servir de novo.
     */
    public void concluirSegundoFator(long passo, Instant agora) {
        if (totpPendente == null) {
            throw new IllegalStateException("Não há configuração de segundo fator em andamento");
        }
        this.totpSegredo = totpPendente;
        this.totpPendente = null;
        this.totpUltimoPasso = passo;
        this.atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Registrar código usado
     * Objetivo: Aceitar um código só se o passo de tempo dele for posterior ao último usado.
     * Decisões: Impede reusar o mesmo código (replay) dentro dos 30 segundos de validade.
     */
    public boolean usarPassoDoCodigo(long passo) {
        if (totpUltimoPasso != null && passo <= totpUltimoPasso) {
            return false;
        }
        totpUltimoPasso = passo;
        return true;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Redefinir o segundo fator
     * Objetivo: Apagar o segundo fator de um membro que perdeu o aplicativo autenticador.
     * Decisões: No próximo login o membro configura de novo; quem pede é o administrador, com registro.
     */
    public void redefinirSegundoFator(Instant agora) {
        this.totpSegredo = null;
        this.totpPendente = null;
        this.totpUltimoPasso = null;
        this.atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Conta interna
     * Objetivo: Dizer se a conta é da equipe interna.
     * Decisões: Definido na criação e nunca muda.
     */
    public boolean isInterna() {
        return interna;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Segundo fator configurado
     * Objetivo: Dizer se o membro já tem o segundo fator valendo.
     * Decisões: Segredo pendente não conta.
     */
    public boolean temSegundoFator() {
        return totpSegredo != null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Acesso ao segredo do segundo fator
     * Objetivo: Expor o segredo cifrado em uso.
     * Decisões: Cifrado; só o serviço de segundo fator decifra.
     */
    public String getTotpSegredo() {
        return totpSegredo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Acesso ao segredo pendente
     * Objetivo: Expor o segredo cifrado ainda em configuração.
     * Decisões: Nulo fora da configuração.
     */
    public String getTotpPendente() {
        return totpPendente;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Acesso à data de desligamento
     * Objetivo: Expor quando o membro foi desligado.
     * Decisões: Nulo enquanto ativo.
     */
    public Instant getDesligadoEm() {
        return desligadoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Banida até
     * Objetivo: Marcar a data usada para o banimento.
     * Decisões: Fim distante em vez de nulo, para que a conta banida e a suspensa usem a mesma regra.
     */
    public static final Instant BANIDA = Instant.parse("9999-12-31T00:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Suspender
     * Objetivo: Impedir o acesso da conta até a data informada (ou para sempre, no banimento).
     * Decisões: Uma suspensão nova substitui a anterior.
     */
    public void suspender(Instant ate, Instant agora) {
        this.suspensaAte = ate;
        this.atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Levantar a suspensão
     * Objetivo: Devolver o acesso quando a sanção é revogada no recurso ou a medida preventiva termina.
     * Decisões: Sem efeito se não havia suspensão.
     */
    public void levantarSuspensao(Instant agora) {
        if (suspensaAte != null) {
            this.suspensaAte = null;
            this.atualizadoEm = agora;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Suspensa no instante
     * Objetivo: Dizer se a conta está suspensa ou banida no instante informado.
     * Decisões: A suspensão termina sozinha na data final.
     */
    public boolean suspensaEm(Instant instante) {
        return suspensaAte != null && suspensaAte.isAfter(instante);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Fim da suspensão
     * Objetivo: Devolver a data final da suspensão ou do banimento.
     * Decisões: Nulo quando a conta não está suspensa.
     */
    public Instant getSuspensaAte() {
        return suspensaAte;
    }
}
