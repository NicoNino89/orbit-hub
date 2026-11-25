package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.compartilhado.documentos.Cnpj;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.empreendimentos.domain.Fundador;
import br.com.orbithub.empreendimentos.domain.PapelNaStartup;
import br.com.orbithub.empreendimentos.domain.RedeSocial;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.service.EquipeDaStartupService;
import br.com.orbithub.empreendimentos.service.StartupsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Cadastro da startup
 * Objetivo: Criar a startup, ler e editar os dados cadastrais e o logotipo, e listar as minhas startups.
 * Decisões: Só empreendedores. A startup vai no endereço (o empreendedor pode ter várias). A participação dos fundadores
 *           (cap table) só sai para a equipe. A chave do logotipo nunca sai na resposta.
 */
@RestController
@RequestMapping("/api/v1/startups")
@SomenteEmpreendedor
@Tag(name = "Startups", description = "Cadastro e gestão das startups")
class StartupController {

    private final StartupsService startups;
    private final EquipeDaStartupService equipe;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Criação do controller
     * Objetivo: Receber os serviços das startups e da equipe.
     * Decisões: Injeção por construtor.
     */
    StartupController(StartupsService startups, EquipeDaStartupService equipe) {
        this.startups = startups;
        this.equipe = equipe;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Criar startup
     * Objetivo: Cadastrar a startup com quem cria como fundador administrador.
     * Decisões: 201 com a startup.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma startup")
    StartupResponse criar(@RequestBody CadastroRequest pedido, @AuthenticationPrincipal Jwt token) {
        return StartupResponse.de(startups.criar(Autenticado.id(token), pedido.cadastro()), PapelNaStartup.FUNDADOR_ADMINISTRADOR, Set.of());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Minhas startups
     * Objetivo: Listar as startups de quem está logado, com o papel.
     * Decisões: Resumo, sem os fundadores.
     */
    @GetMapping("/minhas")
    @Operation(summary = "Startups de que a conta participa")
    List<ResumoResponse> minhas(@AuthenticationPrincipal Jwt token) {
        return startups.minhas(Autenticado.id(token)).stream().map(v -> new ResumoResponse(v.startup().getId(), v.startup().getNome(),
                v.startup().getSituacao(), v.startup().constituida(), v.papel())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Detalhe
     * Objetivo: Devolver os dados cadastrais da startup.
     * Decisões: Quem não é da equipe recebe 404.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Dados cadastrais de uma startup")
    StartupResponse detalhe(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        StartupsService.Vinculo v = startups.detalhe(Autenticado.id(token), id);
        return StartupResponse.de(v.startup(), v.papel(), equipe.minhasSecoes(Autenticado.id(token), id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Editar cadastro
     * Objetivo: Salvar os dados cadastrais, as redes e os fundadores.
     * Decisões: Regras no serviço.
     */
    @PutMapping("/{id}/cadastro")
    @Operation(summary = "Salva os dados cadastrais")
    StartupResponse editar(@PathVariable UUID id, @RequestBody CadastroRequest pedido, @AuthenticationPrincipal Jwt token) {
        UUID eu = Autenticado.id(token);
        return StartupResponse.de(startups.editarCadastro(eu, id, pedido.cadastro()), startups.papel(eu, id), equipe.minhasSecoes(eu, id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Trocar logotipo
     * Objetivo: Receber o arquivo do logotipo.
     * Decisões: Formato e tamanho conferidos no serviço.
     */
    @PutMapping(path = "/{id}/logotipo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Troca o logotipo")
    StartupResponse logotipo(@PathVariable UUID id, @RequestParam MultipartFile arquivo, @AuthenticationPrincipal Jwt token) {
        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new RegraDeNegocioException("logotipo_ilegivel", "Não foi possível ler o arquivo. Tente de novo.");
        }
        UUID eu = Autenticado.id(token);
        return StartupResponse.de(startups.trocarLogotipo(eu, id, conteudo), startups.papel(eu, id), equipe.minhasSecoes(eu, id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Ver logotipo
     * Objetivo: Devolver o logotipo para a equipe.
     * Decisões: Cache privado; sem adivinhação de tipo.
     */
    @GetMapping("/{id}/logotipo")
    @Operation(summary = "Logotipo da startup")
    ResponseEntity<InputStreamResource> verLogotipo(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        StartupsService.Logotipo l = startups.lerLogotipo(Autenticado.id(token), id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(l.tipo())).cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff").body(new InputStreamResource(l.conteudo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Pedido do cadastro
     * Objetivo: Receber os dados cadastrais.
     * Decisões: As regras de conteúdo ficam no serviço.
     */
    record CadastroRequest(String nome, String nomeFantasia, String razaoSocial, String cnpj, String segmento,
                           String localizacao, String site, Map<RedeSocial, String> redes, List<Fundador> fundadores) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 22:33
         * Cartão Jira: KAN-21
         * Título: Para o serviço
         * Objetivo: Converter o pedido nos dados cadastrais.
         * Decisões: Listas ausentes viram vazias.
         */
        Startup.Cadastro cadastro() {
            return new Startup.Cadastro(nome, nomeFantasia, razaoSocial, cnpj, segmento, localizacao, site,
                    redes == null ? Map.of() : redes, fundadores == null ? List.of() : fundadores);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Resumo na resposta
     * Objetivo: Expor uma startup na lista "minhas startups".
     * Decisões: Inclui se já é constituída (tem CNPJ).
     */
    record ResumoResponse(UUID id, String nome, SituacaoDaStartup situacao, boolean constituida, PapelNaStartup meuPapel) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Startup na resposta
     * Objetivo: Expor os dados cadastrais com o CNPJ formatado, o papel de quem pergunta e, colaborador, as seções
     *           que ele edita (KAN-65).
     * Decisões: "constituida" falso = selo "empresa ainda não constituída".
     */
    record StartupResponse(UUID id, String nome, String nomeFantasia, String razaoSocial, String cnpj, String segmento,
                           String localizacao, String site, Map<RedeSocial, String> redes, List<Fundador> fundadores,
                           SituacaoDaStartup situacao, boolean constituida, boolean temLogotipo, Instant atualizadaEm,
                           PapelNaStartup meuPapel, Set<SecaoDaStartup> minhasSecoes) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 22:33
         * Cartão Jira: KAN-21
         * Título: Conversão
         * Objetivo: Montar a resposta a partir da startup.
         * Decisões: CNPJ formatado.
         */
        static StartupResponse de(Startup s, PapelNaStartup papel, Set<SecaoDaStartup> secoes) {
            Startup.Cadastro c = s.cadastro();
            return new StartupResponse(s.getId(), c.nome(), c.nomeFantasia(), c.razaoSocial(),
                    c.cnpj() == null ? null : Cnpj.formatar(c.cnpj()), c.segmento(), c.localizacao(), c.site(), c.redes(),
                    c.fundadores(), s.getSituacao(), s.constituida(), s.getLogotipoChave() != null, s.getAtualizadaEm(), papel, secoes);
        }
    }
}
