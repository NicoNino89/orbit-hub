package br.com.orbithub.conexoes.service;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.conexoes.domain.ResultadoDaRodada;
import br.com.orbithub.conexoes.domain.Rodada;
import br.com.orbithub.conexoes.domain.SituacaoDaRodada;
import br.com.orbithub.conexoes.domain.TermoDaRodada;
import br.com.orbithub.conexoes.repository.HistoricoDaRodada;
import br.com.orbithub.conexoes.repository.RodadaRepository;
import br.com.orbithub.empreendimentos.service.StartupsService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Rodadas de investimento
 * Objetivo: Criar, editar, abrir, pôr em negociação e encerrar a rodada da startup, conferindo os termos comuns e os
 *           específicos do instrumento.
 * Decisões: Só o fundador administrador mexe na rodada; a equipe vê. Só startup com CNPJ (constituída) abre rodada. Uma
 *           rodada não encerrada por startup. O rascunho aceita termos incompletos; abrir exige todos (comuns e
 *           específicos). Participantes, líder e intenções chegam com o Match (S7); o registro do resultado, com o KAN-64.
 */
@Service
public class RodadasService {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final int MESES_MAXIMOS = 240;
    private static final int TEXTO_MAXIMO = 1000;

    private final RodadaRepository rodadas;
    private final HistoricoDaRodada historico;
    private final StartupsService startups;
    private final ListasService listas;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Criação do serviço
     * Objetivo: Receber os repositórios, o serviço das startups, as listas e o relógio.
     * Decisões: Usa só serviços públicos do módulo empreendimentos.
     */
    public RodadasService(RodadaRepository rodadas, HistoricoDaRodada historico, StartupsService startups, ListasService listas,
                          Clock relogio) {
        this.rodadas = rodadas;
        this.historico = historico;
        this.startups = startups;
        this.listas = listas;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Rodadas da startup
     * Objetivo: Listar as rodadas da startup.
     * Decisões: Qualquer membro da equipe vê; quem é de fora recebe 404.
     */
    @Transactional(readOnly = true)
    public List<Rodada> listar(UUID eu, UUID startupId) {
        startups.papel(eu, startupId);
        return rodadas.findByStartupIdOrderByCriadaEmDesc(startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Criar rodada
     * Objetivo: Começar uma rodada em rascunho.
     * Decisões: Recusa se já houver rodada em andamento ou se a startup não tiver CNPJ.
     */
    @Transactional
    public Rodada criar(UUID eu, UUID startupId, Rodada.Termos termos) {
        startups.exigirAdministrador(eu, startupId);
        exigirConstituida(eu, startupId);
        if (rodadas.findFirstByStartupIdAndSituacaoNot(startupId, SituacaoDaRodada.ENCERRADA).isPresent()) {
            throw new RegraDeNegocioException("rodada_em_andamento", "A startup já tem uma rodada em andamento. Encerre-a antes de criar outra.");
        }
        Instant agora = relogio.instant();
        try {
            Rodada rodada = rodadas.saveAndFlush(Rodada.nova(startupId, conferir(termos, false), eu, agora));
            historico.registrar(rodada.getId(), SituacaoDaRodada.RASCUNHO, SituacaoDaRodada.RASCUNHO, eu, "Rodada criada", agora);
            return rodada;
        } catch (DataIntegrityViolationException e) {
            throw new RegraDeNegocioException("rodada_em_andamento", "A startup já tem uma rodada em andamento. Encerre-a antes de criar outra.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Editar rodada
     * Objetivo: Trocar os termos da rodada em rascunho ou aberta.
     * Decisões: Aberta exige os termos completos também na edição.
     */
    @Transactional
    public Rodada editar(UUID eu, UUID rodadaId, Rodada.Termos termos) {
        Rodada rodada = daEquipe(eu, rodadaId, true);
        rodada.atualizar(conferir(termos, rodada.getSituacao() == SituacaoDaRodada.ABERTA), relogio.instant());
        return rodada;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Abrir rodada
     * Objetivo: Publicar a rodada para a captação.
     * Decisões: Exige os termos completos e a startup com CNPJ.
     */
    @Transactional
    public Rodada abrir(UUID eu, UUID rodadaId) {
        Rodada rodada = daEquipe(eu, rodadaId, true);
        exigirConstituida(eu, rodada.getStartupId());
        conferir(rodada.termos(), true);
        Instant agora = relogio.instant();
        historico.registrar(rodada.getId(), rodada.abrir(agora), rodada.getSituacao(), eu, null, agora);
        return rodada;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Iniciar negociação
     * Objetivo: Marcar a rodada aberta como em negociação.
     * Decisões: Manual por enquanto; com os participantes (S7), acontece quando o primeiro investidor entra.
     */
    @Transactional
    public Rodada iniciarNegociacao(UUID eu, UUID rodadaId) {
        Rodada rodada = daEquipe(eu, rodadaId, true);
        historico.registrar(rodada.getId(), rodada.iniciarNegociacao(), rodada.getSituacao(), eu, null, relogio.instant());
        return rodada;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Encerrar rodada
     * Objetivo: Encerrar a rodada como concluída ou cancelada, com o motivo.
     * Decisões: Resultado obrigatório; o detalhe do investimento é registrado no KAN-64.
     */
    @Transactional
    public Rodada encerrar(UUID eu, UUID rodadaId, ResultadoDaRodada resultado, String motivo) {
        if (resultado == null) {
            throw new RegraDeNegocioException("resultado_obrigatorio", "Diga se a rodada foi concluída ou cancelada.");
        }
        Rodada rodada = daEquipe(eu, rodadaId, true);
        Instant agora = relogio.instant();
        historico.registrar(rodada.getId(), rodada.encerrar(resultado, agora), rodada.getSituacao(), eu,
                motivo == null || motivo.isBlank() ? resultado.name() : resultado.name() + ": " + motivo.strip(), agora);
        return rodada;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Histórico
     * Objetivo: Listar as mudanças de situação da rodada.
     * Decisões: Qualquer membro da equipe vê.
     */
    @Transactional(readOnly = true)
    public List<HistoricoDaRodada.Mudanca> historico(UUID eu, UUID rodadaId) {
        daEquipe(eu, rodadaId, false);
        return historico.listar(rodadaId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Rodada aberta
     * Objetivo: Devolver a rodada aberta ou em negociação da startup, para o catálogo, os filtros e a compatibilidade.
     * Decisões: Serviço público, sem conferir quem pergunta (a visibilidade é do catálogo, KAN-28). Vazio = "sem rodada
     *           aberta no momento".
     */
    @Transactional(readOnly = true)
    public Optional<Rodada> rodadaAberta(UUID startupId) {
        return rodadas.findFirstByStartupIdAndSituacaoNot(startupId, SituacaoDaRodada.ENCERRADA)
                .filter(r -> r.getSituacao() != SituacaoDaRodada.RASCUNHO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Rodada da equipe
     * Objetivo: Ler a rodada e conferir que quem pede é da equipe (ou o administrador, para mudar).
     * Decisões: Rodada de outra startup aparece como inexistente.
     */
    private Rodada daEquipe(UUID eu, UUID rodadaId, boolean paraMudar) {
        Rodada rodada = rodadas.findById(rodadaId).orElseThrow(() -> new RecursoNaoEncontradoException("Rodada não encontrada."));
        if (paraMudar) {
            startups.exigirAdministrador(eu, rodada.getStartupId());
        } else {
            startups.papel(eu, rodada.getStartupId());
        }
        return rodada;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Exigir CNPJ
     * Objetivo: Recusar rodada de startup ainda não constituída.
     * Decisões: Nenhum instrumento funciona sem pessoa jurídica (KAN-21).
     */
    private void exigirConstituida(UUID eu, UUID startupId) {
        if (!startups.detalhe(eu, startupId).startup().constituida()) {
            throw new RegraDeNegocioException("startup_sem_cnpj", "Cadastre o CNPJ da startup nos dados cadastrais para abrir uma rodada.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Conferir termos
     * Objetivo: Validar os termos comuns e os específicos do instrumento, e normalizar os textos.
     * Decisões: Com "completos", todos são obrigatórios. Mínimo ≤ alvo ≤ máximo; prazo depois de hoje e em até 2 anos.
     *           Termos específicos de outro instrumento são recusados.
     */
    Rodada.Termos conferir(Rodada.Termos t, boolean completos) {
        List<String> faltando = new ArrayList<>();
        String estagio = codigo(Lista.ESTAGIO_RODADA, t.estagio(), "o estágio da rodada", faltando);
        String instrumento = codigo(Lista.INSTRUMENTO, t.instrumento(), "o instrumento", faltando);
        positivo(t.valorAlvo(), "o valor alvo", faltando);
        positivo(t.valorMinimo(), "o valor mínimo", faltando);
        positivo(t.valorMaximo(), "o valor máximo", faltando);
        if (t.valorAlvo() != null && t.valorMinimo() != null && t.valorMinimo().compareTo(t.valorAlvo()) > 0
                || t.valorAlvo() != null && t.valorMaximo() != null && t.valorMaximo().compareTo(t.valorAlvo()) < 0) {
            throw new RegraDeNegocioException("valores_fora_de_ordem", "O valor mínimo não pode passar do alvo, nem o alvo do máximo.");
        }
        LocalDate hoje = LocalDate.now(relogio.withZone(BRASILIA));
        if (t.prazo() == null) {
            faltando.add("o prazo");
        } else if (!t.prazo().isAfter(hoje) || t.prazo().isAfter(hoje.plusYears(2))) {
            throw new RegraDeNegocioException("prazo_invalido", "O prazo da rodada precisa ser uma data futura, em até 2 anos.");
        }
        Map<TermoDaRodada, String> especificos = new EnumMap<>(TermoDaRodada.class);
        Map<TermoDaRodada, String> recebidos = t.especificos() == null ? Map.of() : t.especificos();
        List<TermoDaRodada> doInstrumento = instrumento == null ? List.of() : TermoDaRodada.doInstrumento(instrumento);
        recebidos.forEach((termo, valor) -> {
            if (!doInstrumento.contains(termo)) {
                throw new RegraDeNegocioException("termo_de_outro_instrumento", "O termo \"" + termo.getRotulo() + "\" não é deste instrumento.");
            }
            if (valor != null && !valor.isBlank()) {
                especificos.put(termo, termo(termo, valor.strip()));
            }
        });
        doInstrumento.stream().filter(termo -> !especificos.containsKey(termo)).forEach(termo -> faltando.add(termo.getRotulo().toLowerCase(Locale.ROOT)));
        if (completos && !faltando.isEmpty()) {
            throw new RegraDeNegocioException("termos_incompletos", "Para abrir a rodada, informe " + String.join(", ", faltando) + ".");
        }
        return new Rodada.Termos(estagio, instrumento, t.valorAlvo(), t.valorMinimo(), t.valorMaximo(), t.prazo(), especificos);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Código da lista
     * Objetivo: Conferir que o código é uma opção ativa da lista, ou anotar que falta.
     * Decisões: Vazio vira nulo.
     */
    private String codigo(Lista lista, String codigo, String nome, List<String> faltando) {
        if (codigo == null || codigo.isBlank()) {
            faltando.add(nome);
            return null;
        }
        return listas.exigirAtivo(lista, codigo.strip()).getCodigo();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Valor positivo
     * Objetivo: Conferir um valor em reais, ou anotar que falta.
     * Decisões: Maior que zero, com até dois centavos.
     */
    private static void positivo(BigDecimal valor, String nome, List<String> faltando) {
        if (valor == null) {
            faltando.add(nome);
        } else if (valor.signum() <= 0 || Math.max(valor.stripTrailingZeros().scale(), 0) > 2) {
            throw new RegraDeNegocioException("valor_invalido", "Informe " + nome + " em reais, maior que zero.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Conferir termo
     * Objetivo: Validar o valor de um termo específico conforme o tipo e devolvê-lo normalizado.
     * Decisões: Vírgula decimal aceita nos números.
     */
    private static String termo(TermoDaRodada termo, String valor) {
        if (termo.getTipo() == TermoDaRodada.Tipo.TEXTO) {
            if (valor.length() > TEXTO_MAXIMO) {
                throw new RegraDeNegocioException("texto_longo", "\"" + termo.getRotulo() + "\" pode ter até " + TEXTO_MAXIMO + " caracteres.");
            }
            return valor;
        }
        BigDecimal n;
        try {
            n = new BigDecimal(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new RegraDeNegocioException("numero_invalido", "Informe um número em \"" + termo.getRotulo() + "\".");
        }
        boolean valido = switch (termo.getTipo()) {
            case VALOR -> n.signum() > 0 && Math.max(n.stripTrailingZeros().scale(), 0) <= 2;
            case PERCENTUAL -> n.signum() >= 0 && n.compareTo(CEM) <= 0 && Math.max(n.stripTrailingZeros().scale(), 0) <= 2;
            case MESES -> n.stripTrailingZeros().scale() <= 0 && n.compareTo(BigDecimal.ONE) >= 0 && n.compareTo(BigDecimal.valueOf(MESES_MAXIMOS)) <= 0;
            case TEXTO -> true;
        };
        if (!valido) {
            throw new RegraDeNegocioException("numero_invalido", "Valor fora da faixa em \"" + termo.getRotulo() + "\".");
        }
        return n.stripTrailingZeros().toPlainString();
    }
}
