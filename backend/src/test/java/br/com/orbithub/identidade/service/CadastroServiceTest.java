package br.com.orbithub.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.administracao.service.DocumentoLegalService;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.StatusUsuario;
import br.com.orbithub.identidade.domain.TokenDeConta;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.TokenDeContaRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
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
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Testes do cadastro e dos links de conta
 * Objetivo: Verificar validações, criação, aceites, envio e confirmação do e-mail e reenvio.
 * Decisões: Serviço de links real sobre repositório simulado em memória, para o link enviado no e-mail
 *           poder ser usado de verdade na confirmação.
 */
class CadastroServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T04:00:00Z");

    private final List<TokenDeConta> tokensGravados = new ArrayList<>();
    private UsuarioRepository usuarios;
    private AceiteService aceites;
    private EnvioDeEmail email;
    private RelogioAjustavel relogio;
    private DocumentoLegal termos;
    private DocumentoLegal privacidade;
    private DocumentoLegal confidencialidade;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Preparação
     * Objetivo: Simular repositórios, documentos vigentes e envio de e-mail.
     * Decisões: Codificador de senha falso ("hash:" + senha) para conferir o que é gravado.
     */
    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        aceites = mock(AceiteService.class);
        email = mock(EnvioDeEmail.class);
        relogio = new RelogioAjustavel(AGORA);
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));
        termos = DocumentoLegal.publicar(TipoDocumento.TERMOS_DE_USO, 1, "T", "t", AGORA, AGORA, null);
        privacidade = DocumentoLegal.publicar(TipoDocumento.POLITICA_DE_PRIVACIDADE, 1, "P", "p", AGORA, AGORA, null);
        confidencialidade = DocumentoLegal.publicar(TipoDocumento.TERMO_DE_CONFIDENCIALIDADE, 1, "C", "c", AGORA, AGORA, null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Montar o serviço
     * Objetivo: Criar o serviço com a confirmação de e-mail ligada ou desligada.
     * Decisões: Repositório de tokens em memória.
     */
    private CadastroService servico(boolean confirmarEmail) {
        DocumentoLegalService documentos = mock(DocumentoLegalService.class);
        when(documentos.vigente(TipoDocumento.TERMOS_DE_USO)).thenReturn(Optional.of(termos));
        when(documentos.vigente(TipoDocumento.POLITICA_DE_PRIVACIDADE)).thenReturn(Optional.of(privacidade));
        when(documentos.vigente(TipoDocumento.TERMO_DE_CONFIDENCIALIDADE)).thenReturn(Optional.of(confidencialidade));
        TokenDeContaRepository tokens = mock(TokenDeContaRepository.class);
        when(tokens.save(any())).thenAnswer(i -> {
            tokensGravados.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(tokens.findByUsuarioIdAndFinalidade(any(), any())).thenAnswer(i -> tokensGravados.stream()
                .filter(t -> t.getUsuarioId().equals(i.getArgument(0)) && t.getFinalidade() == i.getArgument(1)).toList());
        when(tokens.findByHash(anyString())).thenAnswer(i -> tokensGravados.stream()
                .filter(t -> hashDe(t).equals(i.getArgument(0))).findFirst());
        PasswordEncoder senhas = mock(PasswordEncoder.class);
        when(senhas.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
        var propriedades = new ContaProperties("https://web.exemplo", confirmarEmail, Duration.ofHours(24),
                Duration.ofMinutes(30), Duration.ofHours(24));
        return new CadastroService(usuarios, senhas, aceites, documentos, new TokenDeContaService(tokens, relogio),
                email, propriedades, relogio);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Hash gravado de um token
     * Objetivo: Ler o hash do token gravado para a busca simulada.
     * Decisões: Reflexão, porque a entidade não expõe o hash.
     */
    private static String hashDe(TokenDeConta token) {
        try {
            var campo = TokenDeConta.class.getDeclaredField("hash");
            campo.setAccessible(true);
            return (String) campo.get(token);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Pedido válido de apoio
     * Objetivo: Montar um cadastro válido com os papéis e aceites informados.
     * Decisões: Senha forte fixa.
     */
    private CadastroService.NovoCadastro pedido(Set<Papel> papeis, List<UUID> aceitesInformados) {
        return new CadastroService.NovoCadastro("Ana Souza", "Ana@Exemplo.com", "uma-senha-bem-longa", papeis,
                aceitesInformados);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Cadastro com os dois papéis
     * Objetivo: Garantir conta pendente, senha só em hash, aceites gravados, e-mail com link e onboardings.
     * Decisões: Investidor e empreendedor ao mesmo tempo, aceitando também a confidencialidade.
     */
    @Test
    void cadastraComOsDoisPapeis() {
        var resultado = servico(true).cadastrar(pedido(Set.of(Papel.INVESTIDOR, Papel.EMPREENDEDOR),
                List.of(termos.getId(), privacidade.getId(), confidencialidade.getId())));

        Usuario criado = resultado.usuario();
        assertThat(criado.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(criado.getSenhaHash()).isEqualTo("hash:uma-senha-bem-longa");
        assertThat(criado.getStatus()).isEqualTo(StatusUsuario.PENDENTE_CONFIRMACAO);
        assertThat(resultado.confirmacaoPendente()).isTrue();
        assertThat(resultado.proximosPassos())
                .containsExactlyInAnyOrder("/investidor/onboarding", "/empreendedor/onboarding");
        verify(aceites).aceitar(criado.getId(), List.of(termos.getId(), privacidade.getId(), confidencialidade.getId()));

        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email).enviar(mensagem.capture());
        assertThat(mensagem.getValue().para()).isEqualTo("ana@exemplo.com");
        assertThat(mensagem.getValue().texto()).contains("https://web.exemplo/confirmar-email?token=");
        assertThat(mensagem.getValue().toString()).doesNotContain("ana@exemplo.com");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Confirmação pelo link
     * Objetivo: Garantir que o link do e-mail ativa a conta e não pode ser usado duas vezes.
     * Decisões: Extrai o token do texto do e-mail enviado.
     */
    @Test
    void linkDoEmailAtivaAContaUmaVez() {
        CadastroService servico = servico(true);
        Usuario criado = servico.cadastrar(pedido(Set.of(Papel.EMPREENDEDOR), List.of(termos.getId(), privacidade.getId())))
                .usuario();
        when(usuarios.findById(criado.getId())).thenReturn(Optional.of(criado));
        String token = tokenDoEmail();

        servico.confirmarEmail(token);

        assertThat(criado.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThatThrownBy(() -> servico.confirmarEmail(token))
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("link_invalido");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Link expirado
     * Objetivo: Garantir que, passadas 24 horas, o link não confirma mais.
     * Decisões: Avança o relógio.
     */
    @Test
    void linkExpiradoNaoConfirma() {
        CadastroService servico = servico(true);
        servico.cadastrar(pedido(Set.of(Papel.EMPREENDEDOR), List.of(termos.getId(), privacidade.getId())));
        relogio.avancar(Duration.ofHours(24));
        assertThatThrownBy(() -> servico.confirmarEmail(tokenDoEmail())).isInstanceOf(RegraDeNegocioException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Sem confirmação de e-mail
     * Objetivo: Garantir que, com a confirmação desligada, a conta nasce ativa e nenhum e-mail sai.
     * Decisões: Configuração usada na homologação enquanto não há provedor de e-mail.
     */
    @Test
    void semConfirmacaoAContaNasceAtiva() {
        var resultado = servico(false).cadastrar(pedido(Set.of(Papel.EMPREENDEDOR),
                List.of(termos.getId(), privacidade.getId())));
        assertThat(resultado.usuario().getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(resultado.confirmacaoPendente()).isFalse();
        verify(email, never()).enviar(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Regras do cadastro
     * Objetivo: Garantir as recusas por papel, e-mail em uso, senha fraca e aceite faltando.
     * Decisões: Investidor sem aceitar a confidencialidade é recusado.
     */
    @Test
    void recusasDoCadastro() {
        CadastroService servico = servico(true);
        List<UUID> basicos = List.of(termos.getId(), privacidade.getId());

        assertThatThrownBy(() -> servico.cadastrar(pedido(Set.of(), basicos))).hasMessageContaining("Escolha como");
        assertThatThrownBy(() -> servico.cadastrar(pedido(Set.of(Papel.ADMIN), basicos))).hasMessageContaining("Escolha como");
        assertThatThrownBy(() -> servico.cadastrar(pedido(Set.of(Papel.INVESTIDOR), basicos)))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("aceite_obrigatorio");
        assertThatThrownBy(() -> servico.cadastrar(pedido(Set.of(Papel.EMPREENDEDOR), null)))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("aceite_obrigatorio");
        assertThatThrownBy(() -> servico.cadastrar(new CadastroService.NovoCadastro("Ana", "ana@exemplo.com",
                "1234567890", Set.of(Papel.EMPREENDEDOR), basicos)))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("senha_fraca");

        when(usuarios.existsByEmail("ana@exemplo.com")).thenReturn(true);
        assertThatThrownBy(() -> servico.cadastrar(pedido(Set.of(Papel.EMPREENDEDOR), basicos)))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("email_em_uso");
        verify(usuarios, never()).save(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Reenvio da confirmação
     * Objetivo: Garantir que só conta pendente recebe link novo e que o link antigo deixa de valer.
     * Decisões: E-mail inexistente e conta ativa não recebem nada.
     */
    @Test
    void reenvioSoParaContaPendente() {
        CadastroService servico = servico(true);
        Usuario criado = servico.cadastrar(pedido(Set.of(Papel.EMPREENDEDOR), List.of(termos.getId(), privacidade.getId())))
                .usuario();
        String primeiro = tokenDoEmail();
        when(usuarios.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(criado));
        when(usuarios.findById(criado.getId())).thenReturn(Optional.of(criado));

        servico.reenviarConfirmacao(" ANA@exemplo.com ");
        servico.reenviarConfirmacao("ninguem@exemplo.com");

        verify(email, org.mockito.Mockito.times(2)).enviar(any());
        assertThatThrownBy(() -> servico.confirmarEmail(primeiro)).isInstanceOf(RegraDeNegocioException.class);
        servico.confirmarEmail(tokenDoEmail());
        servico.reenviarConfirmacao("ana@exemplo.com");
        verify(email, org.mockito.Mockito.times(2)).enviar(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Token do último e-mail
     * Objetivo: Extrair o token do link do último e-mail enviado.
     * Decisões: O token é tudo depois de "token=" até o fim da linha.
     */
    private String tokenDoEmail() {
        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, org.mockito.Mockito.atLeastOnce()).enviar(mensagem.capture());
        String texto = mensagem.getValue().texto();
        int inicio = texto.indexOf("token=") + 6;
        return texto.substring(inicio, texto.indexOf('\n', inicio));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Representação segura do pedido
     * Objetivo: Garantir que a senha não aparece no toString do pedido.
     * Decisões: Mostra só os papéis.
     */
    @Test
    void pedidoNaoMostraASenha() {
        assertThat(pedido(Set.of(Papel.INVESTIDOR), List.of()).toString()).doesNotContain("uma-senha-bem-longa");
    }
}
