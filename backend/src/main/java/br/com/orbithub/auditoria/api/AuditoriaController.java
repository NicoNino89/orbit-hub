package br.com.orbithub.auditoria.api;

import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.auditoria.service.AcessoJustificadoService;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.auditoria.service.TrilhaDeAuditoria;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.AdminOuEncarregado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEquipe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Consulta da trilha de auditoria e acesso justificado
 * Objetivo: Permitir ao administrador consultar a trilha com filtros e à equipe registrar o acesso
 *           justificado a dados confidenciais dentro de um caso.
 * Decisões: Só leitura da trilha: não existe rota de edição nem de exclusão. A consulta da própria trilha
 *           também fica registrada (com os filtros usados). Consulta: administrador e encarregado de dados;
 *           registro de acesso justificado: qualquer papel interno que atenda casos (KAN-75).
 */
@RestController
@RequestMapping("/api/v1/admin/auditoria")
@AdminOuEncarregado
@Tag(name = "Auditoria", description = "Trilha imutável das ações da equipe interna")
class AuditoriaController {

    private final TrilhaDeAuditoria trilha;
    private final AcessoJustificadoService acessos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Criação do controller
     * Objetivo: Receber a trilha e o serviço de acesso justificado.
     * Decisões: Injeção por construtor.
     */
    AuditoriaController(TrilhaDeAuditoria trilha, AcessoJustificadoService acessos) {
        this.trilha = trilha;
        this.acessos = acessos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Consultar a trilha
     * Objetivo: Listar registros por pessoa, período, ação e objeto, paginados.
     * Decisões: Datas em ISO-8601 (instantes); a página tem no máximo 100 itens.
     */
    @GetMapping
    @Operation(summary = "Consulta a trilha de auditoria com filtros (administrador ou encarregado de dados)")
    PaginaResponse consultar(@RequestParam(required = false) UUID ator,
                             @RequestParam(required = false) String acao,
                             @RequestParam(required = false) String objetoTipo,
                             @RequestParam(required = false) String objetoId,
                             @RequestParam(required = false) Instant de,
                             @RequestParam(required = false) Instant ate,
                             @RequestParam(defaultValue = "0") int pagina,
                             @RequestParam(defaultValue = "50") int tamanho) {
        ContextoDeAuditoria.descrever("AUDITORIA_CONSULTADA", null, null, resumo(ator, acao, objetoTipo, de, ate));
        Page<RegistroAuditoria> resultado = trilha.consultar(
                new TrilhaDeAuditoria.Filtro(ator, acao, objetoTipo, objetoId, de, ate), pagina, tamanho);
        return new PaginaResponse(resultado.getContent().stream().map(RegistroResponse::de).toList(),
                resultado.getNumber(), resultado.getSize(), resultado.getTotalElements());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Tipos de ação
     * Objetivo: Listar os códigos de ação para o filtro da tela.
     * Decisões: Só os que já aparecem na trilha.
     */
    @GetMapping("/acoes")
    @Operation(summary = "Códigos de ação presentes na trilha (administrador ou encarregado de dados)")
    List<String> acoes() {
        ContextoDeAuditoria.descrever("AUDITORIA_ACOES_LISTADAS", null, null, null);
        return trilha.acoes();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Pessoas da trilha
     * Objetivo: Listar quem aparece na trilha, para o filtro por pessoa da tela.
     * Decisões: Inclui pessoas já desligadas, porque a trilha continua valendo para elas.
     */
    @GetMapping("/atores")
    @Operation(summary = "Pessoas que aparecem na trilha (administrador ou encarregado de dados)")
    List<RegistroAuditoriaRepository.Ator> atores() {
        ContextoDeAuditoria.descrever("AUDITORIA_ATORES_LISTADOS", null, null, null);
        return trilha.atores();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Registrar acesso justificado
     * Objetivo: Liberar e registrar o acesso de um usuário interno aos dados de Nível 2 de uma startup.
     * Decisões: Exige caso aberto sobre a startup e justificativa; a resposta só confirma o registro. As
     *           telas que mostram os dados (Data Room, KAN-30) chamam o mesmo serviço antes de devolvê-los.
     */
    @PostMapping("/acessos-nivel2")
    @SomenteEquipe
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra o acesso justificado a dados confidenciais dentro de um caso")
    RegistroResponse registrarAcesso(@Valid @RequestBody AcessoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return RegistroResponse.de(acessos.exigir(Autenticado.id(token), pedido.casoTipo(), pedido.casoId(),
                pedido.startupId(), pedido.justificativa()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Resumo dos filtros
     * Objetivo: Descrever, no registro da consulta, quais filtros foram usados.
     * Decisões: Só os filtros preenchidos.
     */
    private static String resumo(UUID ator, String acao, String objetoTipo, Instant de, Instant ate) {
        StringBuilder texto = new StringBuilder("Filtros:");
        if (ator != null) {
            texto.append(" ator=").append(ator);
        }
        if (acao != null) {
            texto.append(" acao=").append(acao);
        }
        if (objetoTipo != null) {
            texto.append(" objeto=").append(objetoTipo);
        }
        if (de != null) {
            texto.append(" de=").append(de);
        }
        if (ate != null) {
            texto.append(" ate=").append(ate);
        }
        return texto.toString();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Pedido de acesso justificado
     * Objetivo: Receber o caso, a startup e a justificativa.
     * Decisões: Validação de formato aqui; a regra (caso aberto, tamanho da justificativa) fica no serviço.
     */
    record AcessoRequest(
            @NotBlank(message = "Informe o tipo do caso.") String casoTipo,
            @NotNull(message = "Informe o caso.") UUID casoId,
            @NotNull(message = "Informe a startup.") UUID startupId,
            @NotBlank(message = "Informe a justificativa.") @Size(max = 1000, message = "Use até 1000 caracteres.")
            String justificativa) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Registro na resposta
     * Objetivo: Expor um registro da trilha para a tela.
     * Decisões: Todos os campos do registro; a tela é só do administrador.
     */
    record RegistroResponse(UUID id, UUID atorId, String atorNome, String acao, String objetoTipo, String objetoId,
                            String resultado, Integer statusHttp, String detalhe, String justificativa,
                            Instant ocorridoEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:09
         * Cartão Jira: KAN-80
         * Título: Conversão do registro
         * Objetivo: Montar a resposta a partir da entidade.
         * Decisões: Resultado como texto.
         */
        static RegistroResponse de(RegistroAuditoria r) {
            return new RegistroResponse(r.getId(), r.getAtorId(), r.getAtorNome(), r.getAcao(), r.getObjetoTipo(),
                    r.getObjetoId(), r.getResultado().name(), r.getStatusHttp(), r.getDetalhe(), r.getJustificativa(),
                    r.getOcorridoEm());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Página da trilha
     * Objetivo: Devolver os registros com os dados de paginação.
     * Decisões: Formato próprio e estável, sem expor o Page do Spring.
     */
    record PaginaResponse(List<RegistroResponse> itens, int pagina, int tamanho, long total) {
    }
}
