package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.empreendimentos.domain.IndicadorDaStartup;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.domain.ValorDeIndicador;
import br.com.orbithub.empreendimentos.repository.ValorDeIndicadorRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: Indicadores de tração da startup
 * Objetivo: Registrar os valores declarados dos indicadores com o mês de referência, mostrar o atual com o histórico e
 *           dizer quais estão desatualizados.
 * Decisões: Os números são declarados pelo empreendedor e não auditados: a plataforma confere só o formato e que a
 *           referência não está no futuro. Quem edita é quem pode editar a seção de tração (KAN-65). Desatualizado =
 *           referência anterior a startup.indicador_desatualizado_meses meses atrás (horário de Brasília).
 */
@Service
public class IndicadoresDaStartupService {

    static final String CHAVE_DESATUALIZADO = "startup.indicador_desatualizado_meses";
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private final ValorDeIndicadorRepository valores;
    private final StartupsService cadastro;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Criação do serviço
     * Objetivo: Receber o repositório, o cadastro, os parâmetros e o relógio.
     * Decisões: Injeção por construtor.
     */
    public IndicadoresDaStartupService(ValorDeIndicadorRepository valores, StartupsService cadastro, LeituraDeParametros parametros,
                                       Clock relogio) {
        this.valores = valores;
        this.cadastro = cadastro;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Listar indicadores
     * Objetivo: Devolver cada indicador com o valor atual, se está desatualizado e o histórico.
     * Decisões: Indicador sem valor aparece vazio, na ordem do enum. Qualquer membro da equipe vê.
     */
    @Transactional(readOnly = true)
    public List<Indicador> listar(UUID eu, UUID startupId) {
        cadastro.papel(eu, startupId);
        Map<IndicadorDaStartup, List<Valor>> porIndicador = new EnumMap<>(IndicadorDaStartup.class);
        for (ValorDeIndicador v : valores.findByStartupIdOrderByReferenciaDescRegistradoEmDesc(startupId)) {
            porIndicador.computeIfAbsent(v.getIndicador(), k -> new ArrayList<>())
                    .add(new Valor(v.getValor(), v.getReferencia(), v.getRegistradoEm()));
        }
        YearMonth limite = limite();
        List<Indicador> lista = new ArrayList<>();
        for (IndicadorDaStartup i : IndicadorDaStartup.values()) {
            List<Valor> historico = porIndicador.getOrDefault(i, List.of());
            Valor atual = historico.isEmpty() ? null : historico.get(0);
            lista.add(new Indicador(i, i.getRotulo(), i.getFormato(), atual, atual != null && atual.referencia().isBefore(limite), historico));
        }
        return lista;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Registrar valor
     * Objetivo: Declarar um novo valor de um indicador.
     * Decisões: Sem referência não salva. Referência no futuro é recusada. Valores anteriores ficam no histórico.
     */
    @Transactional
    public Indicador registrar(UUID eu, UUID startupId, IndicadorDaStartup indicador, BigDecimal valor, String referencia) {
        cadastro.exigirEditor(eu, startupId, SecaoDaStartup.TRACAO);
        if (indicador == null) {
            throw new RegraDeNegocioException("indicador_obrigatorio", "Escolha o indicador.");
        }
        if (valor == null || !indicador.getFormato().aceita(valor)) {
            throw new RegraDeNegocioException("valor_invalido",
                    "Em \"" + indicador.getRotulo() + "\", informe " + indicador.getFormato().explicacao() + ".");
        }
        YearMonth mes = mes(referencia);
        if (mes.isAfter(YearMonth.now(relogio.withZone(BRASILIA)))) {
            throw new RegraDeNegocioException("referencia_futura", "O mês de referência não pode estar no futuro.");
        }
        Instant agora = relogio.instant();
        valores.save(ValorDeIndicador.novo(startupId, indicador, valor, mes, eu, agora));
        return listar(eu, startupId).stream().filter(i -> i.indicador() == indicador).findFirst().orElseThrow();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Startups com indicador desatualizado
     * Objetivo: Listar as startups com algum indicador cuja referência mais recente passou do limite.
     * Decisões: Usado pela R8.
     */
    @Transactional(readOnly = true)
    public List<UUID> startupsComIndicadorDesatualizado() {
        return valores.startupsComIndicadorAntesDe(limite().atDay(1));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Limite de atualização
     * Objetivo: Calcular o mês a partir do qual um indicador ainda está atualizado.
     * Decisões: Mês atual em Brasília menos o parâmetro (padrão 6).
     */
    private YearMonth limite() {
        return YearMonth.now(relogio.withZone(BRASILIA)).minusMonths(parametros.inteiro(CHAVE_DESATUALIZADO));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Mês de referência
     * Objetivo: Ler o mês e o ano no formato AAAA-MM.
     * Decisões: Vazio ou inválido: o indicador não é salvo.
     */
    private static YearMonth mes(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            throw new RegraDeNegocioException("referencia_obrigatoria", "Informe o mês e o ano a que o número se refere.");
        }
        try {
            return YearMonth.parse(referencia.strip());
        } catch (DateTimeParseException e) {
            throw new RegraDeNegocioException("referencia_obrigatoria", "Informe o mês e o ano a que o número se refere (AAAA-MM).");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Indicador
     * Objetivo: Levar um indicador com o valor atual, a marca de desatualizado e o histórico.
     * Decisões: O histórico inclui o valor atual, do mais recente ao mais antigo.
     */
    public record Indicador(IndicadorDaStartup indicador, String rotulo, IndicadorDaStartup.Formato formato, Valor atual,
                            boolean desatualizado, List<Valor> historico) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Valor
     * Objetivo: Levar um valor declarado com a referência e a data da declaração.
     * Decisões: Sem o autor, que é dado interno da equipe.
     */
    public record Valor(BigDecimal valor, YearMonth referencia, Instant registradoEm) {
    }
}
