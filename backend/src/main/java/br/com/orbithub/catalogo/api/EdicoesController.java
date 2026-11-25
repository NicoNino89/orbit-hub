package br.com.orbithub.catalogo.api;

import br.com.orbithub.catalogo.domain.Edicao;
import br.com.orbithub.catalogo.service.ApresentacaoService;
import br.com.orbithub.catalogo.service.EdicoesService;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.CuradoriaOuAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Edições temáticas
 * Objetivo: Deixar a curadoria montar, ordenar, publicar e arquivar as edições, e o usuário cadastrado ver as vigentes.
 * Decisões: Montagem em /api/v1/admin/edicoes (curador ou administrador); leitura em /api/v1/catalogo/edicoes.
 */
@RestController
@Tag(name = "Catálogo", description = "Apresentação das startups e edições temáticas")
class EdicoesController {

    static final String ADMIN = "/api/v1/admin/edicoes";

    private final EdicoesService edicoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Criação
     * Objetivo: Receber o serviço.
     * Decisões: Injeção por construtor.
     */
    EdicoesController(EdicoesService edicoes) {
        this.edicoes = edicoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Listar edições
     * Objetivo: Listar todas as edições para a curadoria.
     * Decisões: Resumo sem startups.
     */
    @GetMapping(ADMIN)
    @CuradoriaOuAdmin
    @Operation(summary = "Edições temáticas (curadoria)")
    List<EdicaoResponse> listar() {
        return edicoes.listar().stream().map(e -> EdicaoResponse.de(e, null)).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Startups elegíveis
     * Objetivo: Listar as startups que podem entrar numa edição.
     * Decisões: Publicadas e participando.
     */
    @GetMapping(ADMIN + "/startups-elegiveis")
    @CuradoriaOuAdmin
    @Operation(summary = "Startups que podem entrar numa edição")
    List<ApresentacaoService.Cartao> elegiveis() {
        return edicoes.elegiveis();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Detalhe
     * Objetivo: Mostrar a edição com a composição e o histórico.
     * Decisões: Inclui as ocultas.
     */
    @GetMapping(ADMIN + "/{id}")
    @CuradoriaOuAdmin
    @Operation(summary = "Detalhe de uma edição (curadoria)")
    DetalheResponse detalhe(@PathVariable UUID id) {
        EdicoesService.Detalhe d = edicoes.detalhe(id);
        return new DetalheResponse(EdicaoResponse.de(d.edicao(), d.curador()), d.edicao().getCriterios(), d.startups(), d.historico());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Criar
     * Objetivo: Criar a edição em rascunho.
     * Decisões: Quem cria é o curador responsável.
     */
    @PostMapping(ADMIN)
    @CuradoriaOuAdmin
    @Operation(summary = "Cria uma edição temática")
    EdicaoResponse criar(@Valid @RequestBody DadosRequest pedido, @AuthenticationPrincipal Jwt token) {
        return EdicaoResponse.de(edicoes.criar(Autenticado.id(token), pedido.dados()), null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Editar
     * Objetivo: Trocar os dados da edição.
     * Decisões: Arquivada é recusada no domínio.
     */
    @PutMapping(ADMIN + "/{id}")
    @CuradoriaOuAdmin
    @Operation(summary = "Edita uma edição temática")
    EdicaoResponse editar(@PathVariable UUID id, @Valid @RequestBody DadosRequest pedido, @AuthenticationPrincipal Jwt token) {
        return EdicaoResponse.de(edicoes.editar(id, Autenticado.id(token), pedido.dados()), null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Composição
     * Objetivo: Trocar as startups e a ordem.
     * Decisões: Lista ordenada de identificadores.
     */
    @PutMapping(ADMIN + "/{id}/startups")
    @CuradoriaOuAdmin
    @Operation(summary = "Define as startups e a ordem da edição")
    EdicaoResponse startups(@PathVariable UUID id, @RequestBody StartupsRequest pedido, @AuthenticationPrincipal Jwt token) {
        return EdicaoResponse.de(edicoes.definirStartups(id, Autenticado.id(token), pedido.startups()), null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Publicar
     * Objetivo: Tirar a edição do rascunho.
     * Decisões: Agendada ou vigente pela data.
     */
    @PostMapping(ADMIN + "/{id}/publicacao")
    @CuradoriaOuAdmin
    @Operation(summary = "Publica uma edição temática")
    EdicaoResponse publicar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return EdicaoResponse.de(edicoes.publicar(id, Autenticado.id(token)), null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Arquivar
     * Objetivo: Tirar a edição do ar.
     * Decisões: Sem volta.
     */
    @PostMapping(ADMIN + "/{id}/arquivamento")
    @CuradoriaOuAdmin
    @Operation(summary = "Arquiva uma edição temática")
    EdicaoResponse arquivar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return EdicaoResponse.de(edicoes.arquivar(id, Autenticado.id(token)), null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Trocar capa
     * Objetivo: Receber a imagem da capa.
     * Decisões: Formato e tamanho no serviço.
     */
    @PutMapping(path = ADMIN + "/{id}/capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CuradoriaOuAdmin
    @Operation(summary = "Troca a capa da edição")
    EdicaoResponse capa(@PathVariable UUID id, @RequestParam MultipartFile arquivo, @AuthenticationPrincipal Jwt token) {
        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new RegraDeNegocioException("capa_ilegivel", "Não foi possível ler o arquivo. Tente de novo.");
        }
        return EdicaoResponse.de(edicoes.trocarCapa(id, Autenticado.id(token), conteudo), null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Capa para a curadoria
     * Objetivo: Servir a capa de qualquer edição à curadoria.
     * Decisões: Sem cache.
     */
    @GetMapping(ADMIN + "/{id}/capa")
    @CuradoriaOuAdmin
    @Operation(summary = "Capa da edição (curadoria)")
    ResponseEntity<InputStreamResource> capaParaCuradoria(@PathVariable UUID id) {
        return imagem(edicoes.capa(id, true));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Edições vigentes
     * Objetivo: Listar as edições no ar para quem navega.
     * Decisões: Só startups visíveis.
     */
    @GetMapping("/api/v1/catalogo/edicoes")
    @Operation(summary = "Edições temáticas vigentes")
    List<EdicoesService.EdicaoNoCatalogo> vigentes() {
        return edicoes.vigentes();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Edição vigente
     * Objetivo: Mostrar uma edição no ar.
     * Decisões: Fora do ar, 404.
     */
    @GetMapping("/api/v1/catalogo/edicoes/{id}")
    @Operation(summary = "Edição temática vigente")
    EdicoesService.EdicaoNoCatalogo vigente(@PathVariable UUID id) {
        return edicoes.vigente(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Capa no catálogo
     * Objetivo: Servir a capa de uma edição vigente.
     * Decisões: Cache privado.
     */
    @GetMapping("/api/v1/catalogo/edicoes/{id}/capa")
    @Operation(summary = "Capa da edição vigente")
    ResponseEntity<InputStreamResource> capaNoCatalogo(@PathVariable UUID id) {
        return imagem(edicoes.capa(id, false));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Resposta de imagem
     * Objetivo: Montar a resposta com o tipo e os cabeçalhos de segurança.
     * Decisões: nosniff.
     */
    private static ResponseEntity<InputStreamResource> imagem(EdicoesService.Capa c) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(c.tipo())).cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff").body(new InputStreamResource(c.conteudo()));
    }

    record DadosRequest(@NotBlank(message = "Informe o título.") @Size(max = 120, message = "O título pode ter até 120 caracteres.") String titulo,
                        @NotBlank(message = "Informe a descrição.") @Size(max = 1000, message = "A descrição pode ter até 1.000 caracteres.") String descricao,
                        @NotBlank(message = "Informe os critérios de inclusão.") @Size(max = 1000, message = "Os critérios podem ter até 1.000 caracteres.") String criterios,
                        @NotNull(message = "Informe o início da vigência.") LocalDate inicio, LocalDate fim) {
        Edicao.Dados dados() {
            return new Edicao.Dados(titulo, descricao, criterios, inicio, fim);
        }
    }

    record StartupsRequest(List<UUID> startups) {
    }

    record EdicaoResponse(UUID id, String titulo, String descricao, LocalDate inicio, LocalDate fim, Edicao.Situacao situacao,
                          boolean temCapa, int quantidade, String curador, Instant atualizadaEm) {
        static EdicaoResponse de(Edicao e, String curador) {
            return new EdicaoResponse(e.getId(), e.getTitulo(), e.getDescricao(), e.getInicio(), e.getFim(), e.getSituacao(),
                    e.getCapaChave() != null, e.getStartups().size(), curador, e.getAtualizadaEm());
        }
    }

    record DetalheResponse(EdicaoResponse edicao, String criterios, List<EdicoesService.StartupNaEdicao> startups,
                           List<EdicoesService.Mudanca> historico) {
    }
}
