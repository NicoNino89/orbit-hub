package br.com.orbithub.investidores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.orbithub.investidores.domain.MembroDaOrganizacao;
import br.com.orbithub.investidores.domain.PapelNaOrganizacao;
import br.com.orbithub.investidores.repository.MembroDaOrganizacaoRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Testes da atuação em nome de organização
 * Objetivo: Conferir a atuação pessoal, a atuação pela organização, a recusa do observador para agir, do cabeçalho
 *           inválido e de quem não é (mais) membro.
 * Decisões: Repositório simulado.
 */
class AtuacaoServiceTest {

    static final UUID ANA = UUID.randomUUID();
    static final UUID ORG = UUID.randomUUID();

    MembroDaOrganizacaoRepository membros = mock(MembroDaOrganizacaoRepository.class);
    AtuacaoService atuacao = new AtuacaoService(membros);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Pessoal e pela organização
     * Objetivo: Sem cabeçalho a ação é pessoal; com vínculo de membro, é da organização e o membro pode agir.
     * Decisões: A pessoa fica registrada nos dois casos.
     */
    @Test
    void pessoalEPelaOrganizacao() {
        AtuacaoService.Atuacao pessoal = atuacao.paraAgir(ANA, " ");
        assertThat(pessoal.emNomeDaOrganizacao()).isFalse();
        assertThat(pessoal.papel()).isEmpty();
        assertThat(pessoal.pessoa()).isEqualTo(ANA);

        vinculo(PapelNaOrganizacao.MEMBRO);
        AtuacaoService.Atuacao pelaOrg = atuacao.paraAgir(ANA, ORG.toString());
        assertThat(pelaOrg.emNomeDaOrganizacao()).isTrue();
        assertThat(pelaOrg.organizacao()).isEqualTo(ORG);
        assertThat(pelaOrg.papel()).contains(PapelNaOrganizacao.MEMBRO);
        assertThat(pelaOrg.pessoa()).isEqualTo(ANA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Recusas
     * Objetivo: Observador vê mas não age; cabeçalho que não é identificador e quem não tem vínculo recebem 403.
     * Decisões: AccessDeniedException vira 403 registrado.
     */
    @Test
    void recusas() {
        vinculo(PapelNaOrganizacao.OBSERVADOR);
        assertThat(atuacao.paraVer(ANA, ORG.toString()).papel()).contains(PapelNaOrganizacao.OBSERVADOR);
        assertThatThrownBy(() -> atuacao.paraAgir(ANA, ORG.toString())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> atuacao.paraVer(ANA, "nao-e-uuid")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> atuacao.paraVer(UUID.randomUUID(), ORG.toString())).isInstanceOf(AccessDeniedException.class);
        assertThat(PapelNaOrganizacao.ADMINISTRADOR.podeAtuar()).isTrue();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Vínculo simulado
     * Objetivo: Fazer o repositório devolver o vínculo de Ana com a organização no papel informado.
     * Decisões: Outras combinações devolvem vazio.
     */
    private void vinculo(PapelNaOrganizacao papel) {
        when(membros.findByOrganizacaoIdAndUsuarioIdAndSaiuEmIsNull(ORG, ANA))
                .thenReturn(Optional.of(MembroDaOrganizacao.novo(ORG, ANA, papel, Instant.now())));
    }
}
