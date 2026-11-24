package br.com.orbithub.administracao.api;

import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Aceites do usuário
 * Objetivo: Mostrar o que falta aceitar, registrar aceites e listar o histórico de aceites da conta.
 * Decisões: Sempre sobre a própria conta (id do token); ninguém registra aceite em nome de outro.
 */
@RestController
@RequestMapping("/api/v1/conta/aceites")
@Tag(name = "Termos e políticas", description = "Documentos legais e avisos versionados")
class AceiteController {

    private final AceiteService aceites;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Criação do controller
     * Objetivo: Receber o serviço de aceites.
     * Decisões: Injeção pelo construtor.
     */
    AceiteController(AceiteService aceites) {
        this.aceites = aceites;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Aceites pendentes
     * Objetivo: Listar as versões vigentes que a conta ainda precisa aceitar.
     * Decisões: O investidor também recebe o Termo de Confidencialidade.
     */
    @GetMapping("/pendentes")
    @Operation(summary = "Documentos vigentes ainda não aceitos pela conta")
    List<DocumentoLegalResponse> pendentes(@AuthenticationPrincipal Jwt token) {
        return aceites.pendentes(Autenticado.id(token), Autenticado.temPapel(token, "INVESTIDOR"))
                .stream().map(DocumentoLegalResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Registrar aceites
     * Objetivo: Gravar o aceite das versões informadas.
     * Decisões: Depois de aceitar, o front-end renova a sessão para receber um token sem pendência.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Registra o aceite das versões informadas")
    void aceitar(@Valid @RequestBody AceiteRequest pedido, @AuthenticationPrincipal Jwt token) {
        aceites.aceitar(Autenticado.id(token), pedido.documentos());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Histórico de aceites
     * Objetivo: Listar qual versão a conta aceitou e quando.
     * Decisões: Transparência para o titular dos dados (LGPD).
     */
    @GetMapping
    @Operation(summary = "Histórico de aceites da conta")
    List<AceiteResponse> historico(@AuthenticationPrincipal Jwt token) {
        return aceites.historico(Autenticado.id(token)).stream()
                .map(a -> new AceiteResponse(a.getDocumentoId(), a.getAceitoEm()))
                .toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Pedido de aceite
     * Objetivo: Ids das versões aceitas.
     * Decisões: Pelo menos uma versão.
     */
    record AceiteRequest(@NotEmpty(message = "Informe os documentos aceitos.") List<UUID> documentos) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Aceite registrado
     * Objetivo: Versão aceita e momento do aceite.
     * Decisões: O texto da versão é consultado pela rota pública de versões.
     */
    record AceiteResponse(UUID documento, Instant aceitoEm) {
    }
}
