package br.com.orbithub.identidade.service;

import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.administracao.service.DocumentoLegalService;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.PerfilDePapel;
import br.com.orbithub.compartilhado.parametros.Chaves;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.identidade.domain.PoliticaDeSenha;
import br.com.orbithub.identidade.domain.StatusUsuario;
import br.com.orbithub.identidade.domain.TokenDeConta.Finalidade;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Cadastro de conta
 * Objetivo: Criar a conta com nome, e-mail, senha e papéis, registrar os aceites e enviar a confirmação
 *           do e-mail (P1).
 * Decisões: Os papéis viram perfis pela fábrica (Factory Method), que informam o onboarding e se o Termo de
 *           Confidencialidade é exigido. O cadastro só passa se a pessoa aceitou exatamente as versões
 *           vigentes exigidas. A conta nasce pendente e é ativada pelo link do e-mail; sem provedor de
 *           e-mail na nuvem, a confirmação pode ser desligada por parâmetro.
 */
@Service
public class CadastroService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final AceiteService aceites;
    private final DocumentoLegalService documentos;
    private final TokenDeContaService links;
    private final EnvioDeEmail email;
    private final ContaProperties propriedades;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Criação do serviço
     * Objetivo: Receber repositório, codificador de senhas, termos, links, e-mail, parâmetros e relógio.
     * Decisões: Injeção pelo construtor.
     */
    public CadastroService(UsuarioRepository usuarios, PasswordEncoder senhas, AceiteService aceites,
                           DocumentoLegalService documentos, TokenDeContaService links, EnvioDeEmail email,
                           ContaProperties propriedades, LeituraDeParametros parametros, Clock relogio) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.aceites = aceites;
        this.documentos = documentos;
        this.links = links;
        this.email = email;
        this.propriedades = propriedades;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Cadastrar conta
     * Objetivo: Validar os dados, criar a conta, registrar os aceites e enviar o link de confirmação.
     * Decisões: E-mail já cadastrado responde 422 "email_em_uso", como pede o critério de unicidade; o risco
     *           de descobrir contas por aqui fica com a limitação de tentativas do login (KAN-13).
     */
    @Transactional
    public ResultadoCadastro cadastrar(NovoCadastro pedido) {
        Set<Papel> papeis = validarPapeis(pedido.papeis());
        String emailNormalizado = Usuario.normalizarEmail(pedido.email());
        if (usuarios.existsByEmail(emailNormalizado)) {
            throw new RegraDeNegocioException("email_em_uso", "Já existe uma conta com este e-mail. Entre ou recupere a senha.");
        }
        PoliticaDeSenha.problema(pedido.senha(), emailNormalizado).ifPresent(motivo -> {
            throw new RegraDeNegocioException("senha_fraca", motivo);
        });
        List<PerfilDePapel> perfis = papeis.stream().map(PerfilDePapel::para).toList();
        List<UUID> exigidos = versoesExigidas(perfis.stream().anyMatch(PerfilDePapel::exigeTermoDeConfidencialidade));
        if (pedido.aceites() == null || !pedido.aceites().containsAll(exigidos)) {
            throw new RegraDeNegocioException("aceite_obrigatorio",
                    "Para criar a conta, leia e aceite os termos indicados.");
        }

        Instant agora = relogio.instant();
        Usuario usuario = Usuario.novo(pedido.nome(), emailNormalizado, senhas.encode(pedido.senha()), papeis, agora);
        if (!propriedades.confirmarEmail()) {
            usuario.confirmarEmail(agora);
        }
        usuarios.save(usuario);
        aceites.aceitar(usuario.getId(), exigidos);
        if (propriedades.confirmarEmail()) {
            enviarConfirmacao(usuario);
        }
        return new ResultadoCadastro(usuario, perfis.stream().map(PerfilDePapel::rotaOnboarding).toList(),
                propriedades.confirmarEmail());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Confirmar e-mail
     * Objetivo: Ativar a conta a partir do link do e-mail.
     * Decisões: Link inválido, usado ou expirado: 422 "link_invalido".
     */
    @Transactional
    public void confirmarEmail(String token) {
        var consumido = links.consumir(token, Finalidade.CONFIRMACAO_EMAIL);
        usuarios.findById(consumido.getUsuarioId()).ifPresent(u -> u.confirmarEmail(relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Reenviar confirmação
     * Objetivo: Mandar um link novo para quem ainda não confirmou o e-mail.
     * Decisões: Sempre "dá certo" para quem pediu: não revela se o e-mail existe nem se já foi confirmado.
     */
    @Transactional
    public void reenviarConfirmacao(String emailInformado) {
        Optional<Usuario> usuario = usuarios.findByEmail(Usuario.normalizarEmail(emailInformado));
        usuario.filter(u -> u.getStatus() == StatusUsuario.PENDENTE_CONFIRMACAO)
                .filter(u -> propriedades.confirmarEmail())
                .ifPresent(this::enviarConfirmacao);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Validar papéis
     * Objetivo: Garantir pelo menos um papel e só papéis de uso público.
     * Decisões: 422 "papel_invalido" para lista vazia ou papel interno.
     */
    private static Set<Papel> validarPapeis(Collection<Papel> pedidos) {
        if (pedidos == null || pedidos.isEmpty() || pedidos.stream().anyMatch(p -> p == null || !p.publico())) {
            throw new RegraDeNegocioException("papel_invalido", "Escolha como vai usar a plataforma: investidor, empreendedor ou os dois.");
        }
        return EnumSet.copyOf(pedidos);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Versões exigidas no cadastro
     * Objetivo: Listar os ids das versões vigentes que a pessoa precisa aceitar.
     * Decisões: Termos de Uso e Política de Privacidade; o Termo de Confidencialidade quando houver papel
     *           de investidor.
     */
    private List<UUID> versoesExigidas(boolean investidor) {
        return AceiteService.exigidos(investidor).stream()
                .map(documentos::vigente)
                .flatMap(Optional::stream)
                .map(DocumentoLegal::getId)
                .toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Enviar e-mail de confirmação
     * Objetivo: Gerar o link e mandar o e-mail de confirmação da conta.
     * Decisões: Link para a tela /confirmar-email da web, com validade de cadastro.link_confirmacao_horas.
     */
    private void enviarConfirmacao(Usuario usuario) {
        Duration validade = parametros.duracao(Chaves.CADASTRO_LINK_CONFIRMACAO);
        String token = links.gerar(usuario.getId(), Finalidade.CONFIRMACAO_EMAIL, validade, null);
        String link = propriedades.webUrl() + "/confirmar-email?token=" + token;
        email.enviar(new EnvioDeEmail.MensagemEmail(usuario.getEmail(), "Confirme seu e-mail no Orbit Hub",
                "Olá, " + usuario.getNome() + ".\n\nPara ativar sua conta no Orbit Hub, abra o link abaixo:\n\n"
                        + link + "\n\nO link vale por " + validade.toHours()
                        + " horas. Se você não criou esta conta, ignore este e-mail.",
                "confirmacao_email"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Pedido de cadastro
     * Objetivo: Dados informados pela pessoa no formulário.
     * Decisões: Aceites são os ids das versões que a pessoa marcou como lidas e aceitas.
     */
    public record NovoCadastro(String nome, String email, String senha, Set<Papel> papeis, List<UUID> aceites) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:00
         * Cartão Jira: KAN-12
         * Título: Representação textual segura
         * Objetivo: Evitar que a senha apareça em logs por acidente.
         * Decisões: Mostra só os papéis.
         */
        @Override
        public String toString() {
            return "NovoCadastro[papeis=" + papeis + "]";
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Resultado do cadastro
     * Objetivo: Conta criada, onboardings a seguir e se falta confirmar o e-mail.
     * Decisões: Os próximos passos vêm dos perfis (Factory Method).
     */
    public record ResultadoCadastro(Usuario usuario, List<String> proximosPassos, boolean confirmacaoPendente) {
    }
}
