package br.com.orbithub.investidores.api;

import br.com.orbithub.compartilhado.documentos.Cnpj;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteInvestidor;
import br.com.orbithub.investidores.domain.ConviteDeOrganizacao;
import br.com.orbithub.investidores.domain.Organizacao;
import br.com.orbithub.investidores.domain.PapelNaOrganizacao;
import br.com.orbithub.investidores.service.OrganizacoesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Organizações investidoras
 * Objetivo: Criar a organização, ver e editar os dados, convidar, mudar o papel e remover membros, e sair.
 * Decisões: Só quem tem o papel de investidor. As regras de quem pode o quê ficam no serviço (membro vê; administrador
 *           gerencia). O e-mail dos membros não é exposto.
 */
@RestController
@RequestMapping("/api/v1/organizacoes")
@SomenteInvestidor
@Tag(name = "Organizações", description = "Fundos, redes de anjos e outras entidades investidoras")
class OrganizacaoController {

    private final OrganizacoesService organizacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Criação do controller
     * Objetivo: Receber o serviço das organizações.
     * Decisões: Injeção por construtor.
     */
    OrganizacaoController(OrganizacoesService organizacoes) {
        this.organizacoes = organizacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Criar organização
     * Objetivo: Cadastrar a organização com quem cria como administrador.
     * Decisões: 201 com a organização criada.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma organização investidora")
    OrganizacaoResponse criar(@Valid @RequestBody DadosRequest pedido, @AuthenticationPrincipal Jwt token) {
        Organizacao o = organizacoes.criar(Autenticado.id(token), pedido.dados());
        return OrganizacaoResponse.de(o, PapelNaOrganizacao.ADMINISTRADOR);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Minhas organizações
     * Objetivo: Listar as organizações de quem está logado, com o papel em cada uma.
     * Decisões: Alimenta o seletor "atuando em nome de".
     */
    @GetMapping("/minhas")
    @Operation(summary = "Organizações de que a conta participa")
    List<OrganizacaoResponse> minhas(@AuthenticationPrincipal Jwt token) {
        return organizacoes.minhas(Autenticado.id(token)).stream()
                .map(v -> OrganizacaoResponse.de(v.organizacao(), v.papel())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Detalhe
     * Objetivo: Mostrar a organização e os membros.
     * Decisões: Quem não é membro recebe 404.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Dados e membros de uma organização")
    DetalheResponse detalhe(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        OrganizacoesService.Detalhe d = organizacoes.detalhe(Autenticado.id(token), id);
        return new DetalheResponse(OrganizacaoResponse.de(d.organizacao(), d.meuPapel()), d.membros());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Editar
     * Objetivo: Trocar nome, tipo, site e descrição.
     * Decisões: O CNPJ enviado é ignorado.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Edita os dados da organização (administrador)")
    OrganizacaoResponse editar(@PathVariable UUID id, @Valid @RequestBody DadosRequest pedido,
                               @AuthenticationPrincipal Jwt token) {
        return OrganizacaoResponse.de(organizacoes.editar(Autenticado.id(token), id, pedido.dados()),
                PapelNaOrganizacao.ADMINISTRADOR);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Convidar
     * Objetivo: Enviar um convite por e-mail.
     * Decisões: 201 com o convite (sem o token, que só vai no e-mail).
     */
    @PostMapping("/{id}/convites")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Convida alguém para a organização (administrador)")
    ConviteResponse convidar(@PathVariable UUID id, @Valid @RequestBody ConviteRequest pedido,
                             @AuthenticationPrincipal Jwt token) {
        return ConviteResponse.de(organizacoes.convidar(Autenticado.id(token), id, pedido.email(), pedido.papel()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Convites pendentes
     * Objetivo: Listar os convites ainda válidos.
     * Decisões: Só administrador.
     */
    @GetMapping("/{id}/convites")
    @Operation(summary = "Convites pendentes (administrador)")
    List<ConviteResponse> convites(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return organizacoes.convitesPendentes(Autenticado.id(token), id).stream().map(ConviteResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Cancelar convite
     * Objetivo: Invalidar um convite pendente.
     * Decisões: 204.
     */
    @DeleteMapping("/{id}/convites/{conviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela um convite (administrador)")
    void cancelarConvite(@PathVariable UUID id, @PathVariable UUID conviteId, @AuthenticationPrincipal Jwt token) {
        organizacoes.cancelarConvite(Autenticado.id(token), id, conviteId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Trocar papel
     * Objetivo: Mudar o papel de um membro.
     * Decisões: 204.
     */
    @PutMapping("/{id}/membros/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Muda o papel de um membro (administrador)")
    void trocarPapel(@PathVariable UUID id, @PathVariable UUID usuarioId, @Valid @RequestBody PapelRequest pedido,
                     @AuthenticationPrincipal Jwt token) {
        organizacoes.trocarPapel(Autenticado.id(token), id, usuarioId, pedido.papel());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Remover ou sair
     * Objetivo: Encerrar o vínculo de um membro; com o próprio id, é sair da organização.
     * Decisões: 204.
     */
    @DeleteMapping("/{id}/membros/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um membro (administrador) ou sai da organização (o próprio membro)")
    void encerrar(@PathVariable UUID id, @PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt token) {
        organizacoes.encerrarVinculo(Autenticado.id(token), id, usuarioId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Pedido com os dados
     * Objetivo: Receber os campos da organização.
     * Decisões: Limites iguais aos das colunas.
     */
    record DadosRequest(@NotBlank(message = "Informe o nome.") @Size(max = 160) String nome,
                        @NotBlank(message = "Escolha o tipo.") String tipo,
                        String cnpj,
                        @Size(max = 200) String site,
                        @Size(max = 2000) String descricao) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 14:05
         * Cartão Jira: KAN-60
         * Título: Para o serviço
         * Objetivo: Converter o pedido nos dados do serviço.
         * Decisões: Copia os campos.
         */
        OrganizacoesService.Dados dados() {
            return new OrganizacoesService.Dados(nome, tipo, cnpj, site, descricao);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Pedido de convite
     * Objetivo: Receber o e-mail e o papel oferecido.
     * Decisões: Papel obrigatório.
     */
    record ConviteRequest(@NotBlank(message = "Informe o e-mail.") String email,
                          @NotNull(message = "Escolha o papel.") PapelNaOrganizacao papel) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Pedido de papel
     * Objetivo: Receber o novo papel de um membro.
     * Decisões: Obrigatório.
     */
    record PapelRequest(@NotNull(message = "Escolha o papel.") PapelNaOrganizacao papel) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Organização na resposta
     * Objetivo: Expor a organização com o CNPJ formatado e o papel de quem pergunta.
     * Decisões: Papel incluído, para a tela mostrar só as ações permitidas.
     */
    record OrganizacaoResponse(UUID id, String nome, String tipo, String cnpj, String site, String descricao,
                               PapelNaOrganizacao meuPapel) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 14:05
         * Cartão Jira: KAN-60
         * Título: Conversão
         * Objetivo: Montar a resposta a partir da entidade e do papel.
         * Decisões: CNPJ formatado.
         */
        static OrganizacaoResponse de(Organizacao o, PapelNaOrganizacao papel) {
            return new OrganizacaoResponse(o.getId(), o.getNome(), o.getTipo(), Cnpj.formatar(o.getCnpj()), o.getSite(),
                    o.getDescricao(), papel);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Detalhe na resposta
     * Objetivo: Levar a organização e os membros.
     * Decisões: Membros com nome, papel e entrada.
     */
    record DetalheResponse(OrganizacaoResponse organizacao, List<OrganizacoesService.Membro> membros) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Convite na resposta
     * Objetivo: Expor um convite pendente.
     * Decisões: Sem token.
     */
    record ConviteResponse(UUID id, String email, PapelNaOrganizacao papel, Instant criadoEm, Instant expiraEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 14:05
         * Cartão Jira: KAN-60
         * Título: Conversão
         * Objetivo: Montar a resposta a partir do convite.
         * Decisões: Copia os campos públicos.
         */
        static ConviteResponse de(ConviteDeOrganizacao c) {
            return new ConviteResponse(c.getId(), c.getEmail(), c.getPapel(), c.getCriadoEm(), c.getExpiraEm());
        }
    }
}
