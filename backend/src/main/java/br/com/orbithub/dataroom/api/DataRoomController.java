package br.com.orbithub.dataroom.api;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteInvestidor;
import br.com.orbithub.dataroom.service.AcessosNivel2Service;
import br.com.orbithub.dataroom.service.DataRoomService;
import br.com.orbithub.empreendimentos.domain.RegraDeConfidencialidade;
import br.com.orbithub.investidores.service.AtuacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-30, KAN-31, KAN-32, KAN-33, KAN-63, KAN-145
 * Título: Data Room e Nível 2
 * Objetivo: Expor à equipe o Data Room, a regra de confidencialidade, as liberações, revogações e o histórico; ao
 *           investidor, a situação, o Nível 2, os links temporários, o NDA e a confirmação; à organização, o aceite do
 *           termo.
 * Decisões: Regras nos serviços. Arquivos sempre com nosniff, sem cache e como anexo.
 */
@RestController
@Tag(name = "Data Room", description = "Informações confidenciais (Nível 2) e documentos")
class DataRoomController {

    private final DataRoomService dataRoom;
    private final AcessosNivel2Service acessos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: DataRoomController
     * Objetivo: Rotina auxiliar DataRoomController.
     * Decisões: Ver a documentação da classe.
     */
    DataRoomController(DataRoomService dataRoom, AcessosNivel2Service acessos) {
        this.dataRoom = dataRoom;
        this.acessos = acessos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: documentos
     * Objetivo: Rotina auxiliar documentos.
     * Decisões: Ver a documentação da classe.
     */
    @GetMapping("/api/v1/startups/{id}/dataroom")
    @SomenteEmpreendedor
    @Operation(summary = "Documentos do Data Room (equipe)")
    List<DocumentoResponse> documentos(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return dataRoom.daEquipe(Autenticado.id(token), id).stream().map(DocumentoResponse::de).toList();
    }

    @PostMapping(path = "/api/v1/startups/{id}/dataroom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SomenteEmpreendedor
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia um documento ao Data Room")
    DocumentoResponse enviar(@PathVariable UUID id, @RequestParam MultipartFile arquivo, @RequestParam String categoria, @RequestParam String nome,
                             @RequestParam(required = false) String descricao, @AuthenticationPrincipal Jwt token) {
        return DocumentoResponse.de(dataRoom.enviar(Autenticado.id(token), id, categoria, nome, descricao, arquivo.getOriginalFilename(), bytes(arquivo)));
    }

    @PostMapping(path = "/api/v1/startups/{id}/dataroom/{documento}/substituicao", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SomenteEmpreendedor
    @Operation(summary = "Envia a nova versão de um documento")
    DocumentoResponse substituir(@PathVariable UUID id, @PathVariable UUID documento, @RequestParam MultipartFile arquivo,
                                 @AuthenticationPrincipal Jwt token) {
        return DocumentoResponse.de(dataRoom.substituir(Autenticado.id(token), id, documento, arquivo.getOriginalFilename(), bytes(arquivo)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: remover
     * Objetivo: Rotina auxiliar remover.
     * Decisões: Ver a documentação da classe.
     */
    @DeleteMapping("/api/v1/startups/{id}/dataroom/{documento}")
    @SomenteEmpreendedor
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um documento do Data Room")
    void remover(@PathVariable UUID id, @PathVariable UUID documento, @AuthenticationPrincipal Jwt token) {
        dataRoom.remover(Autenticado.id(token), id, documento);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: painel
     * Objetivo: Rotina auxiliar painel.
     * Decisões: Ver a documentação da classe.
     */
    @GetMapping("/api/v1/startups/{id}/nivel2")
    @SomenteEmpreendedor
    @Operation(summary = "Regra de confidencialidade e acessos ao Nível 2 (equipe)")
    AcessosNivel2Service.Painel painel(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return acessos.painel(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: regra
     * Objetivo: Rotina auxiliar regra.
     * Decisões: Ver a documentação da classe.
     */
    @PutMapping("/api/v1/startups/{id}/nivel2/regra")
    @SomenteEmpreendedor
    @Operation(summary = "Define a regra de confidencialidade")
    RegraResponse regra(@PathVariable UUID id, @Valid @RequestBody RegraRequest pedido, @AuthenticationPrincipal Jwt token) {
        return new RegraResponse(acessos.definirRegra(Autenticado.id(token), id, pedido.regra()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: liberar
     * Objetivo: Rotina auxiliar liberar.
     * Decisões: Ver a documentação da classe.
     */
    @PostMapping("/api/v1/startups/{id}/nivel2/acessos")
    @SomenteEmpreendedor
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Libera o Nível 2 para o investidor de uma conexão")
    AcessosNivel2Service.Acesso liberar(@PathVariable UUID id, @Valid @RequestBody LiberacaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return acessos.liberar(Autenticado.id(token), id, pedido.conexaoId(), pedido.dias());
    }

    @PostMapping("/api/v1/startups/{id}/nivel2/acessos/{acesso}/renovacao")
    @SomenteEmpreendedor
    @Operation(summary = "Renova um acesso ao Nível 2")
    AcessosNivel2Service.Acesso renovar(@PathVariable UUID id, @PathVariable UUID acesso, @RequestBody(required = false) PrazoRequest pedido,
                                        @AuthenticationPrincipal Jwt token) {
        return acessos.renovar(Autenticado.id(token), id, acesso, pedido == null ? null : pedido.dias());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: revogar
     * Objetivo: Rotina auxiliar revogar.
     * Decisões: Ver a documentação da classe.
     */
    @PostMapping("/api/v1/startups/{id}/nivel2/acessos/{acesso}/revogacao")
    @SomenteEmpreendedor
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoga um acesso ao Nível 2")
    void revogar(@PathVariable UUID id, @PathVariable UUID acesso, @AuthenticationPrincipal Jwt token) {
        acessos.revogar(Autenticado.id(token), id, acesso);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: historico
     * Objetivo: Rotina auxiliar historico.
     * Decisões: Ver a documentação da classe.
     */
    @GetMapping("/api/v1/startups/{id}/nivel2/historico")
    @SomenteEmpreendedor
    @Operation(summary = "Histórico de acessos ao Nível 2")
    List<AcessosNivel2Service.Evento> historico(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return acessos.historico(Autenticado.id(token), id);
    }

    @PostMapping("/api/v1/conexoes/{id}/nda")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirma o NDA bilateral pela parte que chama")
    void confirmarNda(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                      @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        acessos.confirmarNda(Autenticado.id(token), organizacao, id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: termoDaOrganizacao
     * Objetivo: Rotina auxiliar termoDaOrganizacao.
     * Decisões: Ver a documentação da classe.
     */
    @PostMapping("/api/v1/organizacoes/{id}/termo-de-confidencialidade")
    @SomenteInvestidor
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Aceita o termo de confidencialidade em nome da organização")
    void termoDaOrganizacao(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        acessos.aceitarTermoDaOrganizacao(Autenticado.id(token), id);
    }

    @GetMapping("/api/v1/catalogo/startups/{id}/nivel2/situacao")
    @SomenteInvestidor
    @Operation(summary = "Situação do investidor quanto ao Nível 2 da startup")
    AcessosNivel2Service.ParaInvestidor situacao(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                                                 @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return acessos.situacao(Autenticado.id(token), organizacao, id);
    }

    @GetMapping("/api/v1/catalogo/startups/{id}/nivel2")
    @SomenteInvestidor
    @Operation(summary = "Informações confidenciais e documentos (investidor autorizado)")
    AcessosNivel2Service.Conteudo conteudo(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                                           @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return acessos.conteudo(Autenticado.id(token), organizacao, id);
    }

    @PostMapping("/api/v1/catalogo/startups/{id}/nivel2/documentos/{documento}/link")
    @SomenteInvestidor
    @Operation(summary = "Gera um link temporário para baixar o documento")
    AcessosNivel2Service.Link link(@PathVariable UUID id, @PathVariable UUID documento, @AuthenticationPrincipal Jwt token,
                                   @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return acessos.gerarLink(Autenticado.id(token), organizacao, id, documento);
    }

    @GetMapping("/api/v1/dataroom/links/{link}")
    @SomenteInvestidor
    @Operation(summary = "Baixa o documento pelo link temporário")
    ResponseEntity<InputStreamResource> baixar(@PathVariable String link, @AuthenticationPrincipal Jwt token,
                                               @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return arquivo(acessos.baixar(Autenticado.id(token), organizacao, link));
    }

    @GetMapping("/api/v1/catalogo/startups/{id}/nda")
    @SomenteInvestidor
    @Operation(summary = "Baixa o NDA bilateral da startup")
    ResponseEntity<InputStreamResource> nda(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                                            @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return arquivo(acessos.baixarNda(Autenticado.id(token), organizacao, id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: arquivo
     * Objetivo: Rotina auxiliar arquivo.
     * Decisões: Ver a documentação da classe.
     */
    private static ResponseEntity<InputStreamResource> arquivo(DataRoomService.Arquivo a) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.tipo())).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(a.nome()).build().toString())
                .body(new InputStreamResource(a.conteudo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: bytes
     * Objetivo: Rotina auxiliar bytes.
     * Decisões: Ver a documentação da classe.
     */
    private static byte[] bytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new RegraDeNegocioException("arquivo_ilegivel", "Não foi possível ler o arquivo. Tente de novo.");
        }
    }

    record RegraRequest(@NotNull(message = "Escolha a regra.") RegraDeConfidencialidade regra) {
    }

    record RegraResponse(RegraDeConfidencialidade regra) {
    }

    record LiberacaoRequest(@NotNull(message = "Escolha a conexão.") UUID conexaoId, Integer dias) {
    }

    record PrazoRequest(Integer dias) {
    }

    record DocumentoResponse(UUID id, String categoria, String categoriaRotulo, String nome, String descricao, String tipo, long tamanho,
                             int versao, boolean atual, String enviadoPor, Instant enviadoEm) {
    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: de
     * Objetivo: Rotina auxiliar de.
     * Decisões: Ver a documentação da classe.
     */
        static DocumentoResponse de(DataRoomService.Documento d) {
            return new DocumentoResponse(d.id(), d.categoria(), d.categoriaRotulo(), d.nome(), d.descricao(), d.tipo(), d.tamanho(), d.versao(),
                    d.atual(), d.enviadoPor(), d.enviadoEm());
        }
    }
}
