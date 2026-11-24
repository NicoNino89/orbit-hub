package br.com.orbithub.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.EventoDaConta;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.TokenDeConta;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.EventoDaContaRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Testes do perfil e dos papéis
 * Objetivo: Verificar edição, foto, troca de e-mail e de senha, ativação e desativação de papéis e histórico.
 * Decisões: Dependências simuladas; o histórico gravado fica numa lista para conferência.
 */
class PerfilServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T04:40:00Z");
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    private final List<EventoDaConta> eventos = new ArrayList<>();
    private UsuarioRepository usuarios;
    private SessaoService sessoes;
    private TokenDeContaService links;
    private AceiteService aceites;
    private ArmazenamentoArquivos arquivos;
    private EnvioDeEmail email;
    private PendenciasDePapel pendencias;
    private PerfilService servico;
    private Usuario ana;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Preparação
     * Objetivo: Montar o serviço com uma conta ativa de empreendedora.
     * Decisões: Codificador de senha que compara "hash:" + senha.
     */
    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        sessoes = mock(SessaoService.class);
        links = mock(TokenDeContaService.class);
        aceites = mock(AceiteService.class);
        arquivos = mock(ArmazenamentoArquivos.class);
        email = mock(EnvioDeEmail.class);
        pendencias = mock(PendenciasDePapel.class);
        when(pendencias.pendencia(any(), any())).thenReturn(Optional.empty());
        EventoDaContaRepository historico = mock(EventoDaContaRepository.class);
        when(historico.save(any())).thenAnswer(i -> {
            eventos.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(historico.findByUsuarioIdOrderByOcorridoEmDesc(any())).thenAnswer(i -> eventos);
        PasswordEncoder senhas = mock(PasswordEncoder.class);
        when(senhas.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
        when(senhas.matches(anyString(), anyString())).thenAnswer(i -> ("hash:" + i.getArgument(0)).equals(i.getArgument(1)));
        var propriedades = new ContaProperties("https://web.exemplo", true, Duration.ofHours(24),
                Duration.ofMinutes(30), Duration.ofHours(24));
        servico = new PerfilService(usuarios, historico, senhas, sessoes, links, aceites, arquivos, email,
                List.of(pendencias), propriedades, new RelogioAjustavel(AGORA));

        ana = Usuario.novo("Ana", "ana@exemplo.com", "hash:senha-atual-longa", Set.of(Papel.EMPREENDEDOR), AGORA);
        ana.confirmarEmail(AGORA);
        when(usuarios.findById(ana.getId())).thenReturn(Optional.of(ana));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Editar nome e telefone
     * Objetivo: Garantir a atualização e que telefone vazio apaga o contato.
     * Decisões: Duas edições seguidas.
     */
    @Test
    void editaNomeETelefone() {
        servico.atualizar(ana.getId(), " Ana Lima ", " (11) 99999-0000 ");
        assertThat(ana.getNome()).isEqualTo("Ana Lima");
        assertThat(ana.getTelefone()).isEqualTo("(11) 99999-0000");
        servico.atualizar(ana.getId(), "Ana Lima", " ");
        assertThat(ana.getTelefone()).isNull();
        assertThatThrownBy(() -> servico.conta(UUID.randomUUID())).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Trocar foto
     * Objetivo: Garantir que a imagem é gravada, a anterior apagada e o formato conferido pelo conteúdo.
     * Decisões: Duas fotos PNG e uma tentativa com texto disfarçado.
     */
    @Test
    void trocaFotoConferindoOConteudo() {
        servico.trocarFoto(ana.getId(), PNG);
        String primeira = ana.getFotoChave();
        assertThat(primeira).startsWith("contas/" + ana.getId() + "/foto-").endsWith(".png");
        verify(arquivos).gravar(eq(primeira), any(), eq((long) PNG.length), eq("image/png"));

        servico.trocarFoto(ana.getId(), PNG);
        verify(arquivos).remover(primeira);

        assertThatThrownBy(() -> servico.trocarFoto(ana.getId(), "<html>".getBytes()))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("foto_invalida");
        assertThatThrownBy(() -> servico.trocarFoto(ana.getId(), new byte[0])).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> servico.trocarFoto(ana.getId(), new byte[(int) PerfilService.TAMANHO_MAXIMO_FOTO + 1]))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Ler foto
     * Objetivo: Garantir o tipo da foto lida e o 404 sem foto.
     * Decisões: Foto em WebP.
     */
    @Test
    void lerFoto() {
        assertThatThrownBy(() -> servico.lerFoto(ana.getId())).isInstanceOf(RecursoNaoEncontradoException.class);
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        servico.trocarFoto(ana.getId(), webp);
        when(arquivos.ler(ana.getFotoChave())).thenReturn(new ByteArrayInputStream(webp));
        assertThat(servico.lerFoto(ana.getId()).tipo()).isEqualTo("image/webp");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Pedir troca de e-mail
     * Objetivo: Garantir o link para o novo endereço, o aviso ao antigo e as recusas.
     * Decisões: Senha errada, mesmo e-mail e e-mail em uso são recusados sem enviar nada.
     */
    @Test
    void pedidoDeTrocaDeEmail() {
        when(links.gerar(eq(ana.getId()), eq(TokenDeConta.Finalidade.TROCA_EMAIL), any(), eq("novo@exemplo.com")))
                .thenReturn("tk");
        servico.solicitarTrocaDeEmail(ana.getId(), " Novo@Exemplo.com ", "senha-atual-longa");

        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagens = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, times(2)).enviar(mensagens.capture());
        assertThat(mensagens.getAllValues().get(0).para()).isEqualTo("novo@exemplo.com");
        assertThat(mensagens.getAllValues().get(0).texto()).contains("https://web.exemplo/confirmar-novo-email?token=tk");
        assertThat(mensagens.getAllValues().get(1).para()).isEqualTo("ana@exemplo.com");

        assertThatThrownBy(() -> servico.solicitarTrocaDeEmail(ana.getId(), "x@exemplo.com", "errada"))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("senha_atual_incorreta");
        assertThatThrownBy(() -> servico.solicitarTrocaDeEmail(ana.getId(), "ANA@exemplo.com", "senha-atual-longa"))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("email_igual");
        when(usuarios.existsByEmail("bia@exemplo.com")).thenReturn(true);
        assertThatThrownBy(() -> servico.solicitarTrocaDeEmail(ana.getId(), "bia@exemplo.com", "senha-atual-longa"))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("email_em_uso");
        verify(email, times(2)).enviar(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Confirmar troca de e-mail
     * Objetivo: Garantir que o link aplica o novo e-mail, registra no histórico e recusa e-mail tomado.
     * Decisões: Token devolvido pelo serviço de links simulado.
     */
    @Test
    void confirmaTrocaDeEmail() {
        TokenDeConta link = TokenDeConta.emitir(ana.getId(), TokenDeConta.Finalidade.TROCA_EMAIL, "h", "novo@exemplo.com",
                AGORA, Duration.ofHours(1));
        when(links.consumir("tk", TokenDeConta.Finalidade.TROCA_EMAIL)).thenReturn(link);

        servico.confirmarTrocaDeEmail("tk");
        assertThat(ana.getEmail()).isEqualTo("novo@exemplo.com");
        assertThat(eventos).extracting(EventoDaConta::getEvento).containsExactly(EventoDaConta.Tipo.EMAIL_ALTERADO);

        when(usuarios.existsByEmail("novo@exemplo.com")).thenReturn(true);
        assertThatThrownBy(() -> servico.confirmarTrocaDeEmail("tk")).isInstanceOf(RegraDeNegocioException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Trocar senha
     * Objetivo: Garantir que exige a atual, aplica a política, mantém a sessão atual e derruba as outras.
     * Decisões: Senha atual errada e senha nova fraca são recusadas.
     */
    @Test
    void trocaSenha() {
        UUID sessaoAtual = UUID.randomUUID();
        assertThatThrownBy(() -> servico.trocarSenha(ana.getId(), sessaoAtual, "errada", "nova-senha-longa"))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("senha_atual_incorreta");
        assertThatThrownBy(() -> servico.trocarSenha(ana.getId(), sessaoAtual, "senha-atual-longa", "1234567890"))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("senha_fraca");

        servico.trocarSenha(ana.getId(), sessaoAtual, "senha-atual-longa", "nova-senha-bem-longa");
        assertThat(ana.getSenhaHash()).isEqualTo("hash:nova-senha-bem-longa");
        verify(sessoes).encerrarOutras(ana.getId(), sessaoAtual, "senha_alterada");
        assertThat(eventos).extracting(EventoDaConta::getEvento).containsExactly(EventoDaConta.Tipo.SENHA_ALTERADA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Ativar investidor
     * Objetivo: Garantir que o papel de investidor exige a confidencialidade e devolve o onboarding.
     * Decisões: Primeiro sem aceite (recusa); depois com o aceite informado.
     */
    @Test
    void ativarInvestidorExigeConfidencialidade() {
        UUID termo = UUID.randomUUID();
        assertThatThrownBy(() -> servico.ativarPapel(ana.getId(), Papel.INVESTIDOR, List.of()))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("aceite_obrigatorio");
        assertThat(ana.getPapeis()).doesNotContain(Papel.INVESTIDOR);

        when(aceites.aceitouVigente(ana.getId(), TipoDocumento.TERMO_DE_CONFIDENCIALIDADE)).thenReturn(true);
        String proximo = servico.ativarPapel(ana.getId(), Papel.INVESTIDOR, List.of(termo));

        verify(aceites).aceitar(ana.getId(), List.of(termo));
        assertThat(proximo).isEqualTo("/investidor/onboarding");
        assertThat(ana.getPapeis()).contains(Papel.INVESTIDOR, Papel.EMPREENDEDOR);
        assertThat(eventos).extracting(EventoDaConta::getDetalhe).containsExactly("INVESTIDOR");

        servico.ativarPapel(ana.getId(), Papel.INVESTIDOR, null);
        assertThat(eventos).hasSize(1);
        assertThatThrownBy(() -> servico.ativarPapel(ana.getId(), Papel.ADMIN, null))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("papel_invalido");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Desativar papel
     * Objetivo: Garantir as recusas por último papel e por pendência e a desativação com histórico.
     * Decisões: A pendência vem do verificador simulado (Strategy).
     */
    @Test
    void desativarPapel() {
        assertThatThrownBy(() -> servico.desativarPapel(ana.getId(), Papel.EMPREENDEDOR))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("ultimo_papel");

        ana.ativarPapel(Papel.INVESTIDOR, AGORA);
        when(pendencias.pendencia(ana.getId(), Papel.EMPREENDEDOR)).thenReturn(Optional.of("1 rodada aberta"));
        assertThatThrownBy(() -> servico.desativarPapel(ana.getId(), Papel.EMPREENDEDOR))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("1 rodada aberta");

        when(pendencias.pendencia(ana.getId(), Papel.EMPREENDEDOR)).thenReturn(Optional.empty());
        servico.desativarPapel(ana.getId(), Papel.EMPREENDEDOR);
        assertThat(ana.getPapeis()).containsExactly(Papel.INVESTIDOR);
        assertThat(servico.historico(ana.getId())).extracting(EventoDaConta::getEvento)
                .containsExactly(EventoDaConta.Tipo.PAPEL_DESATIVADO);

        servico.desativarPapel(ana.getId(), Papel.EMPREENDEDOR);
        assertThat(eventos).hasSize(1);
        verify(arquivos, never()).gravar(anyString(), any(), anyLong(), anyString());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Regras do domínio de papéis
     * Objetivo: Garantir que a entidade recusa papel interno e a remoção do último papel.
     * Decisões: Chamadas diretas na entidade.
     */
    @Test
    void regrasDaEntidade() {
        assertThatThrownBy(() -> ana.ativarPapel(Papel.ADMIN, AGORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ana.desativarPapel(Papel.EMPREENDEDOR, AGORA)).isInstanceOf(IllegalStateException.class);
        assertThat(FormatoDeImagem.detectar(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})).contains("image/jpeg");
        assertThat(FormatoDeImagem.extensao("image/jpeg")).isEqualTo("jpg");
        assertThat(FormatoDeImagem.tipoDaExtensao("jpg")).isEqualTo("image/jpeg");
        assertThat(FormatoDeImagem.tipoDaExtensao("png")).isEqualTo("image/png");
    }
}
