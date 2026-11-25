package br.com.orbithub.catalogo.service;

import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Rotina R10 (vigência das edições)
 * Objetivo: Pôr no ar as edições agendadas cujo início chegou e arquivar as que passaram do fim.
 * Decisões: Todo dia às 00:05 (Brasília), logo depois da virada do dia. Sem erros por item: a mudança é só de situação.
 */
@Component
class VigenciaDasEdicoes extends RotinaBase {

    private final EdicoesService edicoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Criação da rotina
     * Objetivo: Registrar código, descrição, horário, módulo e parâmetros.
     * Decisões: Lê catalogo.frequencia_edicoes só na criação das edições; aqui não há parâmetro.
     */
    VigenciaDasEdicoes(EdicoesService edicoes) {
        super("R10", "Publica e arquiva as edições temáticas pela vigência", "0 5 0 * * *", "catalogo", List.of());
        this.edicoes = edicoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Executar
     * Objetivo: Seguir a vigência de todas as edições agendadas e vigentes.
     * Decisões: Devolve quantas mudaram.
     */
    @Override
    public Resultado executar(Instant agora) {
        int mudaram = edicoes.seguirVigencias();
        return new Resultado(mudaram, 0, mudaram + " edição(ões) mudaram de situação pela vigência");
    }
}
