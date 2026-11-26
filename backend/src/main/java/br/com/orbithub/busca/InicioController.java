package br.com.orbithub.busca;

import br.com.orbithub.administracao.service.BannersService;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.empreendimentos.service.VitrineDaStartup;
import br.com.orbithub.investidores.service.AtuacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:05
 * Cartão Jira: KAN-167, KAN-170, KAN-171
 * Título: Página inicial e vitrine
 * Objetivo: Expor a busca global, os banners vigentes (e o cadastro deles ao administrador), a vitrine da startup (capa
 *           e chamada).
 * Decisões: Regras nos serviços.
 */
@RestController
@Tag(name = "Início", description = "Busca global, banners e vitrine")
class InicioController {

    private final BuscaService busca;
    private final BannersService banners;
    private final VitrineDaStartup vitrine;
    private final AtuacaoService atuacao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-167
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    InicioController(BuscaService busca, BannersService banners, VitrineDaStartup vitrine, AtuacaoService atuacao) {
        this.busca = busca;
        this.banners = banners;
        this.vitrine = vitrine;
        this.atuacao = atuacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-167
     * Título: Buscar
     * Objetivo: Busca global.
     * Decisões: Organização da atuação vale para os bloqueios.
     */
    @GetMapping("/api/v1/busca")
    @Operation(summary = "Busca global")
    BuscaService.Resultado buscar(@RequestParam String q, @AuthenticationPrincipal Jwt token,
                                  @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        UUID eu = Autenticado.id(token);
        UUID org = null;
        if (organizacao != null && !organizacao.isBlank()) {
            org = atuacao.paraVer(eu, organizacao).organizacao();
        }
        return busca.buscar(eu, org, q);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-171
     * Título: Banners vigentes
     * Objetivo: Listar os banners do papel.
     * Decisões: Papel informado pela tela (INVESTIDOR ou EMPREENDEDOR).
     */
    @GetMapping("/api/v1/inicio/banners")
    @Operation(summary = "Banners e chamadas vigentes")
    List<BannersService.Banner> vigentes(@RequestParam(required = false) String papel) {
        return banners.vigentes("INVESTIDOR".equals(papel) || "EMPREENDEDOR".equals(papel) ? papel : null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-171
     * Título: Imagem do banner
     * Objetivo: Servir a imagem do banner.
     * Decisões: Cache privado.
     */
    @GetMapping("/api/v1/inicio/banners/{id}/imagem")
    @Operation(summary = "Imagem do banner")
    ResponseEntity<InputStreamResource> imagem(@PathVariable UUID id) {
        BannersService.Imagem i = banners.imagem(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(i.tipo())).cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff").body(new InputStreamResource(i.conteudo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-171
     * Título: Todos os banners
     * Objetivo: Listar para o administrador.
     * Decisões: Só administrador.
     */
    @GetMapping("/api/v1/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Banners (administração)")
    List<BannersService.Banner> todos() {
        return banners.todos();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-171
     * Título: Criar banner
     * Objetivo: Criar banner ou chamada.
     * Decisões: Só administrador.
     */
    @PostMapping("/api/v1/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria banner")
    BannersService.Banner criar(@RequestBody BannersService.Dados d, @AuthenticationPrincipal Jwt token) {
        return banners.salvar(Autenticado.id(token), null, d);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-171
     * Título: Alterar banner
     * Objetivo: Alterar banner ou chamada.
     * Decisões: Só administrador.
     */
    @PutMapping("/api/v1/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Altera banner")
    BannersService.Banner alterar(@PathVariable UUID id, @RequestBody BannersService.Dados d, @AuthenticationPrincipal Jwt token) {
        return banners.salvar(Autenticado.id(token), id, d);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-171
     * Título: Imagem do banner
     * Objetivo: Enviar a imagem.
     * Decisões: Só administrador.
     */
    @PutMapping(path = "/api/v1/admin/banners/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Troca a imagem do banner")
    BannersService.Banner trocarImagem(@PathVariable UUID id, @RequestParam MultipartFile arquivo, @AuthenticationPrincipal Jwt token) {
        return banners.trocarImagem(Autenticado.id(token), id, bytes(arquivo));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-170
     * Título: Vitrine
     * Objetivo: Ler a chamada e se há capa.
     * Decisões: Equipe da startup.
     */
    @GetMapping("/api/v1/startups/{id}/vitrine")
    @Operation(summary = "Vitrine da startup")
    VitrineDaStartup.Vitrine lerVitrine(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return vitrine.vitrine(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-170
     * Título: Chamada
     * Objetivo: Gravar a chamada da startup.
     * Decisões: Fundadores.
     */
    @PutMapping("/api/v1/startups/{id}/vitrine")
    @Operation(summary = "Define a chamada da startup")
    Map<String, String> definirChamada(@PathVariable UUID id, @RequestBody Map<String, String> corpo, @AuthenticationPrincipal Jwt token) {
        String c = vitrine.definirChamada(Autenticado.id(token), id, corpo.get("chamada"));
        return c == null ? Map.of() : Map.of("chamada", c);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-170
     * Título: Trocar capa
     * Objetivo: Enviar a capa da startup.
     * Decisões: Fundadores.
     */
    @PutMapping(path = "/api/v1/startups/{id}/capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Troca a capa da startup")
    VitrineDaStartup.Vitrine trocarCapa(@PathVariable UUID id, @RequestParam MultipartFile arquivo, @AuthenticationPrincipal Jwt token) {
        vitrine.trocarCapa(Autenticado.id(token), id, bytes(arquivo));
        return vitrine.vitrine(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-170
     * Título: Capa para a equipe
     * Objetivo: Servir a capa à equipe da startup.
     * Decisões: Cache privado.
     */
    @GetMapping("/api/v1/startups/{id}/capa")
    @Operation(summary = "Capa da startup (equipe)")
    ResponseEntity<InputStreamResource> capaDaEquipe(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return capa(vitrine.capaDaEquipe(Autenticado.id(token), id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:05
     * Cartão Jira: KAN-170
     * Título: Capa no catálogo
     * Objetivo: Servir a capa da startup publicada.
     * Decisões: Quem tem login.
     */
    @GetMapping("/api/v1/catalogo/startups/{id}/capa")
    @Operation(summary = "Capa da startup no catálogo")
    ResponseEntity<InputStreamResource> capaPublicada(@PathVariable UUID id) {
        return capa(vitrine.capaPublicada(id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:59
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: capa
     * Objetivo: Rotina auxiliar capa.
     * Decisões: Ver a documentação da classe.
     */
    private static ResponseEntity<InputStreamResource> capa(VitrineDaStartup.Capa c) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(c.tipo())).cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff").body(new InputStreamResource(c.conteudo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 23:59
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: bytes
     * Objetivo: Rotina auxiliar bytes.
     * Decisões: Ver a documentação da classe.
     */
    private static byte[] bytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new RegraDeNegocioException("arquivo_ilegivel", "Não foi possível ler o arquivo. Tente de novo.");
        }
    }
}
