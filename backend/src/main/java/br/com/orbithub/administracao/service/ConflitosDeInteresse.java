package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.VinculoDeMembro;
import br.com.orbithub.administracao.repository.VinculoDeMembroRepository;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.compartilhado.documentos.Cnpj;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Conflito de interesse
 * Objetivo: Guardar os vínculos declarados pela equipe e os impedimentos por caso, e dizer se um membro pode atuar num
 *           caso que envolve certas organizações, startups e pessoas.
 * Decisões: Serviço público para todas as filas (verificação agora; curadoria, moderação, disputa e recurso quando
 *           chegarem). Conflito = vínculo com o CNPJ ou com o e-mail de uma conta envolvida, ou impedimento declarado
 *           no caso. Declarar e remover vínculo, e declarar impedimento, vão para a trilha de auditoria.
 */
@Service
public class ConflitosDeInteresse {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final VinculoDeMembroRepository vinculos;
    private final ConsultaDeContas contas;
    private final JdbcTemplate jdbc;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Criação do serviço
     * Objetivo: Receber o repositório, as contas, o banco e o relógio.
     * Decisões: Injeção por construtor.
     */
    public ConflitosDeInteresse(VinculoDeMembroRepository vinculos, ConsultaDeContas contas, JdbcTemplate jdbc, Clock relogio) {
        this.vinculos = vinculos;
        this.contas = contas;
        this.jdbc = jdbc;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Meus vínculos
     * Objetivo: Listar os vínculos declarados por um membro.
     * Decisões: Mais novos primeiro.
     */
    @Transactional(readOnly = true)
    public List<VinculoDeMembro> doMembro(UUID membro) {
        return vinculos.findByMembroIdOrderByCriadoEmDesc(membro);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Vínculos da equipe
     * Objetivo: Listar os vínculos de toda a equipe, para a revisão periódica de acessos.
     * Decisões: Agrupados por membro.
     */
    @Transactional(readOnly = true)
    public List<VinculoDeMembro> daEquipe() {
        return vinculos.findAllByOrderByMembroIdAscCriadoEmDesc();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Declarar vínculo
     * Objetivo: Registrar uma organização ou startup (CNPJ) ou pessoa (e-mail) com a qual o membro tem interesse.
     * Decisões: CNPJ com dígitos conferidos; e-mail com formato válido; descrição obrigatória (até 200).
     */
    @Transactional
    public VinculoDeMembro declarar(UUID membro, VinculoDeMembro.Tipo tipo, String alvo, String descricao) {
        if (tipo == null) {
            throw new RegraDeNegocioException("vinculo_tipo", "Escolha o tipo do vínculo.");
        }
        String nota = descricao == null ? "" : descricao.strip();
        if (nota.isEmpty() || nota.length() > 200) {
            throw new RegraDeNegocioException("vinculo_descricao", "Descreva o vínculo (até 200 caracteres).");
        }
        String cnpj = null;
        String email = null;
        if (tipo == VinculoDeMembro.Tipo.PESSOA) {
            email = alvo == null ? "" : alvo.strip().toLowerCase(Locale.ROOT);
            if (!EMAIL.matcher(email).matches()) {
                throw new RegraDeNegocioException("vinculo_email", "Informe o e-mail da pessoa.");
            }
        } else {
            cnpj = Cnpj.validar(alvo).orElseThrow(() -> new RegraDeNegocioException("vinculo_cnpj", "CNPJ inválido."));
        }
        VinculoDeMembro vinculo = vinculos.save(VinculoDeMembro.novo(membro, tipo, cnpj, email, nota, relogio.instant()));
        ContextoDeAuditoria.descrever("VINCULO_DECLARADO", "vinculo_membro", vinculo.getId(), tipo + ": " + nota);
        return vinculo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Remover vínculo
     * Objetivo: Tirar um vínculo que deixou de existir.
     * Decisões: Só o próprio membro; fica na trilha de auditoria.
     */
    @Transactional
    public void remover(UUID membro, UUID vinculoId) {
        VinculoDeMembro vinculo = vinculos.findById(vinculoId).filter(v -> v.getMembroId().equals(membro))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo não encontrado."));
        vinculos.delete(vinculo);
        ContextoDeAuditoria.descrever("VINCULO_REMOVIDO", "vinculo_membro", vinculoId, vinculo.getTipo() + ": " + vinculo.getDescricao());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Declarar impedimento
     * Objetivo: Registrar que o membro não pode atuar num caso, com o motivo.
     * Decisões: Vale para o caso informado (tipo + identificador); repetir não duplica.
     */
    @Transactional
    public void declararImpedimento(String casoTipo, UUID casoId, UUID membro, String motivo) {
        String nota = motivo == null ? "" : motivo.strip();
        if (nota.isEmpty() || nota.length() > 500) {
            throw new RegraDeNegocioException("impedimento_motivo", "Diga o motivo do impedimento (até 500 caracteres).");
        }
        jdbc.update("""
                INSERT INTO impedimento_caso (id, caso_tipo, caso_id, membro_id, motivo, criado_em) VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (caso_tipo, caso_id, membro_id) DO NOTHING""",
                UUID.randomUUID(), casoTipo, casoId, membro, nota, Timestamp.from(relogio.instant()));
        ContextoDeAuditoria.descrever("IMPEDIMENTO_DECLARADO", casoTipo.toLowerCase(Locale.ROOT), casoId, nota);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Tem conflito
     * Objetivo: Dizer se o membro tem vínculo com algum envolvido no caso ou se declarou impedimento nele.
     * Decisões: Pessoa envolvida é comparada pelo e-mail da conta.
     */
    @Transactional(readOnly = true)
    public boolean temConflito(UUID membro, String casoTipo, UUID casoId, Envolvidos envolvidos) {
        Integer impedimentos = jdbc.queryForObject(
                "SELECT count(*) FROM impedimento_caso WHERE caso_tipo = ? AND caso_id = ? AND membro_id = ?",
                Integer.class, casoTipo, casoId, membro);
        if (impedimentos != null && impedimentos > 0) {
            return true;
        }
        List<VinculoDeMembro> declarados = vinculos.findByMembroIdOrderByCriadoEmDesc(membro);
        if (declarados.isEmpty()) {
            return false;
        }
        Set<String> emails = envolvidos.contas().stream().map(contas::emailParaAviso).flatMap(Optional::stream)
                .map(e -> e.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        return declarados.stream().anyMatch(v -> v.getCnpj() != null ? envolvidos.cnpjs().contains(v.getCnpj())
                : emails.contains(v.getEmail()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Exigir sem conflito
     * Objetivo: Recusar a atribuição ou a decisão de um caso por quem tem conflito.
     * Decisões: Mensagem explica para onde o caso vai.
     */
    public void exigirSemConflito(UUID membro, String casoTipo, UUID casoId, Envolvidos envolvidos) {
        if (temConflito(membro, casoTipo, casoId, envolvidos)) {
            throw new RegraDeNegocioException("conflito_de_interesse",
                    "Você tem vínculo declarado ou impedimento neste caso. Ele fica com outra pessoa do time ou com o administrador.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Envolvidos num caso
     * Objetivo: Levar os CNPJs (organizações, startups) e as contas (pessoas) envolvidos.
     * Decisões: Conjuntos vazios quando não houver.
     */
    public record Envolvidos(Set<String> cnpjs, Set<UUID> contas) {
    }
}
