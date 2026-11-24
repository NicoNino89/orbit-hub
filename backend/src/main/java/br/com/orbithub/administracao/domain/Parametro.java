package br.com.orbithub.administracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Parâmetro configurável
 * Objetivo: Representar um prazo, limite ou opção de regra de negócio, com tipo, unidade, limites aceitos e valor
 *           padrão.
 * Decisões: O catálogo nasce na migração (não há criação de parâmetro pela tela: um parâmetro novo sempre vem com
 *           a regra que o usa). O valor é texto interpretado pelo tipo; a validação e a normalização ficam aqui,
 *           para a mesma regra valer na tela, na proposta de segunda aprovação e na aplicação.
 */
@Entity
@Table(name = "parametro")
public class Parametro {

    @Id
    @Column(length = 80)
    private String chave;

    @Column(nullable = false, length = 40)
    private String grupo;

    @Column(nullable = false, length = 300)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Unidade unidade;

    @Column(nullable = false, length = 200)
    private String valor;

    @Column(name = "valor_padrao", nullable = false, length = 200)
    private String valorPadrao;

    private Long minimo;

    private Long maximo;

    @Column(length = 300)
    private String opcoes;

    @Column(nullable = false)
    private boolean sensivel;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @Column(name = "atualizado_por")
    private UUID atualizadoPor;

    @Version
    private long versao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o parâmetro ao ler do banco.
     * Decisões: Protegido; a fábrica {@link #novo} existe para os testes.
     */
    protected Parametro() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Novo parâmetro
     * Objetivo: Montar um parâmetro em memória (testes e catálogo).
     * Decisões: O valor inicial é o padrão.
     */
    public static Parametro novo(String chave, String grupo, String descricao, Tipo tipo, Unidade unidade,
                                 String valorPadrao, Long minimo, Long maximo, String opcoes, boolean sensivel) {
        Parametro p = new Parametro();
        p.chave = chave;
        p.grupo = grupo;
        p.descricao = descricao;
        p.tipo = tipo;
        p.unidade = unidade;
        p.valorPadrao = valorPadrao;
        p.valor = valorPadrao;
        p.minimo = minimo;
        p.maximo = maximo;
        p.opcoes = opcoes;
        p.sensivel = sensivel;
        return p;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Validar um valor novo
     * Objetivo: Conferir se o valor informado vale para o tipo e os limites, e devolvê-lo normalizado.
     * Decisões: Inteiro fora dos limites, lista vazia ou com item fora dos limites e opção desconhecida são
     *           recusados com mensagem em português (IllegalArgumentException). Listas saem em ordem crescente e sem
     *           repetição ("21, 7" vira "7,21").
     */
    public String validar(String informado) {
        String bruto = informado == null ? "" : informado.strip();
        return switch (tipo) {
            case INTEIRO -> String.valueOf(dentroDosLimites(numero(bruto)));
            case LISTA_INTEIROS -> {
                List<Long> numeros = Arrays.stream(bruto.split(","))
                        .map(String::strip).filter(s -> !s.isEmpty()).map(this::numero).map(this::dentroDosLimites)
                        .distinct().sorted().toList();
                if (numeros.isEmpty()) {
                    throw new IllegalArgumentException("Informe pelo menos um valor.");
                }
                yield numeros.stream().map(String::valueOf).collect(Collectors.joining(","));
            }
            case OPCAO -> {
                if (!opcoesAceitas().contains(bruto)) {
                    throw new IllegalArgumentException("Escolha uma das opções: " + String.join(", ", opcoesAceitas()) + ".");
                }
                yield bruto;
            }
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Aplicar valor
     * Objetivo: Trocar o valor do parâmetro e devolver o anterior, para o histórico.
     * Decisões: Valida de novo (o valor pode ter sido proposto antes de uma mudança de limites); valor igual ao
     *           atual é recusado, para não gerar histórico vazio.
     */
    public String aplicar(String novo, UUID autor, Instant agora) {
        String normalizado = validar(novo);
        if (normalizado.equals(valor)) {
            throw new IllegalArgumentException("O valor informado é igual ao atual.");
        }
        String anterior = valor;
        valor = normalizado;
        atualizadoPor = Objects.requireNonNull(autor, "autor");
        atualizadoEm = agora;
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Valor como duração
     * Objetivo: Converter o valor de um prazo em minutos, horas ou dias para Duration.
     * Decisões: Meses e prazos úteis dependem de calendário e são recusados aqui.
     */
    public Duration duracao() {
        long n = Long.parseLong(valor);
        return switch (unidade) {
            case MINUTOS -> Duration.ofMinutes(n);
            case HORAS -> Duration.ofHours(n);
            case DIAS -> Duration.ofDays(n);
            default -> throw new IllegalStateException("O parâmetro " + chave + " (" + unidade + ") não é uma duração");
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Opções aceitas
     * Objetivo: Listar as opções de um parâmetro do tipo OPCAO.
     * Decisões: Vazio nos outros tipos.
     */
    public List<String> opcoesAceitas() {
        return opcoes == null ? List.of() : Arrays.stream(opcoes.split(",")).map(String::strip).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Ler número
     * Objetivo: Converter um texto em número inteiro.
     * Decisões: Mensagem de negócio para texto que não é número.
     */
    private long numero(String texto) {
        try {
            return Long.parseLong(texto);
        } catch (NumberFormatException erro) {
            throw new IllegalArgumentException("Informe um número inteiro.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Conferir limites
     * Objetivo: Recusar número abaixo do mínimo ou acima do máximo.
     * Decisões: A mensagem traz a faixa aceita e a unidade.
     */
    private long dentroDosLimites(long n) {
        if ((minimo != null && n < minimo) || (maximo != null && n > maximo)) {
            throw new IllegalArgumentException("Valor fora dos limites aceitos: de " + minimo + " a " + maximo + " "
                    + unidade.getTexto() + ".");
        }
        return n;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso à chave
     * Objetivo: Expor a chave do parâmetro.
     * Decisões: Identificador estável usado no código.
     */
    public String getChave() {
        return chave;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso ao grupo
     * Objetivo: Expor o grupo do parâmetro, para a tela agrupar.
     * Decisões: Somente leitura.
     */
    public String getGrupo() {
        return grupo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso à descrição
     * Objetivo: Expor a explicação do parâmetro.
     * Decisões: Somente leitura.
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso ao tipo
     * Objetivo: Expor o tipo do valor.
     * Decisões: Somente leitura.
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso à unidade
     * Objetivo: Expor a unidade do valor.
     * Decisões: Somente leitura.
     */
    public Unidade getUnidade() {
        return unidade;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso ao valor
     * Objetivo: Expor o valor atual.
     * Decisões: Sempre normalizado.
     */
    public String getValor() {
        return valor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso ao valor padrão
     * Objetivo: Expor o valor do catálogo.
     * Decisões: Somente leitura.
     */
    public String getValorPadrao() {
        return valorPadrao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso ao mínimo
     * Objetivo: Expor o menor valor aceito.
     * Decisões: Nulo em opções.
     */
    public Long getMinimo() {
        return minimo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso ao máximo
     * Objetivo: Expor o maior valor aceito.
     * Decisões: Nulo em opções.
     */
    public Long getMaximo() {
        return maximo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Sensível
     * Objetivo: Dizer se a alteração exige segunda aprovação.
     * Decisões: Definido no catálogo.
     */
    public boolean isSensivel() {
        return sensivel;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso à última alteração
     * Objetivo: Expor quando o valor mudou pela última vez.
     * Decisões: Nulo enquanto vale o padrão da migração.
     */
    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Tipo do valor
     * Objetivo: Enumerar como o valor é interpretado.
     * Decisões: Três tipos bastam para o catálogo do MVP.
     */
    public enum Tipo {
        INTEIRO,
        LISTA_INTEIROS,
        OPCAO
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Unidade do valor
     * Objetivo: Enumerar as unidades, com o texto para a tela.
     * Decisões: Prazos úteis seguem o horário de atendimento dos times (seção 1.5).
     */
    public enum Unidade {
        MINUTOS("minutos"),
        HORAS("horas"),
        HORAS_UTEIS("horas úteis"),
        DIAS("dias"),
        DIAS_UTEIS("dias úteis"),
        MESES("meses"),
        QUANTIDADE("vezes"),
        PERCENTUAL("%"),
        TEXTO("");

        private final String texto;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 11:08
         * Cartão Jira: KAN-78
         * Título: Criação da unidade
         * Objetivo: Guardar o texto da unidade.
         * Decisões: Em português.
         */
        Unidade(String texto) {
            this.texto = texto;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 11:08
         * Cartão Jira: KAN-78
         * Título: Texto da unidade
         * Objetivo: Devolver o texto para a tela e as mensagens.
         * Decisões: Somente leitura.
         */
        public String getTexto() {
            return texto;
        }
    }
}
