package br.com.orbithub.identidade.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.identidade.domain.EventoDaConta;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.PoliticaDeSenha;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Minha conta
 * Objetivo: Expor os dados do perfil, a foto, a troca de e-mail e de senha, os papéis e o histórico da conta
 *           do usuário logado.
 * Decisões: Sempre sobre a própria conta (id do token); não há como editar outra conta por aqui.
 */
@RestController
@RequestMapping("/api/v1/conta")
@Tag(name = "Conta", description = "Cadastro, confirmação e dados da conta")
class MinhaContaController {

    private final PerfilService perfil;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Criação do controller
     * Objetivo: Receber o serviço de perfil.
     * Decisões: Injeção pelo construtor.
     */
    MinhaContaController(PerfilService perfil) {
        this.perfil = perfil;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Dados da conta
     * Objetivo: Devolver nome, e-mail, telefone, papéis e se há foto.
     * Decisões: A foto é buscada à parte, em /conta/foto.
     */
    @GetMapping
    @Operation(summary = "Dados da conta logada")
    ContaResponse consultar(@AuthenticationPrincipal Jwt token) {
        return ContaResponse.de(perfil.conta(Autenticado.id(token)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Editar perfil
     * Objetivo: Trocar nome e telefone de contato.
     * Decisões: Telefone opcional, com dígitos, espaços, parênteses, hífen e "+".
     */
    @PutMapping
    @Operation(summary = "Atualiza nome e telefone")
    ContaResponse atualizar(@Valid @RequestBody PerfilRequest pedido, @AuthenticationPrincipal Jwt token) {
        return ContaResponse.de(perfil.atualizar(Autenticado.id(token), pedido.nome(), pedido.telefone()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Trocar foto
     * Objetivo: Receber a imagem do formulário e gravar como foto do perfil.
     * Decisões: Formato e tamanho conferidos no serviço.
     */
    @PutMapping(path = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Troca a foto do perfil (JPEG, PNG ou WebP, até 2 MB)")
    ContaResponse trocarFoto(@RequestParam("arquivo") MultipartFile arquivo, @AuthenticationPrincipal Jwt token)
            throws IOException {
        return ContaResponse.de(perfil.trocarFoto(Autenticado.id(token), arquivo.getBytes()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Ver foto
     * Objetivo: Devolver a imagem da foto do perfil.
     * Decisões: Cache só no navegador (private), porque a rota exige login.
     */
    @GetMapping("/foto")
    @Operation(summary = "Foto do perfil")
    ResponseEntity<InputStreamResource> foto(@AuthenticationPrincipal Jwt token) {
        var foto = perfil.lerFoto(Autenticado.id(token));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipo()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(foto.conteudo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Pedir troca de e-mail
     * Objetivo: Enviar o link de confirmação ao novo endereço.
     * Decisões: Exige a senha atual; 202 porque a troca só acontece depois da confirmação.
     */
    @PostMapping("/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Pede a troca de e-mail (confirmação pelo novo endereço)")
    void trocarEmail(@Valid @RequestBody TrocaEmailRequest pedido, @AuthenticationPrincipal Jwt token) {
        perfil.solicitarTrocaDeEmail(Autenticado.id(token), pedido.novoEmail(), pedido.senhaAtual());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Trocar senha
     * Objetivo: Trocar a senha informando a atual.
     * Decisões: Os outros aparelhos são desconectados; este continua.
     */
    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Troca a senha (exige a senha atual)")
    void trocarSenha(@Valid @RequestBody TrocaSenhaRequest pedido, @AuthenticationPrincipal Jwt token) {
        perfil.trocarSenha(Autenticado.id(token), Autenticado.sessao(token), pedido.senhaAtual(), pedido.novaSenha());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Ativar papel
     * Objetivo: Acrescentar investidor ou empreendedor e informar o onboarding do papel.
     * Decisões: Para investidor, o corpo pode trazer o aceite do Termo de Confidencialidade.
     */
    @PostMapping("/papeis/{papel}")
    @Operation(summary = "Ativa um papel na conta")
    AtivacaoResponse ativarPapel(@PathVariable Papel papel, @RequestBody(required = false) AtivacaoRequest pedido,
                                 @AuthenticationPrincipal Jwt token) {
        List<UUID> aceites = pedido == null ? List.of() : pedido.aceites();
        return new AtivacaoResponse(perfil.ativarPapel(Autenticado.id(token), papel, aceites));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Desativar papel
     * Objetivo: Retirar um papel sem pendências.
     * Decisões: 422 quando há pendência ou é o último papel.
     */
    @DeleteMapping("/papeis/{papel}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa um papel da conta")
    void desativarPapel(@PathVariable Papel papel, @AuthenticationPrincipal Jwt token) {
        perfil.desativarPapel(Autenticado.id(token), papel);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Histórico da conta
     * Objetivo: Listar as mudanças de papéis, e-mail e senha.
     * Decisões: Do mais recente ao mais antigo.
     */
    @GetMapping("/historico")
    @Operation(summary = "Histórico de mudanças da conta")
    List<EventoResponse> historico(@AuthenticationPrincipal Jwt token) {
        return perfil.historico(Autenticado.id(token)).stream().map(EventoResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Dados da conta na API
     * Objetivo: Representar a conta para a tela de perfil.
     * Decisões: Papéis ordenados.
     */
    record ContaResponse(UUID id, String nome, String email, String telefone, boolean temFoto, Set<Papel> papeis) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:40
         * Cartão Jira: KAN-16
         * Título: Conversão da entidade
         * Objetivo: Montar a resposta a partir da conta.
         * Decisões: Não expõe a chave da foto.
         */
        static ContaResponse de(Usuario u) {
            return new ContaResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getFotoChave() != null,
                    new TreeSet<>(u.getPapeis()));
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Pedido de edição do perfil
     * Objetivo: Nome e telefone.
     * Decisões: Telefone em formato livre e curto; vazio apaga.
     */
    record PerfilRequest(
            @NotBlank(message = "Informe seu nome.") @Size(max = 120, message = "Use até 120 caracteres.") String nome,
            @Size(max = 20, message = "Use até 20 caracteres.")
            @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Use só números, espaços, parênteses, hífen e +.")
            String telefone) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Pedido de troca de e-mail
     * Objetivo: Novo e-mail e senha atual.
     * Decisões: toString sem a senha.
     */
    record TrocaEmailRequest(
            @NotBlank(message = "Informe o novo e-mail.") @Email(message = "E-mail inválido.")
            @Size(max = 254, message = "E-mail longo demais.") String novoEmail,
            @NotBlank(message = "Informe sua senha atual.") String senhaAtual) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:40
         * Cartão Jira: KAN-16
         * Título: Representação textual segura
         * Objetivo: Evitar senha e e-mail em logs.
         * Decisões: Não mostra nenhum campo.
         */
        @Override
        public String toString() {
            return "TrocaEmailRequest[]";
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Pedido de troca de senha
     * Objetivo: Senha atual e senha nova.
     * Decisões: toString sem as senhas.
     */
    record TrocaSenhaRequest(
            @NotBlank(message = "Informe sua senha atual.") String senhaAtual,
            @NotBlank(message = "Crie uma senha.")
            @Size(min = PoliticaDeSenha.MINIMO, max = PoliticaDeSenha.MAXIMO,
                    message = "A senha precisa ter de 10 a 128 caracteres.") String novaSenha) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:40
         * Cartão Jira: KAN-16
         * Título: Representação textual segura
         * Objetivo: Evitar senhas em logs.
         * Decisões: Não mostra nenhum campo.
         */
        @Override
        public String toString() {
            return "TrocaSenhaRequest[]";
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Pedido de ativação de papel
     * Objetivo: Ids das versões aceitas junto com a ativação (Termo de Confidencialidade).
     * Decisões: Opcional para empreendedor.
     */
    record AtivacaoRequest(List<UUID> aceites) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Papel ativado
     * Objetivo: Informar o onboarding do papel ativado.
     * Decisões: A tela leva a pessoa para lá.
     */
    record AtivacaoResponse(String proximoPasso) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Evento do histórico na API
     * Objetivo: Tipo, detalhe e momento de um evento da conta.
     * Decisões: Conversão direta.
     */
    record EventoResponse(EventoDaConta.Tipo evento, String detalhe, Instant ocorridoEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:40
         * Cartão Jira: KAN-16
         * Título: Conversão da entidade
         * Objetivo: Montar a resposta a partir do evento.
         * Decisões: Sem dados pessoais.
         */
        static EventoResponse de(EventoDaConta e) {
            return new EventoResponse(e.getEvento(), e.getDetalhe(), e.getOcorridoEm());
        }
    }
}
