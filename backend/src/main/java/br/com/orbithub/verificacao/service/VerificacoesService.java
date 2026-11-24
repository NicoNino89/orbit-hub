package br.com.orbithub.verificacao.service;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.investidores.domain.PapelNaOrganizacao;
import br.com.orbithub.investidores.service.AtuacaoService.Atuacao;
import br.com.orbithub.investidores.service.OrganizacoesService;
import br.com.orbithub.verificacao.domain.DocumentoDeVerificacao;
import br.com.orbithub.verificacao.domain.PedidoDeVerificacao;
import br.com.orbithub.verificacao.domain.Selo;
import br.com.orbithub.verificacao.domain.SituacaoDoPedido;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import br.com.orbithub.verificacao.repository.DocumentoDeVerificacaoRepository;
import br.com.orbithub.verificacao.repository.PedidoDeVerificacaoRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Verificação de identidade (lado de quem pede)
 * Objetivo: Deixar a pessoa física ou a organização enviar os documentos exigidos, mandar o pedido para análise e ver
 *           o andamento; e informar a qualquer módulo o selo de um sujeito.
 * Decisões: O sujeito vem da atuação (KAN-60): por si, a pessoa; por uma organização, só o administrador envia (os
 *           demais membros veem). Documentos exigidos vêm da lista DOCUMENTO_VERIFICACAO, pelo grupo do sujeito; a
 *           autorização da CVM só é exigida de fundos de VC. Formatos PDF, JPEG e PNG, conferidos pelos bytes, até
 *           verificacao.documento_max_mb. O arquivo só é lido pelo dono e pela equipe de verificação (KAN-83).
 */
@Service
public class VerificacoesService {

    static final String CHAVE_TAMANHO = "verificacao.documento_max_mb";
    static final String DOCUMENTO_CVM = "ORG_AUTORIZACAO_CVM";
    static final String TIPO_FUNDO = "FUNDO_VC";
    static final Set<SituacaoDoPedido> EM_ABERTO = EnumSet.of(SituacaoDoPedido.RASCUNHO, SituacaoDoPedido.EM_ANALISE,
            SituacaoDoPedido.PENDENTE_COMPLEMENTO);

    private final PedidoDeVerificacaoRepository pedidos;
    private final DocumentoDeVerificacaoRepository documentos;
    private final ListasService listas;
    private final OrganizacoesService organizacoes;
    private final ArmazenamentoArquivos arquivos;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, listas, organizações, armazenamento, parâmetros e relógio.
     * Decisões: Injeção por construtor.
     */
    public VerificacoesService(PedidoDeVerificacaoRepository pedidos, DocumentoDeVerificacaoRepository documentos,
                               ListasService listas, OrganizacoesService organizacoes, ArmazenamentoArquivos arquivos,
                               LeituraDeParametros parametros, Clock relogio) {
        this.pedidos = pedidos;
        this.documentos = documentos;
        this.listas = listas;
        this.organizacoes = organizacoes;
        this.arquivos = arquivos;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Selo de um sujeito
     * Objetivo: Dizer se o sujeito está verificado, em análise ou não verificado.
     * Decisões: Serviço público (catálogo, manifestações de interesse, Nível 2). Verificado vale enquanto dentro da
     *           validade, mesmo com uma revalidação em análise.
     */
    @Transactional(readOnly = true)
    public Selo selo(SujeitoDaVerificacao tipo, UUID sujeito) {
        Instant agora = relogio.instant();
        List<PedidoDeVerificacao> historico = pedidos.findBySujeitoTipoAndSujeitoIdOrderByCriadoEmDesc(tipo, sujeito);
        if (historico.stream().anyMatch(p -> p.verificadoEm(agora))) {
            return Selo.VERIFICADO;
        }
        boolean emAnalise = historico.stream().anyMatch(p -> p.getSituacao() == SituacaoDoPedido.EM_ANALISE
                || p.getSituacao() == SituacaoDoPedido.PENDENTE_COMPLEMENTO);
        return emAnalise ? Selo.EM_ANALISE : Selo.NAO_VERIFICADO;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Minha verificação
     * Objetivo: Devolver o selo, o pedido mais recente, os documentos exigidos (com os já enviados) e se quem atua pode
     *           enviar.
     * Decisões: Sem pedido ainda: só o selo e os exigidos.
     */
    @Transactional(readOnly = true)
    public Situacao situacao(Atuacao atuacao) {
        Sujeito sujeito = sujeito(atuacao);
        Optional<PedidoDeVerificacao> atual = pedidos.findBySujeitoTipoAndSujeitoIdOrderByCriadoEmDesc(sujeito.tipo(), sujeito.id())
                .stream().findFirst();
        List<DocumentoDeVerificacao> enviados = atual.map(p -> documentos.findByPedidoIdOrderByEnviadoEm(p.getId())).orElse(List.of());
        return new Situacao(sujeito.tipo(), selo(sujeito.tipo(), sujeito.id()), podeEnviar(atuacao), exigidos(atuacao, sujeito),
                atual, enviados);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Enviar um documento
     * Objetivo: Guardar um documento de um tipo exigido no pedido em rascunho (ou aguardando complemento).
     * Decisões: Abre o rascunho se não houver pedido em aberto. Pedido em análise não aceita documento. O mesmo tipo
     *           enviado de novo substitui o anterior (o arquivo antigo é apagado).
     */
    @Transactional
    public DocumentoDeVerificacao enviarDocumento(Atuacao atuacao, String tipo, String nomeOriginal, byte[] conteudo) {
        exigirQuemEnvia(atuacao);
        Sujeito sujeito = sujeito(atuacao);
        if (exigidos(atuacao, sujeito).stream().noneMatch(e -> e.codigo().equals(tipo))) {
            throw new RegraDeNegocioException("documento_nao_exigido", "Este documento não faz parte da verificação.");
        }
        long maximo = parametros.inteiro(CHAVE_TAMANHO) * 1024 * 1024;
        if (conteudo == null || conteudo.length == 0 || conteudo.length > maximo) {
            throw new RegraDeNegocioException("documento_tamanho",
                    "O arquivo precisa ter até " + parametros.inteiro(CHAVE_TAMANHO) + " MB.");
        }
        String formato = FormatoDeDocumento.detectar(conteudo).orElseThrow(() -> new RegraDeNegocioException(
                "documento_formato", "Envie o documento em PDF, JPG ou PNG."));
        Instant agora = relogio.instant();
        PedidoDeVerificacao pedido = pedidos.findFirstBySujeitoTipoAndSujeitoIdAndSituacaoIn(sujeito.tipo(), sujeito.id(), EM_ABERTO)
                .orElseGet(() -> pedidos.save(PedidoDeVerificacao.rascunho(sujeito.tipo(), sujeito.id(), atuacao.pessoa(), agora)));
        if (!pedido.aceitaDocumentos()) {
            throw new RegraDeNegocioException("pedido_em_analise",
                    "O pedido está em análise. Aguarde o resultado ou o pedido de complemento.");
        }
        documentos.findByPedidoIdAndTipo(pedido.getId(), tipo).ifPresent(antigo -> {
            documentos.delete(antigo);
            documentos.flush();
            arquivos.remover(antigo.getChaveArquivo());
        });
        UUID id = UUID.randomUUID();
        String chave = "verificacoes/" + pedido.getId() + "/" + id + "." + FormatoDeDocumento.extensao(formato);
        arquivos.gravar(chave, new ByteArrayInputStream(conteudo), conteudo.length, formato);
        return documentos.save(DocumentoDeVerificacao.novo(id, pedido.getId(), tipo, chave, nomeOriginal, formato,
                conteudo.length, agora));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Remover um documento
     * Objetivo: Tirar um documento do pedido que ainda aceita documentos.
     * Decisões: Apaga o arquivo do armazenamento.
     */
    @Transactional
    public void removerDocumento(Atuacao atuacao, UUID documentoId) {
        exigirQuemEnvia(atuacao);
        DocumentoDeVerificacao documento = documentoDoSujeito(atuacao, documentoId);
        PedidoDeVerificacao pedido = pedidos.findById(documento.getPedidoId()).orElseThrow();
        if (!pedido.aceitaDocumentos()) {
            throw new RegraDeNegocioException("pedido_em_analise", "O pedido está em análise e não pode ser alterado.");
        }
        documentos.delete(documento);
        arquivos.remover(documento.getChaveArquivo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Mandar para análise
     * Objetivo: Enviar o pedido para a fila da verificação, com todos os documentos exigidos.
     * Decisões: Faltando algum, a mensagem diz quais.
     */
    @Transactional
    public PedidoDeVerificacao enviarParaAnalise(Atuacao atuacao) {
        exigirQuemEnvia(atuacao);
        Sujeito sujeito = sujeito(atuacao);
        PedidoDeVerificacao pedido = pedidos.findFirstBySujeitoTipoAndSujeitoIdAndSituacaoIn(sujeito.tipo(), sujeito.id(), EM_ABERTO)
                .filter(PedidoDeVerificacao::aceitaDocumentos)
                .orElseThrow(() -> new RegraDeNegocioException("sem_rascunho", "Envie os documentos antes de pedir a análise."));
        Set<String> enviados = documentos.findByPedidoIdOrderByEnviadoEm(pedido.getId()).stream()
                .map(DocumentoDeVerificacao::getTipo).collect(Collectors.toSet());
        List<String> faltando = exigidos(atuacao, sujeito).stream().filter(e -> !enviados.contains(e.codigo()))
                .map(Exigido::rotulo).toList();
        if (!faltando.isEmpty()) {
            throw new RegraDeNegocioException("documentos_faltando", "Faltam documentos: " + String.join("; ", faltando) + ".");
        }
        pedido.enviar(relogio.instant());
        return pedido;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Ler um documento próprio
     * Objetivo: Devolver o arquivo de um documento do próprio sujeito, para a pessoa conferir o que enviou.
     * Decisões: Documento de outro sujeito: 404.
     */
    @Transactional(readOnly = true)
    public Arquivo lerDocumento(Atuacao atuacao, UUID documentoId) {
        DocumentoDeVerificacao documento = documentoDoSujeito(atuacao, documentoId);
        return new Arquivo(arquivos.ler(documento.getChaveArquivo()), documento.getTipoConteudo(), documento.getNomeOriginal());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Documentos exigidos
     * Objetivo: Listar os documentos exigidos do sujeito, da lista gerenciável.
     * Decisões: A autorização da CVM só para organização do tipo fundo de VC.
     */
    List<Exigido> exigidos(Atuacao atuacao, Sujeito sujeito) {
        boolean fundo = sujeito.tipo() == SujeitoDaVerificacao.ORGANIZACAO
                && TIPO_FUNDO.equals(organizacoes.detalhe(atuacao.pessoa(), sujeito.id()).organizacao().getTipo());
        return listas.ativos(Lista.DOCUMENTO_VERIFICACAO).stream()
                .filter(i -> sujeito.tipo().getGrupoDeDocumentos().equals(i.getGrupo()))
                .filter(i -> fundo || !DOCUMENTO_CVM.equals(i.getCodigo()))
                .map(i -> new Exigido(i.getCodigo(), i.getRotulo())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Documento do sujeito
     * Objetivo: Buscar um documento conferindo que é de um pedido do sujeito da atuação.
     * Decisões: 404 para documento de outro sujeito.
     */
    private DocumentoDeVerificacao documentoDoSujeito(Atuacao atuacao, UUID documentoId) {
        Sujeito sujeito = sujeito(atuacao);
        return documentos.findById(documentoId)
                .filter(d -> pedidos.findById(d.getPedidoId())
                        .filter(p -> p.getSujeitoTipo() == sujeito.tipo() && p.getSujeitoId().equals(sujeito.id())).isPresent())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Sujeito da atuação
     * Objetivo: Converter a atuação no sujeito da verificação.
     * Decisões: Pessoa física ou organização.
     */
    private static Sujeito sujeito(Atuacao atuacao) {
        return atuacao.emNomeDaOrganizacao() ? new Sujeito(SujeitoDaVerificacao.ORGANIZACAO, atuacao.organizacao())
                : new Sujeito(SujeitoDaVerificacao.PESSOA, atuacao.pessoa());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Pode enviar
     * Objetivo: Dizer se quem atua pode enviar documentos.
     * Decisões: Pessoa física sempre; organização só o administrador.
     */
    private static boolean podeEnviar(Atuacao atuacao) {
        return !atuacao.emNomeDaOrganizacao() || atuacao.papel().orElse(null) == PapelNaOrganizacao.ADMINISTRADOR;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Exigir quem envia
     * Objetivo: Recusar membro ou observador que tenta enviar pela organização.
     * Decisões: 403.
     */
    private static void exigirQuemEnvia(Atuacao atuacao) {
        if (!podeEnviar(atuacao)) {
            throw new AccessDeniedException("Só o administrador da organização envia a verificação dela.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Sujeito
     * Objetivo: Levar o tipo e o identificador de quem é verificado.
     * Decisões: Interno ao módulo.
     */
    record Sujeito(SujeitoDaVerificacao tipo, UUID id) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Documento exigido
     * Objetivo: Levar o código e o nome de um documento exigido.
     * Decisões: Da lista gerenciável.
     */
    public record Exigido(String codigo, String rotulo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Situação da verificação
     * Objetivo: Levar o selo, se pode enviar, os exigidos, o pedido mais recente e os documentos dele.
     * Decisões: Pedido opcional.
     */
    public record Situacao(SujeitoDaVerificacao sujeito, Selo selo, boolean podeEnviar, List<Exigido> exigidos,
                           Optional<PedidoDeVerificacao> pedido, List<DocumentoDeVerificacao> documentos) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Arquivo
     * Objetivo: Levar o conteúdo, o tipo e o nome de um documento.
     * Decisões: Quem chama fecha o fluxo.
     */
    public record Arquivo(InputStream conteudo, String tipo, String nome) {
    }
}
