package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.AceiteDocumento;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.repository.AceiteDocumentoRepository;
import br.com.orbithub.administracao.repository.DocumentoLegalRepository;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Aceite de termos e políticas
 * Objetivo: Registrar aceites e dizer quais versões vigentes cada usuário ainda precisa aceitar.
 * Decisões: Só a versão vigente pode ser aceita (ninguém aceita um texto antigo ou ainda não publicado).
 *           Aceitar de novo a mesma versão não duplica o registro. Este módulo não conhece os papéis da
 *           conta: quem chama informa se o usuário é investidor.
 */
@Service
public class AceiteService {

    private final AceiteDocumentoRepository aceites;
    private final DocumentoLegalRepository documentos;
    private final DocumentoLegalService documentosLegais;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, o serviço de documentos e o relógio.
     * Decisões: Injeção pelo construtor.
     */
    public AceiteService(AceiteDocumentoRepository aceites, DocumentoLegalRepository documentos,
                         DocumentoLegalService documentosLegais, Clock relogio) {
        this.aceites = aceites;
        this.documentos = documentos;
        this.documentosLegais = documentosLegais;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Documentos exigidos
     * Objetivo: Dizer quais tipos o usuário precisa ter aceito.
     * Decisões: Todos aceitam Termos de Uso e Política de Privacidade; o investidor também o Termo de
     *           Confidencialidade.
     */
    public static Set<TipoDocumento> exigidos(boolean investidor) {
        Set<TipoDocumento> tipos = EnumSet.noneOf(TipoDocumento.class);
        Arrays.stream(TipoDocumento.values())
                .filter(t -> t.aceite() == TipoDocumento.Aceite.TODOS_NO_ACESSO
                        || (investidor && t.aceite() == TipoDocumento.Aceite.INVESTIDOR_ANTES_DO_NIVEL_2))
                .forEach(tipos::add);
        return tipos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Aceites pendentes
     * Objetivo: Listar as versões vigentes que o usuário ainda não aceitou.
     * Decisões: Considera só os tipos exigidos para o perfil.
     */
    @Transactional(readOnly = true)
    public List<DocumentoLegal> pendentes(UUID usuarioId, boolean investidor) {
        return exigidos(investidor).stream()
                .map(documentosLegais::vigente)
                .flatMap(java.util.Optional::stream)
                .filter(d -> !aceites.existsByUsuarioIdAndDocumentoId(usuarioId, d.getId()))
                .toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Pendência que bloqueia o acesso
     * Objetivo: Dizer se falta aceitar Termos de Uso ou Política de Privacidade vigentes.
     * Decisões: É o que impede o uso até o aceite "no próximo acesso"; a confidencialidade só bloqueia o
     *           Nível 2.
     */
    @Transactional(readOnly = true)
    public boolean temPendenciaBloqueante(UUID usuarioId) {
        return pendentes(usuarioId, false).stream()
                .anyMatch(d -> d.getTipo().aceite() == TipoDocumento.Aceite.TODOS_NO_ACESSO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Aceitou a versão vigente
     * Objetivo: Dizer se o usuário aceitou a versão em vigor de um tipo.
     * Decisões: Usado pelo Nível 2 (S8) para cobrar o Termo de Confidencialidade; sem versão vigente, não
     *           há o que cobrar.
     */
    @Transactional(readOnly = true)
    public boolean aceitouVigente(UUID usuarioId, TipoDocumento tipo) {
        return documentosLegais.vigente(tipo)
                .map(d -> aceites.existsByUsuarioIdAndDocumentoId(usuarioId, d.getId()))
                .orElse(true);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Registrar aceites
     * Objetivo: Gravar o aceite de cada versão informada.
     * Decisões: Cada id precisa ser a versão vigente de um tipo que exige aceite; caso contrário, nada é
     *           gravado (tudo ou nada) e o usuário recebe 422 para recarregar os textos.
     */
    @Transactional
    public void aceitar(UUID usuarioId, Collection<UUID> documentoIds) {
        Instant agora = relogio.instant();
        List<DocumentoLegal> escolhidos = documentoIds.stream().distinct()
                .map(id -> documentos.findById(id).orElseThrow(this::versaoInvalida))
                .toList();
        for (DocumentoLegal documento : escolhidos) {
            boolean ehVigente = documentosLegais.vigente(documento.getTipo())
                    .map(v -> v.getId().equals(documento.getId()))
                    .orElse(false);
            if (!documento.getTipo().exigeAceite() || !ehVigente) {
                throw versaoInvalida();
            }
        }
        escolhidos.stream()
                .filter(d -> !aceites.existsByUsuarioIdAndDocumentoId(usuarioId, d.getId()))
                .forEach(d -> aceites.save(AceiteDocumento.registrar(usuarioId, d.getId(), agora)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Histórico de aceites
     * Objetivo: Listar quem aceitou qual versão e quando, para a conta do usuário.
     * Decisões: Do mais recente ao mais antigo.
     */
    @Transactional(readOnly = true)
    public List<AceiteDocumento> historico(UUID usuarioId) {
        return aceites.findByUsuarioIdOrderByAceitoEmDesc(usuarioId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Versão inválida para aceite
     * Objetivo: Montar o erro de versão inexistente, sem aceite ou fora de vigor.
     * Decisões: Mensagem única: a tela recarrega os textos vigentes.
     */
    private RegraDeNegocioException versaoInvalida() {
        return new RegraDeNegocioException("versao_nao_vigente",
                "Um dos documentos foi atualizado. Recarregue a página e leia a versão atual.");
    }
}
