package br.com.orbithub.auditoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Acao;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Resultado;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Testes da trilha de auditoria
 * Objetivo: Garantir a cópia do nome do autor, o momento do registro e as regras da consulta.
 * Decisões: Mockito nos repositórios; relógio fixo.
 */
class TrilhaDeAuditoriaTest {

    static final Instant AGORA = Instant.parse("2026-11-24T13:00:00Z");

    RegistroAuditoriaRepository registros;
    ConsultaDeContas usuarios;
    TrilhaDeAuditoria trilha;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Preparação
     * Objetivo: Montar a trilha com repositórios simulados e relógio fixo.
     * Decisões: O repositório devolve o registro gravado.
     */
    @BeforeEach
    void preparar() {
        registros = mock(RegistroAuditoriaRepository.class);
        usuarios = mock(ConsultaDeContas.class);
        when(registros.save(any())).thenAnswer(i -> i.getArgument(0));
        trilha = new TrilhaDeAuditoria(registros, usuarios, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Nome do autor copiado
     * Objetivo: Conferir que o nome da conta é gravado junto com o registro.
     * Decisões: O momento vem do relógio do serviço, não de quem chama.
     */
    @Test
    void copiaONomeDoAutorEUsaORelogio() {
        Usuario bia = Usuario.novo("Bia Lima", "bia@orbithub.com", "{noop}x", Set.of(Papel.ADMIN), AGORA);
        when(usuarios.nome(bia.getId())).thenReturn(Optional.of(bia.getNome()));

        RegistroAuditoria registro = trilha.registrar(new Acao(bia.getId(), null, "DOCUMENTO_LEGAL_PUBLICADO",
                "documento_legal", "1", Resultado.SUCESSO, 201, null, null));

        assertThat(registro.getAtorNome()).isEqualTo("Bia Lima");
        assertThat(registro.getOcorridoEm()).isEqualTo(AGORA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Autor sem conta ou ação do sistema
     * Objetivo: Gravar mesmo quando a conta não existe e não buscar conta em ação do sistema.
     * Decisões: Nome nulo nesses casos.
     */
    @Test
    void gravaSemNomeQuandoNaoHaConta() {
        UUID desconhecido = UUID.randomUUID();
        when(usuarios.nome(desconhecido)).thenReturn(Optional.empty());
        assertThat(trilha.registrar(new Acao(desconhecido, null, "X", null, null, Resultado.FALHA, 403, null, null))
                .getAtorNome()).isNull();

        trilha.registrar(new Acao(null, null, "ROTINA_EXECUTADA", null, null, Resultado.SUCESSO, null, null, null));
        verify(usuarios, never()).nome(null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Regras da consulta
     * Objetivo: Recusar período invertido e limitar o tamanho da página.
     * Decisões: Ordem do mais recente para o mais antigo.
     */
    @Test
    @SuppressWarnings("unchecked")
    void validaPeriodoELimitaAPagina() {
        assertThatThrownBy(() -> trilha.consultar(new TrilhaDeAuditoria.Filtro(null, null, null, null, AGORA,
                AGORA.minusSeconds(60)), 0, 10))
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting("codigo").isEqualTo("periodo_invalido");

        when(registros.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(i -> new PageImpl<RegistroAuditoria>(List.of(), (Pageable) i.getArgument(1), 0));
        Page<RegistroAuditoria> pagina = trilha.consultar(new TrilhaDeAuditoria.Filtro(null, " ", null, null, null,
                null), -3, 5000);

        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        verify(registros).findAll(any(Specification.class), pedido.capture());
        assertThat(pedido.getValue().getPageSize()).isEqualTo(TrilhaDeAuditoria.TAMANHO_MAXIMO_PAGINA);
        assertThat(pedido.getValue().getPageNumber()).isZero();
        assertThat(pedido.getValue().getSort().getOrderFor("ocorridoEm").isDescending()).isTrue();
        assertThat(pagina.getContent()).isEmpty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Ações e pessoas conhecidas
     * Objetivo: Conferir que as listas de ações e de pessoas para os filtros vêm do repositório.
     * Decisões: Sem transformação.
     */
    @Test
    void listaAsAcoesEAsPessoasRegistradas() {
        when(registros.acoesRegistradas()).thenReturn(List.of("A", "B"));
        assertThat(trilha.acoes()).containsExactly("A", "B");
        RegistroAuditoriaRepository.Ator bia = new RegistroAuditoriaRepository.Ator(UUID.randomUUID(), "Bia");
        when(registros.atoresRegistrados()).thenReturn(List.of(bia));
        assertThat(trilha.atores()).containsExactly(bia);
    }
}
