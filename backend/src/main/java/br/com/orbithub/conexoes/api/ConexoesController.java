package br.com.orbithub.conexoes.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteInvestidor;
import br.com.orbithub.conexoes.domain.Conexao;
import br.com.orbithub.conexoes.service.ConexoesService;
import br.com.orbithub.investidores.service.AtuacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-45, KAN-46, KAN-47, KAN-48, KAN-49
 * Título: Conexões
 * Objetivo: Expor a manifestação de interesse, a retirada, o aceite (Match), a recusa, o avanço, o encerramento, os
 *           painéis dos dois lados e o histórico.
 * Decisões: O investidor manda o cabeçalho de organização quando atua por ela. As regras de quem pode o quê ficam no
 *           serviço.
 */
@RestController
@Tag(name = "Conexões", description = "Interesse, Match e ciclo das conexões")
class ConexoesController {

    private final ConexoesService conexoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Criação
     * Objetivo: Receber o serviço.
     * Decisões: Injeção por construtor.
     */
    ConexoesController(ConexoesService conexoes) {
        this.conexoes = conexoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Situação para o investidor
     * Objetivo: Dizer se pode manifestar interesse e a conexão ativa.
     * Decisões: Para o botão da apresentação.
     */
    @GetMapping("/api/v1/catalogo/startups/{id}/conexao")
    @SomenteInvestidor
    @Operation(summary = "Conexão do investidor com a startup")
    ConexoesService.ParaInvestidor situacao(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                                            @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return conexoes.situacao(Autenticado.id(token), organizacao, id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45, KAN-46
     * Título: Manifestar interesse
     * Objetivo: Criar a conexão pendente.
     * Decisões: 201 com a conexão.
     */
    @PostMapping("/api/v1/catalogo/startups/{id}/interesse")
    @SomenteInvestidor
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Demonstra interesse na startup")
    IdResponse manifestar(@PathVariable UUID id, @RequestBody InteresseRequest pedido, @AuthenticationPrincipal Jwt token,
                          @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        Conexao c = conexoes.manifestar(Autenticado.id(token), organizacao, id,
                new Conexao.Interesse(pedido.valor(), pedido.condicoes(), pedido.pedido(), pedido.mensagem()));
        return new IdResponse(c.getId(), c.getSituacao());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Painel do investidor
     * Objetivo: Listar as conexões da pessoa ou da organização.
     * Decisões: Pelo cabeçalho de organização.
     */
    @GetMapping("/api/v1/conexoes")
    @SomenteInvestidor
    @Operation(summary = "Conexões do investidor")
    List<ConexoesService.Item> doInvestidor(@AuthenticationPrincipal Jwt token,
                                            @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return conexoes.doInvestidor(Autenticado.id(token), organizacao);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-47
     * Título: Painel da startup
     * Objetivo: Listar as conexões da startup, com filtro de situação.
     * Decisões: Fundadores.
     */
    @GetMapping("/api/v1/startups/{id}/conexoes")
    @SomenteEmpreendedor
    @Operation(summary = "Conexões da startup")
    List<ConexoesService.Item> daStartup(@PathVariable UUID id, @RequestParam(required = false) Conexao.Situacao situacao,
                                         @AuthenticationPrincipal Jwt token) {
        return conexoes.daStartup(Autenticado.id(token), id, situacao);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Retirar
     * Objetivo: Retirar o interesse pendente.
     * Decisões: Só o investidor.
     */
    @PostMapping("/api/v1/conexoes/{id}/retirada")
    @Operation(summary = "Retira o interesse pendente")
    IdResponse retirar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                       @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return IdResponse.de(conexoes.retirar(Autenticado.id(token), organizacao, id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-48
     * Título: Aceitar
     * Objetivo: Aceitar a manifestação (Match).
     * Decisões: Só o fundador administrador.
     */
    @PostMapping("/api/v1/conexoes/{id}/aceite")
    @Operation(summary = "Aceita a manifestação de interesse (Match)")
    IdResponse aceitar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return IdResponse.de(conexoes.aceitar(Autenticado.id(token), id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-48
     * Título: Recusar
     * Objetivo: Recusar a manifestação.
     * Decisões: Motivo opcional, só para a plataforma.
     */
    @PostMapping("/api/v1/conexoes/{id}/recusa")
    @Operation(summary = "Recusa a manifestação de interesse")
    IdResponse recusar(@PathVariable UUID id, @RequestBody(required = false) MotivoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return IdResponse.de(conexoes.recusar(Autenticado.id(token), id, pedido == null ? null : pedido.motivo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Avançar
     * Objetivo: Passar para em análise ou em negociação.
     * Decisões: Qualquer lado.
     */
    @PostMapping("/api/v1/conexoes/{id}/avanco")
    @Operation(summary = "Avança a conexão")
    IdResponse avancar(@PathVariable UUID id, @Valid @RequestBody AvancoRequest pedido, @AuthenticationPrincipal Jwt token,
                       @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return IdResponse.de(conexoes.avancar(Autenticado.id(token), organizacao, id, pedido.para()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Encerrar
     * Objetivo: Encerrar a conexão com motivo.
     * Decisões: Qualquer lado.
     */
    @PostMapping("/api/v1/conexoes/{id}/encerramento")
    @Operation(summary = "Encerra a conexão")
    IdResponse encerrar(@PathVariable UUID id, @Valid @RequestBody EncerramentoRequest pedido, @AuthenticationPrincipal Jwt token,
                        @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return IdResponse.de(conexoes.encerrar(Autenticado.id(token), organizacao, id, pedido.motivo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Histórico
     * Objetivo: Listar as mudanças da conexão.
     * Decisões: Só as partes.
     */
    @GetMapping("/api/v1/conexoes/{id}/historico")
    @Operation(summary = "Histórico da conexão")
    List<ConexoesService.Mudanca> historico(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                                            @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return conexoes.historico(Autenticado.id(token), organizacao, id);
    }

    record InteresseRequest(BigDecimal valor, String condicoes, Conexao.Pedido pedido, String mensagem) {
    }

    record MotivoRequest(String motivo) {
    }

    record AvancoRequest(@NotNull(message = "Diga para onde a conexão avança.") Conexao.Situacao para) {
    }

    record EncerramentoRequest(@NotBlank(message = "Escolha o motivo.") String motivo) {
    }

    record IdResponse(UUID id, Conexao.Situacao situacao) {
        static IdResponse de(Conexao c) {
            return new IdResponse(c.getId(), c.getSituacao());
        }
    }
}
