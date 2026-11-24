package br.com.orbithub.verificacao.api;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteInvestidor;
import br.com.orbithub.investidores.service.AtuacaoService;
import br.com.orbithub.verificacao.domain.DocumentoDeVerificacao;
import br.com.orbithub.verificacao.domain.PedidoDeVerificacao;
import br.com.orbithub.verificacao.domain.Selo;
import br.com.orbithub.verificacao.domain.SituacaoDoPedido;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import br.com.orbithub.verificacao.service.VerificacoesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Verificação de quem atua como investidor
 * Objetivo: Ver o selo e o pedido, enviar e remover documentos, baixar um documento próprio e mandar para análise.
 * Decisões: Investidor, por si ou pela organização da atuação (cabeçalho X-Orbit-Organizacao). Os documentos sobem um
 *           por vez (multipart), para não depender de uma requisição grande. A chave do arquivo nunca sai na resposta.
 */
@RestController
@RequestMapping("/api/v1/verificacoes")
@SomenteInvestidor
@Tag(name = "Verificação", description = "Verificação de identidade e selo")
class VerificacaoController {

    private final VerificacoesService verificacoes;
    private final AtuacaoService atuacao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Criação do controller
     * Objetivo: Receber os serviços de verificação e de atuação.
     * Decisões: Injeção por construtor.
     */
    VerificacaoController(VerificacoesService verificacoes, AtuacaoService atuacao) {
        this.verificacoes = verificacoes;
        this.atuacao = atuacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Minha verificação
     * Objetivo: Devolver o selo, o pedido mais recente, os documentos exigidos e os enviados.
     * Decisões: Qualquer membro da organização vê.
     */
    @GetMapping("/minha")
    @Operation(summary = "Selo e pedido de verificação de quem atua")
    SituacaoResponse minha(@RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao,
                           @AuthenticationPrincipal Jwt token) {
        return SituacaoResponse.de(verificacoes.situacao(atuacao.paraVer(Autenticado.id(token), organizacao)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Enviar documento
     * Objetivo: Receber um documento de um tipo exigido.
     * Decisões: 201 com os metadados; formato e tamanho conferidos no serviço.
     */
    @PostMapping(path = "/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia um documento para a verificação")
    DocumentoResponse enviarDocumento(@RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao,
                                      @RequestParam String tipo, @RequestParam MultipartFile arquivo,
                                      @AuthenticationPrincipal Jwt token) {
        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new RegraDeNegocioException("documento_ilegivel", "Não foi possível ler o arquivo. Tente de novo.");
        }
        return DocumentoResponse.de(verificacoes.enviarDocumento(atuacao.paraAgir(Autenticado.id(token), organizacao), tipo,
                arquivo.getOriginalFilename(), conteudo));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Remover documento
     * Objetivo: Tirar um documento do pedido em rascunho ou em complemento.
     * Decisões: 204.
     */
    @DeleteMapping("/documentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um documento do pedido")
    void remover(@RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao,
                 @PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        verificacoes.removerDocumento(atuacao.paraAgir(Autenticado.id(token), organizacao), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Baixar documento próprio
     * Objetivo: Devolver o arquivo de um documento do próprio sujeito.
     * Decisões: Sem cache compartilhado e sem adivinhação de tipo pelo navegador.
     */
    @GetMapping("/documentos/{id}/arquivo")
    @Operation(summary = "Baixa um documento enviado")
    ResponseEntity<InputStreamResource> arquivo(@RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao,
                                                @PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        VerificacoesService.Arquivo a = verificacoes.lerDocumento(atuacao.paraVer(Autenticado.id(token), organizacao), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.tipo()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(a.nome()).build().toString())
                .body(new InputStreamResource(a.conteudo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Mandar para análise
     * Objetivo: Enviar o pedido para a fila da verificação.
     * Decisões: Falta de documento: 422 com a lista.
     */
    @PostMapping("/envio")
    @Operation(summary = "Envia o pedido de verificação para análise")
    PedidoResponse enviar(@RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao,
                          @AuthenticationPrincipal Jwt token) {
        return PedidoResponse.de(verificacoes.enviarParaAnalise(atuacao.paraAgir(Autenticado.id(token), organizacao)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Pedido na resposta
     * Objetivo: Expor a situação e o resultado do pedido.
     * Decisões: Sem dados de quem analisou. Com o prazo do complemento (KAN-83).
     */
    record PedidoResponse(UUID id, SituacaoDoPedido situacao, Instant enviadoEm, String motivo, String observacao,
                          Instant validoAte, Instant prazoComplementoAte) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 17:33
         * Cartão Jira: KAN-83
         * Título: Conversão
         * Objetivo: Montar a resposta a partir do pedido.
         * Decisões: Copia os campos públicos.
         */
        static PedidoResponse de(PedidoDeVerificacao p) {
            return new PedidoResponse(p.getId(), p.getSituacao(), p.getEnviadoEm(), p.getMotivo(), p.getObservacao(),
                    p.getValidoAte(), p.getPrazoComplementoAte());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Documento na resposta
     * Objetivo: Expor os metadados de um documento enviado.
     * Decisões: Sem a chave do armazenamento.
     */
    record DocumentoResponse(UUID id, String tipo, String nome, String formato, long tamanho, Instant enviadoEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 17:19
         * Cartão Jira: KAN-62
         * Título: Conversão
         * Objetivo: Montar a resposta a partir do documento.
         * Decisões: Copia os metadados.
         */
        static DocumentoResponse de(DocumentoDeVerificacao d) {
            return new DocumentoResponse(d.getId(), d.getTipo(), d.getNomeOriginal(), d.getTipoConteudo(), d.getTamanho(),
                    d.getEnviadoEm());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Situação na resposta
     * Objetivo: Levar o selo, se pode enviar, os exigidos, o pedido e os documentos.
     * Decisões: Pedido nulo quando não houver.
     */
    record SituacaoResponse(SujeitoDaVerificacao sujeito, Selo selo, boolean podeEnviar,
                            List<VerificacoesService.Exigido> exigidos, PedidoResponse pedido,
                            List<DocumentoResponse> documentos) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 17:19
         * Cartão Jira: KAN-62
         * Título: Conversão
         * Objetivo: Montar a resposta a partir da situação do serviço.
         * Decisões: Optional vira nulo.
         */
        static SituacaoResponse de(VerificacoesService.Situacao s) {
            return new SituacaoResponse(s.sujeito(), s.selo(), s.podeEnviar(), s.exigidos(),
                    s.pedido().map(PedidoResponse::de).orElse(null), s.documentos().stream().map(DocumentoResponse::de).toList());
        }
    }
}
