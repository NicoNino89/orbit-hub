package br.com.orbithub.administracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:14
 * Cartão Jira: KAN-79
 * Título: Item de uma lista gerenciável
 * Objetivo: Representar uma opção de lista, com código estável (gravado pelo sistema) e rótulo (exibido).
 * Decisões: O código nunca muda depois de criado; renomear troca só o rótulo, então os registros antigos continuam
 *           apontando para o item certo. Não há exclusão: desativar tira o item dos formulários e mantém o histórico.
 */
@Entity
@Table(name = "lista_item")
public class ItemDeLista {

    static final int TAMANHO_MAXIMO_CODIGO = 60;

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Lista lista;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String rotulo;

    @Column(length = 60)
    private String grupo;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    private long versao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o item ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #novo}.
     */
    protected ItemDeLista() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Novo item
     * Objetivo: Criar um item ativo no fim da lista.
     * Decisões: Sem código informado, o código nasce do rótulo (sem acentos, em maiúsculas, com sublinhado). Lista
     *           com grupos exige grupo.
     */
    public static ItemDeLista novo(Lista lista, String codigo, String rotulo, String grupo, int ordem, Instant agora) {
        String rotuloLimpo = exigirRotulo(rotulo);
        String codigoFinal = codigo == null || codigo.isBlank() ? codigoDe(rotuloLimpo) : codigoDe(codigo);
        if (codigoFinal.isEmpty()) {
            throw new IllegalArgumentException("Não foi possível gerar um código a partir do rótulo.");
        }
        String grupoLimpo = grupo == null || grupo.isBlank() ? null : codigoDe(grupo);
        if (lista.isComGrupo() && grupoLimpo == null) {
            throw new IllegalArgumentException("Informe o grupo do item.");
        }
        ItemDeLista item = new ItemDeLista();
        item.id = UUID.randomUUID();
        item.lista = Objects.requireNonNull(lista, "lista");
        item.codigo = codigoFinal;
        item.rotulo = rotuloLimpo;
        item.grupo = grupoLimpo;
        item.ordem = ordem;
        item.ativo = true;
        item.criadoEm = agora;
        item.atualizadoEm = agora;
        return item;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 13:00
     * Cartão Jira: KAN-79
     * Título: Código a partir de um texto
     * Objetivo: Transformar um texto em código estável (por exemplo, "Saúde (healthtech)" vira "SAUDE_HEALTHTECH").
     * Decisões: Remove acentos, deixa só letras, números e sublinhado, e limita o tamanho. A expressão que tira o
     *           sublinhado das pontas agrupa cada alternativa, para a precedência ficar explícita.
     */
    public static String codigoDe(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String codigo = semAcento.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_+)|(_+$)", "");
        return codigo.length() > TAMANHO_MAXIMO_CODIGO ? codigo.substring(0, TAMANHO_MAXIMO_CODIGO) : codigo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Renomear
     * Objetivo: Trocar o rótulo exibido, mantendo o código.
     * Decisões: Devolve o rótulo anterior, para o histórico; rótulo igual é recusado.
     */
    public String renomear(String novoRotulo, Instant agora) {
        String limpo = exigirRotulo(novoRotulo);
        if (limpo.equals(rotulo)) {
            throw new IllegalArgumentException("O rótulo informado é igual ao atual.");
        }
        String anterior = rotulo;
        rotulo = limpo;
        atualizadoEm = agora;
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Desativar ou reativar
     * Objetivo: Tirar o item dos formulários ou devolvê-lo.
     * Decisões: Recusa trocar para a situação em que o item já está.
     */
    public void definirAtivo(boolean novo, Instant agora) {
        if (ativo == novo) {
            throw new IllegalArgumentException(novo ? "O item já está ativo." : "O item já está desativado.");
        }
        ativo = novo;
        atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Exigir rótulo
     * Objetivo: Recusar rótulo vazio ou longo demais.
     * Decisões: Remove espaços nas pontas.
     */
    private static String exigirRotulo(String rotulo) {
        if (rotulo == null || rotulo.isBlank()) {
            throw new IllegalArgumentException("Informe o rótulo.");
        }
        String limpo = rotulo.strip();
        if (limpo.length() > 160) {
            throw new IllegalArgumentException("Use até 160 caracteres no rótulo.");
        }
        return limpo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador do item.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso à lista
     * Objetivo: Expor a lista do item.
     * Decisões: Somente leitura.
     */
    public Lista getLista() {
        return lista;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso ao código
     * Objetivo: Expor o código estável do item.
     * Decisões: Nunca muda.
     */
    public String getCodigo() {
        return codigo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso ao rótulo
     * Objetivo: Expor o texto exibido ao usuário.
     * Decisões: Somente leitura.
     */
    public String getRotulo() {
        return rotulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso ao grupo
     * Objetivo: Expor o grupo do item.
     * Decisões: Nulo nas listas sem grupo.
     */
    public String getGrupo() {
        return grupo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso à ordem
     * Objetivo: Expor a posição do item na lista.
     * Decisões: "Outros" fica por último.
     */
    public int getOrdem() {
        return ordem;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Ativo
     * Objetivo: Dizer se o item aparece nos formulários.
     * Decisões: Somente leitura.
     */
    public boolean isAtivo() {
        return ativo;
    }
}
