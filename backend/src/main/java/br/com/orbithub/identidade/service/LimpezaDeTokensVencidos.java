package br.com.orbithub.identidade.service;

import br.com.orbithub.compartilhado.parametros.Chaves;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.rotinas.EtapaDeRetencao;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: R12 – Limpeza de links e sessões vencidos
 * Objetivo: Apagar os links de e-mail (confirmação, redefinição, troca de e-mail, convite, desafio do segundo fator) e
 *           as sessões que venceram há mais de retencao.tokens_dias, para não guardar dado sem uso (LGPD).
 * Decisões: Primeira parte da R12 (limpeza e retenção), diária às 3h. Idempotente: apagar o que já foi apagado não muda
 *           nada. A sessão sai só quando a família inteira venceu (o limite absoluto é de horas, bem menor que a
 *           retenção), para a detecção de reuso de token continuar valendo. A remoção dos cadastros não confirmados
 *           (cadastro.prazo_confirmacao_dias) entra com a política de retenção (KAN-93).
 */
@Component
class LimpezaDeTokensVencidos extends RotinaBase {

    private final JdbcTemplate jdbc;
    private final LeituraDeParametros parametros;
    private final List<EtapaDeRetencao> etapas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Criação da rotina
     * Objetivo: Descrever a rotina e receber o acesso ao banco e os parâmetros.
     * Decisões: SQL direto: é uma limpeza em lote.
     */
    LimpezaDeTokensVencidos(JdbcTemplate jdbc, LeituraDeParametros parametros, List<EtapaDeRetencao> etapas) {
        super("R12", "Limpeza e retenção: links e sessões vencidos e os prazos de guarda dos dados pessoais", "0 0 3 * * *", "identidade",
                Stream.concat(Stream.of(Chaves.RETENCAO_TOKENS), etapas.stream().flatMap(e -> e.parametros().stream())).distinct().toList());
        this.jdbc = jdbc;
        this.parametros = parametros;
        this.etapas = etapas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Apagar vencidos
     * Objetivo: Remover links e sessões vencidos além da retenção e aplicar as etapas de retenção dos módulos (KAN-93).
     * Decisões: Uma transação para todas as limpezas; o resumo traz só as quantidades por tipo.
     */
    @Override
    @Transactional
    public Resultado executar(Instant agora) {
        Timestamp limite = Timestamp.from(agora.minus(parametros.duracao(Chaves.RETENCAO_TOKENS)));
        int links = jdbc.update("DELETE FROM token_conta WHERE expira_em < ?", limite);
        int sessoes = jdbc.update("DELETE FROM refresh_token WHERE familia_expira_em < ?", limite);
        int total = links + sessoes;
        List<String> partes = new ArrayList<>(List.of(links + " link(s)", sessoes + " sessão(ões)"));
        for (EtapaDeRetencao etapa : etapas) {
            int n = etapa.aplicar(agora);
            total += n;
            partes.add(n + " " + etapa.tipo());
        }
        return new Resultado(total, 0, String.join(", ", partes));
    }
}
