package br.com.orbithub.administracao.api;

import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.service.DocumentoLegalService;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteAdmin;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * Título: Publicação de termos pela equipe
 * Objetivo: Permitir que o administrador publique uma versão nova com data de vigência e veja as
 *           versões agendadas.
 * Decisões: Só ADMIN, conferido no servidor. A tela no painel chega com o backoffice (S2); até lá a
 *           publicação é feita pela API (Swagger).
 */
@RestController
@RequestMapping("/api/v1/admin/documentos-legais")
@SomenteAdmin
@Tag(name = "Termos e políticas", description = "Documentos legais e avisos versionados")
class PublicacaoDocumentoController {

    private final DocumentoLegalService documentos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Criação do controller
     * Objetivo: Receber o serviço de documentos.
     * Decisões: Injeção pelo construtor.
     */
    PublicacaoDocumentoController(DocumentoLegalService documentos) {
        this.documentos = documentos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Publicar versão
     * Objetivo: Gravar a próxima versão de um documento.
     * Decisões: Registra quem publicou (id do administrador).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publica uma nova versão (somente administrador)")
    DocumentoLegalResponse publicar(@Valid @RequestBody PublicacaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return DocumentoLegalResponse.de(documentos.publicar(pedido.tipo(), pedido.titulo(), pedido.conteudo(),
                pedido.vigenteDesde(), Autenticado.id(token)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Todas as versões de um documento
     * Objetivo: Listar as versões de um tipo, inclusive as agendadas.
     * Decisões: Visão da equipe, para conferir uma publicação antes de ela entrar em vigor.
     */
    @GetMapping("/{tipo}/versoes")
    @Operation(summary = "Todas as versões de um documento, inclusive agendadas (somente administrador)")
    List<DocumentoLegalResponse> versoes(@PathVariable TipoDocumento tipo) {
        return documentos.historicoCompleto(tipo).stream().map(DocumentoLegalResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Pedido de publicação
     * Objetivo: Dados de uma versão nova.
     * Decisões: Vigência opcional (sem ela, vale a partir de agora); limite de 100 mil caracteres por texto.
     */
    record PublicacaoRequest(
            @NotNull(message = "Informe o tipo do documento.") TipoDocumento tipo,
            @NotBlank(message = "Informe o título.") @Size(max = 160, message = "Use até 160 caracteres.")
            String titulo,
            @NotBlank(message = "Informe o texto.") @Size(max = 100_000, message = "Texto longo demais.")
            String conteudo,
            Instant vigenteDesde) {
    }
}
