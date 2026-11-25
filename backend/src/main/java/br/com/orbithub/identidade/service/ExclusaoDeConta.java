package br.com.orbithub.identidade.service;

import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.ContaSuspensa;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-17, KAN-90
 * Título: Exclusão da conta
 * Objetivo: Excluir a conta a pedido do titular (com senha e confirmação) ou por decisão aprovada do encarregado:
 *           encerrar sessões e efeitos (Nível 2, interesses, rodadas, reuniões), apagar marcações e anotações, sair das
 *           startups e organizações, anonimizar a conta e avisar por e-mail.
 * Decisões: A linha da conta fica, anonimizada, para não quebrar conexões, decisões e trilha de auditoria (guarda
 *           legal restrita, lgpd.retencao_legal_meses). Quem é o único administrador de uma startup ou organização com
 *           outros membros precisa transferir a administração antes. Startup sem mais ninguém sai do catálogo.
 */
@Service
public class ExclusaoDeConta {

    static final String CONFIRMACAO = "EXCLUIR";

    private final UsuarioRepository usuarios;
    private final SessaoService sessoes;
    private final PasswordEncoder senhas;
    private final JdbcTemplate jdbc;
    private final EnvioDeEmail email;
    private final ArmazenamentoArquivos arquivos;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public ExclusaoDeConta(UsuarioRepository usuarios, SessaoService sessoes, PasswordEncoder senhas, JdbcTemplate jdbc, EnvioDeEmail email,
                           ArmazenamentoArquivos arquivos, ApplicationEventPublisher eventos, Clock relogio) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.senhas = senhas;
        this.jdbc = jdbc;
        this.email = email;
        this.arquivos = arquivos;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17
     * Título: Excluir a pedido do titular
     * Objetivo: Conferir a senha e a confirmação digitada e excluir a conta.
     * Decisões: Confirmação = digitar EXCLUIR. O pedido fica registrado como concluído (KAN-90).
     */
    @Transactional
    public void excluirAPedido(UUID eu, String senha, String confirmacao) {
        Usuario u = conta(eu);
        if (senha == null || !senhas.matches(senha, u.getSenhaHash())) {
            throw new RegraDeNegocioException("senha_incorreta", "Senha incorreta.");
        }
        if (!CONFIRMACAO.equals(confirmacao)) {
            throw new RegraDeNegocioException("confirmacao_invalida", "Digite EXCLUIR para confirmar.");
        }
        Instant agora = relogio.instant();
        jdbc.update("INSERT INTO pedido_titular (id, usuario_id, tipo, situacao, prazo, resposta, respondido_em, criado_em) "
                + "VALUES (?, ?, 'ELIMINACAO', 'CONCLUIDO', ?, 'Conta excluída pelo próprio titular.', ?, ?)", UUID.randomUUID(), eu,
                Timestamp.from(agora), Timestamp.from(agora), Timestamp.from(agora));
        excluir(u);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90, KAN-81
     * Título: Excluir por decisão aprovada
     * Objetivo: Excluir a conta pela decisão do encarregado aprovada por outra pessoa (fora do fluxo normal).
     * Decisões: Mesmas regras do pedido do titular.
     */
    @Transactional
    public void excluirPorDecisao(UUID usuarioId) {
        excluir(conta(usuarioId));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17
     * Título: Pendências para excluir
     * Objetivo: Listar as startups e organizações em que a pessoa é a única administradora e há outros membros.
     * Decisões: A tela mostra antes de pedir a senha.
     */
    @Transactional(readOnly = true)
    public List<String> pendencias(UUID eu) {
        List<String> startups = jdbc.queryForList("SELECT s.nome FROM startup_membro m JOIN startup s ON s.id = m.startup_id WHERE m.usuario_id = ? "
                + "AND m.saiu_em IS NULL AND m.papel = 'FUNDADOR_ADMINISTRADOR' AND NOT EXISTS (SELECT 1 FROM startup_membro o WHERE o.startup_id = m.startup_id "
                + "AND o.saiu_em IS NULL AND o.papel = 'FUNDADOR_ADMINISTRADOR' AND o.usuario_id <> m.usuario_id) AND EXISTS (SELECT 1 FROM startup_membro o "
                + "WHERE o.startup_id = m.startup_id AND o.saiu_em IS NULL AND o.usuario_id <> m.usuario_id)", String.class, eu);
        List<String> organizacoes = jdbc.queryForList("SELECT g.nome FROM organizacao_membro m JOIN organizacao g ON g.id = m.organizacao_id WHERE "
                + "m.usuario_id = ? AND m.saiu_em IS NULL AND m.papel = 'ADMINISTRADOR' AND NOT EXISTS (SELECT 1 FROM organizacao_membro o WHERE "
                + "o.organizacao_id = m.organizacao_id AND o.saiu_em IS NULL AND o.papel = 'ADMINISTRADOR' AND o.usuario_id <> m.usuario_id) AND EXISTS "
                + "(SELECT 1 FROM organizacao_membro o WHERE o.organizacao_id = m.organizacao_id AND o.saiu_em IS NULL AND o.usuario_id <> m.usuario_id)",
                String.class, eu);
        return java.util.stream.Stream.concat(startups.stream(), organizacoes.stream()).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: excluir
     * Objetivo: Rotina auxiliar excluir.
     * Decisões: Ver a documentação da classe.
     */
    private void excluir(Usuario u) {
        List<String> pendentes = pendencias(u.getId());
        if (!pendentes.isEmpty()) {
            throw new RegraDeNegocioException("administracao_pendente", "Antes de excluir a conta, passe a administração de "
                    + String.join(", ", pendentes) + " para outra pessoa (ou peça a transferência ao atendimento).");
        }
        Instant agora = relogio.instant();
        Timestamp t = Timestamp.from(agora);
        String destino = u.getEmail();
        String nome = u.getNome();
        eventos.publishEvent(new ContaSuspensa(u.getId(), false));
        sessoes.encerrarTodas(u.getId(), "EXCLUSAO");
        jdbc.update("UPDATE startup SET situacao = 'DESPUBLICADA', versao = versao + 1 WHERE situacao = 'PUBLICADA' AND id IN (SELECT m.startup_id "
                + "FROM startup_membro m WHERE m.usuario_id = ? AND m.saiu_em IS NULL AND NOT EXISTS (SELECT 1 FROM startup_membro o WHERE "
                + "o.startup_id = m.startup_id AND o.saiu_em IS NULL AND o.usuario_id <> m.usuario_id))", u.getId());
        jdbc.update("UPDATE startup_membro SET saiu_em = ? WHERE usuario_id = ? AND saiu_em IS NULL", t, u.getId());
        jdbc.update("UPDATE organizacao_membro SET saiu_em = ?, motivo_saida = 'SAIU' WHERE usuario_id = ? AND saiu_em IS NULL", t, u.getId());
        for (String tabela : List.of("catalogo_favorito", "catalogo_visualizacao", "catalogo_descarte", "reuniao_anotacao")) {
            jdbc.update("DELETE FROM " + tabela + " WHERE usuario_id = ?", u.getId());
        }
        String foto = u.anonimizar(agora);
        usuarios.saveAndFlush(u);
        jdbc.update("UPDATE usuario SET excluida_em = ? WHERE id = ?", t, u.getId());
        if (foto != null) {
            arquivos.remover(foto);
        }
        email.enviar(new EnvioDeEmail.MensagemEmail(destino, "Sua conta no Orbit Hub foi excluída", "Olá, " + nome
                + ".\n\nA sua conta foi excluída e os seus dados pessoais foram removidos ou anonimizados. Registros que a lei obriga a guardar "
                + "ficam restritos pelo prazo legal e depois são apagados.\n\nSe você não pediu a exclusão, responda pela central de ajuda.", "conta_excluida"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: conta
     * Objetivo: Rotina auxiliar conta.
     * Decisões: Ver a documentação da classe.
     */
    private Usuario conta(UUID id) {
        Usuario u = usuarios.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
        if (u.isInterna()) {
            throw new RegraDeNegocioException("conta_da_equipe", "Contas da equipe são desligadas pelo administrador, não excluídas.");
        }
        return u;
    }
}
