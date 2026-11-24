package br.com.orbithub.identidade.service;

import br.com.orbithub.compartilhado.parametros.Chaves;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.EventoDaConta;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.PerfilDePapel;
import br.com.orbithub.identidade.domain.PoliticaDeSenha;
import br.com.orbithub.identidade.domain.TokenDeConta;
import br.com.orbithub.identidade.domain.TokenDeConta.Finalidade;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.EventoDaContaRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Perfil e papéis da conta
 * Objetivo: Editar nome, foto e contato; trocar e-mail (com confirmação pelo novo endereço) e senha (com a
 *           atual); ativar e desativar papéis, registrando tudo no histórico da conta.
 * Decisões: Ativar investidor exige o Termo de Confidencialidade vigente aceito. Desativar um papel consulta
 *           as pendências de todos os módulos (Strategy) e exige que a conta continue com um papel. A troca
 *           de e-mail pede a senha atual e avisa o endereço antigo, para a conta não ser tomada por quem só
 *           tem a sessão aberta.
 */
@Service
public class PerfilService {

    static final long TAMANHO_MAXIMO_FOTO = 2L * 1024 * 1024;

    private final UsuarioRepository usuarios;
    private final EventoDaContaRepository historico;
    private final PasswordEncoder senhas;
    private final SessaoService sessoes;
    private final TokenDeContaService links;
    private final AceiteService aceites;
    private final ArmazenamentoArquivos arquivos;
    private final EnvioDeEmail email;
    private final List<PendenciasDePapel> pendencias;
    private final ContaProperties propriedades;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, serviços, armazenamento, e-mail, verificadores de pendência e relógio.
     * Decisões: A lista de verificadores pode estar vazia (nenhum módulo com pendências ainda).
     */
    public PerfilService(UsuarioRepository usuarios, EventoDaContaRepository historico, PasswordEncoder senhas,
                         SessaoService sessoes, TokenDeContaService links, AceiteService aceites,
                         ArmazenamentoArquivos arquivos, EnvioDeEmail email, List<PendenciasDePapel> pendencias,
                         ContaProperties propriedades, LeituraDeParametros parametros, Clock relogio) {
        this.usuarios = usuarios;
        this.historico = historico;
        this.senhas = senhas;
        this.sessoes = sessoes;
        this.links = links;
        this.aceites = aceites;
        this.arquivos = arquivos;
        this.email = email;
        this.pendencias = pendencias;
        this.propriedades = propriedades;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Consultar a conta
     * Objetivo: Ler a conta do usuário logado.
     * Decisões: Conta inexistente (removida durante a sessão) vira 404.
     */
    @Transactional(readOnly = true)
    public Usuario conta(UUID usuarioId) {
        return usuarios.findById(usuarioId).orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Atualizar perfil
     * Objetivo: Trocar nome e telefone.
     * Decisões: Formato validado na API.
     */
    @Transactional
    public Usuario atualizar(UUID usuarioId, String nome, String telefone) {
        Usuario usuario = conta(usuarioId);
        usuario.atualizarPerfil(nome, telefone, relogio.instant());
        return usuario;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Trocar foto
     * Objetivo: Validar e gravar a foto nova e apagar a anterior.
     * Decisões: Até 2 MB, só JPEG, PNG ou WebP, conferidos pelos primeiros bytes do arquivo (não pela extensão
     *           nem pelo tipo informado pelo navegador). A chave é aleatória, para a foto antiga não ficar em
     *           cache com o mesmo endereço.
     */
    @Transactional
    public Usuario trocarFoto(UUID usuarioId, byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0 || conteudo.length > TAMANHO_MAXIMO_FOTO) {
            throw new RegraDeNegocioException("foto_invalida", "Envie uma imagem de até 2 MB.");
        }
        String tipo = FormatoDeImagem.detectar(conteudo)
                .orElseThrow(() -> new RegraDeNegocioException("foto_invalida", "Use uma imagem JPEG, PNG ou WebP."));
        Usuario usuario = conta(usuarioId);
        String chave = "contas/" + usuarioId + "/foto-" + UUID.randomUUID() + "." + FormatoDeImagem.extensao(tipo);
        arquivos.gravar(chave, new ByteArrayInputStream(conteudo), conteudo.length, tipo);
        String anterior = usuario.definirFoto(chave, relogio.instant());
        if (anterior != null) {
            arquivos.remover(anterior);
        }
        return usuario;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Ler foto
     * Objetivo: Abrir o conteúdo e o tipo da foto da conta.
     * Decisões: Sem foto: 404.
     */
    @Transactional(readOnly = true)
    public FotoLida lerFoto(UUID usuarioId) {
        String chave = Optional.ofNullable(conta(usuarioId).getFotoChave())
                .orElseThrow(() -> new RecursoNaoEncontradoException("A conta não tem foto."));
        String extensao = chave.substring(chave.lastIndexOf('.') + 1);
        return new FotoLida(arquivos.ler(chave), FormatoDeImagem.tipoDaExtensao(extensao));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Pedir troca de e-mail
     * Objetivo: Conferir a senha atual e enviar o link de confirmação ao novo endereço.
     * Decisões: O e-mail só muda depois da confirmação; o endereço antigo é avisado do pedido.
     */
    @Transactional
    public void solicitarTrocaDeEmail(UUID usuarioId, String novoEmail, String senhaAtual) {
        Usuario usuario = conta(usuarioId);
        conferirSenhaAtual(usuario, senhaAtual);
        String normalizado = Usuario.normalizarEmail(novoEmail);
        if (normalizado.equals(usuario.getEmail())) {
            throw new RegraDeNegocioException("email_igual", "Esse já é o seu e-mail.");
        }
        if (usuarios.existsByEmail(normalizado)) {
            throw new RegraDeNegocioException("email_em_uso", "Já existe uma conta com este e-mail.");
        }
        String token = links.gerar(usuarioId, Finalidade.TROCA_EMAIL,
                parametros.duracao(Chaves.CONTA_LINK_TROCA_EMAIL), normalizado);
        email.enviar(new EnvioDeEmail.MensagemEmail(normalizado, "Confirme seu novo e-mail no Orbit Hub",
                "Olá, " + usuario.getNome() + ".\n\nPara passar a usar este endereço na sua conta do Orbit Hub, abra o "
                        + "link abaixo:\n\n" + propriedades.webUrl() + "/confirmar-novo-email?token=" + token
                        + "\n\nSe você não pediu a troca, ignore este e-mail.", "troca_email"));
        email.enviar(new EnvioDeEmail.MensagemEmail(usuario.getEmail(), "Pedido de troca de e-mail no Orbit Hub",
                "Olá, " + usuario.getNome() + ".\n\nRecebemos um pedido para trocar o e-mail da sua conta. A troca só "
                        + "acontece depois da confirmação pelo novo endereço. Se não foi você, troque sua senha.",
                "aviso_troca_email"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Confirmar troca de e-mail
     * Objetivo: Aplicar o novo e-mail a partir do link enviado a ele.
     * Decisões: Confere de novo a unicidade (alguém pode ter se cadastrado com o endereço no meio-tempo).
     */
    @Transactional
    public void confirmarTrocaDeEmail(String token) {
        TokenDeConta link = links.consumir(token, Finalidade.TROCA_EMAIL);
        Usuario usuario = conta(link.getUsuarioId());
        if (usuarios.existsByEmail(link.getDado())) {
            throw new RegraDeNegocioException("email_em_uso", "Já existe uma conta com este e-mail.");
        }
        Instant agora = relogio.instant();
        usuario.trocarEmail(link.getDado(), agora);
        historico.save(EventoDaConta.registrar(usuario.getId(), EventoDaConta.Tipo.EMAIL_ALTERADO, null, agora));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Trocar senha
     * Objetivo: Trocar a senha com a senha atual e desconectar os outros aparelhos.
     * Decisões: A sessão em uso continua; as demais caem.
     */
    @Transactional
    public void trocarSenha(UUID usuarioId, UUID sessaoAtual, String senhaAtual, String novaSenha) {
        Usuario usuario = conta(usuarioId);
        conferirSenhaAtual(usuario, senhaAtual);
        PoliticaDeSenha.problema(novaSenha, usuario.getEmail()).ifPresent(motivo -> {
            throw new RegraDeNegocioException("senha_fraca", motivo);
        });
        Instant agora = relogio.instant();
        usuario.trocarSenha(senhas.encode(novaSenha), agora);
        sessoes.encerrarOutras(usuarioId, sessaoAtual, "senha_alterada");
        historico.save(EventoDaConta.registrar(usuarioId, EventoDaConta.Tipo.SENHA_ALTERADA, null, agora));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Ativar papel
     * Objetivo: Acrescentar investidor ou empreendedor à conta e devolver o onboarding do papel.
     * Decisões: Para investidor, registra o aceite do Termo de Confidencialidade informado (ou exige que já
     *           exista). O token da sessão só passa a ter o papel novo depois da renovação, feita pela tela.
     *           Conta da equipe é recusada com mensagem clara: quem é da equipe e investe usa outra conta.
     */
    @Transactional
    public String ativarPapel(UUID usuarioId, Papel papel, List<UUID> aceitesInformados) {
        PerfilDePapel perfil = perfil(papel);
        Usuario usuario = conta(usuarioId);
        if (usuario.isInterna()) {
            throw new RegraDeNegocioException("conta_interna", "Contas da equipe não atuam como investidor ou "
                    + "empreendedor. Use uma conta pessoal separada.");
        }
        if (perfil.exigeTermoDeConfidencialidade()) {
            if (aceitesInformados != null && !aceitesInformados.isEmpty()) {
                aceites.aceitar(usuarioId, aceitesInformados);
            }
            if (!aceites.aceitouVigente(usuarioId, TipoDocumento.TERMO_DE_CONFIDENCIALIDADE)) {
                throw new RegraDeNegocioException("aceite_obrigatorio",
                        "Para atuar como investidor, aceite o Termo de Confidencialidade.");
            }
        }
        Instant agora = relogio.instant();
        if (usuario.ativarPapel(papel, agora)) {
            historico.save(EventoDaConta.registrar(usuarioId, EventoDaConta.Tipo.PAPEL_ATIVADO, papel.name(), agora));
        }
        return perfil.rotaOnboarding();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Desativar papel
     * Objetivo: Retirar um papel da conta quando não há pendências nele.
     * Decisões: A primeira pendência encontrada é mostrada ao usuário; sem outro papel, a desativação é
     *           recusada (para encerrar a conta existe a exclusão, KAN-17).
     */
    @Transactional
    public void desativarPapel(UUID usuarioId, Papel papel) {
        perfil(papel);
        Usuario usuario = conta(usuarioId);
        if (!usuario.getPapeis().contains(papel)) {
            return;
        }
        if (usuario.getPapeis().stream().filter(Papel::publico).count() <= 1) {
            throw new RegraDeNegocioException("ultimo_papel",
                    "Sua conta precisa de pelo menos um papel. Para encerrar a conta, use a exclusão de conta.");
        }
        pendencias.stream().map(p -> p.pendencia(usuarioId, papel)).flatMap(Optional::stream).findFirst()
                .ifPresent(motivo -> {
                    throw new RegraDeNegocioException("papel_com_pendencias",
                            "Não é possível desativar este papel agora: " + motivo + ".");
                });
        Instant agora = relogio.instant();
        usuario.desativarPapel(papel, agora);
        historico.save(EventoDaConta.registrar(usuarioId, EventoDaConta.Tipo.PAPEL_DESATIVADO, papel.name(), agora));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Histórico da conta
     * Objetivo: Listar as mudanças da conta do mais recente ao mais antigo.
     * Decisões: Inclui papéis, e-mail e senha.
     */
    @Transactional(readOnly = true)
    public List<EventoDaConta> historico(UUID usuarioId) {
        return historico.findByUsuarioIdOrderByOcorridoEmDesc(usuarioId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Perfil de papel público
     * Objetivo: Obter o perfil do papel ou recusar papel interno.
     * Decisões: ADMIN não é ativado nem desativado pelo próprio usuário.
     */
    private static PerfilDePapel perfil(Papel papel) {
        if (papel == null || !papel.publico()) {
            throw new RegraDeNegocioException("papel_invalido", "Esse papel não pode ser alterado pelo perfil.");
        }
        return PerfilDePapel.para(papel);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Conferir senha atual
     * Objetivo: Exigir a senha atual antes de mudanças sensíveis.
     * Decisões: 422 "senha_atual_incorreta".
     */
    private void conferirSenhaAtual(Usuario usuario, String senhaAtual) {
        if (senhaAtual == null || !senhas.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("senha_atual_incorreta", "A senha atual está incorreta.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Foto lida
     * Objetivo: Conteúdo e tipo da foto para a resposta HTTP.
     * Decisões: Quem chama fecha o fluxo.
     */
    public record FotoLida(InputStream conteudo, String tipo) {
    }
}
