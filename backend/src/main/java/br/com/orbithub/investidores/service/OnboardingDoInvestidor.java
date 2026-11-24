package br.com.orbithub.investidores.service;

import br.com.orbithub.investidores.service.AtuacaoService.Atuacao;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Onboarding do investidor
 * Objetivo: Dizer se o investidor concluiu a tese, pulou para completar depois ou ainda está pendente, e registrar o pulo.
 * Decisões: Concluído = o dono da atuação (pessoa ou organização) tem perfil com tese. Pular é da pessoa e vale até ela
 *           cadastrar; o catálogo (S6) libera para "concluído" e "pulado". Serviço público, para o catálogo consultar.
 */
@Service
public class OnboardingDoInvestidor {

    private final PerfilDeInvestimentoService perfis;
    private final JdbcTemplate jdbc;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:25
     * Cartão Jira: KAN-18
     * Título: Criação do serviço
     * Objetivo: Receber o serviço de perfis, o banco e o relógio.
     * Decisões: Injeção por construtor.
     */
    public OnboardingDoInvestidor(PerfilDeInvestimentoService perfis, JdbcTemplate jdbc, Clock relogio) {
        this.perfis = perfis;
        this.jdbc = jdbc;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:25
     * Cartão Jira: KAN-18
     * Título: Estado do onboarding
     * Objetivo: Devolver se a tese de quem atua está concluída, se a pessoa pulou ou se está pendente.
     * Decisões: Perfil cadastrado vence o pulo.
     */
    @Transactional(readOnly = true)
    public Estado estado(Atuacao atuacao) {
        if (perfis.cadastrado(atuacao)) {
            return Estado.CONCLUIDO;
        }
        Integer pulos = jdbc.queryForObject("SELECT count(*) FROM investidor_onboarding WHERE usuario_id = ?",
                Integer.class, atuacao.pessoa());
        return pulos != null && pulos > 0 ? Estado.PULADO : Estado.PENDENTE;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:25
     * Cartão Jira: KAN-18
     * Título: Pular
     * Objetivo: Registrar que a pessoa preferiu completar a tese depois.
     * Decisões: Idempotente: mantém a primeira data.
     */
    @Transactional
    public void pular(UUID pessoa) {
        jdbc.update("INSERT INTO investidor_onboarding (usuario_id, pulado_em) VALUES (?, ?) ON CONFLICT DO NOTHING",
                pessoa, Timestamp.from(relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:25
     * Cartão Jira: KAN-18
     * Título: Estado
     * Objetivo: Enumerar a situação do onboarding.
     * Decisões: O catálogo libera CONCLUIDO e PULADO.
     */
    public enum Estado {
        PENDENTE,
        CONCLUIDO,
        PULADO;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 14:25
         * Cartão Jira: KAN-18
         * Título: Libera o catálogo
         * Objetivo: Dizer se o investidor já pode ver o catálogo.
         * Decisões: Concluído ou pulado.
         */
        public boolean liberaCatalogo() {
            return this != PENDENTE;
        }
    }
}
