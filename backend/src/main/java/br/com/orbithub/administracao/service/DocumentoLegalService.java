package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.repository.DocumentoLegalRepository;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Publicação e consulta de documentos legais
 * Objetivo: Publicar versões novas de termos, políticas e avisos e responder qual versão vale agora.
 * Decisões: O jurídico redige e o administrador publica (P14); a publicação é só a gravação de uma versão
 *           nova com vigência. Versões futuras não aparecem para o público até a data chegar.
 */
@Service
public class DocumentoLegalService {

    /** Folga para o relógio do navegador do administrador estar um pouco atrasado. */
    static final Duration TOLERANCIA_VIGENCIA_PASSADA = Duration.ofMinutes(5);

    private final DocumentoLegalRepository documentos;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Criação do serviço
     * Objetivo: Receber o repositório e o relógio.
     * Decisões: Injeção pelo construtor.
     */
    public DocumentoLegalService(DocumentoLegalRepository documentos, Clock relogio) {
        this.documentos = documentos;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Versão vigente de um tipo
     * Objetivo: Buscar a versão em vigor agora.
     * Decisões: Vazio quando o tipo ainda não tem versão em vigor (por exemplo, a Política de Publicidade
     *           antes da F2).
     */
    @Transactional(readOnly = true)
    public Optional<DocumentoLegal> vigente(TipoDocumento tipo) {
        return documentos.findFirstByTipoAndVigenteDesdeLessThanEqualOrderByVersaoDesc(tipo, relogio.instant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Todas as versões vigentes
     * Objetivo: Listar a versão em vigor de cada tipo que tem alguma.
     * Decisões: Na ordem do enum, que é a ordem de exibição na tela.
     */
    @Transactional(readOnly = true)
    public List<DocumentoLegal> vigentes() {
        return Arrays.stream(TipoDocumento.values())
                .map(this::vigente)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Histórico público de um tipo
     * Objetivo: Listar as versões já em vigor de um tipo, da mais nova para a mais antiga.
     * Decisões: Versões anteriores continuam consultáveis (P14); as agendadas para o futuro ficam de fora.
     */
    @Transactional(readOnly = true)
    public List<DocumentoLegal> historico(TipoDocumento tipo) {
        Instant agora = relogio.instant();
        return documentos.findByTipoOrderByVersaoDesc(tipo).stream().filter(d -> d.emVigor(agora)).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Histórico completo de um tipo
     * Objetivo: Listar todas as versões, inclusive as agendadas, para o painel da equipe.
     * Decisões: Só a equipe vê o que ainda não entrou em vigor.
     */
    @Transactional(readOnly = true)
    public List<DocumentoLegal> historicoCompleto(TipoDocumento tipo) {
        return documentos.findByTipoOrderByVersaoDesc(tipo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Buscar uma versão
     * Objetivo: Ler uma versão pelo id.
     * Decisões: Versão futura é tratada como inexistente para o público.
     */
    @Transactional(readOnly = true)
    public DocumentoLegal buscarPublicado(UUID id) {
        return documentos.findById(id)
                .filter(d -> d.emVigor(relogio.instant()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:25
     * Cartão Jira: KAN-80
     * Título: Publicar nova versão
     * Objetivo: Gravar a próxima versão de um tipo com a data de vigência informada.
     * Decisões: Sem data, vale a partir de agora. Vigência no passado é recusada (fora uma folga de 5 min),
     *           para ninguém "publicar retroativamente" um texto que os usuários não viram (KAN-92). A
     *           publicação é descrita para a trilha de auditoria como DOCUMENTO_LEGAL_PUBLICADO (KAN-80).
     */
    @Transactional
    public DocumentoLegal publicar(TipoDocumento tipo, String titulo, String conteudo, Instant vigenteDesde,
                                   UUID publicadoPor) {
        Instant agora = relogio.instant();
        Instant inicio = vigenteDesde != null ? vigenteDesde : agora;
        ContextoDeAuditoria.descrever("DOCUMENTO_LEGAL_PUBLICADO", "documento_legal", null, "Tipo " + tipo);
        if (inicio.isBefore(agora.minus(TOLERANCIA_VIGENCIA_PASSADA))) {
            throw new RegraDeNegocioException("vigencia_no_passado",
                    "A vigência não pode começar no passado.");
        }
        Integer maior = documentos.maiorVersao(tipo);
        int proxima = maior == null ? 1 : maior + 1;
        DocumentoLegal publicado = documentos.save(
                DocumentoLegal.publicar(tipo, proxima, titulo, conteudo, inicio, agora, publicadoPor));
        ContextoDeAuditoria.descrever("DOCUMENTO_LEGAL_PUBLICADO", "documento_legal", publicado.getId(),
                "Tipo " + tipo + ", versão " + proxima + ", vigente desde " + inicio);
        return publicado;
    }
}
