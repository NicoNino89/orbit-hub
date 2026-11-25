package br.com.orbithub.dataroom.service;

import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-105
 * Título: Rotinas do Nível 2 (R3 e R4)
 * Objetivo: Encerrar de hora em hora os acessos vencidos (R3) e avisar todo dia os que vencem em breve (R4).
 * Decisões: Cada rotina é um componente; a regra fica no serviço de acessos.
 */
final class RotinasDoNivel2 {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: RotinasDoNivel2
     * Objetivo: Rotina auxiliar RotinasDoNivel2.
     * Decisões: Ver a documentação da classe.
     */
    private RotinasDoNivel2() {
    }

    @Component
    static class ExpirarAcessos extends RotinaBase {

        private final AcessosNivel2Service acessos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: ExpirarAcessos
     * Objetivo: Rotina auxiliar ExpirarAcessos.
     * Decisões: Ver a documentação da classe.
     */
        ExpirarAcessos(AcessosNivel2Service acessos) {
            super("R3", "Encerra os acessos ao Nível 2 vencidos", "0 0 * * * *", "dataroom", List.of(AcessosNivel2Service.VALIDADE));
            this.acessos = acessos;
        }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: executar
     * Objetivo: Rotina auxiliar executar.
     * Decisões: Ver a documentação da classe.
     */
        @Override
        public Resultado executar(Instant agora) {
            int n = acessos.expirarVencidos();
            return new Resultado(n, 0, n + " acesso(s) ao Nível 2 encerrado(s) por vencimento");
        }
    }

    @Component
    static class AvisarVencimento extends RotinaBase {

        private final AcessosNivel2Service acessos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: AvisarVencimento
     * Objetivo: Rotina auxiliar AvisarVencimento.
     * Decisões: Ver a documentação da classe.
     */
        AvisarVencimento(AcessosNivel2Service acessos) {
            super("R4", "Avisa investidor e startup dos acessos ao Nível 2 perto de vencer", "0 0 8 * * *", "dataroom",
                    List.of(AcessosNivel2Service.AVISO));
            this.acessos = acessos;
        }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: executar
     * Objetivo: Rotina auxiliar executar.
     * Decisões: Ver a documentação da classe.
     */
        @Override
        public Resultado executar(Instant agora) {
            int n = acessos.avisarVencimentos();
            return new Resultado(n, 0, n + " aviso(s) de vencimento do Nível 2");
        }
    }
}
