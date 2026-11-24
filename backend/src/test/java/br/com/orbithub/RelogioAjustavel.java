package br.com.orbithub;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Relógio ajustável para testes
 * Objetivo: Permitir que um teste avance o tempo (expiração de sessão, bloqueio, links).
 * Decisões: Clock próprio em UTC, porque Clock.fixed não avança e esperar de verdade deixaria os testes
 *           lentos e instáveis.
 */
public class RelogioAjustavel extends Clock {

    private Instant agora;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Criação do relógio
     * Objetivo: Começar no instante informado.
     * Decisões: Instante fixo, para o teste ser reproduzível.
     */
    public RelogioAjustavel(Instant inicio) {
        this.agora = inicio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Avançar o tempo
     * Objetivo: Somar a duração informada ao instante atual.
     * Decisões: Só avança; voltar no tempo não acontece em produção.
     */
    public void avancar(Duration duracao) {
        agora = agora.plus(duracao);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Instante atual
     * Objetivo: Devolver o instante controlado pelo teste.
     * Decisões: Implementação do contrato de Clock.
     */
    @Override
    public Instant instant() {
        return agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Fuso do relógio
     * Objetivo: Informar o fuso.
     * Decisões: Sempre UTC, como o relógio da aplicação.
     */
    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Troca de fuso
     * Objetivo: Atender o contrato de Clock.
     * Decisões: Ignora o fuso pedido e devolve o próprio relógio, que é sempre UTC.
     */
    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }
}
