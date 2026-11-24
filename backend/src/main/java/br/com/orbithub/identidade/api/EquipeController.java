package br.com.orbithub.identidade.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteAdmin;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.RevisaoDeAcesso;
import br.com.orbithub.identidade.domain.StatusUsuario;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.service.EquipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Gestão da equipe interna
 * Objetivo: Rotas do administrador para listar, convidar, trocar papéis, desligar e redefinir o segundo fator da
 *           equipe, e para a revisão periódica de acessos.
 * Decisões: Somente administrador. Tudo em /api/v1/admin, então cada ação entra na trilha de auditoria (KAN-80).
 *           Não há rota para definir senha de ninguém.
 */
@RestController
@RequestMapping("/api/v1/admin/equipe")
@SomenteAdmin
@Tag(name = "Equipe interna", description = "Membros da equipe, papéis, desligamento e revisão de acessos")
class EquipeController {

    private final EquipeService equipe;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Criação do controller
     * Objetivo: Receber o serviço da equipe.
     * Decisões: Injeção por construtor.
     */
    EquipeController(EquipeService equipe) {
        this.equipe = equipe;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Listar a equipe
     * Objetivo: Devolver todos os membros, com papéis, situação e segundo fator.
     * Decisões: Inclui convidados e desligados.
     */
    @GetMapping
    @Operation(summary = "Lista os membros da equipe interna")
    List<MembroResponse> listar() {
        return equipe.listar().stream().map(MembroResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Convidar membro
     * Objetivo: Criar a conta interna e enviar o convite.
     * Decisões: O membro escolhe a própria senha pelo link do e-mail.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Convida um membro para a equipe")
    MembroResponse convidar(@Valid @RequestBody ConviteRequest pedido) {
        return MembroResponse.de(equipe.convidar(pedido.nome(), pedido.email(), pedido.papeis()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Reenviar convite
     * Objetivo: Mandar o convite de novo a quem ainda não aceitou.
     * Decisões: 204 sem corpo.
     */
    @PostMapping("/{id}/convite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reenvia o convite de um membro que ainda não aceitou")
    void reenviarConvite(@PathVariable UUID id) {
        equipe.reenviarConvite(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Trocar papéis
     * Objetivo: Definir os papéis de um membro.
     * Decisões: As sessões do membro caem, para os papéis novos valerem na hora.
     */
    @PutMapping("/{id}/papeis")
    @Operation(summary = "Define os papéis de um membro")
    MembroResponse alterarPapeis(@PathVariable UUID id, @Valid @RequestBody PapeisRequest pedido) {
        return MembroResponse.de(equipe.alterarPapeis(id, pedido.papeis()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Desligar membro
     * Objetivo: Revogar o acesso de quem saiu da equipe.
     * Decisões: Devolve quantos casos voltaram para a fila.
     */
    @PostMapping("/{id}/desligamento")
    @Operation(summary = "Desliga um membro: revoga o acesso e devolve os casos à fila")
    DesligamentoResponse desligar(@PathVariable UUID id, @Valid @RequestBody DesligamentoRequest pedido,
                                  @AuthenticationPrincipal Jwt token) {
        return new DesligamentoResponse(equipe.desligar(Autenticado.id(token), id, pedido.justificativa()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Redefinir segundo fator
     * Objetivo: Apagar o segundo fator de um membro que perdeu o aplicativo.
     * Decisões: 204 sem corpo; o membro configura de novo no próximo login.
     */
    @PostMapping("/{id}/segundo-fator/redefinicao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Redefine o segundo fator de um membro")
    void redefinirSegundoFator(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        equipe.redefinirSegundoFator(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Situação da revisão de acessos
     * Objetivo: Informar a última revisão, a próxima e se está vencida.
     * Decisões: Intervalo de acesso.revisao_meses.
     */
    @GetMapping("/revisao")
    @Operation(summary = "Situação da revisão periódica de acessos")
    EquipeService.SituacaoRevisao situacaoRevisao() {
        return equipe.situacaoRevisao();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Confirmar revisão de acessos
     * Objetivo: Registrar que os acessos da equipe foram conferidos.
     * Decisões: Devolve a fotografia dos membros confirmados.
     */
    @PostMapping("/revisao")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Confirma a revisão de acessos da equipe")
    RevisaoResponse confirmarRevisao(@Valid @RequestBody RevisaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return RevisaoResponse.de(equipe.confirmarRevisao(Autenticado.id(token), pedido.observacao()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Pedido de convite
     * Objetivo: Receber nome, e-mail e papéis do novo membro.
     * Decisões: Formato aqui; regras (e-mail livre, papéis internos) no serviço.
     */
    record ConviteRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 120, message = "Use até 120 caracteres.") String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 254) String email,
            @NotEmpty(message = "Escolha pelo menos um papel.") Set<Papel> papeis) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Pedido de papéis
     * Objetivo: Receber o novo conjunto de papéis.
     * Decisões: Conjunto inteiro (substitui o anterior).
     */
    record PapeisRequest(@NotEmpty(message = "Escolha pelo menos um papel.") Set<Papel> papeis) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Pedido de desligamento
     * Objetivo: Receber o motivo do desligamento.
     * Decisões: Obrigatório; vai para a trilha como justificativa.
     */
    record DesligamentoRequest(
            @NotBlank(message = "Informe o motivo do desligamento.") @Size(max = 1000) String justificativa) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Pedido de revisão
     * Objetivo: Receber a observação da revisão.
     * Decisões: Opcional.
     */
    record RevisaoRequest(@Size(max = 1000, message = "Use até 1000 caracteres.") String observacao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Resultado do desligamento
     * Objetivo: Informar quantos casos voltaram para a fila.
     * Decisões: Zero enquanto não existem filas de casos.
     */
    record DesligamentoResponse(int casosDevolvidos) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Membro na resposta
     * Objetivo: Expor um membro da equipe para a tela.
     * Decisões: Situação em linguagem de negócio (CONVIDADO, ATIVO, DESLIGADO); nunca expõe o segredo do
     *           segundo fator, só se ele está configurado.
     */
    record MembroResponse(UUID id, String nome, String email, Set<Papel> papeis, String situacao,
                          boolean segundoFatorAtivo, Instant criadoEm, Instant desligadoEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:39
         * Cartão Jira: KAN-75
         * Título: Conversão do membro
         * Objetivo: Montar a resposta a partir da conta.
         * Decisões: Desligado tem precedência sobre a situação da conta.
         */
        static MembroResponse de(Usuario u) {
            String situacao = u.getDesligadoEm() != null ? "DESLIGADO"
                    : u.getStatus() == StatusUsuario.PENDENTE_CONFIRMACAO ? "CONVIDADO" : "ATIVO";
            return new MembroResponse(u.getId(), u.getNome(), u.getEmail(), new TreeSet<>(u.getPapeis()), situacao,
                    u.temSegundoFator(), u.getCriadoEm(), u.getDesligadoEm());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Revisão na resposta
     * Objetivo: Expor a revisão registrada.
     * Decisões: Inclui a fotografia dos membros.
     */
    record RevisaoResponse(UUID id, Instant realizadaEm, String observacao, List<RevisaoDeAcesso.Item> membros) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:39
         * Cartão Jira: KAN-75
         * Título: Conversão da revisão
         * Objetivo: Montar a resposta a partir da entidade.
         * Decisões: Copia os campos.
         */
        static RevisaoResponse de(RevisaoDeAcesso r) {
            return new RevisaoResponse(r.getId(), r.getRealizadaEm(), r.getObservacao(), r.getItens());
        }
    }
}
