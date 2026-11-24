package br.com.orbithub.administracao.api;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.administracao.service.SegundaAprovacaoService;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEquipe;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
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
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Fila de ações com segunda aprovação
 * Objetivo: Mostrar à equipe as propostas de ações graves e deixar a segunda pessoa aprovar ou rejeitar, e quem
 *           propôs cancelar.
 * Decisões: A equipe toda vê a fila; quem pode decidir cada proposta depende do papel exigido pela ação e é
 *           conferido no servidor. As propostas nascem nos módulos donos de cada ação (por exemplo, parâmetros
 *           sensíveis, KAN-78), não aqui.
 */
@RestController
@RequestMapping("/api/v1/admin/aprovacoes")
@SomenteEquipe
@Tag(name = "Segunda aprovação", description = "Ações graves que exigem duas pessoas")
class AprovacaoController {

    private final SegundaAprovacaoService aprovacoes;
    private final ConsultaDeContas contas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Criação do controller
     * Objetivo: Receber o serviço de aprovações e a consulta de nomes.
     * Decisões: Injeção por construtor.
     */
    AprovacaoController(SegundaAprovacaoService aprovacoes, ConsultaDeContas contas) {
        this.aprovacoes = aprovacoes;
        this.contas = contas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Fila de propostas
     * Objetivo: Listar as pendentes e as últimas decididas, dizendo o que a pessoa pode fazer em cada uma.
     * Decisões: Os nomes vêm numa só consulta.
     */
    @GetMapping
    @Operation(summary = "Propostas pendentes e decididas recentemente")
    FilaResponse listar(@AuthenticationPrincipal Jwt token) {
        UUID eu = Autenticado.id(token);
        Set<Papel> papeis = Autenticado.papeis(token);
        List<PropostaDeAprovacao> lista = aprovacoes.listar();
        Map<UUID, String> nomes = contas.nomes(lista.stream()
                .flatMap(p -> Stream.of(p.getPropostoPor(), p.getDecididoPor())).toList());
        List<PropostaResponse> itens = lista.stream().map(p -> PropostaResponse.de(p, nomes,
                aprovacoes.vencida(p), aprovacoes.podeDecidir(p, eu, papeis), p.getPropostoPor().equals(eu))).toList();
        List<AcaoResponse> acoes = Arrays.stream(AcaoGrave.values())
                .map(a -> new AcaoResponse(a, a.getDescricao(), aprovacoes.acoesDisponiveis().contains(a))).toList();
        return new FilaResponse(itens, acoes);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Aprovar
     * Objetivo: Aprovar a proposta e executar a ação.
     * Decisões: Justificativa obrigatória.
     */
    @PostMapping("/{id}/aprovacao")
    @Operation(summary = "Aprova uma proposta (a ação é executada)")
    PropostaResponse aprovar(@PathVariable UUID id, @Valid @RequestBody DecisaoRequest pedido,
                             @AuthenticationPrincipal Jwt token) {
        return resposta(aprovacoes.aprovar(id, Autenticado.id(token), Autenticado.papeis(token), pedido.justificativa()),
                token);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Rejeitar
     * Objetivo: Rejeitar a proposta; nada é executado.
     * Decisões: Justificativa obrigatória.
     */
    @PostMapping("/{id}/rejeicao")
    @Operation(summary = "Rejeita uma proposta")
    PropostaResponse rejeitar(@PathVariable UUID id, @Valid @RequestBody DecisaoRequest pedido,
                              @AuthenticationPrincipal Jwt token) {
        return resposta(aprovacoes.rejeitar(id, Autenticado.id(token), Autenticado.papeis(token),
                pedido.justificativa()), token);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Cancelar
     * Objetivo: Deixar quem propôs desistir da proposta.
     * Decisões: Só quem propôs.
     */
    @PostMapping("/{id}/cancelamento")
    @Operation(summary = "Cancela a própria proposta pendente")
    PropostaResponse cancelar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return resposta(aprovacoes.cancelar(id, Autenticado.id(token)), token);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Resposta de uma proposta
     * Objetivo: Montar a resposta de uma proposta depois de uma decisão.
     * Decisões: Mesmo formato da fila.
     */
    private PropostaResponse resposta(PropostaDeAprovacao p, Jwt token) {
        Map<UUID, String> nomes = contas.nomes(Stream.of(p.getPropostoPor(), p.getDecididoPor()).toList());
        UUID eu = Autenticado.id(token);
        return PropostaResponse.de(p, nomes, aprovacoes.vencida(p), aprovacoes.podeDecidir(p, eu,
                Autenticado.papeis(token)), p.getPropostoPor().equals(eu));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Pedido de decisão
     * Objetivo: Receber a justificativa da decisão.
     * Decisões: Obrigatória.
     */
    record DecisaoRequest(
            @NotBlank(message = "Explique a sua decisão.") @Size(max = 1000, message = "Use até 1000 caracteres.")
            String justificativa) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Fila na resposta
     * Objetivo: Levar as propostas e a lista de ações graves (com as já disponíveis).
     * Decisões: As ações sem executor aparecem como "em breve" na tela.
     */
    record FilaResponse(List<PropostaResponse> propostas, List<AcaoResponse> acoes) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Ação grave na resposta
     * Objetivo: Descrever uma ação grave e se ela já está disponível.
     * Decisões: Disponível = tem executor registrado.
     */
    record AcaoResponse(AcaoGrave acao, String descricao, boolean disponivel) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Proposta na resposta
     * Objetivo: Expor uma proposta com nomes, prazo e o que a pessoa pode fazer.
     * Decisões: "possoDecidir" e "minha" orientam os botões; o servidor confere de novo na decisão.
     */
    record PropostaResponse(UUID id, AcaoGrave tipo, String tipoDescricao, String objetoTipo, String objetoId,
                            String resumo, Map<String, String> dados, String justificativa, String propostoPor,
                            Instant propostoEm, String situacao, boolean vencida, String decididoPor, Instant decididoEm,
                            String justificativaDecisao, boolean possoDecidir, boolean minha) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:52
         * Cartão Jira: KAN-81
         * Título: Conversão da proposta
         * Objetivo: Montar a resposta a partir da entidade e dos nomes.
         * Decisões: Nome ausente vira "conta removida".
         */
        static PropostaResponse de(PropostaDeAprovacao p, Map<UUID, String> nomes, boolean vencida, boolean possoDecidir,
                                   boolean minha) {
            return new PropostaResponse(p.getId(), p.getTipo(), p.getTipo().getDescricao(), p.getObjetoTipo(),
                    p.getObjetoId(), p.getResumo(), p.getDados(), p.getJustificativa(),
                    nomes.getOrDefault(p.getPropostoPor(), "conta removida"), p.getPropostoEm(), p.getSituacao().name(),
                    vencida, p.getDecididoPor() == null ? null : nomes.getOrDefault(p.getDecididoPor(), "conta removida"),
                    p.getDecididoEm(), p.getJustificativaDecisao(), possoDecidir, minha);
        }
    }
}
