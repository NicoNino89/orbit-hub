package br.com.orbithub.atendimento.api;

import br.com.orbithub.atendimento.service.AtendimentoService;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-88, KAN-89, KAN-103
 * Título: Central de atendimento
 * Objetivo: Expor o catálogo de serviços e os artigos (sem login), o chamado sem conta, os chamados de quem tem conta e,
 *           à equipe, a fila, o atendimento, as respostas padrão, os artigos e os indicadores.
 * Decisões: Regras no serviço. Rotas da equipe em /api/v1/admin/atendimento.
 */
@RestController
@Tag(name = "Atendimento", description = "Central de ajuda, chamados e disputas")
class AtendimentoController {

    private static final String SUPORTE = "hasAnyRole('ADMIN', 'SUPORTE_N1', 'SUPORTE_N2', 'MODERADOR', 'ENCARREGADO_DADOS', 'JURIDICO')";
    private static final String LIDER = "hasAnyRole('ADMIN', 'SUPORTE_N2')";

    private final AtendimentoService atendimento;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Criação
     * Objetivo: Receber o serviço.
     * Decisões: Injeção por construtor.
     */
    AtendimentoController(AtendimentoService atendimento) {
        this.atendimento = atendimento;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-88
     * Título: Serviços
     * Objetivo: Devolver o catálogo de serviços.
     * Decisões: Aberto sem login.
     */
    @GetMapping("/api/v1/ajuda/servicos")
    @Operation(summary = "Catálogo de serviços da plataforma")
    List<AtendimentoService.ServicoDaPlataforma> servicos() {
        return atendimento.servicos();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Artigos
     * Objetivo: Devolver os artigos publicados da central de ajuda.
     * Decisões: Aberto sem login.
     */
    @GetMapping("/api/v1/ajuda/artigos")
    @Operation(summary = "Artigos da central de ajuda")
    List<AtendimentoService.Artigo> artigos() {
        return atendimento.artigos(false);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Chamado sem conta
     * Objetivo: Receber o chamado de quem não consegue entrar.
     * Decisões: 202 sempre; o chamado entra na fila depois da confirmação do e-mail.
     */
    @PostMapping("/api/v1/ajuda/chamados-publicos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Abre um chamado sem conta")
    void abrirPublico(@RequestBody AtendimentoService.NovoChamadoPublico pedido) {
        atendimento.abrirPublico(pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Confirmar chamado sem conta
     * Objetivo: Confirmar o e-mail e pôr o chamado na fila.
     * Decisões: Devolve o protocolo.
     */
    @PostMapping("/api/v1/ajuda/chamados-publicos/confirmacao")
    @Operation(summary = "Confirma o chamado sem conta")
    Map<String, UUID> confirmarPublico(@RequestBody TokenRequest pedido) {
        return Map.of("protocolo", atendimento.confirmarPublico(pedido.token()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89, KAN-103
     * Título: Abrir chamado
     * Objetivo: Abrir um chamado (ou uma disputa) com a pessoa identificada.
     * Decisões: Qualquer pessoa com conta.
     */
    @PostMapping("/api/v1/chamados")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre um chamado")
    AtendimentoService.Chamado abrir(@RequestBody AtendimentoService.NovoChamado pedido, @AuthenticationPrincipal Jwt token) {
        return atendimento.abrir(Autenticado.id(token), pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Meus chamados
     * Objetivo: Listar os chamados da pessoa.
     * Decisões: Inclui as disputas em que ela é a outra parte.
     */
    @GetMapping("/api/v1/chamados")
    @Operation(summary = "Lista os meus chamados")
    List<AtendimentoService.Resumo> meus(@AuthenticationPrincipal Jwt token) {
        return atendimento.meus(Autenticado.id(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Ler chamado
     * Objetivo: Mostrar o chamado a quem abriu ou à outra parte.
     * Decisões: Sem as notas internas.
     */
    @GetMapping("/api/v1/chamados/{id}")
    @Operation(summary = "Detalhe do meu chamado")
    AtendimentoService.Chamado ler(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return atendimento.ler(Autenticado.id(token), Set.of(), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Responder
     * Objetivo: Acrescentar uma mensagem ao chamado.
     * Decisões: Quem abriu ou a outra parte.
     */
    @PostMapping("/api/v1/chamados/{id}/mensagens")
    @Operation(summary = "Responde ao chamado")
    AtendimentoService.Chamado responder(@PathVariable UUID id, @RequestBody TextoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return atendimento.responder(Autenticado.id(token), id, pedido.texto());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Avaliar
     * Objetivo: Avaliar o atendimento e fechar o chamado.
     * Decisões: Nota de 1 a 5.
     */
    @PostMapping("/api/v1/chamados/{id}/avaliacao")
    @Operation(summary = "Avalia o atendimento")
    AtendimentoService.Chamado avaliar(@PathVariable UUID id, @RequestBody AvaliacaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return atendimento.avaliar(Autenticado.id(token), id, pedido.nota(), pedido.comentario());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Fila do atendimento
     * Objetivo: Listar os chamados dos níveis da pessoa.
     * Decisões: Pelo prazo.
     */
    @GetMapping("/api/v1/admin/atendimento/chamados")
    @PreAuthorize(SUPORTE)
    @Operation(summary = "Fila do atendimento")
    List<AtendimentoService.ItemDaFila> fila(@AuthenticationPrincipal Jwt token) {
        return atendimento.fila(Autenticado.papeis(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Chamado para a equipe
     * Objetivo: Mostrar o chamado com as notas internas.
     * Decisões: Só no nível da pessoa.
     */
    @GetMapping("/api/v1/admin/atendimento/chamados/{id}")
    @PreAuthorize(SUPORTE)
    @Operation(summary = "Detalhe do chamado para a equipe")
    AtendimentoService.Chamado lerEquipe(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return atendimento.ler(Autenticado.id(token), Autenticado.papeis(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Assumir chamado
     * Objetivo: Pôr o chamado em atendimento.
     * Decisões: Ver o serviço.
     */
    @PostMapping("/api/v1/admin/atendimento/chamados/{id}/assumir")
    @PreAuthorize(SUPORTE)
    @Operation(summary = "Assume o chamado")
    AtendimentoService.Chamado assumir(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return atendimento.assumir(Autenticado.id(token), Autenticado.papeis(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Responder como equipe
     * Objetivo: Responder ao usuário ou anotar internamente.
     * Decisões: Pode deixar aguardando o usuário.
     */
    @PostMapping("/api/v1/admin/atendimento/chamados/{id}/mensagens")
    @PreAuthorize(SUPORTE)
    @Operation(summary = "Responde ao chamado como equipe")
    AtendimentoService.Chamado responderEquipe(@PathVariable UUID id, @RequestBody RespostaRequest pedido, @AuthenticationPrincipal Jwt token) {
        return atendimento.responderEquipe(Autenticado.id(token), Autenticado.papeis(token), id, pedido.texto(), Boolean.TRUE.equals(pedido.interna()),
                Boolean.TRUE.equals(pedido.aguardarUsuario()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Escalar
     * Objetivo: Passar o chamado a um nível acima.
     * Decisões: Motivo obrigatório.
     */
    @PostMapping("/api/v1/admin/atendimento/chamados/{id}/escalonamento")
    @PreAuthorize(SUPORTE)
    @Operation(summary = "Escala o chamado")
    AtendimentoService.Chamado escalar(@PathVariable UUID id, @RequestBody EscalonamentoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return atendimento.escalar(Autenticado.id(token), Autenticado.papeis(token), id, pedido.nivel(), pedido.motivo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89, KAN-103
     * Título: Resolver
     * Objetivo: Resolver o chamado (e a disputa).
     * Decisões: Disputa exige o resultado.
     */
    @PostMapping("/api/v1/admin/atendimento/chamados/{id}/resolucao")
    @PreAuthorize(SUPORTE)
    @Operation(summary = "Resolve o chamado")
    AtendimentoService.Chamado resolver(@PathVariable UUID id, @RequestBody ResolucaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return atendimento.resolver(Autenticado.id(token), Autenticado.papeis(token), id, pedido.texto(), pedido.resultadoDisputa());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Respostas padrão
     * Objetivo: Listar as respostas padrão.
     * Decisões: Toda a equipe de atendimento.
     */
    @GetMapping("/api/v1/admin/atendimento/respostas")
    @PreAuthorize(SUPORTE)
    @Operation(summary = "Respostas padrão")
    List<AtendimentoService.RespostaPadrao> respostas() {
        return atendimento.respostas();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Criar resposta padrão
     * Objetivo: Criar uma resposta padrão.
     * Decisões: Líder do time.
     */
    @PostMapping("/api/v1/admin/atendimento/respostas")
    @PreAuthorize(LIDER)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma resposta padrão")
    AtendimentoService.RespostaPadrao criarResposta(@RequestBody RespostaPadraoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return atendimento.salvarResposta(Autenticado.id(token), null, pedido.titulo(), pedido.texto());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Alterar resposta padrão
     * Objetivo: Alterar uma resposta padrão.
     * Decisões: Líder do time.
     */
    @PutMapping("/api/v1/admin/atendimento/respostas/{id}")
    @PreAuthorize(LIDER)
    @Operation(summary = "Altera uma resposta padrão")
    AtendimentoService.RespostaPadrao alterarResposta(@PathVariable UUID id, @RequestBody RespostaPadraoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return atendimento.salvarResposta(Autenticado.id(token), id, pedido.titulo(), pedido.texto());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Artigos para a equipe
     * Objetivo: Listar todos os artigos, publicados ou não.
     * Decisões: Equipe de atendimento.
     */
    @GetMapping("/api/v1/admin/atendimento/artigos")
    @PreAuthorize(SUPORTE)
    @Operation(summary = "Artigos da central de ajuda (todos)")
    List<AtendimentoService.Artigo> todosOsArtigos() {
        return atendimento.artigos(true);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Criar artigo
     * Objetivo: Criar um artigo da central de ajuda.
     * Decisões: Time de atendimento.
     */
    @PostMapping("/api/v1/admin/atendimento/artigos")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPORTE_N1', 'SUPORTE_N2')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um artigo")
    AtendimentoService.Artigo criarArtigo(@RequestBody ArtigoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return atendimento.salvarArtigo(Autenticado.id(token), null, pedido.titulo(), pedido.conteudo(), pedido.publicado());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Alterar artigo
     * Objetivo: Alterar um artigo da central de ajuda.
     * Decisões: Time de atendimento.
     */
    @PutMapping("/api/v1/admin/atendimento/artigos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPORTE_N1', 'SUPORTE_N2')")
    @Operation(summary = "Altera um artigo")
    AtendimentoService.Artigo alterarArtigo(@PathVariable UUID id, @RequestBody ArtigoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return atendimento.salvarArtigo(Autenticado.id(token), id, pedido.titulo(), pedido.conteudo(), pedido.publicado());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-88, KAN-89
     * Título: Indicadores
     * Objetivo: Devolver volume, prazo cumprido e satisfação por serviço.
     * Decisões: Equipe de atendimento.
     */
    @GetMapping("/api/v1/admin/atendimento/indicadores")
    @PreAuthorize(SUPORTE)
    @Operation(summary = "Indicadores do atendimento")
    List<AtendimentoService.Indicador> indicadores() {
        return atendimento.indicadores();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89, KAN-103
     * Título: Pedidos
     * Objetivo: Tipar os corpos das rotas.
     * Decisões: Boolean em vez de boolean, para o corpo sem o campo não falhar.
     */
    record TokenRequest(String token) {
    }

    record TextoRequest(String texto) {
    }

    record AvaliacaoRequest(Integer nota, String comentario) {
    }

    record RespostaRequest(String texto, Boolean interna, Boolean aguardarUsuario) {
    }

    record EscalonamentoRequest(AtendimentoService.Nivel nivel, String motivo) {
    }

    record ResolucaoRequest(String texto, String resultadoDisputa) {
    }

    record RespostaPadraoRequest(String titulo, String texto) {
    }

    record ArtigoRequest(String titulo, String conteudo, Boolean publicado) {
    }
}
