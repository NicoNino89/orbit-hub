package br.com.orbithub.administracao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.administracao.domain.AceiteDocumento;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.repository.AceiteDocumentoRepository;
import br.com.orbithub.administracao.repository.DocumentoLegalRepository;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Testes do serviço de aceites
 * Objetivo: Verificar documentos exigidos, pendências, bloqueio, registro e histórico de aceites.
 * Decisões: Vigentes: Termos v2, Privacidade v1 e Confidencialidade v1; Termos v1 é a versão antiga.
 */
class AceiteServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T03:00:00Z");
    private final UUID ana = UUID.randomUUID();

    private AceiteDocumentoRepository aceites;
    private DocumentoLegalRepository repositorio;
    private DocumentoLegalService documentos;
    private AceiteService servico;
    private DocumentoLegal termosV1;
    private DocumentoLegal termosV2;
    private DocumentoLegal privacidade;
    private DocumentoLegal confidencialidade;
    private DocumentoLegal aviso;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Preparação
     * Objetivo: Montar o serviço com documentos vigentes simulados.
     * Decisões: Nenhum aceite gravado de início.
     */
    @BeforeEach
    void preparar() {
        aceites = mock(AceiteDocumentoRepository.class);
        repositorio = mock(DocumentoLegalRepository.class);
        documentos = mock(DocumentoLegalService.class);
        servico = new AceiteService(aceites, repositorio, documentos, new RelogioAjustavel(AGORA));

        termosV1 = DocumentoLegalServiceTest.documento(TipoDocumento.TERMOS_DE_USO, 1, AGORA.minusSeconds(999));
        termosV2 = DocumentoLegalServiceTest.documento(TipoDocumento.TERMOS_DE_USO, 2, AGORA.minusSeconds(9));
        privacidade = DocumentoLegalServiceTest.documento(TipoDocumento.POLITICA_DE_PRIVACIDADE, 1, AGORA.minusSeconds(9));
        confidencialidade = DocumentoLegalServiceTest.documento(TipoDocumento.TERMO_DE_CONFIDENCIALIDADE, 1,
                AGORA.minusSeconds(9));
        aviso = DocumentoLegalServiceTest.documento(TipoDocumento.AVISO_NAO_COMPROMISSO, 1, AGORA.minusSeconds(9));

        when(documentos.vigente(any())).thenReturn(Optional.empty());
        when(documentos.vigente(TipoDocumento.TERMOS_DE_USO)).thenReturn(Optional.of(termosV2));
        when(documentos.vigente(TipoDocumento.POLITICA_DE_PRIVACIDADE)).thenReturn(Optional.of(privacidade));
        when(documentos.vigente(TipoDocumento.TERMO_DE_CONFIDENCIALIDADE)).thenReturn(Optional.of(confidencialidade));
        when(documentos.vigente(TipoDocumento.AVISO_NAO_COMPROMISSO)).thenReturn(Optional.of(aviso));
        for (DocumentoLegal d : List.of(termosV1, termosV2, privacidade, confidencialidade, aviso)) {
            when(repositorio.findById(d.getId())).thenReturn(Optional.of(d));
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Documentos exigidos por perfil
     * Objetivo: Garantir que o investidor também aceita a confidencialidade.
     * Decisões: Empreendedor e investidor.
     */
    @Test
    void investidorTambemAceitaConfidencialidade() {
        assertThat(AceiteService.exigidos(false))
                .containsExactlyInAnyOrder(TipoDocumento.TERMOS_DE_USO, TipoDocumento.POLITICA_DE_PRIVACIDADE);
        assertThat(AceiteService.exigidos(true)).contains(TipoDocumento.TERMO_DE_CONFIDENCIALIDADE);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Pendências e bloqueio
     * Objetivo: Garantir que, sem aceites, termos e privacidade bloqueiam e, aceitos, não bloqueiam mais.
     * Decisões: O investidor continua com a confidencialidade pendente, que não bloqueia o acesso.
     */
    @Test
    void pendenciasEBloqueio() {
        assertThat(servico.pendentes(ana, true)).containsExactlyInAnyOrder(termosV2, privacidade, confidencialidade);
        assertThat(servico.temPendenciaBloqueante(ana)).isTrue();

        when(aceites.existsByUsuarioIdAndDocumentoId(ana, termosV2.getId())).thenReturn(true);
        when(aceites.existsByUsuarioIdAndDocumentoId(ana, privacidade.getId())).thenReturn(true);

        assertThat(servico.pendentes(ana, true)).containsExactly(confidencialidade);
        assertThat(servico.temPendenciaBloqueante(ana)).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Aceite da vigente para o Nível 2
     * Objetivo: Garantir a resposta da cobrança de confidencialidade e o caso sem versão vigente.
     * Decisões: Sem versão vigente (Política de Publicidade antes da F2), não há o que cobrar.
     */
    @Test
    void aceitouVigente() {
        assertThat(servico.aceitouVigente(ana, TipoDocumento.TERMO_DE_CONFIDENCIALIDADE)).isFalse();
        when(aceites.existsByUsuarioIdAndDocumentoId(ana, confidencialidade.getId())).thenReturn(true);
        assertThat(servico.aceitouVigente(ana, TipoDocumento.TERMO_DE_CONFIDENCIALIDADE)).isTrue();
        assertThat(servico.aceitouVigente(ana, TipoDocumento.POLITICA_DE_PUBLICIDADE)).isTrue();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Registrar aceites
     * Objetivo: Garantir que cada versão vigente é gravada uma vez, com a data do servidor.
     * Decisões: Id repetido no pedido e versão já aceita não duplicam.
     */
    @Test
    void registraCadaVersaoUmaVez() {
        when(aceites.existsByUsuarioIdAndDocumentoId(ana, privacidade.getId())).thenReturn(true);

        servico.aceitar(ana, List.of(termosV2.getId(), termosV2.getId(), privacidade.getId()));

        verify(aceites, times(1)).save(any(AceiteDocumento.class));
        verify(aceites).save(org.mockito.ArgumentMatchers.argThat(a ->
                a.getUsuarioId().equals(ana) && a.getDocumentoId().equals(termosV2.getId())
                        && a.getAceitoEm().equals(AGORA)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Versão que não pode ser aceita
     * Objetivo: Garantir que versão antiga, aviso ou id inexistente invalidam o pedido inteiro.
     * Decisões: Nada é gravado quando um item é inválido.
     */
    @Test
    void recusaVersaoAntigaAvisoOuInexistente() {
        assertThatThrownBy(() -> servico.aceitar(ana, List.of(privacidade.getId(), termosV1.getId())))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> servico.aceitar(ana, List.of(aviso.getId())))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> servico.aceitar(ana, List.of(UUID.randomUUID())))
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("versao_nao_vigente");
        verify(aceites, never()).save(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Histórico de aceites
     * Objetivo: Garantir que o histórico vem do repositório na ordem do mais recente.
     * Decisões: Um aceite registrado.
     */
    @Test
    void historicoDeAceites() {
        AceiteDocumento registro = AceiteDocumento.registrar(ana, termosV2.getId(), AGORA);
        when(aceites.findByUsuarioIdOrderByAceitoEmDesc(ana)).thenReturn(List.of(registro));
        assertThat(servico.historico(ana)).containsExactly(registro);
    }
}
