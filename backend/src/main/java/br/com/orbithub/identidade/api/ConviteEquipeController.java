package br.com.orbithub.identidade.api;

import br.com.orbithub.identidade.service.EquipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:20
 * Cartão Jira: KAN-75
 * Título: Aceite do convite da equipe
 * Objetivo: Deixar o membro convidado escolher a própria senha a partir do link do e-mail.
 * Decisões: Rota pública (o membro ainda não tem sessão), protegida pelo token de uso único do convite. É a única
 *           forma de uma conta interna ganhar senha.
 */
@RestController
@RequestMapping("/api/v1/equipe/convite")
@Tag(name = "Equipe interna", description = "Membros da equipe, papéis, desligamento e revisão de acessos")
class ConviteEquipeController {

    private final EquipeService equipe;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:20
     * Cartão Jira: KAN-75
     * Título: Criação do controller
     * Objetivo: Receber o serviço da equipe.
     * Decisões: Injeção por construtor.
     */
    ConviteEquipeController(EquipeService equipe) {
        this.equipe = equipe;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:20
     * Cartão Jira: KAN-75
     * Título: Aceitar convite
     * Objetivo: Ativar a conta interna com a senha escolhida.
     * Decisões: 204 sem corpo; em seguida o membro entra e configura o segundo fator.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Aceita o convite da equipe e define a senha")
    void aceitar(@Valid @RequestBody AceiteRequest pedido) {
        equipe.aceitarConvite(pedido.token(), pedido.senha());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:20
     * Cartão Jira: KAN-75
     * Título: Pedido de aceite
     * Objetivo: Receber o token do convite e a senha escolhida.
     * Decisões: O toString não mostra nenhum dos dois.
     */
    record AceiteRequest(@NotBlank(message = "Link inválido.") String token,
                         @NotBlank(message = "Escolha uma senha.") String senha) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 11:20
         * Cartão Jira: KAN-75
         * Título: Texto sem os dados
         * Objetivo: Evitar que token e senha apareçam em logs.
         * Decisões: Texto fixo.
         */
        @Override
        public String toString() {
            return "AceiteRequest[]";
        }
    }
}
