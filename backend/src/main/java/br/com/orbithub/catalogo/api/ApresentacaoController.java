package br.com.orbithub.catalogo.api;

import br.com.orbithub.catalogo.service.ApresentacaoService;
import br.com.orbithub.catalogo.service.MarcacoesDoInvestidor;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.CuradoriaOuAdmin;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.investidores.service.AtuacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-29, KAN-36, KAN-84
 * Título: Apresentação da startup
 * Objetivo: Servir a apresentação em três páginas no catálogo (qualquer usuário cadastrado), na pré-visualização da
 *           equipe e, completa, para a curadoria; e o logotipo da startup publicada.
 * Decisões: O catálogo exige login (nenhum dado para visitante anônimo ou buscador). A pré-visualização fica na rota da
 *           startup; a versão completa, no backoffice.
 */
@RestController
@Tag(name = "Catálogo", description = "Apresentação das startups e edições temáticas")
class ApresentacaoController {

    private final ApresentacaoService apresentacao;
    private final StartupsService startups;
    private final MarcacoesDoInvestidor marcacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-36
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    ApresentacaoController(ApresentacaoService apresentacao, StartupsService startups, MarcacoesDoInvestidor marcacoes) {
        this.marcacoes = marcacoes;
        this.apresentacao = apresentacao;
        this.startups = startups;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-36
     * Título: Apresentação no catálogo
     * Objetivo: Mostrar a startup publicada a quem navega.
     * Decisões: O cabeçalho de organização define o selo usado para os dados da rodada. A visita entra no histórico
     *           privado da pessoa (KAN-43).
     */
    @GetMapping("/api/v1/catalogo/startups/{id}")
    @Operation(summary = "Apresentação da startup no catálogo")
    ApresentacaoService.Apresentacao doCatalogo(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                                                @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        ApresentacaoService.Apresentacao a = apresentacao.doCatalogo(Autenticado.id(token), organizacao, id);
        marcacoes.registrarVisita(Autenticado.id(token), id);
        return a;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-36
     * Título: Logotipo no catálogo
     * Objetivo: Servir o logotipo da startup publicada.
     * Decisões: Cache privado curto.
     */
    @GetMapping("/api/v1/catalogo/startups/{id}/logotipo")
    @Operation(summary = "Logotipo da startup no catálogo")
    ResponseEntity<InputStreamResource> logotipo(@PathVariable UUID id) {
        StartupsService.Logotipo l = startups.lerLogotipoPublicado(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(l.tipo())).cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff").body(new InputStreamResource(l.conteudo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-29
     * Título: Pré-visualização
     * Objetivo: Mostrar à equipe como a startup aparece, como investidor verificado ou não.
     * Decisões: Padrão: como verificado, que mostra o máximo que pode aparecer.
     */
    @GetMapping("/api/v1/startups/{id}/apresentacao")
    @SomenteEmpreendedor
    @Operation(summary = "Pré-visualização da apresentação da startup")
    ApresentacaoService.Apresentacao previa(@PathVariable UUID id, @RequestParam(defaultValue = "true") boolean verificado,
                                            @AuthenticationPrincipal Jwt token) {
        return apresentacao.previa(Autenticado.id(token), id, verificado);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Apresentação completa
     * Objetivo: Mostrar ao curador todos os campos da startup.
     * Decisões: Backoffice.
     */
    @GetMapping("/api/v1/admin/curadoria/startups/{id}/apresentacao")
    @CuradoriaOuAdmin
    @Operation(summary = "Apresentação completa da startup para a curadoria")
    ApresentacaoService.Apresentacao completa(@PathVariable UUID id) {
        return apresentacao.completa(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Logotipo para a curadoria
     * Objetivo: Servir o logotipo de qualquer startup ao curador.
     * Decisões: Mesmo arquivo da equipe.
     */
    @GetMapping("/api/v1/admin/curadoria/startups/{id}/logotipo")
    @CuradoriaOuAdmin
    @Operation(summary = "Logotipo da startup para a curadoria")
    ResponseEntity<InputStreamResource> logotipoParaCuradoria(@PathVariable UUID id) {
        StartupsService.Logotipo l = startups.lerLogotipoParaEquipeInterna(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(l.tipo())).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(new InputStreamResource(l.conteudo()));
    }
}
