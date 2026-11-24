package br.com.orbithub.identidade.service;

import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.PoliticaDeSenha;
import br.com.orbithub.identidade.domain.StatusUsuario;
import br.com.orbithub.identidade.domain.TokenDeConta;
import br.com.orbithub.identidade.domain.TokenDeConta.Finalidade;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.BloqueioDeLoginRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:30
 * Cartão Jira: KAN-14
 * Título: Recuperação de senha
 * Objetivo: Enviar o link de redefinição por e-mail e trocar a senha a partir dele.
 * Decisões: O pedido responde igual exista ou não a conta. O link vale 30 minutos e só uma vez, e só é gasto
 *           se a senha nova passar na política. Depois da troca, todas as sessões da conta caem (KAN-57) e o
 *           bloqueio de login do e-mail é zerado. No máximo um e-mail por minuto para a mesma conta.
 */
@Service
public class RecuperacaoSenhaService {

    static final Duration INTERVALO_MINIMO_ENTRE_PEDIDOS = Duration.ofMinutes(1);
    static final String MOTIVO_SENHA_REDEFINIDA = "senha_redefinida";
    private static final Logger LOG = LoggerFactory.getLogger(RecuperacaoSenhaService.class);

    private final UsuarioRepository usuarios;
    private final BloqueioDeLoginRepository bloqueios;
    private final TokenDeContaService links;
    private final SessaoService sessoes;
    private final PasswordEncoder senhas;
    private final EnvioDeEmail email;
    private final ContaProperties propriedades;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, links, sessões, codificador, e-mail, parâmetros e relógio.
     * Decisões: Injeção pelo construtor.
     */
    public RecuperacaoSenhaService(UsuarioRepository usuarios, BloqueioDeLoginRepository bloqueios,
                                   TokenDeContaService links, SessaoService sessoes, PasswordEncoder senhas,
                                   EnvioDeEmail email, ContaProperties propriedades, Clock relogio) {
        this.usuarios = usuarios;
        this.bloqueios = bloqueios;
        this.links = links;
        this.sessoes = sessoes;
        this.senhas = senhas;
        this.email = email;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Pedir redefinição
     * Objetivo: Enviar o link de redefinição para o e-mail da conta ativa.
     * Decisões: Sem conta, conta não ativa ou pedido repetido em menos de um minuto: nada é enviado, e quem
     *           pediu recebe a mesma resposta.
     */
    @Transactional
    public void solicitar(String emailInformado) {
        usuarios.findByEmail(Usuario.normalizarEmail(emailInformado))
                .filter(u -> u.getStatus() == StatusUsuario.ATIVO)
                .filter(u -> !links.emitidoHaMenosDe(u.getId(), Finalidade.REDEFINICAO_SENHA, INTERVALO_MINIMO_ENTRE_PEDIDOS))
                .ifPresent(this::enviarLink);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Redefinir senha
     * Objetivo: Trocar a senha pela do formulário, gastar o link e encerrar todas as sessões.
     * Decisões: Senha fraca: 422 "senha_fraca" e o link continua valendo para nova tentativa.
     */
    @Transactional
    public void redefinir(String token, String novaSenha) {
        TokenDeConta link = links.buscarValido(token, Finalidade.REDEFINICAO_SENHA);
        Usuario usuario = usuarios.findById(link.getUsuarioId())
                .orElseThrow(() -> new RegraDeNegocioException("link_invalido", "Este link é inválido ou expirou. Peça um novo."));
        PoliticaDeSenha.problema(novaSenha, usuario.getEmail()).ifPresent(motivo -> {
            throw new RegraDeNegocioException("senha_fraca", motivo);
        });
        var agora = relogio.instant();
        usuario.trocarSenha(senhas.encode(novaSenha), agora);
        link.marcarUsado(agora);
        int sessoesEncerradas = sessoes.encerrarTodas(usuario.getId(), MOTIVO_SENHA_REDEFINIDA);
        bloqueios.deleteById(SessaoService.hash(usuario.getEmail()));
        LOG.info("Senha redefinida: {} sessões encerradas (usuario={})", sessoesEncerradas, usuario.getId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Enviar o link
     * Objetivo: Gerar o token e mandar o e-mail com o link de redefinição.
     * Decisões: O texto avisa quem não pediu que pode ignorar (a senha não muda sem o link).
     */
    private void enviarLink(Usuario usuario) {
        String token = links.gerar(usuario.getId(), Finalidade.REDEFINICAO_SENHA, propriedades.linkRedefinicao(), null);
        String link = propriedades.webUrl() + "/redefinir-senha?token=" + token;
        email.enviar(new EnvioDeEmail.MensagemEmail(usuario.getEmail(), "Redefinição de senha do Orbit Hub",
                "Olá, " + usuario.getNome() + ".\n\nRecebemos um pedido para redefinir a senha da sua conta. "
                        + "Para criar uma senha nova, abra o link abaixo:\n\n" + link + "\n\nO link vale por "
                        + propriedades.linkRedefinicao().toMinutes() + " minutos e só pode ser usado uma vez. "
                        + "Se você não pediu, ignore este e-mail: sua senha continua a mesma.",
                "redefinicao_senha"));
    }
}
