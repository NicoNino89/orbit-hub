package br.com.orbithub.catalogo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Testes da edição temática
 * Objetivo: Conferir o ciclo rascunho → agendada → vigente → arquivada e as recusas.
 * Decisões: Datas fixas.
 */
class EdicaoTest {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 25);
    static final Instant AGORA = Instant.parse("2026-11-25T14:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Ciclo pela vigência
     * Objetivo: Publicar antes do início (agendada), entrar no ar e arquivar no fim.
     * Decisões: seguirVigencia devolve se mudou.
     */
    @Test
    void cicloPelaVigencia() {
        Edicao e = Edicao.nova(new Edicao.Dados(" Verde ", "Desc", "Crit", HOJE.plusDays(2), HOJE.plusDays(5)), UUID.randomUUID(), AGORA);
        assertThat(e.getTitulo()).isEqualTo("Verde");
        e.definirStartups(List.of(UUID.randomUUID()), AGORA);
        e.publicar(HOJE, AGORA);
        assertThat(e.getSituacao()).isEqualTo(Edicao.Situacao.AGENDADA);
        assertThat(e.seguirVigencia(HOJE, AGORA)).isFalse();
        assertThat(e.seguirVigencia(HOJE.plusDays(2), AGORA)).isTrue();
        assertThat(e.getSituacao()).isEqualTo(Edicao.Situacao.VIGENTE);
        assertThat(e.seguirVigencia(HOJE.plusDays(6), AGORA)).isTrue();
        assertThat(e.getSituacao()).isEqualTo(Edicao.Situacao.ARQUIVADA);
        assertThatThrownBy(() -> e.trocarCapa("x", "image/png", AGORA)).isInstanceOf(RegraDeNegocioException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Recusas
     * Objetivo: Recusar vigência invertida, edição vazia, vigência encerrada e republicação.
     * Decisões: Códigos de erro.
     */
    @Test
    void recusas() {
        assertThatThrownBy(() -> Edicao.nova(new Edicao.Dados("T", "D", "C", HOJE, HOJE.minusDays(1)), UUID.randomUUID(), AGORA))
                .hasMessageContaining("antes do início");
        Edicao e = Edicao.nova(new Edicao.Dados("T", "D", "C", HOJE.minusDays(5), HOJE.minusDays(1)), UUID.randomUUID(), AGORA);
        assertThatThrownBy(() -> e.publicar(HOJE, AGORA)).hasMessageContaining("ao menos uma startup");
        e.definirStartups(List.of(UUID.randomUUID()), AGORA);
        assertThatThrownBy(() -> e.publicar(HOJE, AGORA)).hasMessageContaining("já terminou");
        e.editar(new Edicao.Dados("T", "D", "C", HOJE, HOJE), AGORA);
        e.publicar(HOJE, AGORA);
        assertThat(e.getSituacao()).isEqualTo(Edicao.Situacao.VIGENTE);
        assertThatThrownBy(() -> e.publicar(HOJE, AGORA)).hasMessageContaining("já foi publicada");
        e.arquivar(AGORA);
        assertThatThrownBy(() -> e.arquivar(AGORA)).hasMessageContaining("arquivada");
    }
}
