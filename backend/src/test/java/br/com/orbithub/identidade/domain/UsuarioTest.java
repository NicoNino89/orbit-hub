package br.com.orbithub.identidade.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Testes da conta de usuário
 * Objetivo: Verificar criação, normalização do e-mail, papéis e ativação da conta.
 * Decisões: Testes de domínio puros, sem Spring.
 */
class UsuarioTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T03:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Conta nova
     * Objetivo: Garantir que a conta nasce aguardando confirmação, com e-mail normalizado e nome sem espaços.
     * Decisões: E-mail com maiúsculas e espaços nas pontas.
     */
    @Test
    void contaNovaAguardaConfirmacaoComEmailNormalizado() {
        Usuario usuario = Usuario.novo("  Ana Souza ", "  Ana@Exemplo.COM ", "{bcrypt}x",
                Set.of(Papel.INVESTIDOR), AGORA);

        assertThat(usuario.getId()).isNotNull();
        assertThat(usuario.getNome()).isEqualTo("Ana Souza");
        assertThat(usuario.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(usuario.getSenhaHash()).isEqualTo("{bcrypt}x");
        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.PENDENTE_CONFIRMACAO);
        assertThat(usuario.podeTerSessao()).isFalse();
        assertThat(usuario.getCriadoEm()).isEqualTo(AGORA);
        assertThat(usuario.getAtualizadoEm()).isEqualTo(AGORA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Os dois papéis
     * Objetivo: Garantir que a mesma conta pode ser investidora e empreendedora.
     * Decisões: Regra central do KAN-12.
     */
    @Test
    void aceitaOsDoisPapeis() {
        Usuario usuario = Usuario.novo("Ana", "ana@exemplo.com", "h",
                Set.of(Papel.INVESTIDOR, Papel.EMPREENDEDOR), AGORA);
        assertThat(usuario.getPapeis()).containsExactlyInAnyOrder(Papel.INVESTIDOR, Papel.EMPREENDEDOR);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Papel obrigatório
     * Objetivo: Garantir que não existe conta sem papel.
     * Decisões: Conjunto vazio e nulo.
     */
    @Test
    void exigePeloMenosUmPapel() {
        assertThatThrownBy(() -> Usuario.novo("Ana", "ana@exemplo.com", "h", Set.of(), AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Usuario.novo("Ana", "ana@exemplo.com", "h", null, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Papéis somente leitura
     * Objetivo: Garantir que ninguém altera os papéis por fora das regras.
     * Decisões: Tentativa direta de adicionar.
     */
    @Test
    void papeisSaoSomenteLeitura() {
        Usuario usuario = Usuario.novo("Ana", "ana@exemplo.com", "h", Set.of(Papel.INVESTIDOR), AGORA);
        assertThatThrownBy(() -> usuario.getPapeis().add(Papel.ADMIN))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Confirmação ativa a conta
     * Objetivo: Garantir que confirmar o e-mail libera a sessão e atualiza a data.
     * Decisões: Confirma uma hora depois.
     */
    @Test
    void confirmarEmailAtivaAConta() {
        Usuario usuario = Usuario.novo("Ana", "ana@exemplo.com", "h", Set.of(Papel.INVESTIDOR), AGORA);
        usuario.confirmarEmail(AGORA.plusSeconds(3600));

        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(usuario.podeTerSessao()).isTrue();
        assertThat(usuario.getAtualizadoEm()).isEqualTo(AGORA.plusSeconds(3600));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Normalização de e-mail nulo
     * Objetivo: Garantir que normalizar nulo devolve nulo, sem exceção.
     * Decisões: A validação de obrigatoriedade fica na borda (API).
     */
    @Test
    void normalizarEmailNuloDevolveNulo() {
        assertThat(Usuario.normalizarEmail(null)).isNull();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Papéis públicos e autoridades
     * Objetivo: Garantir quais papéis o usuário escolhe e o nome da autoridade no Spring Security.
     * Decisões: ADMIN nunca é público.
     */
    @Test
    void papeisPublicosEAutoridades() {
        assertThat(Papel.INVESTIDOR.publico()).isTrue();
        assertThat(Papel.EMPREENDEDOR.publico()).isTrue();
        assertThat(Papel.ADMIN.publico()).isFalse();
        assertThat(Papel.EMPREENDEDOR.autoridade()).isEqualTo("ROLE_EMPREENDEDOR");
    }
}
