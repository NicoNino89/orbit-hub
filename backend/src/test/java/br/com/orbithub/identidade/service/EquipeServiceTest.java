package br.com.orbithub.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.compartilhado.parametros.ParametrosFixos;
import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.RevisaoDeAcesso;
import br.com.orbithub.identidade.domain.TokenDeConta.Finalidade;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.RevisaoDeAcessoRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Testes das regras da gestão da equipe
 * Objetivo: Garantir as regras que a tela não consegue garantir sozinha: e-mail próprio, nunca ficar sem
 *           administrador, ninguém se desliga sozinho, casos devolvidos à fila, sessões derrubadas e o
 *           procedimento idempotente do primeiro administrador.
 * Decisões: Mockito nos repositórios e serviços; uma fila de casos de teste.
 */
class EquipeServiceTest {

    static final Instant AGORA = Instant.parse("2026-11-24T14:00:00Z");

    UsuarioRepository usuarios;
    RevisaoDeAcessoRepository revisoes;
    TokenDeContaService links;
    SessaoService sessoes;
    EnvioDeEmail email;
    EquipeService servico;
    Usuario admin;
    Usuario curadora;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Preparação
     * Objetivo: Montar o serviço com um administrador e uma curadora ativos e uma fila que devolve 2 casos.
     * Decisões: Revisão a cada 3 meses e convite de 14 dias, os padrões do catálogo.
     */
    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        revisoes = mock(RevisaoDeAcessoRepository.class);
        links = mock(TokenDeContaService.class);
        sessoes = mock(SessaoService.class);
        email = mock(EnvioDeEmail.class);
        PasswordEncoder senhas = mock(PasswordEncoder.class);
        when(senhas.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));
        when(revisoes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(links.gerar(any(), eq(Finalidade.CONVITE_EQUIPE), any(), any())).thenReturn("tok");
        CasosDoMembro curadoria = new CasosDoMembro() {
            @Override
            public String fila() {
                return "curadoria";
            }

            @Override
            public int devolverParaFila(UUID membroId) {
                return 2;
            }
        };
        servico = new EquipeService(usuarios, revisoes, senhas, links, sessoes, email,
                new ContaProperties("https://web", true), new ParametrosFixos(),
                List.of(curadoria), new RelogioAjustavel(AGORA));
        admin = ativo("Bia Admin", "bia@orbithub.com", Papel.ADMIN);
        curadora = ativo("Cris Curadora", "cris@orbithub.com", Papel.CURADOR);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Convite com e-mail próprio
     * Objetivo: Conferir que o convite cria a conta interna pendente, manda o link e recusa e-mail já usado.
     * Decisões: O texto do e-mail leva o link da web e os papéis.
     */
    @Test
    void convidaComEmailProprioEMandaOLink() {
        when(usuarios.existsByEmail("ana@exemplo.com")).thenReturn(true);
        assertThatThrownBy(() -> servico.convidar("Ana", "Ana@Exemplo.com", Set.of(Papel.SUPORTE_N1)))
                .extracting("codigo").isEqualTo("email_em_uso");
        assertThatThrownBy(() -> servico.convidar("Davi", "davi@orbithub.com", Set.of(Papel.INVESTIDOR)))
                .extracting("codigo").isEqualTo("papeis_invalidos");

        Usuario davi = servico.convidar("Davi", "davi@orbithub.com", Set.of(Papel.SUPORTE_N1, Papel.SUPORTE_N2));
        assertThat(davi.isInterna()).isTrue();
        assertThat(davi.getSenhaHash()).startsWith("hash:");
        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email).enviar(mensagem.capture());
        assertThat(mensagem.getValue().para()).isEqualTo("davi@orbithub.com");
        assertThat(mensagem.getValue().texto()).contains("https://web/equipe/convite?token=tok")
                .contains("SUPORTE_N1, SUPORTE_N2").contains("14 dias");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Nunca sem administrador
     * Objetivo: Conferir que o último administrador ativo não perde o papel nem é desligado.
     * Decisões: Com dois administradores, a troca é permitida e derruba as sessões.
     */
    @Test
    void naoDeixaAPlataformaSemAdministrador() {
        UUID outroAdmin = UUID.randomUUID();
        when(usuarios.contarEquipeAtivaComPapel(Papel.ADMIN)).thenReturn(1L);
        assertThatThrownBy(() -> servico.alterarPapeis(admin.getId(), Set.of(Papel.CURADOR)))
                .extracting("codigo").isEqualTo("ultimo_administrador");
        assertThatThrownBy(() -> servico.desligar(outroAdmin, admin.getId(), "Saiu da empresa"))
                .extracting("codigo").isEqualTo("ultimo_administrador");

        when(usuarios.contarEquipeAtivaComPapel(Papel.ADMIN)).thenReturn(2L);
        servico.alterarPapeis(admin.getId(), Set.of(Papel.CURADOR, Papel.JURIDICO));
        assertThat(admin.getPapeis()).containsExactlyInAnyOrder(Papel.CURADOR, Papel.JURIDICO);
        verify(sessoes).encerrarTodas(admin.getId(), "papeis_alterados");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Desligamento
     * Objetivo: Conferir que o desligamento exige motivo, não vale para si mesmo, derruba as sessões e devolve
     *           os casos à fila.
     * Decisões: A fila de teste devolve 2 casos.
     */
    @Test
    void desligaDerrubaSessoesEDevolveCasos() {
        assertThatThrownBy(() -> servico.desligar(admin.getId(), curadora.getId(), " "))
                .extracting("codigo").isEqualTo("justificativa_obrigatoria");
        assertThatThrownBy(() -> servico.desligar(admin.getId(), admin.getId(), "Teste"))
                .extracting("codigo").isEqualTo("auto_desligamento");

        int devolvidos = servico.desligar(admin.getId(), curadora.getId(), "Fim do contrato");
        assertThat(devolvidos).isEqualTo(2);
        assertThat(curadora.getDesligadoEm()).isEqualTo(AGORA);
        assertThat(curadora.podeTerSessao()).isFalse();
        verify(sessoes).encerrarTodas(curadora.getId(), "desligamento");
        assertThatThrownBy(() -> servico.alterarPapeis(curadora.getId(), Set.of(Papel.CURADOR)))
                .extracting("codigo").isEqualTo("membro_desligado");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Só contas da equipe
     * Objetivo: Conferir que as rotas da equipe não enxergam contas públicas.
     * Decisões: Resposta 404, sem revelar a conta.
     */
    @Test
    void naoMexeEmContaPublica() {
        Usuario ana = Usuario.novo("Ana", "ana@exemplo.com", "h", Set.of(Papel.INVESTIDOR), AGORA);
        when(usuarios.findById(ana.getId())).thenReturn(Optional.of(ana));
        assertThatThrownBy(() -> servico.alterarPapeis(ana.getId(), Set.of(Papel.ADMIN)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.redefinirSegundoFator(admin.getId(), ana.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.redefinirSegundoFator(admin.getId(), admin.getId()))
                .extracting("codigo").isEqualTo("auto_redefinicao");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Revisão de acessos
     * Objetivo: Conferir que sem revisão ela já vence, que a confirmação fotografa só os ativos e que a próxima
     *           cai 3 meses depois.
     * Decisões: Convidado pendente não entra na fotografia.
     */
    @Test
    void conduzARevisaoDeAcessos() {
        when(revisoes.findFirstByOrderByRealizadaEmDesc()).thenReturn(Optional.empty());
        assertThat(servico.situacaoRevisao().vencida()).isTrue();

        Usuario convidado = Usuario.novoInterno("Edu", "edu@orbithub.com", "h", Set.of(Papel.JURIDICO), AGORA);
        when(usuarios.findByInternaTrueOrderByNomeAsc()).thenReturn(List.of(admin, curadora, convidado));
        RevisaoDeAcesso revisao = servico.confirmarRevisao(admin.getId(), "Revisão do trimestre");
        assertThat(revisao.getItens()).extracting(RevisaoDeAcesso.Item::nome)
                .containsExactly("Bia Admin", "Cris Curadora");
        assertThat(revisao.getItens().get(0).papeis()).isEqualTo("ADMIN");

        when(revisoes.findFirstByOrderByRealizadaEmDesc()).thenReturn(Optional.of(revisao));
        EquipeService.SituacaoRevisao situacao = servico.situacaoRevisao();
        assertThat(situacao.vencida()).isFalse();
        assertThat(situacao.proximaEm()).isEqualTo(Instant.parse("2027-02-24T14:00:00Z"));
        assertThat(situacao.ultimoRevisor()).isEqualTo("Bia Admin");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Primeiro administrador
     * Objetivo: Conferir que o procedimento cria o administrador só quando não há nenhum ativo e recusa e-mail
     *           de conta pública.
     * Decisões: Devolve o link do convite.
     */
    @Test
    void criaOPrimeiroAdministradorUmaVez() {
        when(usuarios.contarEquipeAtivaComPapel(Papel.ADMIN)).thenReturn(1L);
        assertThat(servico.garantirPrimeiroAdministrador("Fábio", "fabio@orbithub.com")).isEmpty();
        verify(usuarios, never()).save(any());

        when(usuarios.contarEquipeAtivaComPapel(Papel.ADMIN)).thenReturn(0L);
        when(usuarios.findByEmail("fabio@orbithub.com")).thenReturn(Optional.empty());
        assertThat(servico.garantirPrimeiroAdministrador("Fábio", "Fabio@orbithub.com"))
                .hasValue("https://web/equipe/convite?token=tok");
        ArgumentCaptor<Usuario> criado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(criado.capture());
        assertThat(criado.getValue().getPapeis()).containsExactly(Papel.ADMIN);
        assertThat(criado.getValue().isInterna()).isTrue();

        Usuario publica = Usuario.novo("Gil", "gil@exemplo.com", "h", Set.of(Papel.EMPREENDEDOR), AGORA);
        when(usuarios.findByEmail("gil@exemplo.com")).thenReturn(Optional.of(publica));
        assertThatThrownBy(() -> servico.garantirPrimeiroAdministrador("Gil", "gil@exemplo.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Convite reenviado só para pendentes
     * Objetivo: Conferir que quem já aceitou não recebe convite de novo.
     * Decisões: Regra de negócio com código próprio.
     */
    @Test
    void reenviaConviteSoParaQuemNaoAceitou() {
        assertThatThrownBy(() -> servico.reenviarConvite(curadora.getId()))
                .isInstanceOf(RegraDeNegocioException.class).extracting("codigo").isEqualTo("convite_aceito");
        Usuario convidado = Usuario.novoInterno("Edu", "edu@orbithub.com", "h", Set.of(Papel.JURIDICO), AGORA);
        when(usuarios.findById(convidado.getId())).thenReturn(Optional.of(convidado));
        servico.reenviarConvite(convidado.getId());
        verify(email).enviar(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Membro ativo de teste
     * Objetivo: Criar um membro que já aceitou o convite e registrá-lo no repositório simulado.
     * Decisões: findById devolve o membro.
     */
    private Usuario ativo(String nome, String emailDoMembro, Papel papel) {
        Usuario membro = Usuario.novoInterno(nome, emailDoMembro, "h", Set.of(papel), AGORA);
        membro.aceitarConvite("h1", AGORA);
        when(usuarios.findById(membro.getId())).thenReturn(Optional.of(membro));
        return membro;
    }
}
