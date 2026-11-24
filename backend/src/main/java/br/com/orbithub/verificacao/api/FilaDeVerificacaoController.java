package br.com.orbithub.verificacao.api;

import br.com.orbithub.administracao.domain.ItemDeLista;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.VerificacaoOuAdmin;
import br.com.orbithub.verificacao.domain.DocumentoDeVerificacao;
import br.com.orbithub.verificacao.domain.PedidoDeVerificacao;
import br.com.orbithub.verificacao.domain.SituacaoDoPedido;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import br.com.orbithub.verificacao.service.FilaDeVerificacao;
import br.com.orbithub.verificacao.service.VerificacoesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Fila de verificação no backoffice
 * Objetivo: Listar a fila, abrir um pedido, assumir, ler documentos e decidir.
 * Decisões: Analista de verificação ou administrador. Rotas em /api/v1/admin, então passam pelo filtro de equipe e pela
 *           trilha de auditoria (KAN-80).
 */
@RestController
@RequestMapping("/api/v1/admin/verificacoes")
@VerificacaoOuAdmin
@Tag(name = "Verificação", description = "Verificação de identidade e selo")
class FilaDeVerificacaoController {

    private final FilaDeVerificacao fila;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Criação do controller
     * Objetivo: Receber o serviço da fila.
     * Decisões: Injeção por construtor.
     */
    FilaDeVerificacaoController(FilaDeVerificacao fila) {
        this.fila = fila;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Fila
     * Objetivo: Listar os pedidos em análise por ordem de chegada.
     * Decisões: Com prazo e atraso.
     */
    @GetMapping
    @Operation(summary = "Fila de verificação")
    List<ItemResponse> fila() {
        return fila.fila().stream().map(i -> new ItemResponse(i.pedido().getId(), i.pedido().getSujeitoTipo(), i.sujeito(),
                i.pedido().getEnviadoEm(), i.prazo(), i.atrasado(), i.responsavel())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Detalhe
     * Objetivo: Devolver o pedido, os documentos, o histórico do sujeito e os motivos de decisão.
     * Decisões: Sem as chaves do armazenamento.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um pedido de verificação")
    DetalheResponse detalhe(@PathVariable UUID id) {
        FilaDeVerificacao.Detalhe d = fila.detalhe(id);
        return new DetalheResponse(PedidoResponse.de(d.pedido()), d.pedido().getSujeitoTipo(), d.sujeito(), d.responsavel(),
                d.documentos().stream().map(DocumentoResponse::de).toList(),
                d.historico().stream().map(PedidoResponse::de).toList(),
                d.motivos().stream().map(MotivoResponse::de).toList());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Assumir
     * Objetivo: Tornar quem pede o responsável pelo pedido.
     * Decisões: O administrador pode redistribuir.
     */
    @PostMapping("/{id}/assumir")
    @Operation(summary = "Assume um pedido da fila")
    PedidoResponse assumir(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return PedidoResponse.de(fila.assumir(id, Autenticado.id(token), Autenticado.temPapel(token, "ADMIN")));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Decidir
     * Objetivo: Registrar verificado, pendente de complemento ou recusado, com motivo e observação.
     * Decisões: Regras no serviço.
     */
    @PostMapping("/{id}/decisao")
    @Operation(summary = "Decide um pedido de verificação")
    PedidoResponse decidir(@PathVariable UUID id, @Valid @RequestBody DecisaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return PedidoResponse.de(fila.decidir(id, Autenticado.id(token), Autenticado.temPapel(token, "ADMIN"),
                pedido.resultado(), pedido.motivo(), pedido.observacao()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Ler documento
     * Objetivo: Devolver o arquivo de um documento do pedido.
     * Decisões: Sem cache; a leitura fica na trilha de auditoria.
     */
    @GetMapping("/{id}/documentos/{documentoId}/arquivo")
    @Operation(summary = "Baixa um documento do pedido")
    ResponseEntity<InputStreamResource> documento(@PathVariable UUID id, @PathVariable UUID documentoId) {
        VerificacoesService.Arquivo a = fila.lerDocumento(id, documentoId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.tipo())).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(a.nome()).build().toString())
                .body(new InputStreamResource(a.conteudo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Pedido de decisão
     * Objetivo: Receber o resultado, o motivo e a observação.
     * Decisões: Observação obrigatória no complemento (conferida no serviço).
     */
    record DecisaoRequest(@NotNull(message = "Escolha a decisão.") SituacaoDoPedido resultado,
                          @NotBlank(message = "Escolha o motivo.") String motivo, String observacao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Item da fila na resposta
     * Objetivo: Expor um pedido da fila.
     * Decisões: Responsável pelo nome.
     */
    record ItemResponse(UUID id, SujeitoDaVerificacao sujeitoTipo, String sujeito, Instant enviadoEm, Instant prazo,
                        boolean atrasado, String responsavel) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Pedido na resposta do backoffice
     * Objetivo: Expor a situação e a decisão de um pedido.
     * Decisões: Inclui o encaminhamento à moderação.
     */
    record PedidoResponse(UUID id, SituacaoDoPedido situacao, Instant criadoEm, Instant enviadoEm, Instant decididoEm,
                          String motivo, String observacao, Instant validoAte, Instant prazoComplementoAte,
                          boolean encaminhadoModeracao, boolean documentosApagados) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 17:33
         * Cartão Jira: KAN-83
         * Título: Conversão
         * Objetivo: Montar a resposta a partir do pedido.
         * Decisões: Copia os campos.
         */
        static PedidoResponse de(PedidoDeVerificacao p) {
            return new PedidoResponse(p.getId(), p.getSituacao(), p.getCriadoEm(), p.getEnviadoEm(), p.getDecididoEm(),
                    p.getMotivo(), p.getObservacao(), p.getValidoAte(), p.getPrazoComplementoAte(), p.isEncaminhadoModeracao(),
                    p.getDocumentosApagadosEm() != null);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Documento na resposta do backoffice
     * Objetivo: Expor os metadados de um documento.
     * Decisões: Sem a chave.
     */
    record DocumentoResponse(UUID id, String tipo, String nome, String formato, long tamanho, Instant enviadoEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 17:33
         * Cartão Jira: KAN-83
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
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Motivo na resposta
     * Objetivo: Expor um motivo de decisão com o grupo.
     * Decisões: Grupo = decisão a que pertence.
     */
    record MotivoResponse(String codigo, String rotulo, String grupo) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 17:33
         * Cartão Jira: KAN-83
         * Título: Conversão
         * Objetivo: Montar a resposta a partir do item da lista.
         * Decisões: Copia os campos.
         */
        static MotivoResponse de(ItemDeLista i) {
            return new MotivoResponse(i.getCodigo(), i.getRotulo(), i.getGrupo());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Detalhe na resposta
     * Objetivo: Levar o pedido, o sujeito, os documentos, o histórico e os motivos.
     * Decisões: Histórico sem o pedido atual.
     */
    record DetalheResponse(PedidoResponse pedido, SujeitoDaVerificacao sujeitoTipo, String sujeito, String responsavel,
                           List<DocumentoResponse> documentos, List<PedidoResponse> historico, List<MotivoResponse> motivos) {
    }
}
