package br.com.orbithub.agenda.service;

import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-94, KAN-96, KAN-97
 * Título: Rotina R5 (reuniões)
 * Objetivo: De hora em hora, expirar propostas vencidas, lembrar as reuniões confirmadas e o registro do resultado.
 * Decisões: A regra fica na agenda.
 */
@Component
class LembretesDeReuniao extends RotinaBase {

    private final AgendaService agenda;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-96
     * Título: Criação da R5
     * Objetivo: Registrar código, horário e parâmetros.
     * Decisões: Módulo agenda.
     */
    LembretesDeReuniao(AgendaService agenda) {
        super("R5", "Expira propostas e lembra reuniões e registros", "0 0 * * * *", "agenda",
                List.of(AgendaService.LEMBRETES, AgendaService.PRAZO_REGISTRO));
        this.agenda = agenda;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-96
     * Título: Executar a R5
     * Objetivo: Chamar a rotina da agenda.
     * Decisões: Devolve quantas reuniões tratou.
     */
    @Override
    public Resultado executar(Instant agora) {
        int n = agenda.rotina();
        return new Resultado(n, 0, n + " reunião(ões) tratada(s)");
    }
}
