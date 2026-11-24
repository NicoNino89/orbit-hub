package br.com.orbithub.identidade.api;

import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.PoliticaDeSenha;
import br.com.orbithub.identidade.service.CadastroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Cadastro e confirmação de conta
 * Objetivo: Expor o cadastro público, a confirmação do e-mail e o reenvio do link.
 * Decisões: Rotas públicas (sem login). As mensagens de validação são as que aparecem no formulário.
 */
@RestController
@RequestMapping("/api/v1/contas")
@Tag(name = "Conta", description = "Cadastro, confirmação e dados da conta")
class ContaController {

    private final CadastroService cadastro;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Criação do controller
     * Objetivo: Receber o serviço de cadastro.
     * Decisões: Injeção pelo construtor.
     */
    ContaController(CadastroService cadastro) {
        this.cadastro = cadastro;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Cadastrar
     * Objetivo: Criar a conta e devolver a situação e os próximos passos (onboardings).
     * Decisões: 201 com o id da conta; a senha nunca volta na resposta.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma conta com papel de investidor, empreendedor ou os dois")
    CadastroResponse cadastrar(@Valid @RequestBody CadastroRequest pedido) {
        var resultado = cadastro.cadastrar(new CadastroService.NovoCadastro(pedido.nome(), pedido.email(),
                pedido.senha(), pedido.papeis(), pedido.aceites()));
        var usuario = resultado.usuario();
        return new CadastroResponse(usuario.getId(), usuario.getStatus().name(), new TreeSet<>(usuario.getPapeis()),
                resultado.confirmacaoPendente(), resultado.proximosPassos());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Confirmar e-mail
     * Objetivo: Ativar a conta pelo token do link enviado por e-mail.
     * Decisões: 204 quando confirmado; 422 "link_invalido" caso contrário.
     */
    @PostMapping("/confirmacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirma o e-mail da conta pelo token do link")
    void confirmar(@Valid @RequestBody ConfirmacaoRequest pedido) {
        cadastro.confirmarEmail(pedido.token());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Reenviar confirmação
     * Objetivo: Pedir um link de confirmação novo.
     * Decisões: 202 sempre, exista ou não a conta.
     */
    @PostMapping("/confirmacao/reenvio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Reenvia o link de confirmação, se houver conta pendente com o e-mail")
    void reenviar(@Valid @RequestBody ReenvioRequest pedido) {
        cadastro.reenviarConfirmacao(pedido.email());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Pedido de cadastro
     * Objetivo: Campos do formulário de cadastro, com as regras de formato.
     * Decisões: Política de senha completa no serviço; aqui só o tamanho, para o erro aparecer no campo.
     */
    record CadastroRequest(
            @NotBlank(message = "Informe seu nome.") @Size(max = 120, message = "Use até 120 caracteres.") String nome,
            @NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.")
            @Size(max = 254, message = "E-mail longo demais.") String email,
            @NotBlank(message = "Crie uma senha.")
            @Size(min = PoliticaDeSenha.MINIMO, max = PoliticaDeSenha.MAXIMO,
                    message = "A senha precisa ter de 10 a 128 caracteres.") String senha,
            @NotEmpty(message = "Escolha pelo menos um papel.") Set<Papel> papeis,
            List<UUID> aceites) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:00
         * Cartão Jira: KAN-12
         * Título: Representação textual segura
         * Objetivo: Evitar que a senha apareça em logs.
         * Decisões: Mostra só os papéis.
         */
        @Override
        public String toString() {
            return "CadastroRequest[papeis=" + papeis + "]";
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Pedido de confirmação
     * Objetivo: Token recebido no link do e-mail.
     * Decisões: Obrigatório.
     */
    record ConfirmacaoRequest(@NotBlank(message = "Link inválido.") String token) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Pedido de reenvio
     * Objetivo: E-mail da conta pendente.
     * Decisões: Formato validado; a existência não é revelada.
     */
    record ReenvioRequest(@NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.") String email) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Conta criada
     * Objetivo: Id, situação, papéis, se falta confirmar o e-mail e as rotas de onboarding.
     * Decisões: O front-end mostra "confirme seu e-mail" ou leva para o login.
     */
    record CadastroResponse(UUID id, String status, Set<Papel> papeis, boolean confirmacaoPendente,
                            List<String> proximosPassos) {
    }
}
