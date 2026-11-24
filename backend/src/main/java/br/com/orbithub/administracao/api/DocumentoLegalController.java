package br.com.orbithub.administracao.api;

import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.service.DocumentoLegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Consulta pública de termos, políticas e avisos
 * Objetivo: Mostrar a versão vigente de cada documento e o histórico de versões.
 * Decisões: Rotas públicas (leitura sem login), porque o visitante lê os termos antes de se cadastrar.
 */
@RestController
@RequestMapping("/api/v1/documentos-legais")
@Tag(name = "Termos e políticas", description = "Documentos legais e avisos versionados")
class DocumentoLegalController {

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
    DocumentoLegalController(DocumentoLegalService documentos) {
        this.documentos = documentos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Documentos vigentes
     * Objetivo: Listar a versão em vigor de cada documento e aviso.
     * Decisões: Usado no cadastro e no rodapé do site.
     */
    @GetMapping
    @Operation(summary = "Versão vigente de cada documento e aviso")
    List<DocumentoLegalResponse> vigentes() {
        return documentos.vigentes().stream().map(DocumentoLegalResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Versões de um documento
     * Objetivo: Listar as versões já em vigor de um tipo, da mais nova para a mais antiga.
     * Decisões: Versões anteriores continuam consultáveis.
     */
    @GetMapping("/{tipo}/versoes")
    @Operation(summary = "Histórico de versões de um documento")
    List<DocumentoLegalResponse> versoes(@PathVariable TipoDocumento tipo) {
        return documentos.historico(tipo).stream().map(DocumentoLegalResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Uma versão
     * Objetivo: Ler uma versão específica pelo id.
     * Decisões: Versão agendada para o futuro responde 404.
     */
    @GetMapping("/versoes/{id}")
    @Operation(summary = "Uma versão específica")
    DocumentoLegalResponse versao(@PathVariable UUID id) {
        return DocumentoLegalResponse.de(documentos.buscarPublicado(id));
    }
}
