package br.com.orbithub.administracao.api;

import br.com.orbithub.administracao.domain.VinculoDeMembro;
import br.com.orbithub.administracao.service.ConflitosDeInteresse;
import br.com.orbithub.compartilhado.documentos.Cnpj;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteAdmin;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEquipe;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Vínculos da equipe
 * Objetivo: Deixar cada membro da equipe declarar e remover os próprios vínculos, e o administrador ver os de todos na
 *           revisão periódica de acessos.
 * Decisões: Em /api/v1/admin, com trilha de auditoria. O administrador vê os vínculos de todos com o nome do membro.
 */
@RestController
@RequestMapping("/api/v1/admin/vinculos")
@Tag(name = "Equipe", description = "Equipe interna")
class VinculoController {

    private final ConflitosDeInteresse conflitos;
    private final ConsultaDeContas contas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Criação do controller
     * Objetivo: Receber o serviço de conflito de interesse e as contas.
     * Decisões: Injeção por construtor.
     */
    VinculoController(ConflitosDeInteresse conflitos, ConsultaDeContas contas) {
        this.conflitos = conflitos;
        this.contas = contas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Meus vínculos
     * Objetivo: Listar os vínculos de quem está logado.
     * Decisões: Qualquer papel da equipe.
     */
    @GetMapping("/meus")
    @SomenteEquipe
    @Operation(summary = "Vínculos declarados pela conta logada")
    List<VinculoResponse> meus(@AuthenticationPrincipal Jwt token) {
        return conflitos.doMembro(Autenticado.id(token)).stream().map(v -> VinculoResponse.de(v, null)).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Declarar vínculo
     * Objetivo: Registrar um vínculo de quem está logado.
     * Decisões: 201.
     */
    @PostMapping
    @SomenteEquipe
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Declara um vínculo")
    VinculoResponse declarar(@Valid @RequestBody VinculoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return VinculoResponse.de(conflitos.declarar(Autenticado.id(token), pedido.tipo(), pedido.alvo(), pedido.descricao()), null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Remover vínculo
     * Objetivo: Tirar um vínculo próprio que deixou de existir.
     * Decisões: 204; vínculo de outra pessoa: 404.
     */
    @DeleteMapping("/{id}")
    @SomenteEquipe
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um vínculo próprio")
    void remover(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        conflitos.remover(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Vínculos da equipe
     * Objetivo: Listar os vínculos de todos os membros, para a revisão de acessos.
     * Decisões: Só administrador.
     */
    @GetMapping
    @SomenteAdmin
    @Operation(summary = "Vínculos de toda a equipe (administrador)")
    List<VinculoResponse> daEquipe() {
        List<VinculoDeMembro> todos = conflitos.daEquipe();
        Map<UUID, String> nomes = contas.nomes(todos.stream().map(VinculoDeMembro::getMembroId).distinct().toList());
        return todos.stream().map(v -> VinculoResponse.de(v, nomes.getOrDefault(v.getMembroId(), "Conta removida"))).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Pedido de vínculo
     * Objetivo: Receber o tipo, o alvo (CNPJ ou e-mail) e a descrição.
     * Decisões: Validação de conteúdo no serviço.
     */
    record VinculoRequest(@NotNull(message = "Escolha o tipo.") VinculoDeMembro.Tipo tipo, String alvo, String descricao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Vínculo na resposta
     * Objetivo: Expor um vínculo com o alvo formatado e, para o administrador, o nome do membro.
     * Decisões: CNPJ formatado.
     */
    record VinculoResponse(UUID id, UUID membroId, String membro, VinculoDeMembro.Tipo tipo, String alvo, String descricao,
                           Instant criadoEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 17:43
         * Cartão Jira: KAN-87
         * Título: Conversão
         * Objetivo: Montar a resposta a partir do vínculo.
         * Decisões: Alvo = CNPJ formatado ou e-mail.
         */
        static VinculoResponse de(VinculoDeMembro v, String membro) {
            return new VinculoResponse(v.getId(), v.getMembroId(), membro, v.getTipo(),
                    v.getCnpj() != null ? Cnpj.formatar(v.getCnpj()) : v.getEmail(), v.getDescricao(), v.getCriadoEm());
        }
    }
}
