package br.com.orbithub.conexoes.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteInvestidor;
import br.com.orbithub.conexoes.domain.Rodada;
import br.com.orbithub.conexoes.service.ConducaoDaRodadaService;
import br.com.orbithub.investidores.service.AtuacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-59, KAN-64, KAN-109
 * Título: Condução da rodada
 * Objetivo: Expor o painel da rodada (startup e investidor), a entrada com intenção, o convite de co-investidor pelo
 *           líder, a aprovação, a liderança, a política, a prorrogação, o pedido de encerramento, o cancelamento, a
 *           conclusão com o resultado e a resposta do investidor ao investimento registrado.
 * Decisões: Regras no serviço. O investidor manda o cabeçalho de organização quando atua por ela.
 */
@RestController
@Tag(name = "Rodadas", description = "Rodadas de investimento")
class ConducaoDaRodadaController {

    private final ConducaoDaRodadaService conducao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Criação
     * Objetivo: Receber o serviço.
     * Decisões: Injeção por construtor.
     */
    ConducaoDaRodadaController(ConducaoDaRodadaService conducao) {
        this.conducao = conducao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Painel da startup
     * Objetivo: Mostrar a rodada completa à equipe.
     * Decisões: Fundadores.
     */
    @GetMapping("/api/v1/rodadas/{id}/painel")
    @SomenteEmpreendedor
    @Operation(summary = "Painel da rodada (startup)")
    ConducaoDaRodadaService.Painel painel(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return conducao.daStartup(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Rodada para o investidor
     * Objetivo: Mostrar a rodada aberta da startup ao investidor.
     * Decisões: Pela startup do catálogo.
     */
    @GetMapping("/api/v1/catalogo/startups/{id}/rodada")
    @SomenteInvestidor
    @Operation(summary = "Rodada aberta da startup (investidor)")
    ConducaoDaRodadaService.ParaInvestidor paraInvestidor(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                                                          @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return conducao.paraInvestidor(Autenticado.id(token), organizacao, id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Entrar
     * Objetivo: Entrar na rodada com intenção.
     * Decisões: 201.
     */
    @PostMapping("/api/v1/rodadas/{id}/participantes")
    @SomenteInvestidor
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Entra na rodada")
    ConducaoDaRodadaService.Participante entrar(@PathVariable UUID id, @RequestBody IntencaoRequest pedido, @AuthenticationPrincipal Jwt token,
                                                @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return conducao.entrar(Autenticado.id(token), organizacao, id, pedido.intencao(), pedido.condicoes());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Atualizar intenção
     * Objetivo: Trocar a intenção do participante.
     * Decisões: Só com a rodada em curso.
     */
    @PutMapping("/api/v1/rodadas/{id}/participantes/minha")
    @SomenteInvestidor
    @Operation(summary = "Atualiza a minha intenção na rodada")
    ConducaoDaRodadaService.Participante intencao(@PathVariable UUID id, @RequestBody IntencaoRequest pedido, @AuthenticationPrincipal Jwt token,
                                                  @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return conducao.atualizarIntencao(Autenticado.id(token), organizacao, id, pedido.intencao(), pedido.condicoes());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Convidar co-investidor
     * Objetivo: O líder convida um investidor pelo e-mail.
     * Decisões: Aguarda aprovação da startup.
     */
    @PostMapping("/api/v1/rodadas/{id}/convites")
    @SomenteInvestidor
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Convida um co-investidor (líder)")
    ConducaoDaRodadaService.Participante convidar(@PathVariable UUID id, @RequestBody EmailRequest pedido, @AuthenticationPrincipal Jwt token,
                                                  @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return conducao.convidarCoInvestidor(Autenticado.id(token), organizacao, id, pedido.email());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Decidir convidado
     * Objetivo: Aprovar ou recusar o co-investidor convidado.
     * Decisões: Fundador administrador.
     */
    @PostMapping("/api/v1/rodadas/{id}/participantes/{participante}/decisao")
    @SomenteEmpreendedor
    @Operation(summary = "Aprova ou recusa o co-investidor convidado pelo líder")
    ConducaoDaRodadaService.Participante decidir(@PathVariable UUID id, @PathVariable UUID participante, @Valid @RequestBody DecisaoRequest pedido,
                                                 @AuthenticationPrincipal Jwt token) {
        return conducao.decidirConvidado(Autenticado.id(token), id, participante, pedido.aprovar());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Convidar líder
     * Objetivo: Convidar um participante para liderar.
     * Decisões: Fundador administrador.
     */
    @PostMapping("/api/v1/rodadas/{id}/participantes/{participante}/lideranca")
    @SomenteEmpreendedor
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Convida um participante para liderar a rodada")
    void convidarLider(@PathVariable UUID id, @PathVariable UUID participante, @AuthenticationPrincipal Jwt token) {
        conducao.convidarLider(Autenticado.id(token), id, participante);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Responder liderança
     * Objetivo: Aceitar ou recusar liderar.
     * Decisões: Investidor convidado.
     */
    @PostMapping("/api/v1/rodadas/{id}/lideranca/resposta")
    @SomenteInvestidor
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Aceita ou recusa liderar a rodada")
    void responderLideranca(@PathVariable UUID id, @Valid @RequestBody DecisaoRequest pedido, @AuthenticationPrincipal Jwt token,
                            @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        conducao.responderLideranca(Autenticado.id(token), organizacao, id, pedido.aprovar());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109
     * Título: Política
     * Objetivo: Declarar a política de alocação.
     * Decisões: Fundador administrador.
     */
    @PutMapping("/api/v1/rodadas/{id}/politica")
    @SomenteEmpreendedor
    @Operation(summary = "Define a política de alocação")
    PoliticaRequest politica(@PathVariable UUID id, @Valid @RequestBody PoliticaRequest pedido, @AuthenticationPrincipal Jwt token) {
        return new PoliticaRequest(conducao.definirPolitica(Autenticado.id(token), id, pedido.politica()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109
     * Título: Prorrogar
     * Objetivo: Dar novo prazo à rodada.
     * Decisões: Fundador administrador.
     */
    @PostMapping("/api/v1/rodadas/{id}/prorrogacao")
    @SomenteEmpreendedor
    @Operation(summary = "Prorroga a rodada")
    SituacaoResponse prorrogar(@PathVariable UUID id, @Valid @RequestBody PrazoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return SituacaoResponse.de(conducao.prorrogar(Autenticado.id(token), id, pedido.prazo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109
     * Título: Pedir encerramento
     * Objetivo: O líder pede o encerramento.
     * Decisões: 204.
     */
    @PostMapping("/api/v1/rodadas/{id}/pedido-de-encerramento")
    @SomenteInvestidor
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Pede o encerramento da rodada (líder)")
    void pedirEncerramento(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                           @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        conducao.pedirEncerramento(Autenticado.id(token), organizacao, id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109
     * Título: Cancelar
     * Objetivo: Cancelar a rodada com motivo.
     * Decisões: Fundador administrador.
     */
    @PostMapping("/api/v1/rodadas/{id}/cancelamento")
    @SomenteEmpreendedor
    @Operation(summary = "Cancela a rodada")
    SituacaoResponse cancelar(@PathVariable UUID id, @RequestBody MotivoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return SituacaoResponse.de(conducao.cancelar(Autenticado.id(token), id, pedido.motivo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-64
     * Título: Concluir
     * Objetivo: Encerrar a rodada como concluída e registrar o resultado.
     * Decisões: Fundador administrador.
     */
    @PostMapping("/api/v1/rodadas/{id}/conclusao")
    @SomenteEmpreendedor
    @Operation(summary = "Conclui a rodada e registra o resultado")
    SituacaoResponse concluir(@PathVariable UUID id, @RequestBody ConducaoDaRodadaService.Resultado pedido, @AuthenticationPrincipal Jwt token) {
        return SituacaoResponse.de(conducao.concluir(Autenticado.id(token), id, pedido));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-64
     * Título: Responder investimento
     * Objetivo: O investidor confirma ou contesta a participação registrada.
     * Decisões: Só o investidor indicado.
     */
    @PostMapping("/api/v1/rodadas/investimentos/{investimento}/resposta")
    @SomenteInvestidor
    @Operation(summary = "Confirma ou contesta o investimento registrado")
    ConducaoDaRodadaService.Investimento responder(@PathVariable UUID investimento, @Valid @RequestBody DecisaoRequest pedido,
                                                   @AuthenticationPrincipal Jwt token,
                                                   @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return conducao.responderInvestimento(Autenticado.id(token), organizacao, investimento, pedido.aprovar());
    }

    record IntencaoRequest(BigDecimal intencao, String condicoes) {
    }

    record EmailRequest(String email) {
    }

    record DecisaoRequest(@NotNull(message = "Diga sim ou não.") Boolean aprovar) {
    }

    record PoliticaRequest(@NotNull(message = "Escolha a política.") ConducaoDaRodadaService.Politica politica) {
    }

    record PrazoRequest(@NotNull(message = "Informe o novo prazo.") LocalDate prazo) {
    }

    record MotivoRequest(String motivo) {
    }

    record SituacaoResponse(UUID id, br.com.orbithub.conexoes.domain.SituacaoDaRodada situacao, br.com.orbithub.conexoes.domain.ResultadoDaRodada resultado) {
        static SituacaoResponse de(Rodada r) {
            return new SituacaoResponse(r.getId(), r.getSituacao(), r.getResultado());
        }
    }
}
