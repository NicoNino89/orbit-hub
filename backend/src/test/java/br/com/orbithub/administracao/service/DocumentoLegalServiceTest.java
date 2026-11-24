package br.com.orbithub.administracao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.repository.DocumentoLegalRepository;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.time.Duration;
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
 * Título: Testes da publicação e consulta de documentos legais
 * Objetivo: Verificar a versão vigente, o histórico público, a numeração e as regras de vigência.
 * Decisões: Repositório simulado; relógio fixo.
 */
class DocumentoLegalServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T03:00:00Z");

    private DocumentoLegalRepository repositorio;
    private DocumentoLegalService servico;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Preparação
     * Objetivo: Montar o serviço com repositório simulado.
     * Decisões: O save devolve o próprio documento.
     */
    @BeforeEach
    void preparar() {
        repositorio = mock(DocumentoLegalRepository.class);
        when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));
        servico = new DocumentoLegalService(repositorio, new RelogioAjustavel(AGORA));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Primeira e próxima versão
     * Objetivo: Garantir que a numeração começa em 1 e segue sequencial.
     * Decisões: Sem versão anterior e com versão 3 existente.
     */
    @Test
    void numeraVersoesEmSequencia() {
        when(repositorio.maiorVersao(TipoDocumento.TERMOS_DE_USO)).thenReturn(null);
        assertThat(servico.publicar(TipoDocumento.TERMOS_DE_USO, "Termos", "Texto", null, null).getVersao()).isEqualTo(1);

        when(repositorio.maiorVersao(TipoDocumento.TERMOS_DE_USO)).thenReturn(3);
        DocumentoLegal quarta = servico.publicar(TipoDocumento.TERMOS_DE_USO, " Termos ", " Texto ",
                AGORA.plus(Duration.ofDays(7)), UUID.randomUUID());
        assertThat(quarta.getVersao()).isEqualTo(4);
        assertThat(quarta.getTitulo()).isEqualTo("Termos");
        assertThat(quarta.getConteudo()).isEqualTo("Texto");
        assertThat(quarta.getVigenteDesde()).isEqualTo(AGORA.plus(Duration.ofDays(7)));
        assertThat(quarta.getPublicadoEm()).isEqualTo(AGORA);
        assertThat(quarta.getPublicadoPor()).isNotNull();
        assertThat(quarta.emVigor(AGORA)).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Sem data, vale agora
     * Objetivo: Garantir que a publicação sem vigência entra em vigor na hora.
     * Decisões: Confere com emVigor no mesmo instante.
     */
    @Test
    void semVigenciaValeAgora() {
        DocumentoLegal documento = servico.publicar(TipoDocumento.POLITICA_DE_PRIVACIDADE, "P", "T", null, null);
        assertThat(documento.getVigenteDesde()).isEqualTo(AGORA);
        assertThat(documento.emVigor(AGORA)).isTrue();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Vigência no passado
     * Objetivo: Garantir que não se publica retroativamente, mas aceita a folga de 5 minutos.
     * Decisões: Uma hora atrás recusa; dois minutos atrás aceita.
     */
    @Test
    void recusaVigenciaNoPassadoForaDaFolga() {
        assertThatThrownBy(() -> servico.publicar(TipoDocumento.TERMOS_DE_USO, "T", "X",
                AGORA.minus(Duration.ofHours(1)), null))
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("vigencia_no_passado");
        assertThat(servico.publicar(TipoDocumento.TERMOS_DE_USO, "T", "X", AGORA.minus(Duration.ofMinutes(2)), null))
                .isNotNull();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Texto obrigatório
     * Objetivo: Garantir que título e conteúdo vazios são recusados.
     * Decisões: Título em branco e conteúdo nulo.
     */
    @Test
    void exigeTituloEConteudo() {
        assertThatThrownBy(() -> servico.publicar(TipoDocumento.TERMOS_DE_USO, " ", "X", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servico.publicar(TipoDocumento.TERMOS_DE_USO, "T", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Vigentes de todos os tipos
     * Objetivo: Garantir que a lista traz só os tipos com versão em vigor.
     * Decisões: Só os Termos de Uso têm versão.
     */
    @Test
    void vigentesTrazSoTiposComVersao() {
        DocumentoLegal termos = documento(TipoDocumento.TERMOS_DE_USO, 1, AGORA.minusSeconds(60));
        when(repositorio.findFirstByTipoAndVigenteDesdeLessThanEqualOrderByVersaoDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(repositorio.findFirstByTipoAndVigenteDesdeLessThanEqualOrderByVersaoDesc(TipoDocumento.TERMOS_DE_USO, AGORA))
                .thenReturn(Optional.of(termos));

        assertThat(servico.vigentes()).containsExactly(termos);
        assertThat(servico.vigente(TipoDocumento.TERMOS_DE_USO)).contains(termos);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Histórico público sem versões futuras
     * Objetivo: Garantir que o público vê versões antigas e a vigente, mas não a agendada.
     * Decisões: Três versões: 1 antiga, 2 vigente, 3 agendada. A equipe vê as três.
     */
    @Test
    void historicoPublicoEscondeVersaoAgendada() {
        DocumentoLegal v1 = documento(TipoDocumento.TERMOS_DE_USO, 1, AGORA.minus(Duration.ofDays(30)));
        DocumentoLegal v2 = documento(TipoDocumento.TERMOS_DE_USO, 2, AGORA.minus(Duration.ofDays(1)));
        DocumentoLegal v3 = documento(TipoDocumento.TERMOS_DE_USO, 3, AGORA.plus(Duration.ofDays(7)));
        when(repositorio.findByTipoOrderByVersaoDesc(TipoDocumento.TERMOS_DE_USO)).thenReturn(List.of(v3, v2, v1));

        assertThat(servico.historico(TipoDocumento.TERMOS_DE_USO)).containsExactly(v2, v1);
        assertThat(servico.historicoCompleto(TipoDocumento.TERMOS_DE_USO)).containsExactly(v3, v2, v1);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Buscar uma versão
     * Objetivo: Garantir que a versão em vigor é devolvida e a agendada ou inexistente vira 404.
     * Decisões: Três buscas.
     */
    @Test
    void buscarPublicadoEscondeAgendadaEInexistente() {
        DocumentoLegal vigente = documento(TipoDocumento.TERMOS_DE_USO, 1, AGORA.minusSeconds(1));
        DocumentoLegal agendada = documento(TipoDocumento.TERMOS_DE_USO, 2, AGORA.plusSeconds(60));
        when(repositorio.findById(vigente.getId())).thenReturn(Optional.of(vigente));
        when(repositorio.findById(agendada.getId())).thenReturn(Optional.of(agendada));

        assertThat(servico.buscarPublicado(vigente.getId())).isSameAs(vigente);
        assertThatThrownBy(() -> servico.buscarPublicado(agendada.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.buscarPublicado(UUID.randomUUID()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Regras de aceite por tipo
     * Objetivo: Garantir quem aceita cada tipo.
     * Decisões: Termos e privacidade bloqueiam o acesso; confidencialidade é do investidor; avisos não têm aceite.
     */
    @Test
    void regrasDeAceitePorTipo() {
        assertThat(TipoDocumento.TERMOS_DE_USO.aceite()).isEqualTo(TipoDocumento.Aceite.TODOS_NO_ACESSO);
        assertThat(TipoDocumento.TERMO_DE_CONFIDENCIALIDADE.aceite())
                .isEqualTo(TipoDocumento.Aceite.INVESTIDOR_ANTES_DO_NIVEL_2);
        assertThat(TipoDocumento.AVISO_NAO_COMPROMISSO.exigeAceite()).isFalse();
        assertThat(TipoDocumento.POLITICA_DE_PRIVACIDADE.exigeAceite()).isTrue();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Documento de apoio
     * Objetivo: Criar uma versão de teste com a vigência informada.
     * Decisões: Publicado agora, sem autor.
     */
    static DocumentoLegal documento(TipoDocumento tipo, int versao, Instant vigenteDesde) {
        return DocumentoLegal.publicar(tipo, versao, "Título", "Texto", vigenteDesde, AGORA, null);
    }
}
